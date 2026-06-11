package com.pluginideahub.roguescape.ui;

import java.util.function.BooleanSupplier;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;

/**
 * Injects a small, visible "RS" button into the side journal's tab strip ({@code TABS}, 629.1)
 * that opens the RogueScape side panel when clicked.
 *
 * <p>We deliberately do NOT render a page inside the journal: the game's content lists are
 * sibling widgets drawn on top of the content area, so an injected page renders behind them.
 * Instead this is just an entry point — the rich UI lives in the RuneLite side panel.
 *
 * <p>Robustness: the journal is rebuilt by clientscripts (which wipes dynamically-added
 * children), so injection is tick-driven via {@link #ensureInjected()} — it re-adds the button
 * if it's missing. All widget access must be on the client thread; every entry point is wrapped
 * so a wrong assumption can't crash the client.
 */
public final class RogueScapeJournalTabAdapter
{
	private static final int COLOR_ACCENT = rgb(RogueScapeTheme.ACCENT);
	private static final int COLOR_BACKING = rgb(RogueScapeTheme.SECTION_HEADER_BG);
	private static final int FONT_PLAIN_12 = 495;

	private static int rgb(java.awt.Color c)
	{
		return c.getRGB() & 0xFFFFFF;
	}

	private final Client client;
	private final BooleanSupplier enabled;
	private final Runnable onOpen;

	private Widget button;
	private Widget backing;

	public RogueScapeJournalTabAdapter(Client client, BooleanSupplier enabled, Runnable onOpen)
	{
		this.client = client;
		this.enabled = enabled;
		this.onOpen = onOpen;
	}

	/** Tick-safe: ensures the opener button is present in the open journal; re-adds if wiped. */
	public void ensureInjected()
	{
		if (client == null || enabled == null || !enabled.getAsBoolean())
		{
			return;
		}
		try
		{
			Widget tabs = client.getWidget(InterfaceID.SideJournal.TABS);
			if (tabs == null)
			{
				button = null;
				backing = null;
				return;
			}
			if (button != null && contains(tabs.getDynamicChildren(), button))
			{
				return;
			}
			inject(tabs);
		}
		catch (RuntimeException ex)
		{
			button = null;
			backing = null;
		}
	}

	private void inject(Widget tabs)
	{
		// A small filled backing so the button is easy to see against the tab strip.
		backing = tabs.createChild(-1, WidgetType.RECTANGLE);
		backing.setFilled(true);
		backing.setTextColor(COLOR_BACKING);
		backing.setOpacity(60);
		backing.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		backing.setOriginalX(3);
		backing.setOriginalY(3);
		backing.setOriginalWidth(30);
		backing.setOriginalHeight(18);
		backing.revalidate();

		button = tabs.createChild(-1, WidgetType.TEXT);
		button.setText("RS");
		button.setName("<col=ff981f>RogueScape</col>");
		button.setTextColor(COLOR_ACCENT);
		button.setFontId(FONT_PLAIN_12);
		button.setTextShadowed(true);
		button.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		button.setOriginalX(6);
		button.setOriginalY(5);
		button.setOriginalWidth(24);
		button.setOriginalHeight(14);
		button.setHasListener(true);
		button.setAction(0, "Open RogueScape");
		button.setOnOpListener((JavaScriptCallback) e -> open());
		button.setNoClickThrough(true);
		button.revalidate();
	}

	private void open()
	{
		if (onOpen != null)
		{
			onOpen.run();
		}
	}

	private static boolean contains(Widget[] children, Widget target)
	{
		if (children == null)
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
}
