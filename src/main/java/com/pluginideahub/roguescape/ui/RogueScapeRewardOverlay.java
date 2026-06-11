package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The "ROLL SUPPLIES" reward window — a fully custom in-game overlay that renders the pending
 * {@link com.pluginideahub.roguescape.core.reward.RewardDraft} as a row of rarity-tiered reward
 * cards (icon, name, description, category tag) with Reroll / Confirm Choice / Skip Reward
 * buttons, matching the project's north-star mockup.
 *
 * <p>Auto-shown: the {@code supplier} returns a {@link RewardView} only while a reward choice is
 * pending, so the window appears at the reward step and hides once resolved. Card icons come from
 * the shared {@link RogueScapeIcons} foundation. Selection/confirm/skip are handled via a
 * registered {@link MouseListener}; text is ASCII-sanitised via {@link RogueScapeWindowOverlay}.
 */
public class RogueScapeRewardOverlay extends Overlay implements MouseListener
{
	private static final int WIDTH = 760;
	private static final int HEIGHT = 458;
	private static final int HEADER_H = 48;
	private static final int FOOTER_H = 58;
	private static final int ARTIFACT_H = 60;
	private static final int PAD = 16;
	private static final int CARD_GAP = 12;
	private static final int RAIL_W = 150;
	private static final int RAIL_GAP = 12;
	private static final int LINE_H = 15;

	/** Display rarity for a reward card; maps to a frame/label colour. */
	public enum Rarity
	{
		COMMON(RogueScapeTheme.RARITY_COMMON),
		RARE(RogueScapeTheme.RARITY_RARE),
		EPIC(RogueScapeTheme.RARITY_EPIC),
		LEGENDARY(RogueScapeTheme.RARITY_LEGENDARY);

		private final Color color;

		Rarity(Color color)
		{
			this.color = color;
		}

		public Color color()
		{
			return color;
		}
	}

	/** One reward card. {@code iconItemId} 0 falls back to a drawn rarity emblem. */
	public static final class Card
	{
		private final String title;
		private final String category;
		private final Rarity rarity;
		private final int iconItemId;
		private final List<String> lines;

		public Card(String title, String category, Rarity rarity, int iconItemId, List<String> lines)
		{
			this.title = title == null ? "" : title;
			this.category = category == null ? "" : category;
			this.rarity = rarity == null ? Rarity.COMMON : rarity;
			this.iconItemId = iconItemId;
			this.lines = lines == null ? new ArrayList<>() : lines;
		}

		public String title()
		{
			return title;
		}

		public Rarity rarity()
		{
			return rarity;
		}

		public int iconItemId()
		{
			return iconItemId;
		}

		public List<String> lines()
		{
			return lines;
		}
	}

	/** The whole window's content for one render: header text plus the card row. */
	public static final class RewardView
	{
		private final String viewId;
		private final String title;
		private final String subtitle;
		private final List<Card> cards;
		private final List<String> railRows;
		private final List<Integer> artifactItemIds;

		public RewardView(String viewId, String title, String subtitle, List<Card> cards)
		{
			this(viewId, title, subtitle, cards, null);
		}

		public RewardView(String viewId, String title, String subtitle, List<Card> cards, List<String> railRows)
		{
			this(viewId, title, subtitle, cards, railRows, null);
		}

		public RewardView(String viewId, String title, String subtitle, List<Card> cards, List<String> railRows,
			List<Integer> artifactItemIds)
		{
			this.viewId = viewId == null ? "" : viewId;
			this.title = title == null ? "ROLL SUPPLIES" : title;
			this.subtitle = subtitle == null ? "" : subtitle;
			this.cards = cards == null ? new ArrayList<>() : cards;
			this.railRows = railRows == null ? new ArrayList<>() : railRows;
			this.artifactItemIds = artifactItemIds == null ? new ArrayList<>() : artifactItemIds;
		}
	}

	private final Supplier<RewardView> supplier;
	private final RogueScapeIcons icons;
	private final IntConsumer onChoose;
	private final Runnable onSkip;
	private final Supplier<Dimension> canvasSize;

