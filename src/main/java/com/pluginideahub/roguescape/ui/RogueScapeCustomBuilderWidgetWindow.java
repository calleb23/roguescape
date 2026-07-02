package com.pluginideahub.roguescape.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
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
 * Dedicated native-widget version of the Custom Builder. This intentionally avoids the decorative
 * overlay renderer so the route builder can behave like an in-game panel: tabs, selector fields,
 * explicit buttons, and a route list container.
 */
public final class RogueScapeCustomBuilderWidgetWindow implements MouseListener
{
	private static final int W = 680;
	private static final int H = 430;
	private static final int TITLE_H = 30;
	private static final int TAB_Y = 38;
	private static final int TAB_H = 24;
	private static final int PAD = 16;
	private static final int CONTENT_Y = 74;
	private static final int COL_BG = 0x181817;
	private static final int COL_PANEL = 0x20201e;
	private static final int COL_PANEL_DARK = 0x131312;
	private static final int COL_BORDER = 0x4a4640;
	private static final int COL_GOLD = 0xff981f;
	private static final int COL_TEXT = 0xd5d0c3;
	private static final int COL_MUTED = 0x9a9385;
	private static final int COL_PURPLE = 0x9b59ff;
	private static final int COL_RED = 0x9e3030;
	private static final int COL_GREEN = 0x3f8f46;

	private static final Logger log = LoggerFactory.getLogger(RogueScapeCustomBuilderWidgetWindow.class);

	private final Client client;
	private final ClientThread clientThread;
	private final BooleanSupplier enabled;
	private final Supplier<View> viewSupplier;
	private final Consumer<String> actionHandler;
	private final List<Rectangle> tabRects = new ArrayList<>();
	private final List<ActionHit> actionHits = new ArrayList<>();
	private final Rectangle closeRect = new Rectangle(W - 32, 6, 22, 22);

	private Widget parent;
	private Widget root;
	private Widget contentLayer;
	private boolean wantOpen;
	private int selectedTab = 2; // open on Rooms because that is the current pain point
	private int winX = 170;
	private int winY = 95;
	private boolean dragging;
	private int grabDx;
	private int grabDy;
	private String diag = "";

