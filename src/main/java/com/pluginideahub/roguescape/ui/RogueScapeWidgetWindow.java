package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.ui.RogueScapeWindowOverlay.Block;
import com.pluginideahub.roguescape.ui.RogueScapeWindowOverlay.Tab;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RogueScape's in-game window built from real client {@link Widget}s (like the Collection Log),
 * NOT a Graphics2D overlay. It attaches a framed container to the viewport, draws the frame from
 * the Collection Log's own native sprites, lays a row of clickable top tabs under the title bar,
 * and renders each tab's content ({@link Block}s: headings, text, stat bars, native item-icon
 * grids, badges, reward cards) as child widgets. Title-bar click-drag and a native close button
 * are wired through the registered {@link MouseListener} / widget ops.
 *
 * <p>Dynamically-created widgets are wiped whenever the client rebuilds the interface, so the
 * window is rebuilt tick-driven via {@link #onTick()} (same approach as the journal injection).
 * All widget mutation runs on the client thread; mouse events (which fire on AWT) marshal across.
 */
public final class RogueScapeWidgetWindow implements MouseListener
{
	private static final int W = 560;
	private static final int H = 350;
	private static final int TITLE_H = 28; // title-bar / drag region height
	private static final int TAB_TOP = 36; // tab strip top (clears the title-bar sprite)
	private static final int TAB_H = 22;
	private static final int CW = 25; // frame corner width
	private static final int CH = 30; // frame corner height

	// Content region (relative to the window origin).
	private static final int CONTENT_X = 16;
	private static final int CONTENT_TOP = TAB_TOP + TAB_H + 8;
	private static final int CONTENT_W = W - 32;
	private static final int CONTENT_BOTTOM = H - 16;
	private static final int LINE_H = 15;

	// Collection Log frame sprites (captured live from interface group 621).
	private static final int SP_BG = 297;
	private static final int SP_TL = 310, SP_TR = 311, SP_BL = 312, SP_BR = 313;
	private static final int SP_TOP = 314, SP_BOTTOM = 173, SP_LEFT = 172, SP_RIGHT = 315;
	private static final int SP_TITLEBAR = 2546;
	private static final int SP_CLOSE = 535;

	// Palette (RGB, matching the overlay theme so both windows read the same).
	private static final int COL_GOLD = 0xff981f;
	private static final int COL_PRIMARY = 0xc8c8c8;
	private static final int COL_MUTED = 0x9f9f8f;
	private static final int COL_HEADING_RULE = 0x5a5340;
	private static final int COL_SLOT_BG = 0x1c1c18;
	private static final int COL_SLOT_BORDER = 0x3a382e;
	private static final int COL_BAR_TRACK = 0x2a281f;

	private static int rgb(Color c)
	{
		return c == null ? COL_PRIMARY : (c.getRGB() & 0xFFFFFF);
	}

	private static final Logger log = LoggerFactory.getLogger(RogueScapeWidgetWindow.class);

	private final Client client;
	private final ClientThread clientThread;
	private final BooleanSupplier enabled;
	private final Supplier<List<Tab>> tabsSupplier;
	private String diag = "";

	private boolean wantOpen;
	private int winX = 40;
	private int winY = 40;
	private int selectedTab;

	private Widget parent;
	private Widget root;
	private Widget contentLayer;
	private Widget tabStripLayer;
	private final List<Widget> tabWidgets = new ArrayList<>();
	// Window-local hit rectangles (used by the MouseListener to handle clicks ourselves and to
	// swallow every event inside the window so nothing falls through to the game world).
	private final List<Rectangle> tabRects = new ArrayList<>();
	private final Rectangle closeRect = new Rectangle();
	private boolean dragging;
	private int grabDx;
	private int grabDy;

	public RogueScapeWidgetWindow(Client client, ClientThread clientThread, BooleanSupplier enabled,
		Supplier<List<Tab>> tabsSupplier)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.enabled = enabled;
		this.tabsSupplier = tabsSupplier;
	}

	public boolean isOpen()
	{
		return wantOpen;
	}

	public void toggle()
	{
		wantOpen = !wantOpen;
	}

	public void setOpen(boolean open)
	{
		wantOpen = open;
	}

	/** Tick-safe: build/show the window when open, hide it when closed, rebuild if the client wiped it. */
	public void onTick()
	{
		if (client == null || enabled == null || !enabled.getAsBoolean())
		{
			return;
		}
		try
		{
			parent = resolveRoot();
			if (parent == null)
			{
				root = null;
				if (wantOpen)
				{
					diag("open=true but NO viewport parent resolved (tried groups 161/164/548)");
				}
				return;
			}
			if (!wantOpen)
			{
				if (root != null)
				{
					root.setHidden(true);
				}
				return;
			}
			if (!attached(parent))
			{
				build(parent);
				diag("built window: parentGroup=" + (parent.getId() >> 16)
					+ " tabs=" + tabWidgets.size() + " at (" + winX + "," + winY + ")");
			}
			else
			{
				root.setHidden(false);
			}
		}
		catch (RuntimeException ex)
		{
			root = null;
			log.debug("[RogueScape widget] rebuild failed", ex);
		}
	}

	public void shutDown()
	{
		try
		{
			if (root != null)
			{
				root.setHidden(true);
			}
		}
		catch (RuntimeException ignored)
		{
			// best-effort
		}
		root = null;
		parent = null;
		contentLayer = null;
		tabWidgets.clear();
		wantOpen = false;
	}

	/** First present viewport container across the three client modes (resizable x2, fixed). */
	private Widget resolveRoot()
	{
		Widget w = client.getWidget(InterfaceID.ToplevelOsrsStretch.HUD_CONTAINER_FRONT);
		if (w == null)
		{
			w = client.getWidget(InterfaceID.ToplevelPreEoc.HUD_CONTAINER_FRONT);
		}
		if (w == null)
		{
			w = client.getWidget(InterfaceID.Toplevel.MAIN);
		}
		return w;
	}

	/**
	 * True if our window root is still a live child of the viewport. Scans every child array the
	 * client might track it under (dynamic / static / nested) — checking only one of them is what
	 * caused the window to be rebuilt (and orphaned, stacking copies) every tick.
	 */
	private boolean attached(Widget host)
	{
		if (root == null || host == null)
		{
			return false;
		}
		return contains(host.getDynamicChildren(), root)
			|| contains(host.getChildren(), root)
			|| contains(host.getNestedChildren(), root);
	}

	private void build(Widget host)
	{
		// Hide any previous root before we drop the reference, so a re-build never leaves a stale
		// copy of the window visible behind the new one.
		if (root != null)
		{
			try
			{
				root.setHidden(true);
			}
			catch (RuntimeException ignored)
			{
				// best-effort
			}
		}

		tabWidgets.clear();
		contentLayer = null;
		tabStripLayer = null;

		root = host.createChild(-1, WidgetType.LAYER);
		root.setOriginalX(winX);
		root.setOriginalY(winY);
		root.setOriginalWidth(W);
		root.setOriginalHeight(H);
		root.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		root.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		root.setHidden(false);
		root.revalidate();

		// Window frame cloned from the real Collection Log's own sprites (9-slice + title bar).
		sprite(root, SP_BG, 2, 2, W - 4, H - 4, true);            // tiled parchment background
		sprite(root, SP_TOP, CW, 0, W - 2 * CW, CH, true);        // edges (tiled)
		sprite(root, SP_BOTTOM, CW, H - CH, W - 2 * CW, CH, true);
		sprite(root, SP_LEFT, 0, CH, CW, H - 2 * CH, true);
		sprite(root, SP_RIGHT, W - CW, CH, CW, H - 2 * CH, true);
		sprite(root, SP_TL, 0, 0, CW, CH, false);                 // corners (fixed)
		sprite(root, SP_TR, W - CW, 0, CW, CH, false);
		sprite(root, SP_BL, 0, H - CH, CW, CH, false);
		sprite(root, SP_BR, W - CW, H - CH, CW, CH, false);
		sprite(root, SP_TITLEBAR, 5, 7, W - 10, 26, false);       // title bar strip

		// Title text (orange, like the Collection Log header).
		Widget titleText = root.createChild(-1, WidgetType.TEXT);
		titleText.setText("RogueScape");
		titleText.setTextColor(COL_GOLD);
		titleText.setFontId(FontID.BOLD_12);
		titleText.setTextShadowed(true);
		fill(titleText, 14, 9, 200, 16);

		// Close button (the game's own close sprite). Clicks are handled by the MouseListener so
		// they're consumed before the game world sees them (no walk-through).
		Widget close = root.createChild(-1, WidgetType.GRAPHIC);
		close.setSpriteId(SP_CLOSE);
		close.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		close.setOriginalX(8);
		close.setOriginalY(6);
		close.setOriginalWidth(26);
		close.setOriginalHeight(23);
		close.setNoClickThrough(true);
		close.revalidate();
		closeRect.setBounds(W - 8 - 26, 6, 26, 23);

		List<Tab> tabs = tabs();
		if (selectedTab >= tabs.size())
		{
			selectedTab = 0;
		}
		rebuildTabStrip(tabs);
		rebuildContent(tabs);
		root.revalidate();
	}

	// ------------------------------------------------------------ tabs

	private void rebuildTabStrip(List<Tab> tabs)
	{
		if (root == null)
		{
			return;
		}
		if (tabStripLayer != null)
		{
			tabStripLayer.setHidden(true);
		}
		tabStripLayer = root.createChild(-1, WidgetType.LAYER);
		fill(tabStripLayer, 0, 0, W, H);

		tabWidgets.clear();
		tabRects.clear();
		int n = Math.max(1, tabs.size());
		int stripY = TAB_TOP;
		int stripX = CW;
		int stripW = W - 2 * CW;
		int tabW = stripW / n;

		// Recessed strip backing.
		Widget strip = tabStripLayer.createChild(-1, WidgetType.RECTANGLE);
		strip.setFilled(true);
		strip.setTextColor(0x000000);
		strip.setOpacity(170);
		fill(strip, stripX, stripY, stripW, TAB_H);

		for (int i = 0; i < tabs.size(); i++)
		{
			int tx = stripX + i * tabW;
			int tw = (i == tabs.size() - 1) ? (stripX + stripW - tx) : tabW;
			boolean sel = i == selectedTab;

			// Selected-tab highlight pill behind the label.
			if (sel)
			{
				Widget hl = tabStripLayer.createChild(-1, WidgetType.RECTANGLE);
				hl.setFilled(true);
				hl.setTextColor(COL_GOLD);
				hl.setOpacity(210);
				fill(hl, tx + 2, stripY + TAB_H - 3, tw - 4, 2);
			}

			Widget tab = tabStripLayer.createChild(-1, WidgetType.TEXT);
			tab.setText(shorten(tabs.get(i).name()));
			tab.setTextColor(sel ? COL_GOLD : COL_MUTED);
			tab.setFontId(FontID.PLAIN_11);
			tab.setTextShadowed(true);
			tab.setXTextAlignment(WidgetTextAlignment.CENTER);
			tab.setYTextAlignment(WidgetTextAlignment.CENTER);
			tab.setNoClickThrough(true);
			fill(tab, tx, stripY, tw, TAB_H);
			tabWidgets.add(tab);
			tabRects.add(new Rectangle(tx, stripY, tw, TAB_H));
		}

		// Gold rule under the strip.
		Widget rule = tabStripLayer.createChild(-1, WidgetType.RECTANGLE);
		rule.setFilled(true);
		rule.setTextColor(COL_HEADING_RULE);
		fill(rule, stripX, stripY + TAB_H, stripW, 1);
	}

	private void selectTab(int idx)
	{
		selectedTab = idx;
		for (int i = 0; i < tabWidgets.size(); i++)
		{
			tabWidgets.get(i).setTextColor(i == idx ? COL_GOLD : COL_MUTED);
			tabWidgets.get(i).revalidate();
		}
		rebuildTabStrip(tabs());
		rebuildContent(tabs());
	}

	// ------------------------------------------------------------ content

	private void rebuildContent(List<Tab> tabs)
	{
		if (root == null)
		{
			return;
		}
		if (contentLayer != null)
		{
			contentLayer.setHidden(true);
		}
		contentLayer = root.createChild(-1, WidgetType.LAYER);
		fill(contentLayer, 0, 0, W, H);
		if (tabs.isEmpty())
		{
			contentLayer.revalidate();
			root.revalidate();
			return;
		}
		Tab tab = tabs.get(Math.min(selectedTab, tabs.size() - 1));
		int y = CONTENT_TOP;
		for (Block b : tab.blocks())
		{
			if (y >= CONTENT_BOTTOM)
			{
				text(contentLayer, CONTENT_X, y, CONTENT_W, "...", COL_MUTED, FontID.PLAIN_12, false);
				break;
			}
			y = drawBlock(b, y);
		}
		contentLayer.revalidate();
		root.revalidate();
	}

	/** Renders one content block starting at window-local y; returns the next y cursor. */
	private int drawBlock(Block b, int y)
	{
		switch (b.kind)
		{
			case GAP:
				return y + 8;
			case HEADING:
			{
				text(contentLayer, CONTENT_X, y, CONTENT_W, RogueScapeWindowOverlay.ascii(b.text),
					COL_GOLD, FontID.BOLD_12, true);
				Widget rule = contentLayer.createChild(-1, WidgetType.RECTANGLE);
				rule.setFilled(true);
				rule.setTextColor(COL_HEADING_RULE);
				fill(rule, CONTENT_X, y + 17, CONTENT_W, 1);
				return y + 24;
			}
			case TEXT:
			{
				return drawWrappedText(RogueScapeWindowOverlay.ascii(b.text), rgb(b.color), y);
			}
			case STATBAR:
				return drawStatBar(b, y);
			case ITEMGRID:
				return drawItemGrid(b, y);
			case BADGE:
				return drawBadge(b, y);
			case CARDS:
				return drawCards(b, y);
			case MODE_TILES:
				return drawModeTiles(b, y);
			case BOSS_BAND:
				return drawBossBand(b, y);
			default:
				return y;
		}
	}

	/**
	 * The boss line-up band with REAL 3D chatheads: each boss card holds a MODEL-type child
	 * widget (WidgetModelType.NPC_CHATHEAD + the boss's NPC id), which the game engine renders
	 * live — the same mechanism as dialog chatheads. Locked bosses are dimmed with a cover
	 * rectangle; felled ones show their name struck (via strikethrough tag).
	 */
	private int drawBossBand(Block b, int y)
	{
		java.util.List<com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter> bosses =
			b.chapters == null ? java.util.Collections.emptyList() : b.chapters;
		if (bosses.isEmpty())
		{
			return y;
		}
		int gap = 6;
		int shown = Math.min(4, bosses.size());
		int cardW = (CONTENT_W - gap * (shown - 1)) / shown;
		int cardH = 74;
		for (int i = 0; i < shown; i++)
		{
			com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter boss = bosses.get(i);
			int px = CONTENT_X + i * (cardW + gap);

			Widget card = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			card.setFilled(true);
			card.setTextColor(boss.isCurrent() ? 0x4A3A24 : 0x3A2E1C);
			card.setOpacity(60);
			fill(card, px, y, cardW, cardH);

			int npcId = com.pluginideahub.roguescape.core.region.BossLibrary.npcIdFor(boss.name());
			if (npcId > 0)
			{
				Widget model = contentLayer.createChild(-1, WidgetType.MODEL);
				model.setModelType(net.runelite.api.widgets.WidgetModelType.NPC_CHATHEAD);
				model.setModelId(npcId);
				model.setModelZoom(796);
				model.setRotationX(40);
				model.setRotationZ(1882);
				fill(model, px + (cardW - 36) / 2, y + 4, 36, 40);
				if (!boss.isDone() && !boss.isCurrent())
				{
					// Locked: dim the head under a translucent cover.
					Widget cover = contentLayer.createChild(-1, WidgetType.RECTANGLE);
					cover.setFilled(true);
					cover.setTextColor(0x14100A);
					cover.setOpacity(120);
					fill(cover, px + 2, y + 2, cardW - 4, cardH - 22);
				}
			}

			String label = boss.isDone()
				? "<str>" + RogueScapeWindowOverlay.ascii(boss.name()) + "</str>"
				: RogueScapeWindowOverlay.ascii(boss.name());
			if (!boss.isDone() && !boss.isCurrent())
			{
				label = "[LOCKED] " + label;
			}
			Widget name = text(contentLayer, px + 2, y + cardH - 18, cardW - 4, label,
				boss.isCurrent() ? COL_GOLD : COL_MUTED, FontID.PLAIN_11, false);
			name.setXTextAlignment(WidgetTextAlignment.CENTER);
		}
		if (bosses.size() > shown)
		{
			text(contentLayer, CONTENT_X + CONTENT_W - 40, y + cardH / 2, 40,
				"+" + (bosses.size() - shown) + " >", COL_MUTED, FontID.PLAIN_11, false);
		}
		return y + cardH + 8;
	}

	private int drawStatBar(Block b, int y)
	{
		text(contentLayer, CONTENT_X, y, CONTENT_W - 80, RogueScapeWindowOverlay.ascii(b.text),
			COL_MUTED, FontID.PLAIN_12, false);
		if (b.value != null && !b.value.isEmpty())
		{
			Widget v = text(contentLayer, CONTENT_X, y, CONTENT_W, RogueScapeWindowOverlay.ascii(b.value),
				COL_PRIMARY, FontID.PLAIN_12, false);
			v.setXTextAlignment(WidgetTextAlignment.RIGHT);
		}
		int barY = y + 14;
		int barH = 11;
		Widget track = contentLayer.createChild(-1, WidgetType.RECTANGLE);
		track.setFilled(true);
		track.setTextColor(COL_BAR_TRACK);
		fill(track, CONTENT_X, barY, CONTENT_W, barH);

		int fillW = (int) Math.round((CONTENT_W - 2) * b.frac);
		if (fillW > 0)
		{
			Widget bar = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bar.setFilled(true);
			bar.setTextColor(rgb(b.color));
			fill(bar, CONTENT_X + 1, barY + 1, fillW, barH - 2);
		}
		Widget border = contentLayer.createChild(-1, WidgetType.RECTANGLE);
		border.setFilled(false);
		border.setTextColor(COL_SLOT_BORDER);
		fill(border, CONTENT_X, barY, CONTENT_W, barH);
		return barY + barH + 8;
	}

	private int drawWrappedText(String value, int color, int y)
	{
		List<String> lines = wrap(value, Math.max(28, CONTENT_W / 6));
		for (String line : lines)
		{
			if (y + LINE_H > CONTENT_BOTTOM)
			{
				break;
			}
			text(contentLayer, CONTENT_X, y, CONTENT_W, line, color, FontID.PLAIN_12, false);
			y += LINE_H;
		}
		return y;
	}

	private int drawItemGrid(Block b, int y)
	{
		int slot = 36;
		int gap = 6;
		int cols = Math.max(1, (CONTENT_W + gap) / (slot + gap));
		int col = 0;
		int sy = y;
		for (int i = 0; i < b.items.length; i++)
		{
			if (sy + slot > CONTENT_BOTTOM)
			{
				break;
			}
			int cellX = CONTENT_X + col * (slot + gap);

			Widget bg = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bg.setFilled(true);
			bg.setTextColor(COL_SLOT_BG);
			bg.setOpacity(60);
			fill(bg, cellX, sy, slot, slot);
			Widget bd = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bd.setFilled(false);
			bd.setTextColor(COL_SLOT_BORDER);
			fill(bd, cellX, sy, slot, slot);

			int id = b.items[i];
			if (id > 0)
			{
				Widget item = contentLayer.createChild(-1, WidgetType.GRAPHIC);
				item.setItemId(id);
				item.setItemQuantity(1);
				item.setItemQuantityMode(0); // never show quantity
				fill(item, cellX + 2, sy + 2, slot - 4, slot - 4);
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

	private int drawBadge(Block b, int y)
	{
		int h = 30;
		Widget bg = contentLayer.createChild(-1, WidgetType.RECTANGLE);
		bg.setFilled(true);
		bg.setTextColor(rgb(b.color));
		bg.setOpacity(180);
		fill(bg, CONTENT_X, y, CONTENT_W, h);
		Widget bd = contentLayer.createChild(-1, WidgetType.RECTANGLE);
		bd.setFilled(false);
		bd.setTextColor(rgb(b.color));
		fill(bd, CONTENT_X, y, CONTENT_W, h);

		int textX = CONTENT_X + 8;
		if (b.iconItemId > 0)
		{
			Widget item = contentLayer.createChild(-1, WidgetType.GRAPHIC);
			item.setItemId(b.iconItemId);
			item.setItemQuantity(1);
			item.setItemQuantityMode(0);
			fill(item, CONTENT_X + 4, y + 3, h - 6, h - 6);
			textX = CONTENT_X + h;
		}
		text(contentLayer, textX, y + 3, CONTENT_W - h, RogueScapeWindowOverlay.ascii(b.text),
			COL_PRIMARY, FontID.BOLD_12, false);
		if (b.sub != null && !b.sub.isEmpty())
		{
			text(contentLayer, textX, y + 16, CONTENT_W - h, RogueScapeWindowOverlay.ascii(b.sub),
				COL_MUTED, FontID.PLAIN_11, false);
		}
		return y + h + 6;
	}

	private int drawCards(Block b, int y)
	{
		int n = b.cards == null ? 0 : b.cards.size();
		if (n == 0)
		{
			return y;
		}
		int gap = 8;
		int cardW = (CONTENT_W - gap * (n - 1)) / n;
		int cardH = Math.min(160, CONTENT_BOTTOM - y);
		for (int i = 0; i < n; i++)
		{
			RogueScapeRewardOverlay.Card card = b.cards.get(i);
			int cx = CONTENT_X + i * (cardW + gap);

			Widget bg = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bg.setFilled(true);
			bg.setTextColor(COL_SLOT_BG);
			bg.setOpacity(40);
			fill(bg, cx, y, cardW, cardH);
			Widget bd = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bd.setFilled(false);
			bd.setTextColor(COL_GOLD);
			fill(bd, cx, y, cardW, cardH);

			Widget title = text(contentLayer, cx + 4, y + 5, cardW - 8,
				RogueScapeWindowOverlay.ascii(card.title()), COL_GOLD, FontID.BOLD_12, false);
			title.setXTextAlignment(WidgetTextAlignment.CENTER);

			if (card.iconItemId() > 0)
			{
				Widget item = contentLayer.createChild(-1, WidgetType.GRAPHIC);
				item.setItemId(card.iconItemId());
				item.setItemQuantity(1);
				item.setItemQuantityMode(0);
				fill(item, cx + (cardW - 36) / 2, y + 24, 36, 36);
			}

			int ly = y + 66;
			for (String line : card.lines())
			{
				if (ly + LINE_H > y + cardH - 4)
				{
					break;
				}
				Widget t = text(contentLayer, cx + 4, ly, cardW - 8,
					RogueScapeWindowOverlay.ascii(line), COL_PRIMARY, FontID.PLAIN_11, false);
				t.setXTextAlignment(WidgetTextAlignment.CENTER);
				ly += LINE_H;
			}
		}
		return y + cardH + 6;
	}

	private int drawModeTiles(Block b, int y)
	{
		int n = b.modeTiles == null ? 0 : b.modeTiles.size();
		if (n == 0)
		{
			return y;
		}
		int cols = Math.min(5, Math.max(1, n));
		int gap = 8;
		int tileW = (CONTENT_W - gap * (cols - 1)) / cols;
		int tileH = Math.min(100, CONTENT_BOTTOM - y);
		for (int i = 0; i < n; i++)
		{
			RogueScapeWindowOverlay.ModeTile tile = b.modeTiles.get(i);
			int row = i / cols;
			int col = i % cols;
			int tx = CONTENT_X + col * (tileW + gap);
			int ty = y + row * (tileH + gap);
			if (ty + tileH > CONTENT_BOTTOM)
			{
				break;
			}

			Widget bg = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bg.setFilled(true);
			bg.setTextColor(rgb(RogueScapeFrame.darken(RogueScapeTheme.SECTION_BG, 2)));
			bg.setOpacity(tile.selected ? 45 : 75);
			fill(bg, tx, ty, tileW, tileH);
			Widget bd = contentLayer.createChild(-1, WidgetType.RECTANGLE);
			bd.setFilled(false);
			bd.setTextColor(tile.selected ? rgb(RogueScapeTheme.lighten(tile.color, 35)) : COL_SLOT_BORDER);
			fill(bd, tx, ty, tileW, tileH);

			Widget title = text(contentLayer, tx + 4, ty + 14, tileW - 8,
				RogueScapeWindowOverlay.ascii(tile.title), tile.selected ? COL_GOLD : COL_PRIMARY,
				FontID.BOLD_12, true);
			title.setXTextAlignment(WidgetTextAlignment.CENTER);

			Widget subtitle = text(contentLayer, tx + 6, ty + 36, tileW - 12,
				RogueScapeWindowOverlay.ascii(tile.subtitle), COL_MUTED, FontID.PLAIN_11, false);
			subtitle.setXTextAlignment(WidgetTextAlignment.CENTER);

			if (tile.detail != null && !tile.detail.isEmpty())
			{
				Widget detail = text(contentLayer, tx + 6, ty + tileH - 20, tileW - 12,
					RogueScapeWindowOverlay.ascii(tile.detail), rgb(tile.color), FontID.PLAIN_11, false);
				detail.setXTextAlignment(WidgetTextAlignment.CENTER);
			}
		}
		int rows = (n + cols - 1) / cols;
		return y + rows * tileH + Math.max(0, rows - 1) * gap + 8;
	}

	// ------------------------------------------------------------ widget helpers

	/** Creates a sprite child at the given bounds; tiled sprites repeat instead of stretching. */
	private void sprite(Widget host, int spriteId, int x, int y, int w, int h, boolean tile)
	{
		Widget g = host.createChild(-1, WidgetType.GRAPHIC);
		g.setSpriteId(spriteId);
		if (tile)
		{
			g.setSpriteTiling(true);
		}
		fill(g, x, y, w, h);
	}

	private Widget text(Widget host, int x, int y, int w, String value, int color, int fontId, boolean shadow)
	{
		Widget t = host.createChild(-1, WidgetType.TEXT);
		t.setText(value == null ? "" : value);
		t.setTextColor(color);
		t.setFontId(fontId);
		t.setTextShadowed(shadow);
		fill(t, x, y, w, LINE_H);
		return t;
	}

	private static void fill(Widget w, int x, int y, int width, int height)
	{
		w.setOriginalX(x);
		w.setOriginalY(y);
		w.setOriginalWidth(width);
		w.setOriginalHeight(height);
		w.revalidate();
	}

	private List<Tab> tabs()
	{
		List<Tab> tabs = tabsSupplier == null ? null : tabsSupplier.get();
		return tabs == null ? new ArrayList<>() : tabs;
	}

	/** Trims long tab labels so six fit across the strip (e.g. "RUN CONTROL" -> "RUN"). */
	private static String shorten(String name)
	{
		String s = RogueScapeWindowOverlay.ascii(name).trim();
		int space = s.indexOf(' ');
		if (space > 0)
		{
			s = s.substring(0, space); // first word
		}
		return s.length() > 9 ? s.substring(0, 8) : s; // "PROGRESSION" -> "PROGRESS"
	}

	private static List<String> wrap(String value, int maxChars)
	{
		List<String> out = new ArrayList<>();
		String s = value == null ? "" : value.trim();
		if (s.isEmpty())
		{
			out.add("");
			return out;
		}
		while (s.length() > maxChars)
		{
			int cut = s.lastIndexOf(' ', maxChars);
			if (cut < maxChars / 2)
			{
				cut = maxChars;
			}
			out.add(s.substring(0, cut).trim());
			s = s.substring(cut).trim();
		}
		out.add(s);
		return out;
	}

	/** Logs a diagnostic line only when the state message changes (avoids per-tick spam). */
	private void diag(String message)
	{
		if (!message.equals(diag))
		{
			diag = message;
			log.info("[RogueScape widget] {}", message);
		}
	}

	private static boolean contains(Widget[] children, Widget target)
	{
		if (children == null || target == null)
		{
			return false;
		}
		for (Widget w : children)
		{
			if (w == target)
			{
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------ mouse

	/** Window-local cursor position, or {@code null} if the window isn't laid out / cursor unknown. */
	private Point local(MouseEvent e)
	{
		if (!wantOpen || root == null)
		{
			return null;
		}
		Point loc = root.getCanvasLocation();
		if (loc == null)
		{
			return null;
		}
		return new Point(e.getX() - loc.getX(), e.getY() - loc.getY());
	}

	private boolean within(Point p)
	{
		return p != null && p.getX() >= 0 && p.getX() <= W && p.getY() >= 0 && p.getY() <= H;
	}

	private boolean overTitleBar(Point p)
	{
		// Title strip, excluding the close button on the right.
		return p.getY() >= 0 && p.getY() <= TITLE_H && p.getX() >= 0 && p.getX() <= W - 40;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		Point p = local(e);
		if (!within(p))
		{
			return e;
		}
		// Consume EVERY click inside the window so it never reaches the game world (no walk-through).
		e.consume();
		try
		{
			if (closeRect.contains(p.getX(), p.getY()))
			{
				setOpen(false);
				return e;
			}
			for (int i = 0; i < tabRects.size(); i++)
			{
				if (tabRects.get(i).contains(p.getX(), p.getY()))
				{
					final int idx = i;
					// Widget mutation MUST happen on the client thread (mouse events fire on AWT).
					clientThread.invoke(() -> selectTab(idx));
					return e;
				}
			}
			if (overTitleBar(p))
			{
				dragging = true;
				grabDx = p.getX();
				grabDy = p.getY();
			}
		}
		catch (RuntimeException ignored)
		{
			// ignore
		}
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e)
	{
		if (dragging && root != null && parent != null)
		{
			try
			{
				Point pp = parent.getCanvasLocation();
				if (pp != null)
				{
					winX = Math.max(0, e.getX() - grabDx - pp.getX());
					winY = Math.max(0, e.getY() - grabDy - pp.getY());
					clientThread.invoke(() ->
					{
						if (root != null)
						{
							root.setOriginalX(winX);
							root.setOriginalY(winY);
							root.revalidate();
						}
					});
				}
			}
			catch (RuntimeException ignored)
			{
				// ignore
			}
			e.consume();
			return e;
		}
		if (within(local(e)))
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e)
	{
		boolean wasDragging = dragging;
		dragging = false;
		if (wasDragging || within(local(e)))
		{
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e)
	{
		if (within(local(e)))
		{
			e.consume();
		}
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
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e)
	{
		if (within(local(e)))
		{
			e.consume();
		}
		return e;
	}
}