	private int selected;
	private String lastViewId = "";
	private boolean wasVisible;

	// Hover + hit state (window-relative), recomputed each render.
	private int hoverCard = -1;
	private boolean hoverConfirm;
	private boolean hoverSkip;
	private final List<Rectangle> cardRects = new ArrayList<>();
	private final Rectangle confirmRect = new Rectangle();
	private final Rectangle skipRect = new Rectangle();
	private final Rectangle rerollRect = new Rectangle();

	public RogueScapeRewardOverlay(Supplier<RewardView> supplier, RogueScapeIcons icons,
		IntConsumer onChoose, Runnable onSkip)
	{
		this(supplier, icons, onChoose, onSkip, null);
	}

	public RogueScapeRewardOverlay(Supplier<RewardView> supplier, RogueScapeIcons icons,
		IntConsumer onChoose, Runnable onSkip, Supplier<Dimension> canvasSize)
	{
		this.supplier = supplier;
		this.icons = icons;
		this.onChoose = onChoose;
		this.onSkip = onSkip;
		this.canvasSize = canvasSize;
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(false);
		setResizable(false);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		RewardView view = supplier == null ? null : supplier.get();
		if (view == null || view.cards.isEmpty())
		{
			wasVisible = false;
			return null;
		}
		if (!view.viewId.equals(lastViewId))
		{
			lastViewId = view.viewId;
			selected = 0;
		}
		if (selected >= view.cards.size())
		{
			selected = 0;
		}
		centerOnAppear();

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		drawFrame(g);
		drawHeader(g, view);
		drawCards(g, view);
		drawRail(g, view);
		drawArtifacts(g, view);
		drawFooter(g, view);

		return new Dimension(WIDTH, HEIGHT);
	}

	/** On the first frame the window appears, snap it to the centre of the game canvas. */
	private void centerOnAppear()
	{
		if (!wasVisible && canvasSize != null)
		{
			Dimension cs = canvasSize.get();
			if (cs != null && cs.width > 0 && cs.height > 0)
			{
				setPreferredLocation(new java.awt.Point(
					Math.max(0, (cs.width - WIDTH) / 2),
					Math.max(0, (cs.height - HEIGHT) / 2)));
			}
		}
		wasVisible = true;
	}

	private void drawFrame(Graphics2D g)
	{
		RogueScapeFrame.background(g, 0, 0, WIDTH, HEIGHT);
		RogueScapeFrame.frame(g, 0, 0, WIDTH, HEIGHT);
	}

