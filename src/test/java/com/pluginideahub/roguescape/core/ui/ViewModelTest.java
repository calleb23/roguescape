package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.*;

public class ViewModelTest
{
	@Test
	public void overlayReflectsRunCounts()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Demo run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");
		run.applyItemDelta("Bronze dagger", 1, ProvenanceHint.OBSERVED_LOOT);

		OverlayViewModel overlay = OverlayViewModel.from(run);
		assertEquals("Demo run", overlay.goal());
		assertEquals("Lumbridge", overlay.currentRoom());
		assertEquals(1, overlay.itemsCollected());
		assertTrue(overlay.currentRegionLegal());
		assertTrue(overlay.warnings().isEmpty());
	}

	@Test
	public void lobbyViewModelShowsModeGoalAndRules()
	{
		SidePanelViewModel vm = SidePanelViewModel.lobby(
			RunMode.FRESH_SOURCE,
			"Kill Obor",
			"No bank\nNo trading"
		);
		assertTrue(vm.isLobby());
		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("ROGUESCAPE")));
		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("Kill Obor")));
		assertTrue(vm.ruleRows().stream().anyMatch(s -> s.contains("No bank")));
		assertTrue(vm.isActionEnabled(PanelAction.START_RUN));
	}

	@Test
	public void activeViewModelShowsRoomAndActions()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Panel run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("R2", RunStageType.ROOM, "Varrock", "");
		base.enterStage("R1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 1_000L);
		loop.markNow(62_000L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertFalse(vm.isLobby());
		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("RUNNING")));
		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("01:01")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.contains("Lumbridge")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("The task:")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.contains("Finish the task to stamp this chapter")));
		assertFalse(vm.isActionEnabled(PanelAction.COMPLETE_STAGE));
		assertTrue(vm.isActionEnabled(PanelAction.FAIL_RUN));
	}

	@Test
	public void travelViewModelShowsDestinationAndWaitsForEntry()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Travel run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "", "Find one legal upgrade", 1);
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, Collections.singleton("lumbridge"), true));
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L)
			.setTravelGatedStages(true)
			.setTimeLimitMillis(60_000L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		assertEquals(RunPhase.TRAVEL_TO_STAGE.getDisplayName(), vm.phaseLabel());
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("CURRENT: Travel")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Travel to Lumbridge.")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.contains("The task waits there: Find one legal upgrade")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Allowed regions: lumbridge")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Room timer: 01:00 once you enter")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.contains("Walk to the allowed room")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Movement is allowed during Travel.")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Pickups outside the target room are blocked.")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Bank, trade, prayer, and potion rules still apply.")));
		assertFalse(vm.isActionEnabled(PanelAction.COMPLETE_STAGE));
		assertTrue(vm.isActionEnabled(PanelAction.FAIL_RUN));
	}

	@Test
	public void activeViewModelShowsObjectiveCompletion()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Objective run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "", "Find one legal upgrade", 1);
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.applyItemDelta("Bronze dagger", 1, ProvenanceHint.OBSERVED_LOOT);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		assertTrue(vm.statusRows().stream().anyMatch(s -> s.contains("The task is done")));
		assertTrue(vm.routeRows().stream().anyMatch(s -> s.contains("1 / 1")));
		assertTrue(vm.isActionEnabled(PanelAction.COMPLETE_STAGE));
	}

	@Test
	public void rewardPhaseEnablesChooseRewardAndSkip()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Reward run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("R2", RunStageType.ROOM, "Varrock", "");
		base.enterStage("R1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);
		base.recordCurrentStageItemGain();
		loop.completeCurrentStage(1_000L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(vm.isActionEnabled(PanelAction.CHOOSE_REWARD_1));
		assertTrue(vm.isActionEnabled(PanelAction.CHOOSE_REWARD_2));
		assertTrue(vm.isActionEnabled(PanelAction.CHOOSE_REWARD_3));
		// Rewards are now relic chests — three relic options are offered.
		assertEquals(3L, vm.statusRows().stream().filter(s -> s.contains("[RELIC]")).count());
		assertTrue(vm.isActionEnabled(PanelAction.SKIP_REWARD));
		assertFalse(vm.isActionEnabled(PanelAction.NEXT_STAGE));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.equals("Choose one before the page turns.")));
	}

	@Test
	public void finalRewardPhaseShowsFinishRunCopy()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Final reward run");
		base.addStage("B1", RunStageType.BOSS, "Obor", "");
		base.enterStage("B1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);
		run.recordBossDefeatSignal("test");
		loop.completeCurrentStage(1_000L);

		SidePanelViewModel choosing = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(choosing.statusRows().stream().anyMatch(s -> s.equals("Choose one final reward — the journal is nearly full.")));
		assertTrue(choosing.isActionEnabled(PanelAction.CHOOSE_REWARD_1));

		loop.skipReward(2_000L);
		SidePanelViewModel resolved = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(resolved.statusRows().stream().anyMatch(s -> s.equals("Final reward resolved. Finish the run when ready.")));
		assertTrue(resolved.isActionEnabled(PanelAction.NEXT_STAGE));
	}

	@Test
	public void activeViewModelExposesModifiersAndProgression()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Prog run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("B1", RunStageType.BOSS, "Obor", "");
		base.enterStage("R1");
		base.clearStage("R1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);
		loop.setTimeLimitMillis(120_000L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		// Modifiers reflect real run rules.
		assertTrue(vm.modifierRows().stream().anyMatch(s -> s.startsWith("Bank unlocks:")));
		assertTrue(vm.modifierRows().stream().anyMatch(s -> s.startsWith("Time limit:")));

		// Progression reflects route tallies.
		assertEquals(1, vm.bossesTotal());
		assertEquals(0, vm.bossesDefeated());
		assertTrue(vm.progressionRows().stream().anyMatch(s -> s.equals("Rooms cleared: 1 / 1")));
		assertTrue(vm.progressionRows().stream().anyMatch(s -> s.startsWith("Score:")));
	}

	@Test
	public void bossViewModelWaitsForBossSignal()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Boss view");
		base.addStage("B1", RunStageType.BOSS, "Obor", "");
		base.enterStage("B1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);

		SidePanelViewModel waiting = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(waiting.statusRows().stream().anyMatch(s -> s.contains("0 / 1")));
		assertFalse(waiting.isActionEnabled(PanelAction.COMPLETE_STAGE));

		run.recordBossDefeatSignal("test");
		SidePanelViewModel ready = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(ready.statusRows().stream().anyMatch(s -> s.contains("The task is done")));
		assertTrue(ready.isActionEnabled(PanelAction.COMPLETE_STAGE));
	}

	@Test
	public void activeRulesShowRelicRestrictions()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Restrict view");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.ModifierLibrary.noFood());
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.ModifierLibrary.twoFoodMax());
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		assertTrue("relic restriction should appear in the live rules",
			vm.statusRows().stream().anyMatch(s -> s.contains("Food") && s.contains("(relic)") && s.contains("✗")));
		assertTrue("relic cap should appear in the live rules",
			vm.statusRows().stream().anyMatch(s -> s.contains("Max 2 Food") && s.contains("(relic)")));
		// And held relics show in the modifiers section.
		assertTrue(vm.modifierRows().stream().anyMatch(s -> s.contains("No Food")));
		assertTrue("held relics should feed the artifacts section",
			vm.relicRows().stream().anyMatch(s -> s.contains("No Food")));
	}

	@Test
	public void completedRunShowsCompletedState()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Done run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		base.completeRun("Done", com.pluginideahub.roguescape.core.RunCompletionReason.MANUAL_SUCCESS);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("COMPLETED")));
	}

	@Test
	public void completedRunShowsRecap()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Recap run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.RelicLibrary.gluttony());
		base.completeRun("Done", com.pluginideahub.roguescape.core.RunCompletionReason.MANUAL_SUCCESS);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("✓ The run is complete")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("Time afoot:")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("Chapters:")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("Bosses:")));
		assertTrue("recap lists held relics",
			vm.statusRows().stream().anyMatch(s -> s.contains("Gluttony")));
		assertTrue("completed runs keep artifact rows available",
			vm.relicRows().stream().anyMatch(s -> s.contains("Gluttony")));
	}

	@Test
	public void failedRunShowsFailedStateAndResetOnly()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Failed run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		base.recordViolation("Manual fail", RogueScapeRunSession.RunEnding.MANUAL_FAIL);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);

		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		assertTrue(vm.headerRows().stream().anyMatch(s -> s.contains("FAILED")));
		assertTrue(vm.statusRows().stream().anyMatch(s -> s.startsWith("\u2717 The run has failed")));
		assertTrue(vm.isActionEnabled(PanelAction.RESET_RUN));
		assertFalse(vm.isActionEnabled(PanelAction.FAIL_RUN));
		assertFalse(vm.isActionEnabled(PanelAction.COMPLETE_STAGE));
	}
}
