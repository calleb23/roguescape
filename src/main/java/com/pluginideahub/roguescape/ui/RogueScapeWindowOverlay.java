package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.api.MenuAction;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The RogueScape custom in-game window: a Collection-Log-style framed interface with a title bar,
 * a row of clickable top tabs, and a content panel that renders typed {@link Block}s (headings,
 * text rows, stat bars, item-icon grids, modifier badges). Skinned with the bundled art sheet
 * ({@link RogueScapeSprites}) where available, procedural {@link RogueScapeFrame} otherwise.
 *
 * <p>Plugins can't reuse the game's real Collection Log widget for custom content, so this is a
 * fully custom-rendered look-alike. Tab switching / close are handled via {@link MouseListener};
 * text is ASCII-sanitised since the OSRS font lacks glyphs like {@code ═ ✓ ✗ • ▸}.
 */
public class RogueScapeWindowOverlay extends Overlay implements MouseListener
{
	public static final String CLOSE_OPTION = "Close";
	public static final String MENU_TARGET = "RogueScape";

	private static final int WIDTH = 524;
	private static final int HEIGHT = 366;
	private static final int TITLE_H = 30;
	private static final int TAB_H = 24;
	private static final int FOOTER_H = 18;
	private static final int PAD = 12;
	private static final int LINE_H = 15;

	/** A content element inside a tab. One struct, kind-tagged, built via the static factories. */
	public static final class Block
	{
		enum Kind { HEADING, TEXT, NOTE, STATBAR, ITEMGRID, BADGE, CARDS, MODE_TILES, CHAPTERS, PAGE_TITLE, COLUMNS, HOURGLASS, GAP }

		final Kind kind;
		String text = "";
		String sub = "";
		String value = "";
		Color color = RogueScapeTheme.TEXT_PRIMARY;
		double frac;
		int[] items;
		int lockedFrom = Integer.MAX_VALUE;
		int iconItemId;
		List<RogueScapeRewardOverlay.Card> cards;
		List<ModeTile> modeTiles;
		List<com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter> chapters;
		List<Block> left;
		List<Block> right;

		private Block(Kind kind)
		{
			this.kind = kind;
		}

		public static Block heading(String text)
		{
			Block b = new Block(Kind.HEADING);
			b.text = text;
			return b;
		}

		public static Block text(String text, Color color)
		{
			Block b = new Block(Kind.TEXT);
			b.text = text;
			b.color = color == null ? RogueScapeTheme.TEXT_PRIMARY : color;
			return b;
		}

		/** Margin-note text: smaller serif, tighter leading (rule lists, asides). */
		public static Block note(String text, Color color)
		{
			Block b = new Block(Kind.NOTE);
			b.text = text;
			b.color = color == null ? RogueScapeTheme.TEXT_MUTED : color;
			return b;
		}

		public static Block statBar(String label, double frac, String value, Color color)
		{
			Block b = new Block(Kind.STATBAR);
			b.text = label;
			b.frac = Math.max(0, Math.min(1, frac));
			b.value = value == null ? "" : value;
			b.color = color == null ? RogueScapeTheme.BAR_PROGRESS : color;
			return b;
		}

		/** @param items item ids per slot (0 = empty); @param lockedFrom first padlocked slot index. */
		public static Block itemGrid(int[] items, int lockedFrom)
		{
			Block b = new Block(Kind.ITEMGRID);
			b.items = items == null ? new int[0] : items;
			b.lockedFrom = lockedFrom;
			return b;
		}

		public static Block badge(String title, String sub, Color color, int iconItemId)
		{
			Block b = new Block(Kind.BADGE);
			b.text = title;
			b.sub = sub == null ? "" : sub;
			b.color = color == null ? RogueScapeTheme.ACCENT : color;
			b.iconItemId = iconItemId;
			return b;
		}

		public static Block cards(List<RogueScapeRewardOverlay.Card> cards)
		{
			Block b = new Block(Kind.CARDS);
			b.cards = cards == null ? new ArrayList<>() : cards;
			return b;
		}

		public static Block modeTiles(List<ModeTile> tiles)
		{
			Block b = new Block(Kind.MODE_TILES);
			b.modeTiles = tiles == null ? new ArrayList<>() : tiles;
			return b;
		}

		public static Block chapters(List<com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter> chapters)
		{
			Block b = new Block(Kind.CHAPTERS);
			b.chapters = chapters == null ? new ArrayList<>() : chapters;
			return b;
		}

		/** Diary-page masthead: ribbon bookmark, big serif title, italic subtitle, ink rule. */
		public static Block pageTitle(String title, String sub)
		{
			Block b = new Block(Kind.PAGE_TITLE);
			b.text = title;
			b.sub = sub == null ? "" : sub;
			return b;
		}

