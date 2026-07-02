package com.pluginideahub.roguescape.core.briefing;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The briefing is the "nothing left to interpretation" contract the player reads before they
 * commit. These tests pin the route, gating, rules, and win/lose copy so the UI can trust it and
 * the wording cannot silently drift from the run that actually gets built.
 */
public class RunBriefingBuilderTest
{
	@Test
	public void seededRoutePreviewIsLockedAndDescribesRoomsAndBoss()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"fixed-seed", "Naked", false, 0);

		assertEquals("Scavenger", briefing.modeLabel());
		assertTrue("a seeded route is locked", briefing.routeLocked());
		assertTrue(briefing.roomCount() >= 1);
		assertEquals(1, briefing.bossCount());
		assertFalse(briefing.rooms().get(0).kindLabel().isEmpty());
		assertTrue("the route ends on a boss", briefing.rooms().get(briefing.rooms().size() - 1).bossStage());
		assertTrue(briefing.hasFinalBoss());
		assertTrue(briefing.winCondition().contains(briefing.finalBossName()));
	}

	@Test
	public void seededAutoRouteIsLocked()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"my-seed", "Naked", false, 0);

		assertTrue(briefing.routeLocked());
		assertEquals("my-seed", briefing.seedLabel());
		assertEquals(3, briefing.roomCount());
		assertEquals(1, briefing.bossCount());
	}

	@Test
	public void seedlessAutoRouteWarnsItWillReRoll()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"", "Naked", false, 0);

		assertFalse(briefing.routeLocked());
		assertTrue(briefing.seedLabel().contains("random"));
		assertTrue(String.join("\n", briefing.lines()).contains("re-roll"));
	}

	@Test
	public void rewardedModeLabelAndSummary()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED,
			"rewarded-seed", "Naked", false, 0);

		assertEquals("Boss Ladder", briefing.modeLabel());
		assertTrue(briefing.modeSummary().toLowerCase().contains("boss"));
		assertEquals(3, briefing.bossCount());
		assertEquals(0, briefing.roomCount());
	}

	@Test
	public void perRoomTimerShowsOnEveryStageAndInRulesAndLoseConditions()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"", "Naked", false, 5);

		for (RunBriefing.RoomLine room : briefing.rooms())
		{
			assertTrue("each stage should carry the per-room timer", room.hasTimer());
			assertTrue(room.gatingLabel().contains("05:00 timer"));
		}
		assertTrue(briefing.timeModelLabel().contains("05:00 timer in each room"));
		assertTrue(briefing.loseConditions().stream().anyMatch(l -> l.contains("timer run out")));
	}

	@Test
	public void noTimerMeansNoRoomTimerAndAPaceYourselfRule()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"", "Naked", false, 0);

		assertFalse(briefing.rooms().get(0).hasTimer());
		assertTrue(briefing.timeModelLabel().contains("no limit"));
		assertFalse(briefing.loseConditions().stream().anyMatch(l -> l.contains("timer")));
	}

	@Test
	public void gatingLabelDescribesObjectiveAndRegionLock()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"fixed-seed", "Naked", false, 0);

		RunBriefing.RoomLine firstRoom = briefing.rooms().get(0);
		assertFalse(firstRoom.bossStage());
		assertTrue(firstRoom.gatingLabel().contains("Collect"));
		assertTrue("library rooms are region locked", firstRoom.gatingLabel().contains("stay in region"));

		RunBriefing.RoomLine boss = briefing.rooms().get(briefing.rooms().size() - 1);
		assertTrue(boss.gatingLabel().contains("Defeat the boss"));
	}

	@Test
	public void loseConditionsCoverDeathAndAbandon()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"", "Naked", false, 0);

		assertTrue(briefing.loseConditions().stream().anyMatch(l -> l.toLowerCase().contains("die")));
		assertTrue(briefing.loseConditions().stream().anyMatch(l -> l.toLowerCase().contains("abandon")));
	}

	@Test
	public void ofReadsAnAlreadyBuiltRunAsLocked()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Hand built", "seed-x",
			RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(
			java.util.Arrays.asList("lumbridge-swamp", "draynor-village"), "boss-giant-mole", session, run);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L).setTravelGatedStages(true);

		RunBriefing briefing = RunBriefingBuilder.of(session, run, loop);

		assertTrue(briefing.routeLocked());
		assertEquals("Hand built", briefing.runTitle());
		assertEquals(3, briefing.rooms().size());
		assertEquals("Giant Mole", briefing.finalBossName());
	}

	@Test
	public void linesProduceAReadableTranscript()
	{
		RunBriefing briefing = RunBriefingBuilder.preview(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED,
			"seed-x", "Naked", false, 0);

		String text = String.join("\n", briefing.lines());
		assertNotNull(text);
		assertTrue(text.contains("THE ROUTE"));
		assertTrue(text.contains("THE RULES"));
		assertTrue(text.contains("WIN:"));
		assertTrue(text.contains("LOSE:"));
	}
}