	private void drawHeader(Graphics2D g, RewardView view)
	{
		int x0 = 3, y0 = 3, w = WIDTH - 6;
		RogueScapeFrame.headerBar(g, x0, y0, w, HEADER_H);

		// Title, centred, flanked by crest diamonds.
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		String title = RogueScapeWindowOverlay.ascii(view.title);
		int tw = fm.stringWidth(title);
		int cx = WIDTH / 2;
		int ty = y0 + 22;
		g.setColor(RogueScapeFrame.SHADOW);
		g.drawString(title, cx - tw / 2 + 1, ty + 1);
		g.setColor(RogueScapeTheme.GOLD);
		g.drawString(title, cx - tw / 2, ty);
		drawDiamond(g, cx - tw / 2 - 16, ty - 5, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
		drawDiamond(g, cx + tw / 2 + 16, ty - 5, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);

		// Subtitle.
		if (!view.subtitle.isEmpty())
		{
			g.setFont(FontManager.getRunescapeFont());
			FontMetrics sfm = g.getFontMetrics();
			String sub = RogueScapeWindowOverlay.ascii(view.subtitle);
			int sw = sfm.stringWidth(sub);
			g.setColor(RogueScapeTheme.TEXT_MUTED);
			g.drawString(sub, cx - sw / 2, y0 + HEADER_H - 8);
		}
	}

	private void drawCards(Graphics2D g, RewardView view)
	{
		cardRects.clear();
		List<Card> cards = view.cards;
		int n = cards.size();
		int areaX = PAD;
		int areaY = 3 + HEADER_H + PAD;
		int railSpace = view.railRows.isEmpty() ? 0 : RAIL_W + RAIL_GAP;
		int areaW = WIDTH - PAD * 2 - railSpace;
		int areaH = HEIGHT - FOOTER_H - ARTIFACT_H - areaY - PAD;
		int cardW = (areaW - CARD_GAP * (n - 1)) / n;

		for (int i = 0; i < n; i++)
		{
			Card card = cards.get(i);
			int x = areaX + i * (cardW + CARD_GAP);
			Rectangle rect = new Rectangle(x, areaY, cardW, areaH);
			cardRects.add(rect);
			drawCard(g, icons, card, rect, i == selected, i == hoverCard);
		}
	}

	private void drawRail(Graphics2D g, RewardView view)
	{
		if (view.railRows.isEmpty())
		{
			return;
		}
		int x = WIDTH - PAD - RAIL_W;
		int y = 3 + HEADER_H + PAD;
		int h = HEIGHT - FOOTER_H - ARTIFACT_H - y - PAD;
		g.setPaint(new GradientPaint(0, y, RogueScapeTheme.SECTION_HEADER_BG,
			0, y + h, darken(RogueScapeTheme.PANEL_BG, 7)));
		g.fillRoundRect(x, y, RAIL_W, h, 10, 10);
		g.setColor(RogueScapeTheme.BORDER);
		g.drawRoundRect(x, y, RAIL_W - 1, h - 1, 10, 10);

		g.setFont(FontManager.getRunescapeBoldFont());
		g.setColor(RogueScapeTheme.GOLD);
		g.drawString("RUN STATUS", x + 12, y + 18);
		g.setColor(new Color(0, 0, 0, 90));
		g.drawLine(x + 10, y + 25, x + RAIL_W - 10, y + 25);
		g.setColor(RogueScapeTheme.BORDER);
		g.drawLine(x + 10, y + 26, x + RAIL_W - 10, y + 26);

		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		int cy = y + 45;
		boolean modifierSection = false;
		for (String raw : view.railRows)
		{
			if (raw == null)
			{
				continue;
			}
			String row = RogueScapeWindowOverlay.ascii(raw);
			if (row.trim().isEmpty())
			{
				cy += 8;
				modifierSection = false;
				continue;
			}
			if (row.endsWith(":"))
			{
				modifierSection = row.equals("Modifiers:");
				g.setColor(RogueScapeTheme.ACCENT);
				g.drawString(row, x + 12, cy);
				cy += LINE_H;
				continue;
			}
			if (modifierSection && row.indexOf(':') < 0)
			{
				cy = drawModifierBadge(g, x + 10, cy - 11, RAIL_W - 20, row) + 4;
				if (cy > y + h - 12)
				{
					return;
				}
				continue;
			}
			for (String line : wrap(row, fm, RAIL_W - 24))
			{
				if (cy > y + h - 12)
				{
					return;
				}
				if (line.indexOf(':') > 0)
				{
					drawStatLine(g, x + 12, cy, RAIL_W - 24, line);
				}
				else
				{
					g.setColor(RogueScapeTheme.TEXT_MUTED);
					g.drawString(line, x + 12, cy);
				}
				cy += LINE_H;
			}
		}
	}

	/** Draws one reward card; shared by the reward popup and the window's CARDS block. */
	static void drawCard(Graphics2D g, RogueScapeIcons icons, Card card, Rectangle r, boolean sel, boolean hov)
	{
		Color rarity = card.rarity.color();
		int cx = r.x + r.width / 2;

		// Soft rarity glow behind the card (always present, stronger when selected/hovered).
		int glowA = sel ? 95 : hov ? 55 : 28;
		g.setColor(new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), glowA));
		g.fillRoundRect(r.x - 5, r.y - 5, r.width + 10, r.height + 10, 16, 16);