		/** Two side-by-side columns of blocks (entry/rules left, record/hourglass right). */
		public static Block columns(List<Block> left, List<Block> right)
		{
			Block b = new Block(Kind.COLUMNS);
			b.left = left == null ? new ArrayList<>() : left;
			b.right = right == null ? new ArrayList<>() : right;
			return b;
		}

		/** Hourglass doodle with the elapsed time beside it. */
		public static Block hourglass(String label, String time)
		{
			Block b = new Block(Kind.HOURGLASS);
			b.text = label;
			b.value = time == null ? "" : time;
			return b;
		}

		public static Block gap()
		{
			return new Block(Kind.GAP);
		}
	}

	/** One lobby-wizard tile in the pop-out Run Builder window. */
	public static final class ModeTile
	{
		final String title;
		final String subtitle;
		final String detail;
		final Color color;
		final boolean selected;
		final String actionId;

		public ModeTile(String title, String subtitle, String detail, Color color, boolean selected)
		{
			this(title, subtitle, detail, color, selected, title);
		}

		public ModeTile(String title, String subtitle, String detail, Color color, boolean selected, String actionId)
		{
			this.title = title == null ? "" : title;
			this.subtitle = subtitle == null ? "" : subtitle;
			this.detail = detail == null ? "" : detail;
			this.color = color == null ? RogueScapeTheme.ACCENT : color;
			this.selected = selected;
			this.actionId = actionId == null ? "" : actionId;
		}
	}

	/** A named top tab and its content blocks. */
	public static final class Tab
	{
		private final String name;
		private final List<Block> blocks;

		public Tab(String name, List<Block> blocks)
		{
			this.name = name == null ? "" : name;
			this.blocks = blocks == null ? new ArrayList<>() : blocks;
		}

		/** Tab label (used by both the overlay and the native widget window). */
		public String name()
		{
			return name;
		}

		/** Content blocks for this tab. */
		public List<Block> blocks()
		{
			return blocks;
		}
	}

	private final Supplier<List<Tab>> supplier;
	private final RogueScapeIcons icons;
	private Consumer<String> modeTileHandler;
	private Supplier<Dimension> canvasSize;
	private boolean wasVisible;
	private boolean open;
	private int selected;

	private final Rectangle closeRect = new Rectangle();
	private final List<Rectangle> tabRects = new ArrayList<>();
	private final List<Rectangle> modeTileRects = new ArrayList<>();
	private final List<String> modeTileActions = new ArrayList<>();
	private int hoverTab = -1;
	private int hoverModeTile = -1;
	private boolean hoverClose;

	public RogueScapeWindowOverlay(Supplier<List<Tab>> supplier)
	{
		this(supplier, null);
	}

