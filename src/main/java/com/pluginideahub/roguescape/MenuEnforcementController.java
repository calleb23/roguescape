package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementDecision;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementEvaluator;
import com.pluginideahub.roguescape.core.enforcement.RogueScapeEnforcementRules;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;

/**
 * Applies RogueScape's region/rule enforcement to the client's right-click menus: it filters
 * BLOCK-decision entries on menu build and decides whether a click should be blocked. The pure
 * decision rules live in {@link MenuEnforcementEvaluator}/{@link RogueScapeEnforcementRules}; this
 * controller only bridges them to the live {@link Client} menu.
 */
final class MenuEnforcementController
{
	private MenuEnforcementController() {}

	/** Enforcement runs only during an ACTIVE run in a travel/room/boss phase. */
	static boolean isActive(RogueScapeRun run, RogueScapeRunLoop loop, RogueScapeRunSession session)
	{
		if (run == null || loop == null || session == null)
		{
			return false;
		}
		if (session.runState() != RunState.ACTIVE)
		{
			return false;
		}
		RunPhase phase = loop.phase();
		return phase == RunPhase.TRAVEL_TO_STAGE || phase == RunPhase.ROOM_ACTIVE || phase == RunPhase.BOSS_ACTIVE;
	}

	/** Removes BLOCK-decision entries from the current right-click menu. */
	static void filterMenuEntries(Client client, RogueScapeRun run)
	{
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		boolean inside = run.currentRegionLegal();
		MenuEntry[] entries = client.getMenuEntries();
		if (entries == null || entries.length == 0)
		{
			return;
		}
		MenuEntry[] filtered = new MenuEntry[entries.length];
		int kept = 0;
		boolean removedAny = false;
		for (MenuEntry entry : entries)
		{
			MenuEnforcementDecision decision = MenuEnforcementEvaluator.evaluate(
				entry.getOption(), entry.getTarget(), rules, inside);
			if (decision == MenuEnforcementDecision.BLOCK)
			{
				removedAny = true;
				continue;
			}
			filtered[kept++] = entry;
		}
		if (removedAny)
		{
			MenuEntry[] trimmed = new MenuEntry[kept];
			System.arraycopy(filtered, 0, trimmed, 0, kept);
			client.setMenuEntries(trimmed);
		}
	}

	/** Whether a clicked menu option should be blocked (and consumed). */
	static boolean shouldBlockClick(String option, String target, RogueScapeRun run)
	{
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		return MenuEnforcementEvaluator.evaluate(option, target, rules, run.currentRegionLegal())
			== MenuEnforcementDecision.BLOCK;
	}
}
