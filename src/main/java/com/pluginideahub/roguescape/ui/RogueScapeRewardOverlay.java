package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
	private static final int PAD = 16;
	private static final int CARD_GAP = 12;
	private static final int LINE_H = 16;
	// The open-book spread: two pages around a central spine.
	private static final int PAGE_Y = 8;
	private static final int PAGE_H = HEIGHT - 16;
	private static final int PAGE_W = 366;
	private static final int LEFT_X = 10;
	private static final int RIGHT_X = WIDTH - 10 - PAGE_W;

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

		drawSpread(g);
		drawLeftPage(g, view);
		drawRightPage(g, view);

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

	/** Dark desk backdrop, two aged pages, and the spine shadow between them. */
	private void drawSpread(Graphics2D g)
	{
		g.setColor(new Color(0x2A2620));
		g.fillRoundRect(0, 0, WIDTH, HEIGHT, 8, 8);
		g.setColor(RogueScapeFrame.SHADOW);
		g.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 8, 8);
		RogueScapePaper.page(g, LEFT_X, PAGE_Y, PAGE_W, PAGE_H);
		RogueScapePaper.page(g, RIGHT_X, PAGE_Y, PAGE_W, PAGE_H);
		int mid = WIDTH / 2;
		g.setPaint(new GradientPaint(mid - 14, 0, new Color(0, 0, 0, 0), mid, 0, new Color(0, 0, 0, 70)));
		g.fillRect(mid - 14, PAGE_Y, 14, PAGE_H);
		g.setPaint(new GradientPaint(mid, 0, new Color(0, 0, 0, 70), mid + 14, 0, new Color(0, 0, 0, 0)));
		g.fillRect(mid, PAGE_Y, 14, PAGE_H);
	}

	/** Left page: the chest — title, the loot cards, the chosen note, TAKE IT / skip. */
	private void drawLeftPage(Graphics2D g, RewardView view)
	{
		int x = LEFT_X + 20;
		int w = PAGE_W - 40;

		g.setFont(new Font(Font.SERIF, Font.BOLD, 19));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(view.title, x, PAGE_Y + 30);
		if (!view.subtitle.isEmpty())
		{
			g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
			g.setColor(RogueScapeTheme.INK_FADED);
			g.drawString(view.subtitle, x, PAGE_Y + 46);
		}
		RogueScapePaper.inkRule(g, x, PAGE_Y + 54, w);

		drawCards(g, view);

		// The chosen card's name, as a margin note above the stamp row.
		int noteY = PAGE_Y + PAGE_H - 52;
		if (selected >= 0 && selected < view.cards.size())
		{
			String pick = "\u00ab " + view.cards.get(selected).title + " calls to you \u00bb";
			g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
			FontMetrics fm = g.getFontMetrics();
			g.setColor(RogueScapeTheme.STAMP);
			g.drawString(pick, x + (w - fm.stringWidth(pick)) / 2, noteY);
		}

		// TAKE IT stamp + the quieter skip as an inked underline link.
		int by = PAGE_Y + PAGE_H - 44;
		confirmRect.setBounds(x, by, 150, 34);
		Graphics2D c = (Graphics2D) g.create();
		c.rotate(Math.toRadians(-1.5), confirmRect.getCenterX(), confirmRect.getCenterY());
		RogueScapePaper.stamp(c, confirmRect.x, confirmRect.y, confirmRect.width, confirmRect.height,
			RogueScapeTheme.STAMP, hoverConfirm);
		c.setFont(new Font(Font.SERIF, Font.BOLD, 15));
		FontMetrics cfm = c.getFontMetrics();
		c.setColor(RogueScapeTheme.STAMP);
		c.drawString("Take It",
			confirmRect.x + (confirmRect.width - cfm.stringWidth("Take It")) / 2,
			confirmRect.y + 22);
		c.dispose();

		g.setFont(new Font(Font.SERIF, Font.ITALIC, 13));
		FontMetrics sfm = g.getFontMetrics();
		String skip = "or leave the chest\u2026";
		int skipW = sfm.stringWidth(skip);
		skipRect.setBounds(x + 170, by + 6, skipW + 8, 24);
		g.setColor(hoverSkip ? RogueScapeTheme.STAMP : RogueScapeTheme.INK_FADED);
		g.drawString(skip, skipRect.x + 4, skipRect.y + 16);
		RogueScapePaper.leader(g, skipRect.x + 2, skipRect.y + 20, skipRect.x + skipW + 6);
	}

	private void drawCards(Graphics2D g, RewardView view)
	{
		cardRects.clear();
		List<Card> cards = view.cards;
		int n = cards.size();
		int areaX = LEFT_X + 20;
		int areaY = PAGE_Y + 64;
		int areaW = PAGE_W - 40;
		int areaH = PAGE_Y + PAGE_H - 64 - areaY;
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

	/** Right page: The Ledger — leader-line stats, modifier notes, and the relic pockets. */
	private void drawRightPage(Graphics2D g, RewardView view)
	{
		int x = RIGHT_X + 24;
		int w = PAGE_W - 48;

		g.setFont(new Font(Font.SERIF, Font.BOLD, 17));
		g.setColor(RogueScapeTheme.INK);
		g.drawString("The Ledger", x, PAGE_Y + 30);
		RogueScapePaper.inkRule(g, x, PAGE_Y + 38, w);

		int pocketsTop = PAGE_Y + PAGE_H - 88;
		int cy = PAGE_Y + 58;
		boolean modifierSection = false;
		for (String row : view.railRows)
		{
			if (row == null || cy > pocketsTop - 8)
			{
				continue;
			}
			String trimmed = row.trim();
			if (trimmed.isEmpty())
			{
				cy += 6;
				modifierSection = false;
				continue;
			}
			if (trimmed.endsWith(":") && trimmed.indexOf(':') == trimmed.length() - 1)
			{
				modifierSection = trimmed.equals("Curses:");
				g.setFont(new Font(Font.SERIF, Font.BOLD, 12));
				g.setColor(RogueScapeTheme.INK);
				g.drawString(trimmed.substring(0, trimmed.length() - 1).toUpperCase(), x, cy);
				cy += LINE_H;
				continue;
			}
			int idx = trimmed.indexOf(':');
			if (!modifierSection && idx > 0)
			{
				drawLedgerLine(g, x, cy, w, trimmed.substring(0, idx).trim(),
					trimmed.substring(idx + 1).trim());
			}
			else
			{
				// Modifier curses and plain notes: red wax dot + ink note.
				if (modifierSection)
				{
					RogueScapePaper.waxSeal(g, x + 5, cy - 4, 4, RogueScapeTheme.WAX_RED);
				}
				g.setFont(new Font(Font.SERIF, modifierSection ? Font.PLAIN : Font.ITALIC, 12));
				g.setColor(modifierSection ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.INK_FADED);
				g.drawString(trimmed, x + (modifierSection ? 14 : 0), cy);
			}
			cy += LINE_H;
		}

		// Relic pockets along the page bottom.
		g.setFont(new Font(Font.SERIF, Font.BOLD, 12));
		g.setColor(RogueScapeTheme.INK);
		g.drawString("RELICS IN POCKET  (" + view.artifactItemIds.size() + ")", x, pocketsTop + 2);
		Color[] seals = {RogueScapeTheme.WAX_BLUE, RogueScapeTheme.WAX_GREEN,
			RogueScapeTheme.WAX_GOLD, RogueScapeTheme.WAX_RED};
		int slots = Math.max(6, Math.min(8, view.artifactItemIds.size() + 1));
		int slot = 38;
		int gap = 6;
		for (int i = 0; i < slots; i++)
		{
			int px = x + i * (slot + gap);
			if (px + slot > RIGHT_X + PAGE_W - 20)
			{
				break;
			}
			boolean filled = i < view.artifactItemIds.size();
			RogueScapePaper.pocket(g, px, pocketsTop + 10, slot, slot,
				filled ? seals[i % seals.length] : null);
		}
	}

	/** One ledger line: ink label, dotted leader, bold value right-aligned. */
	private static void drawLedgerLine(Graphics2D g, int x, int baseline, int w, String label, String value)
	{
		g.setFont(new Font(Font.SERIF, Font.PLAIN, 13));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(label, x, baseline);
		int labelEnd = x + g.getFontMetrics().stringWidth(label) + 6;
		g.setFont(new Font(Font.SERIF, Font.BOLD, 13));
		FontMetrics vfm = g.getFontMetrics();
		int vx = Math.max(labelEnd + 10, x + w - vfm.stringWidth(value));
		RogueScapePaper.leader(g, labelEnd, baseline - 4, vx - 6);
		Color valueColor = label.equalsIgnoreCase("Illegal") && !value.equals("0")
			? RogueScapeTheme.NEGATIVE : RogueScapeTheme.INK;
		g.setColor(valueColor);
		g.drawString(value, vx, baseline);
	}

	static void drawCard(Graphics2D g, RogueScapeIcons icons, Card card, Rectangle r, boolean sel, boolean hov)
	{
		Color rarity = card.rarity.color();
		int cx = r.x + r.width / 2;

		// Card body: an aged-paper loot card with a drop shadow on the page.
		g.setColor(new Color(0, 0, 0, 60));
		g.fillRoundRect(r.x + 2, r.y + 4, r.width, r.height, 10, 10);
		g.setColor(sel ? RogueScapeTheme.lighten(RogueScapeTheme.PAPER_CARD, 8) : RogueScapeTheme.PAPER_CARD);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);

		// Border: rarity ink normally; the chosen card wears the red ink frame.
		if (sel)
		{
			g.setStroke(new java.awt.BasicStroke(2.4f));
			g.setColor(RogueScapeTheme.STAMP);
			g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
			g.setStroke(new java.awt.BasicStroke(1f));
		}
		else
		{
			g.setColor(hov ? RogueScapeTheme.lighten(rarity, 20) : rarity);
			g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
		}
		drawCardCornerAccents(g, r, rarity);

		// Wax seal at the top (its color is the rarity), with the tier named beneath in ink.
		int bannerH = 19;
		RogueScapePaper.waxSeal(g, cx, r.y + 16, 12, rarity);
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		String rword = card.rarity.name();
		g.setColor(RogueScapeTheme.INK_FADED);
		g.drawString(rword, cx - fm.stringWidth(rword) / 2, r.y + 42);

		// Title (item / relic name), wrapped to at most two lines.
		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics tfm = g.getFontMetrics();
		List<String> nameLines = wrap(RogueScapeWindowOverlay.ascii(card.title), tfm, r.width - 12);
		int nameY = r.y + bannerH + 37;
		for (int i = 0; i < Math.min(2, nameLines.size()); i++)
		{
			String ln = nameLines.get(i);
			int lx = cx - tfm.stringWidth(ln) / 2;
			g.setColor(RogueScapeTheme.INK);
			g.drawString(ln, lx, nameY);
			nameY += 14;
		}

		// Model pedestal — a recessed disc where the item icon / spinning 3D model sits.
		// Shrinks on short cards so the description lines below keep at least two rows.
		int pedR = Math.max(14, Math.min(Math.min(30, r.width / 2 - 16), (r.height - 120) / 2));
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

		if (sel)
		{
			// The chosen card: a tilted red CHOSEN stamp at the bottom.
			Graphics2D c = (Graphics2D) g.create();
			c.rotate(Math.toRadians(-10), cx, r.y + r.height - 16);
			c.setFont(new Font(Font.SERIF, Font.BOLD, 13));
			FontMetrics sfm = c.getFontMetrics();
			c.setColor(RogueScapeTheme.STAMP);
			c.drawString("CHOSEN", cx - sfm.stringWidth("CHOSEN") / 2, r.y + r.height - 12);
			c.dispose();
		}
		else if (!card.category.isEmpty())
		{
			// Category pill at the bottom (unchosen cards).
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