	public RogueScapeCustomBuilderWidgetWindow(Client client, ClientThread clientThread, BooleanSupplier enabled,
		Supplier<View> viewSupplier, Consumer<String> actionHandler)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.enabled = enabled;
		this.viewSupplier = viewSupplier;
		this.actionHandler = actionHandler;
	}

	public boolean isOpen()
	{
		return wantOpen;
	}

	public void setOpen(boolean open)
	{
		wantOpen = open;
	}

	public void toggle()
	{
		wantOpen = !wantOpen;
	}

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
				diag("built custom builder widget at (" + winX + "," + winY + ")");
			}
			else
			{
				root.setHidden(false);
				rebuildContent();
			}
		}
		catch (RuntimeException ex)
		{
			root = null;
			log.debug("[RogueScape custom builder widget] rebuild failed", ex);
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
			// best effort
		}
		root = null;
		contentLayer = null;
		wantOpen = false;
	}

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

	private void build(Widget host)
	{
		if (root != null)
		{
			try
			{
				root.setHidden(true);
			}
			catch (RuntimeException ignored)
			{
				// best effort
			}
		}
		tabRects.clear();
		actionHits.clear();

		root = host.createChild(-1, WidgetType.LAYER);
		root.setOriginalX(winX);
		root.setOriginalY(winY);
		root.setOriginalWidth(W);
		root.setOriginalHeight(H);
		root.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
		root.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		root.setHidden(false);
		root.revalidate();

		rect(root, 0, 0, W, H, COL_BG, true, 35);
		rect(root, 5, 5, W - 10, H - 10, COL_BORDER, false, 0);
		rect(root, 9, 9, W - 18, H - 18, COL_PANEL, true, 120);
		diamond(root, 7, 7, 18, COL_PURPLE);
		diamond(root, W - 25, 7, 18, COL_PURPLE);
		diamond(root, 7, H - 25, 18, COL_PURPLE);
		diamond(root, W - 25, H - 25, 18, COL_PURPLE);

		text(root, 22, 10, 260, "RogueScape Custom Builder", COL_GOLD, FontID.BOLD_12, true);
		text(root, W - 29, 9, 16, "X", COL_MUTED, FontID.BOLD_12, true).setXTextAlignment(WidgetTextAlignment.CENTER);
		drawTabs();
		rebuildContent();
	}

	private void drawTabs()
	{
		String[] tabs = tabs();
		int stripX = PAD;
		int stripW = W - PAD * 2;
		int tabW = stripW / tabs.length;
		rect(root, stripX, TAB_Y, stripW, TAB_H, COL_PANEL_DARK, true, 75);
		for (int i = 0; i < tabs.length; i++)
		{
			int x = stripX + i * tabW;
			int w = i == tabs.length - 1 ? stripX + stripW - x : tabW;
			boolean selected = i == selectedTab;
			if (selected)
			{
				rect(root, x + 2, TAB_Y + TAB_H - 3, w - 4, 2, COL_GOLD, true, 0);
			}
			Widget label = text(root, x, TAB_Y + 5, w, tabs[i], selected ? COL_GOLD : COL_MUTED, FontID.PLAIN_11, true);
			label.setXTextAlignment(WidgetTextAlignment.CENTER);
			tabRects.add(new Rectangle(x, TAB_Y, w, TAB_H));
		}
		rect(root, stripX, TAB_Y + TAB_H, stripW, 1, COL_BORDER, true, 0);
	}

	private void rebuildContent()
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
		actionHits.clear();
		View view = view();
		String tab = tabs()[Math.min(selectedTab, tabs().length - 1)];
		if ("MODE".equals(tab))
		{
			drawMode(view);
		}
		else if ("LOADOUT".equals(tab))
		{
			drawLoadout(view);
		}
		else if ("ROOMS".equals(tab))
		{
			drawRooms(view);
		}
		else if ("BOSSES".equals(tab))
		{
			drawBosses(view);
		}
		else if ("ROUTE".equals(tab))
		{
			drawRoute(view);
		}
		else if ("MODS".equals(tab))
		{
			drawModifiers(view);
		}
		else if ("RULES".equals(tab))
		{
			drawRules(view);
		}
		else
		{
			drawSeed(view);
		}
		contentLayer.revalidate();
		root.revalidate();
	}

	private void drawMode(View view)
	{
		heading("Custom Mode", 0);
		button(24, 112, 260, 86, "Scavenger", "Rooms define what can be collected.", "custom:mode-scavenger",
			"Scavenger".equals(view.gameMode) ? COL_GREEN : COL_BORDER);
		button(306, 112, 260, 86, "Rewarded", "Bosses and normal reward rules drive the run.", "custom:mode-rewarded",
			"Rewarded".equals(view.gameMode) ? COL_GOLD : COL_BORDER);
		note(24, 226, "Selected: " + view.gameMode);
	}

	private void drawLoadout(View view)
	{
		heading("Starting Loadout", 0);
		String[] names = {"Naked", "Low Gear", "Mid Gear", "Custom Kit"};
		String[] actions = {"custom:loadout-naked", "custom:loadout-low", "custom:loadout-mid", "custom:loadout-custom"};
		for (int i = 0; i < names.length; i++)
		{
			int x = 24 + (i % 2) * 282;
			int y = 104 + (i / 2) * 78;
			button(x, y, 260, 62, names[i], selectedCopy(names[i]), actions[i],
				names[i].equals(view.loadout) ? COL_GOLD : COL_BORDER);
		}
		panelBox(24, 274, 542, 84);
		text(contentLayer, 38, 290, 220, "Selected Kit: " + view.loadout, COL_GOLD, FontID.BOLD_12, true);
		int y = 312;
		for (String line : view.loadoutKitLines)
		{
			if (y + 16 > 352)
			{
				text(contentLayer, 38, y, 500, "...", COL_MUTED, FontID.PLAIN_12, false);
				break;
			}
			text(contentLayer, 38, y, 500, "- " + line, COL_TEXT, FontID.PLAIN_12, true);
			y += 16;
		}
	}

	private void drawRooms(View view)
	{
		heading("Rooms", 0);
		listBox(24, 112, 252, 210, "All Rooms", view.roomOptions, view.selectedRoomIndex, "custom:room-index:");
		smallButton(24, 334, 120, 26, "Page Up", "custom:room-page-up", COL_BORDER);
		smallButton(156, 334, 120, 26, "Page Down", "custom:room-page-down", COL_BORDER);
		listBox(294, 112, 140, 196, "Allowed Type", view.allowanceOptions, view.selectedAllowanceIndex, "custom:type-index:");
		smallButton(294, 326, 140, 32, "Add Room", "custom:add-selected-room", COL_GREEN);
		routeBox(view, 456, 112, 200, 246, true);
	}

	private void drawBosses(View view)
	{
		heading("Bosses", 0);
		listBox(24, 112, 376, 210, "Boss Pool", view.bossOptions, view.selectedBossIndex, "custom:boss-index:");
		smallButton(24, 334, 180, 26, "Page Up", "custom:boss-page-up", COL_BORDER);
		smallButton(220, 334, 180, 26, "Page Down", "custom:boss-page-down", COL_BORDER);
		smallButton(424, 112, 150, 32, "Add Boss", "custom:add-selected-boss", COL_RED);
		field(424, 164, 150, 34, view.bossLabel);
		routeBox(view, 424, 232, 232, 126, true);
	}

	private void drawRoute(View view)
	{
		heading("Route Control", 0);
		routeBox(view, 24, 104, 376, 246, true);
		smallButton(424, 112, 150, 32, "Select Up", "custom:select-up", COL_BORDER);
		smallButton(424, 152, 150, 32, "Select Down", "custom:select-down", COL_BORDER);
		smallButton(424, 204, 150, 32, "Move Up", "custom:move-up", COL_PURPLE);
		smallButton(424, 244, 150, 32, "Move Down", "custom:move-down", COL_PURPLE);
		smallButton(424, 296, 150, 32, "Remove", "custom:remove-room", COL_RED);
		smallButton(424, 336, 150, 32, "Clear Route", "custom:clear-route", COL_RED);
	}

	private void drawModifiers(View view)
	{
		heading("Modifiers", 0);
		checkListBox(24, 112, 376, 246, "Starting Curses", view.modifierOptions,
			view.selectedModifierIndexes, view.modifierPageStart, "custom:modifier-index:");
		smallButton(24, 366, 180, 26, "Page Up", "custom:modifier-page-up", COL_BORDER);
		smallButton(220, 366, 180, 26, "Page Down", "custom:modifier-page-down", COL_BORDER);
		text(contentLayer, 424, 94, 220, "Selected", COL_GOLD, FontID.BOLD_12, true);
		panelBox(424, 112, 232, 192);
		List<String> selected = view.selectedModifierLabels == null ? Collections.emptyList() : view.selectedModifierLabels;
		if (selected.isEmpty())
		{
			text(contentLayer, 436, 128, 208, "No starting curses selected.", COL_MUTED, FontID.PLAIN_12, false);
		}
		else
		{
			int y = 124;
			for (String row : selected)
			{
				if (y + 18 > 300)
				{
					text(contentLayer, 436, y, 208, "...", COL_MUTED, FontID.PLAIN_12, false);
					break;
				}
				text(contentLayer, 436, y, 208, "- " + row, COL_TEXT, FontID.PLAIN_12, true);
				y += 18;
			}
		}
		smallButton(424, 326, 150, 32, "Clear Curses", "custom:clear-modifiers", COL_RED);
	}

	private void drawRules(View view)
	{
		heading("Constraints", 0);
		button(24, 112, 260, 70, "Bank Unlocks", view.bankUnlocks ? "Bank withdrawals allowed." : "Bank is blocked during the run.",
			"custom:toggle-bank", view.bankUnlocks ? COL_GREEN : COL_RED);
		button(24, 206, 260, 70, "Time Limit", view.timeLimitMinutes <= 0 ? "No timer limit." : view.timeLimitMinutes + " minute target.",
			"custom:cycle-time", COL_PURPLE);
		button(306, 206, 260, 70, "Boss Limit", view.bossLimit <= 0 ? "No boss cap." : "Stop adding bosses after " + view.bossLimit + ".",
			"custom:cycle-boss-limit", COL_RED);
		note(24, 306, "Rules are included in the seed preview and applied when custom runs start.");
	}

	private void drawSeed(View view)
	{
		heading("Seed Preview", 0);
		panelBox(24, 112, 550, 160);
		int y = 128;
		for (String line : wrap(view.seedPreview, 68))
		{
			text(contentLayer, 38, y, 522, line, COL_TEXT, FontID.PLAIN_12, false);
			y += 16;
		}
		panelBox(24, 292, 376, 66);
		text(contentLayer, 38, 308, 348, "Mode: " + view.gameMode + "   Loadout: " + view.loadout,
			COL_TEXT, FontID.PLAIN_12, true);
		text(contentLayer, 38, 326, 348, view.routeRows.isEmpty()
			? "No custom route selected: start will auto-build a fallback route."
			: "Route " + view.routeRows.size() + " steps | Curses " + view.selectedModifierLabels.size(),
				COL_MUTED, FontID.PLAIN_12, false);
		smallButton(424, 292, 150, 32, "Start Custom Run", "custom:start-run", COL_GREEN);
		smallButton(424, 326, 70, 32, "Load Seed", "custom:load-seed", COL_PURPLE);
		smallButton(504, 326, 70, 32, "Clear", "custom:clear-route", COL_RED);
	}

	private void selector(int x, int y, String label, String value, String prevAction, String nextAction)
	{
		text(contentLayer, x, y - 18, 220, label, COL_MUTED, FontID.PLAIN_11, true);
		smallButton(x, y, 34, 34, "<", prevAction, COL_BORDER);
		field(x + 42, y, 350, 34, value);
		smallButton(x + 400, y, 34, 34, ">", nextAction, COL_BORDER);
	}

	private void listBox(int x, int y, int w, int h, String title, List<String> rows, int selectedIndex, String actionPrefix)
	{
		text(contentLayer, x, y - 18, w, title, COL_GOLD, FontID.BOLD_12, true);
		panelBox(x, y, w, h);
		List<String> safeRows = rows == null ? Collections.emptyList() : rows;
		if (safeRows.isEmpty())
		{
			text(contentLayer, x + 10, y + 12, w - 20, "(empty)", COL_MUTED, FontID.PLAIN_12, false);
			return;
		}

		int rowH = 20;
		int rowY = y + 8;
		int maxRows = Math.max(1, (h - 16) / rowH);
		int start = 0;
		if (selectedIndex >= maxRows)
		{
			start = selectedIndex - maxRows + 1;
		}
		for (int row = 0; row < maxRows && start + row < safeRows.size(); row++)
		{
			int idx = start + row;
			boolean selected = idx == selectedIndex;
			if (selected)
			{
				rect(contentLayer, x + 6, rowY - 2, w - 12, rowH, COL_GOLD, true, 190);
			}
			String prefix = selected ? "> " : "  ";
			text(contentLayer, x + 10, rowY, w - 20, prefix + safeRows.get(idx),
				selected ? COL_GOLD : COL_TEXT, FontID.PLAIN_12, true);
			actionHits.add(new ActionHit(new Rectangle(x + 6, rowY - 4, w - 12, rowH + 2), actionPrefix + idx));
			rowY += rowH;
		}
		if (safeRows.size() > maxRows)
		{
			text(contentLayer, x + w - 44, y + h - 16, 36, (start + 1) + "-" + Math.min(safeRows.size(), start + maxRows),
				COL_MUTED, FontID.PLAIN_11, false);
		}
	}

	private void checkListBox(int x, int y, int w, int h, String title, List<String> rows, List<Integer> selectedIndexes,
		int pageStart, String actionPrefix)
	{
		text(contentLayer, x, y - 18, w, title, COL_GOLD, FontID.BOLD_12, true);
		panelBox(x, y, w, h);
		List<String> safeRows = rows == null ? Collections.emptyList() : rows;
		List<Integer> selected = selectedIndexes == null ? Collections.emptyList() : selectedIndexes;
		if (safeRows.isEmpty())
		{
			text(contentLayer, x + 10, y + 12, w - 20, "(empty)", COL_MUTED, FontID.PLAIN_12, false);
			return;
		}

		int rowH = 20;
		int rowY = y + 8;
		int maxRows = Math.max(1, (h - 16) / rowH);
		int start = Math.max(0, Math.min(pageStart, Math.max(0, safeRows.size() - maxRows)));
		for (int row = 0; row < maxRows && start + row < safeRows.size(); row++)
		{
			int idx = start + row;
			boolean checked = selected.contains(idx);
			if (checked)
			{
				rect(contentLayer, x + 6, rowY - 2, w - 12, rowH, COL_PURPLE, true, 205);
			}
			text(contentLayer, x + 10, rowY, w - 20, (checked ? "[x] " : "[ ] ") + safeRows.get(idx),
				checked ? COL_GOLD : COL_TEXT, FontID.PLAIN_12, true);
			actionHits.add(new ActionHit(new Rectangle(x + 6, rowY - 4, w - 12, rowH + 2), actionPrefix + idx));
			rowY += rowH;
		}
		if (safeRows.size() > maxRows)
		{
			text(contentLayer, x + w - 76, y + h - 16, 70, (start + 1) + "-" + Math.min(safeRows.size(), start + maxRows)
					+ "/" + safeRows.size(),
				COL_MUTED, FontID.PLAIN_11, false);
		}
	}

	private void routeBox(View view, int x, int y, int w, int h, boolean selectable)
	{
		text(contentLayer, x, y - 18, w, "Confirmed Route", COL_GOLD, FontID.BOLD_12, true);
		panelBox(x, y, w, h);
		List<String> rows = view.routeRows == null ? Collections.emptyList() : view.routeRows;
		if (rows.isEmpty())
		{
			text(contentLayer, x + 12, y + 16, w - 24, "No rooms or bosses confirmed yet.", COL_MUTED, FontID.PLAIN_12, false);
			return;
		}
		int rowY = y + 10;
		for (int i = 0; i < rows.size() && rowY + 20 < y + h; i++)
		{
			boolean selected = selectable && i == view.selectedRouteIndex;
			if (selected)
			{
				rect(contentLayer, x + 8, rowY - 2, w - 16, 20, COL_GOLD, true, 190);
			}
			text(contentLayer, x + 14, rowY, w - 28, (i + 1) + ". " + rows.get(i),
				selected ? COL_GOLD : COL_TEXT, FontID.PLAIN_12, true);
			if (selectable)
			{
				actionHits.add(new ActionHit(new Rectangle(x + 8, rowY - 4, w - 16, 20), "custom:route-index:" + i));
			}
			rowY += 22;
		}
	}

	private void heading(String value, int yOffset)
	{
		int y = CONTENT_Y + yOffset;
		text(contentLayer, 24, y, 300, value, COL_GOLD, FontID.BOLD_12, true);
		rect(contentLayer, 24, y + 20, W - 48, 1, COL_BORDER, true, 0);
	}

	private void note(int x, int y, String value)
	{
		panelBox(x, y, W - 48, 44);
		text(contentLayer, x + 12, y + 14, W - 72, value, COL_TEXT, FontID.PLAIN_12, false);
	}

	private void button(int x, int y, int w, int h, String title, String sub, String action, int border)
	{
		panelBox(x, y, w, h);
		rect(contentLayer, x, y, w, h, border, false, 0);
		text(contentLayer, x + 10, y + 14, w - 20, title, border == COL_BORDER ? COL_TEXT : border, FontID.BOLD_12, true);
		for (String line : wrap(sub, Math.max(24, w / 7)))
		{
			text(contentLayer, x + 10, y + 38, w - 20, line, COL_MUTED, FontID.PLAIN_11, false);
			break;
		}
		actionHits.add(new ActionHit(new Rectangle(x, y, w, h), action));
	}

	private void smallButton(int x, int y, int w, int h, String label, String action, int border)
	{
		rect(contentLayer, x, y, w, h, COL_PANEL_DARK, true, 80);
		rect(contentLayer, x, y, w, h, border, false, 0);
		Widget t = text(contentLayer, x, y + 10, w, label, border == COL_BORDER ? COL_TEXT : border, FontID.PLAIN_12, true);
		t.setXTextAlignment(WidgetTextAlignment.CENTER);
		actionHits.add(new ActionHit(new Rectangle(x, y, w, h), action));
	}

	private void field(int x, int y, int w, int h, String value)
	{
		rect(contentLayer, x, y, w, h, COL_PANEL_DARK, true, 65);
		rect(contentLayer, x, y, w, h, COL_BORDER, false, 0);
		text(contentLayer, x + 10, y + 10, w - 20, value, COL_TEXT, FontID.PLAIN_12, true);
	}

	private void panelBox(int x, int y, int w, int h)
	{
		rect(contentLayer, x, y, w, h, COL_PANEL_DARK, true, 65);
		rect(contentLayer, x, y, w, h, COL_BORDER, false, 0);
	}

	private Widget text(Widget host, int x, int y, int w, String value, int color, int fontId, boolean shadow)
	{
		Widget t = host.createChild(-1, WidgetType.TEXT);
		t.setText(RogueScapeWindowOverlay.ascii(value == null ? "" : value));
		t.setTextColor(color);
		t.setFontId(fontId);
		t.setTextShadowed(shadow);
		fill(t, x, y, w, 16);
		return t;
	}

	private void rect(Widget host, int x, int y, int w, int h, int color, boolean filled, int opacity)
	{
		Widget r = host.createChild(-1, WidgetType.RECTANGLE);
		r.setFilled(filled);
		r.setTextColor(color);
		if (opacity > 0)
		{
			r.setOpacity(opacity);
		}
		fill(r, x, y, w, h);
	}

	private void diamond(Widget host, int x, int y, int size, int color)
	{
		Widget d = host.createChild(-1, WidgetType.TEXT);
		d.setText("◇");
		d.setTextColor(color);
		d.setFontId(FontID.BOLD_12);
		d.setTextShadowed(true);
		d.setXTextAlignment(WidgetTextAlignment.CENTER);
		fill(d, x, y + 2, size, size);
	}

	private static void fill(Widget w, int x, int y, int width, int height)
	{
		w.setOriginalX(x);
		w.setOriginalY(y);
		w.setOriginalWidth(width);
		w.setOriginalHeight(height);
		w.revalidate();
	}

	private View view()
	{
		View v = viewSupplier == null ? null : viewSupplier.get();
		return v == null ? View.empty() : v;
	}

	private static String[] tabs()
	{
		return new String[]{"MODE", "LOADOUT", "ROOMS", "BOSSES", "ROUTE", "MODS", "RULES", "SEED"};
	}

	private static String selectedCopy(String name)
	{
		if ("Naked".equals(name)) return "Start with nothing.";
		if ("Low Gear".equals(name)) return "Small starter kit.";
		if ("Mid Gear".equals(name)) return "More forgiving start.";
		return "Sword, shortbow, arrows, and food.";
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
		return p.getY() >= 0 && p.getY() <= TITLE_H && p.getX() >= 0 && p.getX() <= W - 42;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e)
	{
		Point p = local(e);
		if (!within(p))
		{
			return e;
		}
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
					clientThread.invoke(() ->
					{
						selectedTab = idx;
						build(parent);
					});
					return e;
				}
			}
			for (ActionHit hit : actionHits)
			{
				if (hit.rect.contains(p.getX(), p.getY()))
				{
					if (actionHandler != null)
					{
						actionHandler.accept(hit.action);
					}
					clientThread.invoke(this::rebuildContent);
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

	private void diag(String message)
	{
		if (!message.equals(diag))
		{
			diag = message;
			log.info("[RogueScape custom builder widget] {}", message);
		}
	}

	public static Dimension renderPreview(Graphics2D g, View view)
	{
		Dimension size = new Dimension(W, H);
		PreviewPainter p = new PreviewPainter(g, view == null ? View.empty() : view);
		p.paint();
		return size;
	}

	private static final class ActionHit
	{
		private final Rectangle rect;
		private final String action;

		private ActionHit(Rectangle rect, String action)
		{
			this.rect = rect;
			this.action = action;
		}
	}

	public static final class View
	{
		public final String gameMode;
		public final String loadout;
		public final List<String> loadoutKitLines;
		public final boolean bankUnlocks;
		public final int timeLimitMinutes;
		public final int bossLimit;
		public final String roomLabel;
		public final String allowanceLabel;
		public final String bossLabel;
		public final List<String> roomOptions;
		public final List<String> allowanceOptions;
		public final List<String> bossOptions;
		public final List<String> modifierOptions;
		public final List<String> selectedModifierLabels;
		public final List<Integer> selectedModifierIndexes;
		public final int modifierPageStart;
		public final int selectedRoomIndex;
		public final int selectedAllowanceIndex;
		public final int selectedBossIndex;
		public final List<String> routeRows;
		public final int selectedRouteIndex;
		public final String seedPreview;

		public View(String gameMode, String loadout, String roomLabel, String allowanceLabel, String bossLabel,
			List<String> routeRows, int selectedRouteIndex, String seedPreview)
		{
			this(gameMode, loadout, Collections.emptyList(), false, 0, 0, roomLabel, allowanceLabel, bossLabel, Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), 0, 0, 0, 0, routeRows, selectedRouteIndex, seedPreview);
		}

		public View(String gameMode, String loadout, List<String> loadoutKitLines,
			boolean bankUnlocks, int timeLimitMinutes, int bossLimit,
			String roomLabel, String allowanceLabel, String bossLabel,
			List<String> roomOptions, List<String> allowanceOptions, List<String> bossOptions,
			List<String> modifierOptions, List<String> selectedModifierLabels, List<Integer> selectedModifierIndexes,
			int modifierPageStart, int selectedRoomIndex, int selectedAllowanceIndex, int selectedBossIndex, List<String> routeRows,
			int selectedRouteIndex, String seedPreview)
		{
			this.gameMode = gameMode == null ? "Scavenger" : gameMode;
			this.loadout = loadout == null ? "Naked" : loadout;
			this.loadoutKitLines = loadoutKitLines == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(loadoutKitLines));
			this.bankUnlocks = bankUnlocks;
			this.timeLimitMinutes = Math.max(0, timeLimitMinutes);
			this.bossLimit = Math.max(0, bossLimit);
			this.roomLabel = roomLabel == null ? "(none)" : roomLabel;
			this.allowanceLabel = allowanceLabel == null ? "All" : allowanceLabel;
			this.bossLabel = bossLabel == null ? "(auto)" : bossLabel;
			this.roomOptions = roomOptions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(roomOptions));
			this.allowanceOptions = allowanceOptions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(allowanceOptions));
			this.bossOptions = bossOptions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(bossOptions));
			this.modifierOptions = modifierOptions == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(modifierOptions));
			this.selectedModifierLabels = selectedModifierLabels == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(selectedModifierLabels));
			this.selectedModifierIndexes = selectedModifierIndexes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(selectedModifierIndexes));
			this.modifierPageStart = Math.max(0, modifierPageStart);
			this.selectedRoomIndex = selectedRoomIndex;
			this.selectedAllowanceIndex = selectedAllowanceIndex;
			this.selectedBossIndex = selectedBossIndex;
			this.routeRows = routeRows == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(routeRows));
			this.selectedRouteIndex = selectedRouteIndex;
			this.seedPreview = seedPreview == null ? "" : seedPreview;
		}

		public static View empty()
		{
			return new View("Scavenger", "Naked", Collections.singletonList("No starter items"),
				false, 0, 0, "Lumbridge Swamp", "Supply", "Bryophyta",
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), 0, 0, 0, 0,
				Collections.emptyList(), -1, "mode=Scavenger;loadout=Naked");
		}
	}

	private static final class PreviewPainter
	{
		private final Graphics2D g;
		private final View view;

		private PreviewPainter(Graphics2D g, View view)
		{
			this.g = g;
			this.view = view;
		}

		private void paint()
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(new Font("Dialog", Font.PLAIN, 12));
			g.setColor(new Color(COL_BG));
			g.fillRect(0, 0, W, H);
			g.setColor(new Color(COL_BORDER));
			g.drawRect(5, 5, W - 10, H - 10);
			g.setColor(new Color(COL_PANEL));
			g.fillRect(9, 9, W - 18, H - 18);
			g.setColor(new Color(COL_PURPLE));
			drawDiamond(7, 7, 18);
			drawDiamond(W - 25, 7, 18);
			drawDiamond(7, H - 25, 18);
			drawDiamond(W - 25, H - 25, 18);
			drawText("RogueScape Custom Builder", 22, 23, new Color(COL_GOLD), true);
			drawTabsPreview();
			drawText("Rooms", 24, 88, new Color(COL_GOLD), true);
			g.setColor(new Color(COL_BORDER));
			g.drawLine(24, 96, W - 24, 96);
			listPreview(24, 112, 252, 246, "All Rooms", view.roomOptions, view.selectedRoomIndex);
			listPreview(294, 112, 140, 196, "Allowed Type", view.allowanceOptions, view.selectedAllowanceIndex);
			buttonPreview(294, 326, 140, 32, "Add Room", new Color(COL_GREEN));
			routePreview(456, 112, 200, 246);
		}

		private void drawTabsPreview()
		{
			String[] names = tabs();
			int x = PAD;
			int w = (W - PAD * 2) / names.length;
			g.setColor(new Color(COL_PANEL_DARK));
			g.fillRect(x, TAB_Y, W - PAD * 2, TAB_H);
			for (int i = 0; i < names.length; i++)
			{
				Color c = i == 2 ? new Color(COL_GOLD) : new Color(COL_MUTED);
				drawText(names[i], x + i * w + w / 2 - 20, TAB_Y + 16, c, false);
			}
			g.setColor(new Color(COL_GOLD));
			g.fillRect(x + 2 * w + 2, TAB_Y + TAB_H - 3, w - 4, 2);
		}

		private void selectorPreview(int x, int y, String label, String value)
		{
			drawText(label, x, y - 5, new Color(COL_MUTED), false);
			buttonPreview(x, y, 34, 34, "<", new Color(COL_BORDER));
			fieldPreview(x + 42, y, 350, 34, value);
			buttonPreview(x + 400, y, 34, 34, ">", new Color(COL_BORDER));
		}

		private void listPreview(int x, int y, int w, int h, String title, List<String> rows, int selectedIndex)
		{
			drawText(title, x, y - 6, new Color(COL_GOLD), true);
			g.setColor(new Color(COL_PANEL_DARK));
			g.fillRect(x, y, w, h);
			g.setColor(new Color(COL_BORDER));
			g.drawRect(x, y, w, h);
			List<String> safeRows = rows == null ? Collections.emptyList() : rows;
			int rowY = y + 24;
			int maxRows = Math.max(1, (h - 16) / 20);
			int start = selectedIndex >= maxRows ? selectedIndex - maxRows + 1 : 0;
			for (int row = 0; row < maxRows && start + row < safeRows.size(); row++)
			{
				int idx = start + row;
				if (idx == selectedIndex)
				{
					g.setColor(new Color(COL_GOLD));
					g.drawRect(x + 6, rowY - 15, w - 12, 20);
				}
				drawText((idx == selectedIndex ? "> " : "  ") + safeRows.get(idx), x + 10, rowY,
					idx == selectedIndex ? new Color(COL_GOLD) : new Color(COL_TEXT), false);
				rowY += 20;
			}
		}

		private void fieldPreview(int x, int y, int w, int h, String value)
		{
			g.setColor(new Color(COL_PANEL_DARK));
			g.fillRect(x, y, w, h);
			g.setColor(new Color(COL_BORDER));
			g.drawRect(x, y, w, h);
			drawText(value, x + 10, y + 22, new Color(COL_TEXT), false);
		}

		private void buttonPreview(int x, int y, int w, int h, String label, Color color)
		{
			g.setColor(new Color(COL_PANEL_DARK));
			g.fillRect(x, y, w, h);
			g.setColor(color);
			g.drawRect(x, y, w, h);
			drawText(label, x + 12, y + 22, color, false);
		}

		private void routePreview(int x, int y, int w, int h)
		{
			drawText("Confirmed Route", x, y - 6, new Color(COL_GOLD), true);
			g.setColor(new Color(COL_PANEL_DARK));
			g.fillRect(x, y, w, h);
			g.setColor(new Color(COL_BORDER));
			g.drawRect(x, y, w, h);
			int rowY = y + 28;
			if (view.routeRows.isEmpty())
			{
				drawText("No rooms or bosses confirmed yet.", x + 12, rowY, new Color(COL_MUTED), false);
				return;
			}
			for (int i = 0; i < view.routeRows.size() && rowY < y + h - 8; i++)
			{
				if (i == view.selectedRouteIndex)
				{
					g.setColor(new Color(COL_GOLD));
					g.setStroke(new BasicStroke(1f));
					g.drawRect(x + 8, rowY - 15, w - 16, 20);
				}
				drawText((i + 1) + ". " + view.routeRows.get(i), x + 14, rowY, new Color(COL_TEXT), false);
				rowY += 22;
			}
		}

		private void drawDiamond(int x, int y, int s)
		{
			int cx = x + s / 2;
			int cy = y + s / 2;
			g.drawPolygon(new int[]{cx, x + s, cx, x}, new int[]{y, cy, y + s, cy}, 4);
		}

		private void drawText(String text, int x, int y, Color color, boolean bold)
		{
			g.setFont(new Font("Dialog", bold ? Font.BOLD : Font.PLAIN, 12));
			g.setColor(color);
			g.drawString(text == null ? "" : text, x, y);
		}
	}
}
