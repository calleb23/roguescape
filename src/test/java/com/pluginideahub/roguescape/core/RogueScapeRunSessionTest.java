package com.pluginideahub.roguescape.core;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.*;

public class RogueScapeRunSessionTest
{
	@Test
	public void runSessionTracksRoomsRewardsRelicsAndCompletion()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Barrows staircase dungeon crawl");

		session.enterRoom("Lumbridge Basement", "rats and one suspicious crate");
		session.observeItemGain("Bronze dagger", 1, RogueScapeRunSession.ItemSource.STARTER_KIT, "Lumbridge Basement", "starter weapon", 1);
		session.addRelic("Crate Goblin", "First supply found in every new room is doubled");
		session.enterRoom("Barbarian Village", "food check before stronghold");
		session.observeItemGain("Trout", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN, "Barbarian Village", "panic healing", 2);
		session.completeRun("Defeated Obor with backpack goblin energy");

		assertEquals("COMPLETE", session.state());
		assertEquals(2, session.roomCount());
		assertEquals(2, session.rewardCount());
		assertEquals(2, session.legalRewardCount());
		assertEquals(0, session.illegalRewardCount());
		assertEquals(1, session.relicCount());
		assertEquals(3, session.runScore());
		assertEquals("Barbarian Village", session.currentRoomName());
		assertTrue(session.overlaySummary().contains("Rooms: 2"));
		assertTrue(session.overlaySummary().contains("Legal: 2"));
		assertTrue(session.recapMarkdown().contains("Bronze dagger [STARTER_KIT] — starter weapon (+1)"));
		assertTrue(session.recapMarkdown().contains("Crate Goblin: First supply found in every new room is doubled"));
	}

	@Test
	public void legalFreshSourceRewardsScoreAndCountCorrectly()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Fresh-source legal scoring test");
		session.enterRoom("Grand Exchange", "buying supplies");
		session.observeItemGain("Shark", 5, RogueScapeRunSession.ItemSource.BOUGHT_DURING_RUN, "Grand Exchange", "healing", 3);
		session.observeItemGain("Oak logs", 10, RogueScapeRunSession.ItemSource.GATHERED_OR_CRAFTED, "Draynor Village", "fuel", 1);
		session.observeItemGain("Lobster", 1, RogueScapeRunSession.ItemSource.MANUALLY_APPROVED, "Fishing Guild", "bonus food", 2);

		assertEquals(3, session.legalRewardCount());
		assertEquals(0, session.illegalRewardCount());
		assertEquals(6, session.runScore());
		assertTrue(session.recapMarkdown().contains("Shark x5 [BOUGHT_DURING_RUN]"));
		assertTrue(session.recapMarkdown().contains("Oak logs x10 [GATHERED_OR_CRAFTED]"));
		assertTrue(session.recapMarkdown().contains("Lobster [MANUALLY_APPROVED]"));
	}

	@Test
	public void unknownItemSourceSurfacesAndCanFailRun()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Source integrity check");
		session.enterRoom("Wilderness", "high-risk zone");
		session.observeItemGain("Dragon bones", 1, RogueScapeRunSession.ItemSource.UNKNOWN_OR_ILLEGAL, "Wilderness", "suspicious drop", 10);

		assertEquals("FAILED", session.state());
		assertEquals(1, session.illegalRewardCount());
		assertEquals(0, session.legalRewardCount());
		assertEquals(1, session.violationCount());
		assertTrue(session.overlaySummary().contains("Failed: UNKNOWN_ITEM_SOURCE"));
		assertTrue(session.recapMarkdown().contains("Dragon bones [UNKNOWN_OR_ILLEGAL]"));
		assertTrue(session.recapMarkdown().contains("UNKNOWN_ITEM_SOURCE"));
	}

	@Test
	public void recapIncludesAllMetadataAndSourceStates()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Chaos Elemental hunt", "seed-42", "Boss Run");
		session.declareStarterKitItem("Tinderbox");
		session.declareStarterKitItem("Bread");
		session.enterRoom("Edgeville", "gateway to wilderness");
		session.observeItemGain("Knife", 1, RogueScapeRunSession.ItemSource.BOUGHT_DURING_RUN, "Edgeville", "utility", 1);
		session.recordViolation("Fell into Wilderness ditch", RogueScapeRunSession.RunEnding.DEATH);

		String recap = session.recapMarkdown();
		assertTrue(recap.contains("seed-42"));
		assertTrue(recap.contains("Boss Run"));
		assertTrue(recap.contains("Tinderbox"));
		assertTrue(recap.contains("Bread"));
		assertTrue(recap.contains("Edgeville"));
		assertTrue(recap.contains("BOUGHT_DURING_RUN"));
		assertTrue(recap.contains("DEATH"));
		assertTrue(recap.contains("FAILED"));
	}

	@Test
	public void afterCompletionFurtherRewardMutationsAreIgnored()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Quick boss run");
		session.enterRoom("Slayer Tower", "entry room");
		session.observeItemGain("Rune scimitar", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN, "Slayer Tower", "main weapon", 5);
		session.completeRun("Boss killed clean");

		session.observeItemGain("Dragon sword", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN, "", "phantom loot", 20);
		session.enterRoom("Phantom Room", "should not record");
		session.addRelic("Ghost Relic", "should not record");

		assertEquals("COMPLETE", session.state());
		assertEquals(1, session.rewardCount());
		assertEquals(1, session.roomCount());
		assertEquals(0, session.relicCount());
	}

	@Test
	public void afterFailureFurtherRewardMutationsAreIgnored()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Doomed run");
		session.enterRoom("Death Room", "entry");
		session.recordViolation("Died to level 3 rat", RogueScapeRunSession.RunEnding.DEATH);

		session.observeItemGain("Dragon platebody", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN, "", "too late", 100);
		session.enterRoom("Afterlife Room", "should not record");

		assertEquals("FAILED", session.state());
		assertEquals(0, session.rewardCount());
		assertEquals(1, session.roomCount());
	}

	@Test
	public void bankingDeathOrExtraInventoryCanEndRun()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("No bank, no death, no second inventory");

		session.enterRoom("Varrock Sewers", "one bag only");
		session.recordViolation("Opened bank chest to refill", RogueScapeRunSession.RunEnding.BANK_USED);
		session.keepReward("Mossy key", "tempting but too late", 5);

		assertEquals("FAILED", session.state());
		assertEquals(1, session.violationCount());
		assertEquals(0, session.rewardCount());
		assertTrue(session.overlaySummary().contains("Failed: BANK_USED"));
		assertTrue(session.recapMarkdown().contains("Opened bank chest to refill"));
	}

	// ---------- Stage 1: Pure Run Engine ----------

	@Test
	public void roguescapeRunStartsWithGoalSeedModeAndPreset()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor",
			"seed-42",
			RunMode.FRESH_SOURCE,
			RunPreset.GOBLIN_RAT);

		assertEquals(RunState.ACTIVE, session.runState());
		assertEquals("Defeat Obor", session.goal());
		assertEquals("seed-42", session.seed());
		assertEquals(RunMode.FRESH_SOURCE, session.mode());
		assertEquals(RunPreset.GOBLIN_RAT, session.preset());
	}

	@Test
	public void routeStoresRoomsAndBossInDefinedOrder()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor", "seed-42",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		session.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		session.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		RunRoute route = session.route();
		List<RunStage> stages = route.stages();
		assertEquals(3, route.size());
		assertEquals("R1", stages.get(0).id());
		assertEquals(RunStageType.ROOM, stages.get(0).type());
		assertEquals("Lumbridge", stages.get(0).name());
		assertEquals("R2", stages.get(1).id());
		assertEquals("B1", stages.get(2).id());
		assertEquals(RunStageType.BOSS, stages.get(2).type());
		assertEquals("Obor", stages.get(2).name());
	}

	@Test
	public void advancingAndClearingStagesPreservesTimelineOrder()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor", "seed-42",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		session.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		session.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		session.enterStage("R1");
		session.clearStage("R1");
		session.enterStage("R2");
		session.clearStage("R2");
		session.enterStage("B1");
		session.clearStage("B1");

		List<RunTimelineEvent> stageEvents = session.timeline().stream()
			.filter(e -> e.type() == RunTimelineEvent.Type.STAGE_ENTERED
				|| e.type() == RunTimelineEvent.Type.STAGE_CLEARED)
			.collect(Collectors.toList());

		assertEquals(6, stageEvents.size());
		assertEquals(RunTimelineEvent.Type.STAGE_ENTERED, stageEvents.get(0).type());
		assertTrue(stageEvents.get(0).description().contains("R1"));
		assertEquals(RunTimelineEvent.Type.STAGE_CLEARED, stageEvents.get(1).type());
		assertTrue(stageEvents.get(1).description().contains("R1"));
		assertEquals(RunTimelineEvent.Type.STAGE_ENTERED, stageEvents.get(2).type());
		assertTrue(stageEvents.get(2).description().contains("R2"));
		assertEquals(RunTimelineEvent.Type.STAGE_CLEARED, stageEvents.get(3).type());
		assertEquals(RunTimelineEvent.Type.STAGE_ENTERED, stageEvents.get(4).type());
		assertTrue(stageEvents.get(4).description().contains("B1"));
		assertEquals(RunTimelineEvent.Type.STAGE_CLEARED, stageEvents.get(5).type());
		assertTrue(stageEvents.get(5).description().contains("B1"));

		int lastSeq = -1;
		for (RunTimelineEvent e : session.timeline())
		{
			assertTrue("timeline sequence must be strictly increasing", e.sequence() > lastSeq);
			lastSeq = e.sequence();
		}
	}

	@Test
	public void clearingFinalBossAndCompletingRunRecordsCompletionReason()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor", "seed-42",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		session.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		session.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		session.enterStage("R1"); session.clearStage("R1");
		session.enterStage("R2"); session.clearStage("R2");
		session.enterStage("B1"); session.clearStage("B1");

		session.completeRun("Obor down clean", RunCompletionReason.GOAL_COMPLETE);

		assertEquals(RunState.COMPLETE, session.runState());
		assertEquals(RunCompletionReason.GOAL_COMPLETE, session.completionReason());
		assertTrue(session.route().isComplete());
		assertEquals(3, session.route().clearedCount());
	}

	@Test
	public void recapIncludesRogueScapeBrandingModeSeedPresetStagesScoreAndCompletionReason()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor", "seed-42",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		session.addStage("B1", RunStageType.BOSS, "Obor", "first boss");
		session.enterStage("R1");
		session.observeItemGain("Bronze dagger", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN,
			"Lumbridge", "starter weapon", 3);
		session.clearStage("R1");
		session.enterStage("B1"); session.clearStage("B1");
		session.completeRun("Obor down", RunCompletionReason.GOAL_COMPLETE);

		String recap = session.recapMarkdown();
		assertTrue("recap should mention RogueScape", recap.contains("RogueScape"));
		assertTrue("recap should mention mode", recap.contains("FRESH_SOURCE"));
		assertTrue("recap should mention seed", recap.contains("seed-42"));
		assertTrue("recap should mention preset", recap.contains("GOBLIN_RAT"));
		assertTrue("recap should list R1", recap.contains("R1"));
		assertTrue("recap should list B1", recap.contains("B1"));
		assertTrue("recap should show score", recap.contains("Score"));
		assertTrue("recap should include completion reason", recap.contains("GOAL_COMPLETE"));
	}

	@Test
	public void failureReasonSurfacesInOverlayAndRecap()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Survive Wilderness", "seed-x",
			RunMode.REGION_CRAWL, RunPreset.WILDERNESS_RAT);

		session.addStage("R1", RunStageType.ROOM, "Edgeville", "gateway");
		session.enterStage("R1");
		session.recordViolation("Died to pker", RogueScapeRunSession.RunEnding.DEATH);

		assertEquals(RunState.FAILED, session.runState());
		assertEquals(RogueScapeRunSession.RunEnding.DEATH, session.failureReason());
		assertTrue(session.overlaySummary().contains("Failed: DEATH"));
		assertTrue(session.recapMarkdown().contains("DEATH"));
		assertTrue(session.recapMarkdown().contains("Died to pker"));
	}

	@Test
	public void simulatedR1ToR2ToB1RunCompletesWithRecap()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Defeat Obor", "stage1-seed",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		session.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		session.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		session.enterStage("R1");
		session.observeItemGain("Bronze dagger", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN,
			"Lumbridge", "starter weapon", 1);
		session.clearStage("R1");

		session.enterStage("R2");
		session.observeItemGain("Trout", 2, RogueScapeRunSession.ItemSource.GATHERED_OR_CRAFTED,
			"Barbarian Village", "healing", 2);
		session.clearStage("R2");

		session.enterStage("B1");
		session.observeItemGain("Obor key", 1, RogueScapeRunSession.ItemSource.FOUND_DURING_RUN,
			"Obor", "trophy", 5);
		session.clearStage("B1");
		session.completeRun("Obor down clean", RunCompletionReason.GOAL_COMPLETE);

		assertEquals(RunState.COMPLETE, session.runState());
		assertEquals(8, session.runScore());
		assertTrue(session.route().isComplete());
		assertEquals(RunCompletionReason.GOAL_COMPLETE, session.completionReason());

		String recap = session.recapMarkdown();
		assertTrue(recap.contains("R1"));
		assertTrue(recap.contains("R2"));
		assertTrue(recap.contains("B1"));
		assertTrue(recap.contains("Lumbridge"));
		assertTrue(recap.contains("Barbarian Village"));
		assertTrue(recap.contains("Obor"));
		assertTrue(recap.contains("GOAL_COMPLETE"));
	}

	@Test
	public void afterCompletionStageAndTimelineMutationsAreIgnored()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Quick run", "seed-z",
			RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);

		session.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter");
		session.addStage("B1", RunStageType.BOSS, "Obor", "boss");
		session.enterStage("R1"); session.clearStage("R1");
		session.enterStage("B1"); session.clearStage("B1");
		session.completeRun("done", RunCompletionReason.GOAL_COMPLETE);

		int timelineSizeBefore = session.timeline().size();
		session.addStage("R99", RunStageType.ROOM, "Phantom", "ignored");
		session.enterStage("R99");
		assertEquals(2, session.route().size());
		assertEquals(timelineSizeBefore, session.timeline().size());
	}
}
