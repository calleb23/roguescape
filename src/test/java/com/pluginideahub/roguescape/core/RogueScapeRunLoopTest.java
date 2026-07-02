package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.reward.ChestType;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.task.RoomTask;
import com.pluginideahub.roguescape.core.unlock.RunUnlockType;
import org.junit.Test;

import static org.junit.Assert.*;
import java.util.Collections;

public class RogueScapeRunLoopTest
{
	private static RogueScapeRunLoop loopWithTwoStages()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Loop test", "seed-loop", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter room");
		base.addStage("B1", RunStageType.BOSS, "Obor", "boss room");
		base.enterStage("R1");
		return new RogueScapeRunLoop(RogueScapeRun.wrap(base), 1_000L);
	}

	private static void completeObjective(RogueScapeRunLoop loop)
	{
		loop.run().session().recordCurrentStageItemGain();
	}

	@Test
	public void completingRoomMovesToBaseRewardWithoutEnteringNextStage()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();

		assertEquals(RunPhase.ROOM_ACTIVE, loop.phase());
		completeObjective(loop);
		loop.completeCurrentStage(61_000L);

		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertEquals("R1", loop.completedStageId());
		assertEquals("R1", loop.run().session().route().stageById("R1").id());
		assertTrue(loop.run().session().route().stageById("R1").isCleared());
		assertFalse(loop.run().session().route().stageById("B1").isEntered());
		assertNotNull(loop.pendingRewardDraft());
	}

	@Test
	public void choosingRewardThenStartingNextStageEntersBoss()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);
		RewardDraft draft = loop.pendingRewardDraft();

		loop.chooseReward(draft.options().get(0).optionId(), 12_000L);
		loop.startNextStage(15_000L);

		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());
		assertTrue(loop.run().session().route().stageById("B1").isEntered());
		assertTrue(draft.isSelected());
	}

	@Test
	public void skippingRewardThenStartingNextStageEntersBoss()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);

		loop.skipReward(11_000L);
		loop.startNextStage(12_000L);

		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());
		assertTrue(loop.run().session().route().stageById("B1").isEntered());
		assertTrue(loop.pendingRewardDraft().isRejected());
	}

	@Test
	public void disabledBaseRewardsMoveDirectlyToNextStage()
	{
		RogueScapeRunLoop loop = loopWithTwoStages().setBaseRewardsEnabled(false);
		completeObjective(loop);

		loop.completeCurrentStage(10_000L);

		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());
		assertNull(loop.pendingRewardDraft());
		assertTrue(loop.run().session().route().stageById("B1").isEntered());
	}

	@Test
	public void completingFinalStageCompletesRun()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);
		loop.skipReward(11_000L);
		loop.startNextStage(12_000L);

		loop.run().recordBossDefeatSignal("test boss kill");
		loop.completeCurrentStage(90_000L);

		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertNotNull(loop.pendingRewardDraft());

		loop.skipReward(91_000L);
		loop.startNextStage(92_000L);

		assertEquals(RunPhase.RUN_COMPLETE, loop.phase());
		assertEquals(RunState.COMPLETE, loop.run().session().runState());
	}

	@Test
	public void bossStageCannotCompleteUntilBossSignal()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);
		loop.skipReward(11_000L);
		loop.startNextStage(12_000L);

		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());
		assertFalse(loop.canCompleteCurrentStage());
		loop.completeCurrentStage(13_000L);
		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());

		loop.run().recordBossDefeatSignal("test boss kill");

		assertTrue(loop.canCompleteCurrentStage());
		loop.completeCurrentStage(14_000L);
		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		loop.skipReward(15_000L);
		loop.startNextStage(16_000L);
		assertEquals(RunPhase.RUN_COMPLETE, loop.phase());
	}

	@Test
	public void finalStageRewardChoiceAppliesBeforeRunCompletes()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);
		loop.skipReward(11_000L);
		loop.startNextStage(12_000L);

		loop.run().recordBossDefeatSignal("test boss kill");
		loop.completeCurrentStage(20_000L);
		RewardDraft draft = loop.pendingRewardDraft();

		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertNotNull(draft);
		loop.chooseReward(draft.options().get(0).optionId(), 21_000L);
		loop.startNextStage(22_000L);

		assertTrue(draft.isSelected());
		assertEquals(RunPhase.RUN_COMPLETE, loop.phase());
		assertEquals(RunState.COMPLETE, loop.run().session().runState());
		assertFalse(loop.run().drafts().isEmpty());
	}

	@Test
	public void timerUsesInjectedMillisAndFormatsElapsed()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.markNow(62_500L);

		assertEquals(61_500L, loop.runElapsedMillis());
		assertEquals(61_500L, loop.phaseElapsedMillis());
		assertEquals("01:01", RunTimer.format(61_500L));
		assertEquals("1:01:01", RunTimer.format(3_661_000L));
	}

	@Test
	public void timeLimitFailsRunWhenElapsed()
	{
		RogueScapeRunLoop loop = loopWithTwoStages().setTimeLimitMillis(60_000L);

		loop.markNow(60_000L);
		assertEquals(RunState.ACTIVE, loop.run().session().runState());
		assertEquals("00:01", loop.timeRemainingLabel());

		loop.markNow(61_000L);
		assertEquals(RunState.FAILED, loop.run().session().runState());
		assertEquals(RunPhase.RUN_FAILED, loop.phase());
		assertEquals(RogueScapeRunSession.RunEnding.TIME_LIMIT, loop.run().session().failureReason());
		assertEquals("00:00", loop.timeRemainingLabel());
	}

	@Test
	public void travelGatedRunStartsRoomTimerWhenEnteringLegalRegion()
	{
		RogueScapeRunLoop loop = loopWithTwoStages().setTravelGatedStages(true).setTimeLimitMillis(60_000L);
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.REGION, Collections.singleton("lumbridge"), true));

		loop.markNow(20_000L);
		assertEquals(RunPhase.TRAVEL_TO_STAGE, loop.phase());
		assertEquals("01:00", loop.timeRemainingLabel());
		assertFalse(loop.run().regionRestrictionArmed());

		loop.run().moveToRegion("lumbridge");
		assertTrue(loop.notifyRegionChanged(25_000L));

		assertEquals(RunPhase.ROOM_ACTIVE, loop.phase());
		assertTrue(loop.run().regionRestrictionArmed());
		assertEquals(0L, loop.phaseElapsedMillis());
		assertEquals("01:00", loop.timeRemainingLabel());
	}

	@Test
	public void travelGatedRoomTimeoutMovesToRewardInsteadOfFailing()
	{
		RogueScapeRunLoop loop = loopWithTwoStages().setTravelGatedStages(true).setTimeLimitMillis(10_000L);
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, Collections.singleton("lumbridge"), true));
		loop.run().moveToRegion("lumbridge");
		loop.notifyRegionChanged(5_000L);

		loop.markNow(15_000L);

		assertEquals(RunState.ACTIVE, loop.run().session().runState());
		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertTrue(loop.run().session().route().stageById("R1").isCleared());
		assertNotNull(loop.pendingRewardDraft());
		assertTrue(loop.run().potionUnlocked());
	}

	@Test
	public void travelGatedNextStageReturnsToTravelWithFreshTimer()
	{
		RogueScapeRunLoop loop = loopWithTwoStages().setTravelGatedStages(true).setTimeLimitMillis(60_000L);
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, Collections.singleton("lumbridge"), true));
		loop.run().setRegionRule("B1", new StageRegionRule(RoomKind.BOSS, Collections.singleton("obor"), false));

		loop.run().moveToRegion("lumbridge");
		assertTrue(loop.notifyRegionChanged(5_000L));
		completeObjective(loop);
		loop.completeCurrentStage(10_000L);
		loop.skipReward(11_000L);
		loop.startNextStage(20_000L);

		assertEquals(RunPhase.TRAVEL_TO_STAGE, loop.phase());
		assertTrue(loop.run().session().route().stageById("B1").isEntered());
		assertFalse(loop.run().regionRestrictionArmed());
		assertEquals("01:00", loop.timeRemainingLabel());
		assertFalse(loop.canCompleteCurrentStage());
		assertTrue(loop.completionBlockReason().contains("Travel"));

		loop.markNow(50_000L);
		assertEquals("01:00", loop.timeRemainingLabel());

		loop.run().moveToRegion("obor");
		assertTrue(loop.notifyRegionChanged(55_000L));
		assertEquals(RunPhase.BOSS_ACTIVE, loop.phase());
		assertTrue(loop.run().regionRestrictionArmed());
		assertEquals(0L, loop.phaseElapsedMillis());
	}

	@Test
	public void baseRewardDraftOffersThreeRelicOptions()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		completeObjective(loop);
		loop.completeCurrentStage(5_000L);

		RewardDraft draft = loop.pendingRewardDraft();
		assertEquals(ChestType.RELIC, draft.chestType());
		assertEquals(3, draft.options().size());
		assertTrue(draft.options().get(0).isRelic());
		assertEquals("R1", draft.stageId());
	}

	@Test
	public void supplyRoomRewardDraftOffersSupplies()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, Collections.singleton("lumbridge"), true));
		completeObjective(loop);

		loop.completeCurrentStage(5_000L);

		RewardDraft draft = loop.pendingRewardDraft();
		assertEquals(ChestType.SUPPLY, draft.chestType());
		assertEquals(3, draft.options().size());
		assertFalse(draft.options().get(0).isRelic());
	}

	@Test
	public void typedRoomRewardDraftCanGrantChosenUnlock()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.WEAPON, Collections.singleton("lumbridge"), true));
		completeObjective(loop);
		loop.completeCurrentStage(5_000L);

		RewardDraft draft = loop.pendingRewardDraft();
		assertEquals(ChestType.UNLOCK, draft.chestType());
		assertTrue(draft.options().get(0).isUnlock());

		loop.chooseReward(draft.options().get(0).optionId(), 6_000L);

		assertTrue(loop.run().hasUnlock(draft.options().get(0).unlock().type()));
	}

	@Test
	public void completionIsBlockedUntilTrackableObjectiveIsDone()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();

		assertFalse(loop.canCompleteCurrentStage());
		loop.completeCurrentStage(10_000L);

		assertEquals(RunPhase.ROOM_ACTIVE, loop.phase());
		assertFalse(loop.run().session().route().stageById("R1").isCleared());
		assertTrue(loop.completionBlockReason().contains("Objective incomplete"));

		completeObjective(loop);

		assertTrue(loop.canCompleteCurrentStage());
		loop.completeCurrentStage(11_000L);
		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertTrue(loop.run().session().route().stageById("R1").isCleared());
	}

	@Test
	public void forceCompleteBypassesTrackableObjectiveForDeveloperControls()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();

		assertFalse(loop.canCompleteCurrentStage());
		loop.forceCompleteCurrentStage(10_000L);

		assertEquals(RunPhase.BASE_REWARD, loop.phase());
		assertTrue(loop.run().session().route().stageById("R1").isCleared());
	}

	@Test
	public void completingRoomGrantsUnlockFromRoomKind()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, Collections.singleton("lumbridge"), true));
		completeObjective(loop);

		loop.completeCurrentStage(10_000L);

		assertTrue(loop.run().hasUnlock(RunUnlockType.POTION));
		assertTrue(loop.run().potionUnlocked());
		assertFalse(loop.run().unlocks().isEmpty());
	}

	@Test
	public void statChangedCompletesRoomTaskObjective()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Task loop");
		RunStage stage = base.addStage("R1", RunStageType.ROOM, "Tree Room", "skill room",
			"Gain Woodcutting XP", RunObjectiveKind.SKILLING_RESOURCE, 0);
		stage.setRoomTask(new RoomTask("Gain skilling XP in Tree Room", "", 1));
		base.enterStage("R1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);

		assertFalse(loop.canCompleteCurrentStage());
		assertTrue(loop.run().recordStatChanged("Woodcutting", 10));

		assertTrue(loop.canCompleteCurrentStage());
	}
}