	public RogueScapeWindowOverlay(Supplier<List<Tab>> supplier, RogueScapeIcons icons)
	{
		this.supplier = supplier;
		this.icons = icons;
		setPosition(OverlayPosition.DETACHED);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(false);
		setResizable(false);
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY, CLOSE_OPTION, MENU_TARGET));
	}

	public void setCanvasSize(Supplier<Dimension> canvasSize)
	{
		this.canvasSize = canvasSize;
	}

	public void setModeTileHandler(Consumer<String> modeTileHandler)
	{
		this.modeTileHandler = modeTileHandler;
	}

	public boolean isOpen()
	{
		return open;
	}

	public void setOpen(boolean open)
	{
		this.open = open;
	}

	public void toggle()
	{
		this.open = !this.open;
	}

	/** Selects the active top tab by index (clamped on render). */
	public void setSelectedTab(int index)
	{
		this.selected = Math.max(0, index);
	}

	private boolean bookMode;
	private java.util.function.BooleanSupplier bookModeSupplier;

	/** When true the window paints as an open-book two-page spread (centre spine, no tab strip). */
	public void setBookMode(boolean v)
	{
		this.bookMode = v;
	}

	/** Dynamic book-mode signal, evaluated each frame (e.g. "a run is active"). */
	public void setBookMode(java.util.function.BooleanSupplier s)
	{
		this.bookModeSupplier = s;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!open)
		{
			wasVisible = false;
			return null;
		}
		List<Tab> tabs = supplier == null ? null : supplier.get();
		if (tabs == null)
		{
			tabs = new ArrayList<>();
		}
		if (selected >= tabs.size())
		{
			selected = 0;
		}
		centerOnAppear();

		if (bookModeSupplier != null)
		{
			bookMode = bookModeSupplier.getAsBoolean();
		}

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		RogueScapeFrame.background(g, 0, 0, WIDTH, HEIGHT);
		boolean art = false;
		RogueScapeFrame.frame(g, 0, 0, WIDTH, HEIGHT);
		modeTileRects.clear();
		modeTileActions.clear();
		drawTitle(g, art);
		if (bookMode)
		{
			drawBookChrome(g);
		}
		else
		{
			drawTabs(g, tabs);
		}
		drawContent(g, tabs.isEmpty() ? null : tabs.get(selected));
		drawFooter(g);

		return new Dimension(WIDTH, HEIGHT);
	}

	private void centerOnAppear()
	{
		if (!wasVisible && canvasSize != null)
		{
			Dimension cs = canvasSize.get();
			if (cs != null && cs.width > 0 && cs.height > 0)
			{
				setPreferredLocation(new Point(
					Math.max(0, (cs.width - WIDTH) / 2),
					Math.max(0, (cs.height - HEIGHT) / 2)));
			}
		}
		wasVisible = true;
	}

	// ------------------------------------------------------------ painting

	private void drawTitle(Graphics2D g, boolean art)
	{
		int x0 = 3, y0 = 3, w = WIDTH - 6;
		int cy = y0 + TITLE_H / 2;
		if (!art)
		{
			// Procedural header bar + crest (the art frame already supplies these).
			RogueScapeFrame.headerBar(g, x0, y0, w, TITLE_H);
			drawDiamond(g, PAD + 4, cy, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
		}
		else
		{
			// Divider under the title row so it reads separately from the tab strip.
			g.setColor(RogueScapeTheme.BORDER);
			g.drawLine(x0 + 4, y0 + TITLE_H, x0 + w - 4, y0 + TITLE_H);
		}

		g.setFont(FontManager.getRunescapeBoldFont());
		int ty = cy + 5;
		g.setColor(RogueScapeFrame.SHADOW);
		g.drawString("RogueScape", PAD + 16 + 1, ty + 1);
		g.setColor(RogueScapeTheme.GOLD);
		g.drawString("RogueScape", PAD + 16, ty);

		int cs = 18;
		closeRect.setBounds(WIDTH - cs - 8, y0 + (TITLE_H - cs) / 2, cs, cs);
		g.setColor(hoverClose ? RogueScapeTheme.BTN_RED_HOVER : RogueScapeTheme.SECTION_BG);
		g.fillRoundRect(closeRect.x, closeRect.y, closeRect.width, closeRect.height, 5, 5);
		g.setColor(hoverClose ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.BORDER);
		g.drawRoundRect(closeRect.x, closeRect.y, closeRect.width, closeRect.height, 5, 5);
		g.setColor(hoverClose ? new Color(0xF0C8C8) : RogueScapeTheme.TEXT_MUTED);
		int ix = closeRect.x + 5, iy = closeRect.y + 5, is = cs - 11;
		g.drawLine(ix, iy, ix + is, iy + is);
		g.drawLine(ix + is, iy, ix, iy + is);
	}

	private void drawTabs(Graphics2D g, List<Tab> tabs)
	{
		tabRects.clear();
		int top = 3 + TITLE_H;
		int x = 3;
		int w = WIDTH - 6;
		int n = Math.max(1, tabs.size());
		int tabW = w / n;

		// Recessed strip backing.
		g.setColor(RogueScapeFrame.darken(RogueScapeTheme.PANEL_BG, 3));
		g.fillRect(x, top, w, TAB_H);

		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		Rectangle selRect = null;
		for (int i = 0; i < tabs.size(); i++)
		{
			int tx = x + i * tabW;
			int tw = (i == tabs.size() - 1) ? (x + w - tx) : tabW;
			Rectangle rect = new Rectangle(tx, top, tw, TAB_H);
			tabRects.add(rect);

			boolean sel = i == selected;
			boolean hov = i == hoverTab && !sel;
			if (sel)
			{
				selRect = rect;
				// Raised gold tab: vertical gradient + bright top accent + side highlights.
				g.setPaint(new java.awt.GradientPaint(0, rect.y, RogueScapeTheme.BTN_GOLD_HOVER,
					0, rect.y + rect.height, RogueScapeTheme.SECTION_HEADER_BG));
				g.fillRect(rect.x, rect.y, rect.width, rect.height);
				g.setColor(RogueScapeTheme.GOLD);
				g.fillRect(rect.x, rect.y, rect.width, 2);
				g.setColor(RogueScapeFrame.darken(RogueScapeTheme.GOLD_DIM, 10));
				g.drawLine(rect.x, rect.y, rect.x, rect.y + rect.height);
				g.drawLine(rect.x + rect.width - 1, rect.y, rect.x + rect.width - 1, rect.y + rect.height);
			}
			else if (hov)
			{
				g.setColor(RogueScapeTheme.SECTION_BG);
				g.fillRect(rect.x + 1, rect.y + 2, rect.width - 2, rect.height - 2);
			}

			if (!sel)
			{
				// Thin bevel separator between unselected tabs.
				g.setColor(RogueScapeFrame.SHADOW);
				g.drawLine(rect.x + rect.width - 1, rect.y + 4, rect.x + rect.width - 1, rect.y + rect.height - 4);
				g.setColor(RogueScapeFrame.darken(RogueScapeTheme.PANEL_BG, -6));
				g.drawLine(rect.x + rect.width, rect.y + 4, rect.x + rect.width, rect.y + rect.height - 4);
			}

			String label = clipTo(ascii(tabs.get(i).name), fm, tw - 6);
			int lx = rect.x + (tw - fm.stringWidth(label)) / 2;
			if (sel)
			{
				g.setColor(RogueScapeFrame.SHADOW);
				g.drawString(label, lx + 1, rect.y + 17);
			}
			g.setColor(sel ? RogueScapeTheme.GOLD : hov ? RogueScapeTheme.TEXT_PRIMARY : RogueScapeTheme.TEXT_MUTED);
			g.drawString(label, lx, rect.y + 16);
		}

		// Gold base rule under the strip, broken under the selected tab so it "connects" to content.
		int ry = top + TAB_H;
		g.setColor(RogueScapeTheme.BORDER_BRIGHT);
		g.drawLine(x, ry, x + w - 1, ry);
		g.setColor(RogueScapeFrame.SHADOW);
		g.drawLine(x, ry + 1, x + w - 1, ry + 1);
		if (selRect != null)
		{
			g.setColor(RogueScapeTheme.SECTION_HEADER_BG);
			g.drawLine(selRect.x + 1, ry, selRect.x + selRect.width - 2, ry);
			g.drawLine(selRect.x + 1, ry + 1, selRect.x + selRect.width - 2, ry + 1);
		}
	}

	/**
	 * Open-book chrome over the content region: a centre spine gutter with page-curl shading and
	 * binding stitches, plus stacked outer page edges, so the parchment reads as a two-page spread
	 * rather than a flat panel.
	 */
	private void drawBookChrome(Graphics2D g)
	{
		int top = 3 + TITLE_H + 2;
		int bottom = HEIGHT - FOOTER_H - 6;
		int cx = WIDTH / 2;

		// Page-curl: both pages dip toward the gutter — shadow deepens toward the crease.
		for (int i = 22; i >= 1; i--)
		{
			int a = (int) (44 * (1 - (i - 1) / 22.0));
			g.setColor(new Color(0x24, 0x18, 0x0C, Math.max(0, a)));
			g.drawLine(cx - i, top, cx - i, bottom);
			g.drawLine(cx + i, top, cx + i, bottom);
		}
		// The crease, with a thin paper highlight to either side.
		g.setColor(new Color(0x22, 0x16, 0x0A));
		g.drawLine(cx, top, cx, bottom);
		g.setColor(new Color(255, 255, 255, 70));
		g.drawLine(cx - 2, top, cx - 2, bottom);
		g.drawLine(cx + 2, top, cx + 2, bottom);

		// Binding stitches down the spine.
		g.setStroke(new java.awt.BasicStroke(1.5f));
		g.setColor(new Color(0x6E, 0x5A, 0x3E, 150));
		for (int sy = top + 16; sy < bottom - 10; sy += 24)
		{
			g.drawLine(cx - 3, sy, cx + 3, sy);
		}
		g.setStroke(new java.awt.BasicStroke(1f));

		// Outer page-stack edges — the book's thickness seen from the side.
		for (int i = 0; i < 4; i++)
		{
			g.setColor(new Color(0x24, 0x18, 0x0C, Math.max(8, 38 - i * 8)));
			g.drawLine(5 + i, top + 4, 5 + i, bottom - 4);
			g.drawLine(WIDTH - 6 - i, top + 4, WIDTH - 6 - i, bottom - 4);
		}
	}

	private void drawContent(Graphics2D g, Tab tab)
	{
		int x = 3 + PAD;
		int top = 3 + TITLE_H + (bookMode ? 0 : TAB_H) + PAD;
		int w = WIDTH - 6 - PAD * 2;
		int bottom = HEIGHT - FOOTER_H - 6;
		if (tab == null)
		{
			return;
		}

		int y = top;
		for (Block b : tab.blocks)
		{
			if (y >= bottom)
			{
				g.setFont(FontManager.getRunescapeFont());
				g.setColor(RogueScapeTheme.TEXT_MUTED);
				g.drawString("...", x, y);
				break;
			}
			y = drawBlock(g, b, x, y, w, bottom);
		}
	}

	/** Renders one block at {@code (x,y)} within width {@code w}; returns the next y cursor. */
	private int drawBlock(Graphics2D g, Block b, int x, int y, int w, int bottom)
	{
		switch (b.kind)
		{
			case GAP:
				return y + 8;
			case HEADING:
			{
				// Journal style: small-caps serif ink over a faint rule.
				g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 13));
				String head = b.text == null ? "" : b.text.toUpperCase();
				g.setColor(RogueScapeTheme.INK);
				g.drawString(head, x, y + 12);
				RogueScapePaper.leader(g, x + g.getFontMetrics().stringWidth(head) + 8, y + 8, x + w);
				return y + 20;
			}
			case NOTE:
			case TEXT:
			{
				// Serif handwriting; Swing fonts carry the check/cross glyphs (the panel
				// already renders them raw), so no ascii() munging here.
				boolean note = b.kind == Block.Kind.NOTE;
				g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.PLAIN, note ? 12 : 13));
				FontMetrics fm = g.getFontMetrics();
				int yy = y;
				for (String line : wrap(b.text == null ? "" : b.text, fm, w))
				{
					if (yy >= bottom)
					{
						break;
					}
					g.setColor(b.color);
					g.drawString(line, x, yy + 11);
					yy += note ? 13 : LINE_H;
				}
				return yy;
			}
			case STATBAR:
				return drawStatBar(g, b, x, y, w);
			case ITEMGRID:
				return drawItemGrid(g, b, x, y, w, bottom);
			case BADGE:
				return drawBadge(g, b, x, y, w);
			case CARDS:
				return drawCards(g, b, x, y, w, bottom);
			case MODE_TILES:
				return drawModeTiles(g, b, x, y, w, bottom);
			case CHAPTERS:
				return drawChapters(g, b, x, y, w);
			case PAGE_TITLE:
				return drawPageTitle(g, b, x, y, w);
			case COLUMNS:
				return drawColumns(g, b, x, y, w, bottom);
			case HOURGLASS:
				return drawHourglass(g, b, x, y);
			default:
				return y;
		}
	}

	private int drawStatBar(Graphics2D g, Block b, int x, int y, int w)
	{
		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		g.drawString(ascii(b.text), x, y + 10);
		if (!b.value.isEmpty())
		{
			String v = ascii(b.value);
			g.setColor(RogueScapeTheme.TEXT_PRIMARY);
			g.drawString(v, x + w - fm.stringWidth(v), y + 10);
		}
		int barY = y + 14;
		int barH = 12;
		g.setColor(RogueScapeTheme.BAR_TRACK);
		g.fillRoundRect(x, barY, w, barH, 5, 5);
		int fillW = (int) Math.round((w - 2) * b.frac);
		if (fillW > 0)
		{
			g.setColor(b.color);
			g.fillRoundRect(x + 1, barY + 1, fillW, barH - 2, 4, 4);
			g.setColor(RogueScapeTheme.lighten(b.color, 35));
			g.fillRoundRect(x + 1, barY + 1, fillW, Math.max(2, (barH - 2) / 2), 4, 4);
		}
		g.setColor(RogueScapeTheme.BORDER);
		g.drawRoundRect(x, barY, w, barH, 5, 5);
		return barY + barH + 8;
	}

	private int drawItemGrid(Graphics2D g, Block b, int x, int y, int w, int bottom)
	{
		int slot = 36;
		int gap = 6;
		int cols = Math.max(1, (w + gap) / (slot + gap));
		int col = 0;
		int sx = x;
		int sy = y;
		for (int i = 0; i < b.items.length; i++)
		{
			if (sy + slot > bottom)
			{
				break;
			}
			int cellX = sx + col * (slot + gap);
			boolean locked = i >= b.lockedFrom;
			// Slot frame.
			g.setColor(RogueScapeTheme.SECTION_BG);
			g.fillRoundRect(cellX, sy, slot, slot, 6, 6);
			g.setColor(locked ? RogueScapeFrame.darken(RogueScapeTheme.BORDER, 30) : RogueScapeTheme.BORDER);
			g.drawRoundRect(cellX, sy, slot, slot, 6, 6);

			int id = b.items[i];
			if (locked)
			{
				drawLock(g, cellX + slot / 2, sy + slot / 2);
			}
			else if (id > 0 && icons != null)
			{
				RogueScapeIcons.draw(g, icons.item(id), cellX + 2, sy + 2, slot - 4);
			}

			col++;
			if (col >= cols)
			{
				col = 0;
				sy += slot + gap;
			}
		}
		if (col > 0)
		{
			sy += slot + gap;
		}
		return sy + 4;
	}

	private int drawBadge(Graphics2D g, Block b, int x, int y, int w)
	{
		int h = 30;
		// Paper note with a wax-colored spine on the left.
		g.setColor(RogueScapeTheme.PAPER_CARD);
		g.fillRoundRect(x, y, w, h, 7, 7);
		g.setColor(b.color);
		g.fillRoundRect(x, y, 5, h, 7, 7);
		g.setColor(RogueScapeFrame.darken(b.color, 30));
		g.drawRoundRect(x, y, w, h, 7, 7);

		int textX = x + 12;
		if (icons != null && b.iconItemId > 0)
		{
			RogueScapeIcons.draw(g, icons.item(b.iconItemId), x + 5, y + 5, h - 10);
			textX = x + h;
		}
		g.setFont(FontManager.getRunescapeBoldFont());
		g.setColor(RogueScapeFrame.darken(b.color, 60));
		g.drawString(ascii(b.text), textX, y + 13);
		if (!b.sub.isEmpty())
		{
			g.setFont(FontManager.getRunescapeFont());
			g.setColor(RogueScapeTheme.TEXT_MUTED);
			g.drawString(ascii(b.sub), textX, y + 25);
		}
		return y + h + 6;
	}

	private int drawCards(Graphics2D g, Block b, int x, int y, int w, int bottom)
	{
		int n = b.cards == null ? 0 : b.cards.size();
		if (n == 0)
		{
			return y;
		}
		int gap = 8;
		int cardW = (w - gap * (n - 1)) / n;
		int cardH = Math.min(190, bottom - y);
		for (int i = 0; i < n; i++)
		{
			Rectangle r = new Rectangle(x + i * (cardW + gap), y, cardW, cardH);
			RogueScapeRewardOverlay.drawCard(g, icons, b.cards.get(i), r, false, false);
		}
		return y + cardH + 6;
	}

	private int drawModeTiles(Graphics2D g, Block b, int x, int y, int w, int bottom)
	{
		int n = b.modeTiles == null ? 0 : b.modeTiles.size();
		if (n == 0)
		{
			return y;
		}
		int cols = Math.min(5, Math.max(1, n));
		int gap = 10;
		int tileW = (w - gap * (cols - 1)) / cols;
		int tileH = Math.min(116, bottom - y);
		g.setFont(FontManager.getRunescapeBoldFont());
		for (int i = 0; i < n; i++)
		{
			ModeTile tile = b.modeTiles.get(i);
			int row = i / cols;
			int col = i % cols;
			int tx = x + col * (tileW + gap);
			int ty = y + row * (tileH + gap);
			if (ty + tileH > bottom)
			{
				break;
			}
			Rectangle r = new Rectangle(tx, ty, tileW, tileH);
			modeTileRects.add(r);
			modeTileActions.add(tile.actionId);
			drawModeTile(g, tile, r, modeTileRects.size() - 1 == hoverModeTile);
		}
		int rows = (n + cols - 1) / cols;
		return y + rows * tileH + Math.max(0, rows - 1) * gap + 8;
	}

	/** THE RECORD: the run's chapters as a table of contents (windowed for long runs). */
	private int drawChapters(Graphics2D g, Block b, int x, int y, int w)
	{
		List<com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter> chapters =
			b.chapters == null ? new ArrayList<>() : b.chapters;
		int lineH = 26;
		if (chapters.isEmpty())
		{
			g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.ITALIC, 13));
			g.setColor(RogueScapeTheme.INK_FADED);
			g.drawString("No chapters yet.", x, y + 13);
			return y + lineH;
		}
		int cur = 0;
		for (int i = 0; i < chapters.size(); i++)
		{
			if (chapters.get(i).isCurrent())
			{
				cur = i;
				break;
			}
		}
		int from = 0;
		int to = chapters.size();
		if (chapters.size() > 10)
		{
			from = Math.max(0, cur - 2);
			to = Math.min(chapters.size(), from + 6);
			from = Math.max(0, to - 6);
			if (from > 0)
			{
				g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.ITALIC, 12));
				g.setColor(RogueScapeTheme.INK_FADED);
				g.drawString("… " + from + " chapters stamped …", x + 12, y + 12);
			}
			y += 18;
		}
		for (int i = from; i < to; i++)
		{
			drawChapterLine(g, x, y, w, chapters.get(i));
			y += lineH;
		}
		if (to < chapters.size())
		{
			g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.ITALIC, 12));
			g.setColor(RogueScapeTheme.INK_FADED);
			g.drawString("… " + (chapters.size() - to) + " chapters unwritten …", x + 12, y + 12);
			y += 18;
		}
		return y + 4;
	}

	private void drawChapterLine(Graphics2D g, int x, int y, int w,
		com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter c)
	{
		boolean done = c.isDone();
		boolean boss = c.isBoss();
		g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 13));
		g.setColor(boss ? RogueScapeTheme.STAMP : RogueScapeTheme.INK);
		g.drawString(c.numeral() + ".", x, y + 12);
		g.setFont(new java.awt.Font(java.awt.Font.SERIF, boss ? java.awt.Font.BOLD : java.awt.Font.PLAIN, 13));
		g.setColor(done ? RogueScapeTheme.INK_FADED : boss ? RogueScapeTheme.STAMP : RogueScapeTheme.INK);
		int nameX = x + 52;
		java.awt.FontMetrics fm = g.getFontMetrics();
		String name = c.name();
		int maxName = w - 52 - 36;
		while (fm.stringWidth(name) > maxName && name.length() > 4)
		{
			name = name.substring(0, name.length() - 2);
		}
		if (!name.equals(c.name()))
		{
			name = name + "…";
		}
		g.drawString(name, nameX, y + 12);
		int nameEnd = nameX + fm.stringWidth(name);
		if (done)
		{
			g.setStroke(new java.awt.BasicStroke(1.4f));
			g.drawLine(x + 50, y + 8, nameEnd + 2, y + 8);
		}
		RogueScapePaper.leader(g, nameEnd + 6, y + 9, x + w - 28);
		int sx = x + w - 13;
		int sy = y + 7;
		if (done)
		{
			RogueScapePaper.clearStamp(g, sx, sy, 11, "CLEAR");
		}
		else if (c.isCurrent())
		{
			RogueScapePaper.ribbon(g, sx, y - 2, 13, 20);
		}
		else
		{
			RogueScapePaper.stampSlot(g, sx, sy, 10, boss);
		}
	}

	/** Diary masthead: ribbon hanging from the page top, serif title, italic sub, ink rule. */
	private int drawPageTitle(Graphics2D g, Block b, int x, int y, int w)
	{
		RogueScapePaper.ribbon(g, x + 10, y - 6, 16, 34);
		g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 19));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(b.text == null ? "" : b.text, x + 30, y + 16);
		if (!b.sub.isEmpty())
		{
			g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.ITALIC, 12));
			g.setColor(RogueScapeTheme.INK_FADED);
			g.drawString(b.sub, x + 30, y + 31);
		}
		RogueScapePaper.inkRule(g, x, y + 38, w);
		return y + 46;
	}

	/** Two block columns side by side; the cursor resumes below the taller one. */
	private int drawColumns(Graphics2D g, Block b, int x, int y, int w, int bottom)
	{
		int leftW = bookMode ? w / 2 - 20 : (int) (w * 0.56) - 8;
		int rightX = bookMode ? x + w / 2 + 20 : x + leftW + 16;
		int rightW = w - (rightX - x);
		int ly = y;
		for (Block child : b.left)
		{
			if (ly >= bottom)
			{
				break;
			}
			ly = drawBlock(g, child, x, ly, leftW, bottom);
		}
		int ry = y;
		for (Block child : b.right)
		{
			if (ry >= bottom)
			{
				break;
			}
			ry = drawBlock(g, child, rightX, ry, rightW, bottom);
		}
		return Math.max(ly, ry);
	}

	/** Hourglass doodle + elapsed time. */
	private int drawHourglass(Graphics2D g, Block b, int x, int y)
	{
		g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 12));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(b.text == null ? "" : b.text.toUpperCase(), x, y + 11);
		int gy = y + 18;
		g.setStroke(new java.awt.BasicStroke(1.6f));
		g.setColor(RogueScapeTheme.INK_FADED);
		g.drawLine(x + 4, gy, x + 24, gy);
		g.drawLine(x + 4, gy + 24, x + 24, gy + 24);
		g.drawLine(x + 4, gy, x + 24, gy + 24);
		g.drawLine(x + 24, gy, x + 4, gy + 24);
		g.setStroke(new java.awt.BasicStroke(1f));
		g.setFont(new java.awt.Font(java.awt.Font.SERIF, java.awt.Font.BOLD, 16));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(b.value == null ? "" : b.value, x + 34, gy + 17);
		return gy + 32;
	}

	private static void drawModeTile(Graphics2D g, ModeTile tile, Rectangle r, boolean hover)
	{
		Color c = tile.color;
		g.setPaint(new java.awt.GradientPaint(0, r.y, RogueScapeTheme.SECTION_HEADER_BG,
			0, r.y + r.height, RogueScapeFrame.darken(RogueScapeTheme.PANEL_BG, 4)));
		g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
		if (tile.selected || hover)
		{
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), tile.selected ? 48 : 28));
			g.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 7, 7);
		}
		g.setColor(tile.selected ? RogueScapeTheme.lighten(c, 40) : hover ? RogueScapeTheme.lighten(c, 20) : RogueScapeTheme.BORDER);
		g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
		drawDiamond(g, r.x + r.width / 2, r.y + 20, 10, RogueScapeFrame.darken(c, 45), c);

		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics fm = g.getFontMetrics();
		String title = clipTo(ascii(tile.title), fm, r.width - 10);
		g.setColor(RogueScapeFrame.SHADOW);
		g.drawString(title, r.x + (r.width - fm.stringWidth(title)) / 2 + 1, r.y + 47);
		g.setColor(tile.selected ? RogueScapeTheme.GOLD : RogueScapeTheme.TEXT_PRIMARY);
		g.drawString(title, r.x + (r.width - fm.stringWidth(title)) / 2, r.y + 46);

		g.setFont(FontManager.getRunescapeFont());
		fm = g.getFontMetrics();
		int ly = r.y + 64;
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		for (String line : wrap(ascii(tile.subtitle), fm, r.width - 12))
		{
			if (ly > r.y + r.height - 22)
			{
				break;
			}
			g.drawString(line, r.x + (r.width - fm.stringWidth(line)) / 2, ly);
			ly += LINE_H;
		}
		if (!tile.detail.isEmpty())
		{
			String detail = clipTo(ascii(tile.detail), fm, r.width - 12);
			g.setColor(tile.selected ? RogueScapeTheme.lighten(c, 55) : RogueScapeTheme.GOLD_DIM);
			g.drawString(detail, r.x + (r.width - fm.stringWidth(detail)) / 2, r.y + r.height - 8);
		}
	}

	private void drawFooter(Graphics2D g)
	{
		int y0 = HEIGHT - FOOTER_H - 3;
		int x0 = 3, w = WIDTH - 6;
		g.setColor(RogueScapeFrame.darken(RogueScapeTheme.PANEL_BG, 2));
		g.fillRect(x0, y0, w, FOOTER_H);
		g.setColor(RogueScapeTheme.BORDER);
		g.drawLine(x0, y0, x0 + w - 1, y0);
		g.setFont(FontManager.getRunescapeFont());
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		g.drawString("Drag to move  -  Right-click for menu", PAD, y0 + 13);
	}

	private static void drawLock(Graphics2D g, int cx, int cy)
	{
		g.setColor(RogueScapeTheme.TEXT_MUTED);
		g.fillRect(cx - 5, cy - 1, 10, 8);
		g.drawArc(cx - 4, cy - 8, 8, 9, 0, 180);
	}

	private static void drawDiamond(Graphics2D g, int cx, int cy, int r, Color outer, Color inner)
	{
		Polygon p = new Polygon();
		p.addPoint(cx, cy - r);
		p.addPoint(cx + r, cy);
		p.addPoint(cx, cy + r);
		p.addPoint(cx - r, cy);
		g.setColor(outer);
		g.fillPolygon(p);
		int ir = Math.max(1, r - 3);
		Polygon q = new Polygon();
		q.addPoint(cx, cy - ir);
		q.addPoint(cx + ir, cy);
		q.addPoint(cx, cy + ir);
		q.addPoint(cx - ir, cy);
		g.setColor(inner);
		g.fillPolygon(q);
	}

	private static String clipTo(String s, FontMetrics fm, int maxWidth)
	{
		if (fm.stringWidth(s) <= maxWidth)
		{
			return s;
		}
		while (s.length() > 1 && fm.stringWidth(s + ".") > maxWidth)
		{
			s = s.substring(0, s.length() - 1);
		}
		return s + ".";
	}

	private static List<String> wrap(String text, FontMetrics fm, int maxWidth)
	{
		List<String> out = new ArrayList<>();
		if (text == null || text.isEmpty())
		{
			out.add("");
			return out;
		}
		StringBuilder cur = new StringBuilder();
		for (String word : text.split(" "))
		{
			String candidate = cur.length() == 0 ? word : cur + " " + word;
			if (fm.stringWidth(candidate) > maxWidth && cur.length() > 0)
			{
				out.add(cur.toString());
				cur = new StringBuilder(word);
			}
			else
			{
				cur = new StringBuilder(candidate);
			}
		}
		if (cur.length() > 0)
		{
			out.add(cur.toString());
		}
		return out;
	}

	/** Replaces glyphs the OSRS font can't render, then strips remaining non-ASCII. */
	static String ascii(String s)
	{
		if (s == null)
		{
			return "";
		}
		String r = s.replace("═", "=").replace("•", "-").replace("▸", ">").replace("▾", "v")
			.replace("✓", "+").replace("✗", "!").replace("✦", "").replace("→", "->")
			.replace("~ ", "  ").replace("…", "...");
		StringBuilder b = new StringBuilder(r.length());
		for (int i = 0; i < r.length(); i++)
		{
			char c = r.charAt(i);
			b.append(c < 128 ? c : ' ');
		}
		return b.toString();
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
		if (!open)
		{
			return e;
		}
		Point local = toLocal(e);
		if (local == null)
		{
			return e;
		}
		if (closeRect.contains(local))
		{
			open = false;
			e.consume();
			return e;
		}
		for (int i = 0; i < tabRects.size(); i++)
		{
			if (tabRects.get(i).contains(local))
			{
				selected = i;
				e.consume();
				return e;
			}
		}
		for (int i = 0; i < modeTileRects.size(); i++)
		{
			if (modeTileRects.get(i).contains(local))
			{
				if (modeTileHandler != null && i < modeTileActions.size())
				{
					modeTileHandler.accept(modeTileActions.get(i));
				}
				e.consume();
				return e;
			}
		}
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		hoverTab = -1;
		hoverModeTile = -1;
		hoverClose = false;
		if (!open)
		{
			return e;
		}
		Point local = toLocal(e);
		if (local == null)
		{
			return e;
		}
		hoverClose = closeRect.contains(local);
		for (int i = 0; i < tabRects.size(); i++)
		{
			if (tabRects.get(i).contains(local))
			{
				hoverTab = i;
				break;
			}
		}
		for (int i = 0; i < modeTileRects.size(); i++)
		{
			if (modeTileRects.get(i).contains(local))
			{
				hoverModeTile = i;
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
		hoverTab = -1;
		hoverModeTile = -1;
		hoverClose = false;
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		return e;
	}
}