		// Card body: vertical gradient for depth (lighter top, darker bottom).
		g.setPaint(new GradientPaint(0, r.y, RogueScapeTheme.SECTION_HEADER_BG,
			0, r.y + r.height, darken(RogueScapeTheme.PANEL_BG, 6)));
		g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

		// Rarity-tinted double border.
		g.setColor(new Color(0, 0, 0, 130));
		g.drawRoundRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3, 9, 9);
		g.setColor(sel ? RogueScapeTheme.lighten(rarity, 55) : hov ? RogueScapeTheme.lighten(rarity, 20) : rarity);
		g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
		drawCardCornerAccents(g, r, rarity);
		if (sel)
		{
			g.setColor(new Color(255, 246, 210, 70));
			g.drawRoundRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7, 8, 8);
		}

		// Rarity banner across the top.
		int bannerH = 19;
		g.setColor(new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), sel ? 235 : 175));
		g.fillRoundRect(r.x + 4, r.y + 4, r.width - 8, bannerH, 8, 8);
		g.setColor(new Color(0, 0, 0, 70));
		g.fillRect(r.x + 4, r.y + 4 + bannerH - 1, r.width - 8, 1);
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		String rword = card.rarity.name();
		int rwx = cx - fm.stringWidth(rword) / 2;
		g.setColor(new Color(0x14110A));
		g.drawString(rword, rwx + 1, r.y + 4 + 14 + 1);
		g.setColor(new Color(0xFFF3D6));
		g.drawString(rword, rwx, r.y + 4 + 14);

		// Title (item / relic name), wrapped to at most two lines.
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics tfm = g.getFontMetrics();
		List<String> nameLines = wrap(RogueScapeWindowOverlay.ascii(card.title), tfm, r.width - 12);
		int nameY = r.y + bannerH + 21;
		for (int i = 0; i < Math.min(2, nameLines.size()); i++)
		{
			String ln = nameLines.get(i);
			int lx = cx - tfm.stringWidth(ln) / 2;
			g.setColor(SHADOW);
			g.drawString(ln, lx + 1, nameY + 1);
			g.setColor(RogueScapeTheme.TEXT_PRIMARY);
			g.drawString(ln, lx, nameY);
			nameY += 14;
		}

		// Model pedestal — a recessed disc where the item icon / spinning 3D model sits.
		int pedR = Math.max(18, Math.min(30, r.width / 2 - 16));
		int pedCy = nameY + pedR + 2;
		// Ground shadow under the pedestal.
		g.setColor(new Color(0, 0, 0, 90));
		g.fillOval(cx - pedR, pedCy + pedR - 5, pedR * 2, 9);
		// Rarity halo.
		g.setColor(new Color(rarity.getRed(), rarity.getGreen(), rarity.getBlue(), 55));
		g.fillOval(cx - pedR - 3, pedCy - pedR - 3, pedR * 2 + 6, pedR * 2 + 6);
		// Recessed disc + rarity ring.
		g.setColor(darken(RogueScapeTheme.PANEL_BG, 14));
		g.fillOval(cx - pedR, pedCy - pedR, pedR * 2, pedR * 2);
		g.setColor(rarity);
		g.drawOval(cx - pedR, pedCy - pedR, pedR * 2, pedR * 2);

		int iconSize = pedR + 14;
		if (icons != null && card.iconItemId > 0)
		{
			RogueScapeIcons.draw(g, icons.item(card.iconItemId), cx - iconSize / 2, pedCy - iconSize / 2, iconSize);
		}
		else
		{
			drawDiamond(g, cx, pedCy, pedR - 7, darken(rarity, 40), rarity);
		}

		// Description lines.
		g.setFont(FontManager.getRunescapeFont());
		FontMetrics dfm = g.getFontMetrics();
		int dy = pedCy + pedR + 16;
		int descBottom = r.y + r.height - 24;
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		outer:
		for (String raw : card.lines)
		{
			for (String ln : wrap(RogueScapeWindowOverlay.ascii(raw), dfm, r.width - 14))
			{
				if (dy > descBottom)
				{
					break outer;
				}
				g.drawString(ln, cx - dfm.stringWidth(ln) / 2, dy);
				dy += LINE_H;
			}
		}

		// Category pill at the bottom.
		if (!card.category.isEmpty())
		{
			String cat = RogueScapeWindowOverlay.ascii(card.category);
			int cw = dfm.stringWidth(cat) + 16;
			int px = cx - cw / 2;
			int py = r.y + r.height - 19;
			g.setColor(darken(rarity, 55));
			g.fillRoundRect(px, py, cw, 15, 7, 7);
			g.setColor(rarity);
			g.drawRoundRect(px, py, cw, 15, 7, 7);
			g.setColor(RogueScapeTheme.lighten(rarity, 50));
			g.drawString(cat, px + 8, py + 11);
		}

		if (sel)
		{
			String selected = "SELECTED";
			g.setFont(FontManager.getRunescapeBoldFont());
			FontMetrics sfm = g.getFontMetrics();
			int sw = sfm.stringWidth(selected) + 14;
			int sx = r.x + r.width - sw - 8;
			int sy = r.y + r.height - 21;
			g.setColor(new Color(0x2C230D));
			g.fillRoundRect(sx, sy, sw, 15, 7, 7);
			g.setColor(RogueScapeTheme.GOLD);
			g.drawRoundRect(sx, sy, sw, 15, 7, 7);
			g.setColor(new Color(0xFFF3D6));
			g.drawString(selected, sx + 7, sy + 11);
		}
	}

	private void drawArtifacts(Graphics2D g, RewardView view)
	{
		int y = HEIGHT - FOOTER_H - ARTIFACT_H - 3;
		int x = PAD;
		int w = WIDTH - PAD * 2;
		g.setPaint(new GradientPaint(0, y, darken(RogueScapeTheme.PANEL_BG, 2),
			0, y + ARTIFACT_H, RogueScapeTheme.SECTION_HEADER_BG));
		g.fillRoundRect(x, y, w, ARTIFACT_H - 8, 8, 8);
		g.setColor(RogueScapeTheme.BORDER);
		g.drawRoundRect(x, y, w, ARTIFACT_H - 8, 8, 8);

		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		int maxSlots = Math.min(12, Math.max(8, view.artifactItemIds.size() + 2));
		String title = "YOUR ARTIFACTS  " + view.artifactItemIds.size() + " / " + maxSlots;
		int tx = x + (w - fm.stringWidth(title)) / 2;
		g.setColor(RogueScapeTheme.GOLD_DIM);
		g.drawString(title, tx, y + 16);

		int slot = 34;
		int gap = 8;
		int rowW = maxSlots * slot + (maxSlots - 1) * gap;
		int sx = x + (w - rowW) / 2;
		int sy = y + 23;
		for (int i = 0; i < maxSlots; i++)
		{
			int px = sx + i * (slot + gap);
			boolean filled = i < view.artifactItemIds.size() && view.artifactItemIds.get(i) != null
				&& view.artifactItemIds.get(i) > 0;
			g.setColor(filled ? RogueScapeTheme.SECTION_BG : darken(RogueScapeTheme.PANEL_BG, 6));
			g.fillRoundRect(px, sy, slot, slot, 5, 5);
			g.setColor(filled ? RogueScapeTheme.ACCENT_DIM : darken(RogueScapeTheme.BORDER, 28));
			g.drawRoundRect(px, sy, slot, slot, 5, 5);
			if (filled && icons != null)
			{
				RogueScapeIcons.draw(g, icons.item(view.artifactItemIds.get(i)), px + 4, sy + 4, slot - 8);
			}
			else if (filled)
			{
				drawArtifactGlyph(g, view.artifactItemIds.get(i), px + slot / 2, sy + slot / 2, slot / 2 - 5);
			}
			else if (!filled)
			{
				drawEmptyArtifactSlot(g, px + slot / 2, sy + slot / 2);
			}
		}
	}

	private void drawFooter(Graphics2D g, RewardView view)
	{
		int y0 = HEIGHT - FOOTER_H - 3;
		int x0 = 3, w = WIDTH - 6;
		g.setPaint(new GradientPaint(0, y0, RogueScapeTheme.PANEL_BG, 0, y0 + FOOTER_H,
			RogueScapeTheme.SECTION_HEADER_BG));
		g.fillRect(x0, y0, w, FOOTER_H);
		g.setColor(SHADOW);
		g.drawLine(x0, y0, x0 + w - 1, y0);
		g.setColor(RogueScapeTheme.BORDER);
		g.drawLine(x0, y0 + 1, x0 + w - 1, y0 + 1);

		if (view != null && selected >= 0 && selected < view.cards.size())
		{
			String pick = "Selected: " + RogueScapeWindowOverlay.ascii(view.cards.get(selected).title);
			g.setFont(FontManager.getRunescapeFont());
			FontMetrics fm = g.getFontMetrics();
			int tx = WIDTH / 2 - fm.stringWidth(pick) / 2;
			g.setColor(RogueScapeTheme.TEXT_MUTED);
			g.drawString(pick, tx, y0 + 14);
		}

		int by = y0 + (FOOTER_H - 28) / 2 + 10;
		int bh = 28;
		// Reroll (disabled — no core support yet), Confirm (primary), Skip (neutral).
		rerollRect.setBounds(PAD, by, 96, bh);
		drawButton(g, rerollRect, "Reroll", RogueScapeTheme.ButtonRole.NEUTRAL, false, false);

		skipRect.setBounds(WIDTH - PAD - 110, by, 110, bh);
		drawButton(g, skipRect, "Skip Reward", RogueScapeTheme.ButtonRole.NEUTRAL, true, hoverSkip);

		int confirmW = 150;
		confirmRect.setBounds(WIDTH / 2 - confirmW / 2, by, confirmW, bh);
		drawButton(g, confirmRect, "Confirm Choice", RogueScapeTheme.ButtonRole.PRIMARY, true, hoverConfirm);
	}

	private static void drawButton(Graphics2D g, Rectangle r, String label,
		RogueScapeTheme.ButtonRole role, boolean enabled, boolean hover)
	{
		Color bg = !enabled ? RogueScapeTheme.BTN_DISABLED
			: hover ? RogueScapeTheme.buttonHoverBg(role) : RogueScapeTheme.buttonBg(role);
		Color fg = !enabled ? RogueScapeTheme.TEXT_MUTED : RogueScapeTheme.buttonText(role);
		g.setColor(bg);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 7, 7);
		g.setColor(enabled ? RogueScapeTheme.BORDER : darken(RogueScapeTheme.BORDER, 40));
		g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 7, 7);
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		String t = RogueScapeWindowOverlay.ascii(label);
		int tx = r.x + (r.width - fm.stringWidth(t)) / 2;
		int ty = r.y + (r.height + fm.getAscent()) / 2 - 2;
		g.setColor(fg);
		g.drawString(t, tx, ty);
	}

	// ------------------------------------------------------------ helpers

	private static void drawStatLine(Graphics2D g, int x, int baseline, int width, String line)
	{
		int idx = line.indexOf(':');
		String label = idx <= 0 ? line : line.substring(0, idx);
		String value = idx <= 0 || idx + 1 >= line.length() ? "" : line.substring(idx + 1).trim();
		drawTinyIcon(g, x + 5, baseline - 5, label);
		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		g.drawString(label, x + 15, baseline);
		if (!value.isEmpty())
		{
			g.setColor(RogueScapeTheme.TEXT_PRIMARY);
			g.drawString(value, x + width - fm.stringWidth(value), baseline);
		}
	}

	private static int drawModifierBadge(Graphics2D g, int x, int y, int w, String label)
	{
		int h = 23;
		g.setPaint(new GradientPaint(0, y, new Color(0x4A1717), 0, y + h, new Color(0x241010)));
		g.fillRoundRect(x, y, w, h, 7, 7);
		g.setColor(new Color(0x8F3030));
		g.drawRoundRect(x, y, w - 1, h - 1, 7, 7);
		drawTinyIcon(g, x + 11, y + h / 2 + 1, "Modifier");
		g.setFont(FontManager.getRunescapeFont());
		g.setColor(RogueScapeTheme.TEXT_PRIMARY);
		g.drawString(RogueScapeWindowOverlay.ascii(label), x + 23, y + 15);
		return y + h;
	}

	private static void drawTinyIcon(Graphics2D g, int cx, int cy, String key)
	{
		String lower = key == null ? "" : key.toLowerCase();
		Color c = lower.contains("floor") ? RogueScapeTheme.BAR_PROGRESS
			: lower.contains("score") || lower.contains("relic") ? RogueScapeTheme.GOLD
			: lower.contains("illegal") || lower.contains("modifier") ? RogueScapeTheme.NEGATIVE
			: lower.contains("legal") ? RogueScapeTheme.POSITIVE
			: lower.contains("signal") ? RogueScapeTheme.ACCENT
			: RogueScapeTheme.INFO;
		if (lower.contains("floor"))
		{
			g.setColor(RogueScapeTheme.lighten(c, 35));
			g.fillRect(cx - 5, cy - 5, 3, 10);
			g.setColor(c);
			g.fillRect(cx - 1, cy - 8, 3, 13);
			g.setColor(RogueScapeFrame.darken(c, 30));
			g.fillRect(cx + 3, cy - 3, 3, 8);
			return;
		}
		if (lower.contains("illegal") || lower.contains("modifier"))
		{
			drawSkull(g, cx, cy, c);
			return;
		}
		if (lower.contains("signal"))
		{
			drawDiamond(g, cx, cy, 6, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
			return;
		}
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 160));
		g.fillOval(cx - 5, cy - 5, 10, 10);
		g.setColor(RogueScapeTheme.lighten(c, 45));
		g.drawOval(cx - 5, cy - 5, 10, 10);
	}

	private static void drawSkull(Graphics2D g, int cx, int cy, Color c)
	{
		g.setColor(c);
		g.fillOval(cx - 5, cy - 6, 10, 9);
		g.fillRect(cx - 3, cy, 6, 5);
		g.setColor(new Color(0x160909));
		g.fillOval(cx - 3, cy - 3, 2, 2);
		g.fillOval(cx + 1, cy - 3, 2, 2);
		g.drawLine(cx - 2, cy + 3, cx + 2, cy + 3);
	}

	private static void drawCardCornerAccents(Graphics2D g, Rectangle r, Color rarity)
	{
		int s = 10;
		g.setColor(RogueScapeTheme.lighten(rarity, 25));
		g.drawLine(r.x + 5, r.y + 5, r.x + s + 5, r.y + 5);
		g.drawLine(r.x + 5, r.y + 5, r.x + 5, r.y + s + 5);
		g.drawLine(r.x + r.width - 6, r.y + 5, r.x + r.width - s - 6, r.y + 5);
		g.drawLine(r.x + r.width - 6, r.y + 5, r.x + r.width - 6, r.y + s + 5);
		g.drawLine(r.x + 5, r.y + r.height - 6, r.x + s + 5, r.y + r.height - 6);
		g.drawLine(r.x + 5, r.y + r.height - 6, r.x + 5, r.y + r.height - s - 6);
		g.drawLine(r.x + r.width - 6, r.y + r.height - 6, r.x + r.width - s - 6, r.y + r.height - 6);
		g.drawLine(r.x + r.width - 6, r.y + r.height - 6, r.x + r.width - 6, r.y + r.height - s - 6);
	}

	private static void drawArtifactGlyph(Graphics2D g, int itemId, int cx, int cy, int rad)
	{
		Color[] colors = {
			RogueScapeTheme.RARITY_RARE,
			RogueScapeTheme.RARITY_EPIC,
			RogueScapeTheme.RARITY_LEGENDARY,
			RogueScapeTheme.NEGATIVE,
			RogueScapeTheme.ACCENT
		};
		Color c = colors[Math.abs(itemId) % colors.length];
		drawDiamond(g, cx, cy, rad, RogueScapeFrame.darken(c, 45), c);
		g.setColor(new Color(255, 255, 255, 70));
		g.drawLine(cx, cy - rad + 4, cx, cy + rad - 4);
		g.drawLine(cx - rad + 4, cy, cx + rad - 4, cy);
	}

	private static void drawEmptyArtifactSlot(Graphics2D g, int cx, int cy)
	{
		g.setColor(new Color(0, 0, 0, 95));
		g.fillOval(cx - 7, cy - 7, 14, 14);
		g.setColor(RogueScapeFrame.darken(RogueScapeTheme.BORDER, 20));
		g.drawOval(cx - 7, cy - 7, 14, 14);
		g.setColor(new Color(RogueScapeTheme.ACCENT.getRed(), RogueScapeTheme.ACCENT.getGreen(),
			RogueScapeTheme.ACCENT.getBlue(), 55));
		g.drawLine(cx, cy - 5, cx, cy + 5);
		g.drawLine(cx - 5, cy, cx + 5, cy);
	}

	private static void drawDiamond(Graphics2D g, int cx, int cy, int rad, Color outer, Color inner)
	{
		Polygon p = new Polygon();
		p.addPoint(cx, cy - rad);
		p.addPoint(cx + rad, cy);
		p.addPoint(cx, cy + rad);
		p.addPoint(cx - rad, cy);
		g.setColor(outer);
		g.fillPolygon(p);
		int ir = Math.max(1, rad - 3);
		Polygon q = new Polygon();
		q.addPoint(cx, cy - ir);
		q.addPoint(cx + ir, cy);
		q.addPoint(cx, cy + ir);
		q.addPoint(cx - ir, cy);
		g.setColor(inner);
		g.fillPolygon(q);
	}

	private static Color darken(Color c, int amount)
	{
		return new Color(
			Math.max(0, c.getRed() - amount),
			Math.max(0, c.getGreen() - amount),
			Math.max(0, c.getBlue() - amount));
	}

	/** Greedy word-wrap to a pixel width. */
	private static List<String> wrap(String text, FontMetrics fm, int maxWidth)
	{
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return out;
		}
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(candidate) > maxWidth && line.length() > 0)
			{
				out.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0)
		{
			out.add(line.toString());
		}
		return out;
	}

	// ------------------------------------------------------------ mouse

	private Point toLocal(MouseEvent e)
	{
		Rectangle bounds = getBounds();
		if (bounds == null || bounds.width == 0)
		{
			return null;
		}
		return new Point(e.getX() - bounds.x, e.getY() - bounds.y);
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		Point local = toLocal(e);
		if (local == null)
		{
			return e;
		}
		// Only consume clicks while the window is actually showing.
		if (supplier == null || supplier.get() == null)
		{
			return e;
		}
		if (confirmRect.contains(local))
		{
			if (onChoose != null && selected >= 0)
			{
				onChoose.accept(selected);
			}
			e.consume();
			return e;
		}
		if (skipRect.contains(local))
		{
			if (onSkip != null)
			{
				onSkip.run();
			}
			e.consume();
			return e;
		}
		for (int i = 0; i < cardRects.size(); i++)
		{
			if (cardRects.get(i).contains(local))
			{
				selected = i;
				e.consume();
				return e;
			}
		}
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		hoverCard = -1;
		hoverConfirm = false;
		hoverSkip = false;
		if (supplier == null || supplier.get() == null)
		{
			return e;
		}
		Point local = toLocal(e);
		if (local == null)
		{
			return e;
		}
		hoverConfirm = confirmRect.contains(local);
		hoverSkip = skipRect.contains(local);
		for (int i = 0; i < cardRects.size(); i++)
		{
			if (cardRects.get(i).contains(local))
			{
				hoverCard = i;
				break;
			}
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e)
	{
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e)
	{
		hoverCard = -1;
		hoverConfirm = false;
		hoverSkip = false;
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}

	private static final Color SHADOW = new Color(0x0A0805);
}
