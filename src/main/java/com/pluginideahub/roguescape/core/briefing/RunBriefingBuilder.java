package com.pluginideahub.roguescape.core.briefing;

import com.pluginideahub.roguescape.core.RogueScapeCustomRunFactory;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunTimer;
import com.pluginideahub.roguescape.core.campaign.CampaignLibrary;
import com.pluginideahub.roguescape.core.item.StarterKit;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a built (or preview) run into a {@link RunBriefing}. Two entry points:
 *
 * <ul>
 *   <li>{@link #of} reads an already-built session/run/loop — the source of truth for a run that
 *       is about to start or is in progress.</li>
 *   <li>{@link #preview} builds a throwaway session/run/loop from a mode/preset/seed so the lobby
 *       can show the exact route before the player commits. Deterministic whenever a seed is set
 *       or the preset is a fixed campaign; otherwise the route re-rolls on Begin and the briefing
 *       says so.</li>
 * </ul>
 *
 * Pure Java; no RuneLite types. All friendly labels live here so the core stays self-contained.
 */
public final class RunBriefingBuilder
{
	private RunBriefingBuilder() {}

	/** Briefing for an already-built run (the route is locked because it is already drawn). */
	public static RunBriefing of(RogueScapeRunSession session, RogueScapeRun run, RogueScapeRunLoop loop)
	{
		if (session == null) throw new IllegalArgumentException("session required");
		if (run == null) throw new IllegalArgumentException("run required");
		return build(session, run, loop, loadoutLabelFromKit(run.starterKit()), seedLabel(session.seed()), true);
	}

	/** Briefing for a not-yet-started lobby selection, built from a deterministic preview run. */
	public static RunBriefing preview(RunMode mode, RunPreset preset, String seedText, String loadout,
		boolean bankAccess, int timeLimitMinutes)
	{
		RunMode m = mode != null ? mode : RunMode.FRESH_SOURCE;
		RunPreset p = preset != null ? preset : RunPreset.UNSPECIFIED;
		String trimmedSeed = seedText == null ? "" : seedText.trim();

		RogueScapeRunSession session = RogueScapeRunSession.start(modeLabel(m), trimmedSeed.isEmpty() ? null : trimmedSeed, m, p);
		RogueScapeRun run = RogueScapeRun.wrap(session)
			.declareStarterKit(RogueScapeCustomRunFactory.starterKitForLoadout(loadout))
			.setBankAccessAllowed(bankAccess);

		RunRouteBuilder.buildRoute(m, p, trimmedSeed, session, run);

		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L)
			.setBaseRewardsEnabled(true)
			.setTravelGatedStages(true);
		if (timeLimitMinutes > 0)
		{
			loop.setTimeLimitMillis(timeLimitMinutes * 60_000L);
		}

		boolean locked = !trimmedSeed.isEmpty() || CampaignLibrary.find(p) != null;
		String loadoutLabel = loadout == null || loadout.trim().isEmpty() ? "Naked" : loadout.trim();
		return build(session, run, loop, loadoutLabel, seedLabel(trimmedSeed.isEmpty() ? null : trimmedSeed), locked);
	}

	private static RunBriefing build(RogueScapeRunSession session, RogueScapeRun run, RogueScapeRunLoop loop,
		String loadoutLabel, String seedLabel, boolean routeLocked)
	{
		List<RunBriefing.RoomLine> rooms = new ArrayList<>();
		String finalBoss = null;
		boolean anyTimer = false;
		long perRoomTimer = perRoomTimerMillis(loop);
		int index = 1;
		for (RunStage stage : session.route().stages())
		{
			StageRegionRule rule = run.regionPolicy().ruleFor(stage.id());
			boolean boss = stage.type() == RunStageType.BOSS;
			RoomKind kind = rule.roomKind();
			boolean regionLocked = rule.restrictsRegion();
			long timer = perRoomTimer;
			anyTimer |= timer > 0L;
			rooms.add(new RunBriefing.RoomLine(index++, stage.name(), kind, kindLabel(kind, boss),
				stage.objectiveLabel(), stage.requiredItemGains(), boss, regionLocked, timer));
			if (boss)
			{
				finalBoss = stage.name();
			}
		}

		return new RunBriefing(
			session.goal(),
			modeLabel(session.mode()),
			modeSummary(session.mode()),
			rooms,
			finalBoss,
			loadoutLabel,
			run.bankAccessAllowed() ? "Bank access: allowed" : "Bank access: locked — find or earn everything",
			timeModelLabel(loop),
			routeLocked,
			seedLabel,
			winCondition(rooms.size(), finalBoss),
			loseConditions(anyTimer));
	}

	private static long perRoomTimerMillis(RogueScapeRunLoop loop)
	{
		// A per-room timer only exists when stages are travel-gated; otherwise the limit (if any)
		// is a whole-run clock, surfaced in the time-model label rather than on each room.
		return loop != null && loop.hasTimeLimit() && loop.travelGatedStages() ? loop.timeLimitMillis() : 0L;
	}

	private static String loadoutLabelFromKit(StarterKit kit)
	{
		if (kit == null || kit.isEmpty())
		{
			return "Naked — start with nothing";
		}
		return kit.totalItems() + " starter item" + (kit.totalItems() == 1 ? "" : "s");
	}

	private static String seedLabel(String seed)
	{
		return seed == null || seed.trim().isEmpty() ? "(random — re-rolls each run)" : seed.trim();
	}

	static String modeLabel(RunMode mode)
	{
		if (mode == null) return "Scavenger";
		switch (mode)
		{
			case BANK_DRAFT: return "Boss Ladder";
			case CUSTOM_CREATOR: return "Custom";
			case REGION_CRAWL: return "Region Crawl";
			case SEEDED_RACE: return "Seeded Race";
			case FRESH_SOURCE:
			case UNSPECIFIED:
			default: return "Scavenger";
		}
	}

	private static String modeSummary(RunMode mode)
	{
		if (mode == null) return scavengerSummary();
		switch (mode)
		{
			case BANK_DRAFT: return "Short prep, then bosses hand you the loot that carries the run.";
			case CUSTOM_CREATOR: return "Your hand-drawn route — the rooms and boss you picked.";
			case REGION_CRAWL: return "Travel a chain of regions, clearing each one's objective.";
			case SEEDED_RACE: return "A fixed seeded route for races and high scores.";
			case FRESH_SOURCE:
			case UNSPECIFIED:
			default: return scavengerSummary();
		}
	}

	private static String scavengerSummary()
	{
		return "Earn power room by room — every upgrade must be found in the room you are standing in.";
	}

	private static String kindLabel(RoomKind kind, boolean boss)
	{
		if (boss)
		{
			return "Boss";
		}
		if (kind == null)
		{
			return "Room";
		}
		switch (kind)
		{
			case COMBAT: return "Combat room";
			case SUPPLY: return "Supply room";
			case WEAPON: return "Weapon room";
			case ARMOUR: return "Armour room";
			case SHOP: return "Shop room";
			case SKILLING: return "Skilling room";
			case CHOICE_CHEST: return "Choice chest";
			case BOSS: return "Boss";
			case REGION:
			default: return "Region room";
		}
	}

	private static String timeModelLabel(RogueScapeRunLoop loop)
	{
		if (loop == null || !loop.hasTimeLimit())
		{
			return "Timing: no limit — clear at your own pace";
		}
		String formatted = RunTimer.format(loop.timeLimitMillis());
		return loop.travelGatedStages()
			? "Timing: " + formatted + " timer in each room"
			: "Timing: " + formatted + " for the whole run";
	}

	private static String winCondition(int stageCount, String finalBoss)
	{
		StringBuilder sb = new StringBuilder("Clear all ").append(stageCount)
			.append(stageCount == 1 ? " stage" : " stages");
		if (finalBoss != null && !finalBoss.isEmpty())
		{
			sb.append(", finishing with ").append(finalBoss);
		}
		sb.append('.');
		return sb.toString();
	}

	private static List<String> loseConditions(boolean anyTimer)
	{
		List<String> out = new ArrayList<>();
		out.add("Die during the run.");
		if (anyTimer)
		{
			out.add("Let a room's timer run out before its objective is done.");
		}
		out.add("Abandon the run from the journal.");
		return out;
	}
}
