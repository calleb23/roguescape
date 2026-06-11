package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RogueScapePlayableFlowTest
{
	@Test
	public void scavengerGeneratedRunCanPlayThroughEveryStage()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Scavenger smoke", "scavenger-flow", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED, "scavenger-flow", session, run);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L)
			.setTravelGatedStages(true);

		int stageCount = session.route().size();
		playAllStages(loop, 10_000L);

		assertEquals(RunPhase.RUN_COMPLETE, loop.phase());
		assertEquals(RunState.COMPLETE, session.runState());
		assertEquals(stageCount, run.drafts().size());
	}

	@Test
	public void rewardedGeneratedRunCanPlayThroughEveryStage()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"Rewarded smoke", "rewarded-flow", RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED, "rewarded-flow", session, run);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L)
			.setTravelGatedStages(true);

		int stageCount = session.route().size();
		playAllStages(loop, 10_000L);

		assertEquals(RunPhase.RUN_COMPLETE, loop.phase());
		assertEquals(RunState.COMPLETE, session.runState());
		assertEquals(stageCount, run.drafts().size());
	}

	@Test
	public void customMixedRunCanPlayThroughEveryStage()
	{
		RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
			RogueScapeCustomRunFactory.Config.builder()
				.goal("Custom smoke")
				.seed("custom-flow")
				.customMode("Rewarded")
				.roomIds(Arrays.asList("lumbridge-swamp", "boss-king-black-dragon", "edgeville"))
				.roomAllowances(Arrays.asList("Weapons", "Boss", "Shopping"))
				.startedAtMillis(1_000L));

		int stageCount = started.session().route().size();
		playAllStages(started.loop(), 10_000L);

		assertEquals(RunPhase.RUN_COMPLETE, started.loop().phase());
		assertEquals(RunState.COMPLETE, started.session().runState());
		assertEquals(stageCount, started.run().drafts().size());
	}

	private static void playAllStages(RogueScapeRunLoop loop, long now)
	{
		while (loop.phase() != RunPhase.RUN_COMPLETE)
		{
			if (loop.phase() == RunPhase.TRAVEL_TO_STAGE)
			{
				moveIntoCurrentStage(loop);
				assertTrue(loop.notifyRegionChanged(now));
			}
			else if (loop.phase() == RunPhase.ROOM_ACTIVE || loop.phase() == RunPhase.BOSS_ACTIVE)
			{
				loop.forceCompleteCurrentStage(now + 1_000L);
			}
			else if (loop.phase() == RunPhase.BASE_REWARD)
			{
				RewardDraft draft = loop.pendingRewardDraft();
				assertFalse(draft.options().isEmpty());
				loop.chooseReward(draft.options().get(0).optionId(), now + 2_000L);
				loop.startNextStage(now + 3_000L);
			}
			else
			{
				throw new AssertionError("Unexpected phase: " + loop.phase());
			}
			now += 10_000L;
		}
	}

	private static void moveIntoCurrentStage(RogueScapeRunLoop loop)
	{
		StageRegionRule rule = loop.run().currentStageRule();
		if (!rule.restrictsRegion() || rule.allowedRegionIds().isEmpty())
		{
			return;
		}
		loop.run().moveToRegion(rule.allowedRegionIds().iterator().next());
	}
}
