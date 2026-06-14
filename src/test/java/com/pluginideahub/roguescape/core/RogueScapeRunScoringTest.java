package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pins the W2 scoring unification: {@link RogueScapeRun#effectiveScore()} delegates to
 * {@link ScoringRules} via the mode-driven {@link ScoringPreset}, and cleared rooms/bosses now
 * contribute to the score.
 */
public class RogueScapeRunScoringTest
{
	private static RogueScapeRun roomBossRun(RunMode mode)
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Score", "", mode, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("B1", RunStageType.BOSS, "Goblin", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");
		return run;
	}

	private static int parity(RogueScapeRun run, long runSeconds)
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.forMode(run.session().mode()));
		return rules.calculateScore(run.session().runScore(), run.illegalCount(),
			run.clearedRooms(), run.clearedBosses(), runSeconds, run.relicEngine().scoreBonus());
	}

	@Test
	public void presetMappingFavoursBalancedByDefault()
	{
		assertEquals(ScoringPreset.BALANCED, ScoringPreset.forMode(RunMode.UNSPECIFIED));
		assertEquals(ScoringPreset.BALANCED, ScoringPreset.forMode(RunMode.FRESH_SOURCE));
		assertEquals(ScoringPreset.BALANCED, ScoringPreset.forMode(RunMode.BANK_DRAFT));
		assertEquals(ScoringPreset.BALANCED, ScoringPreset.forMode(RunMode.REGION_CRAWL));
		assertEquals(ScoringPreset.SPEEDRUN, ScoringPreset.forMode(RunMode.SEEDED_RACE));
		assertEquals(ScoringPreset.CREATOR_CHAOS, ScoringPreset.forMode(RunMode.CUSTOM_CREATOR));
		assertEquals(ScoringPreset.BALANCED, ScoringPreset.forMode(null));
	}

	@Test
	public void effectiveScoreDelegatesToScoringRules()
	{
		RogueScapeRun run = roomBossRun(RunMode.UNSPECIFIED);
		run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_LOOT);
		run.chooseRelic(RelicLibrary.gluttony()); // food scores +3 each
		run.applyItemDelta("Lobster", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(parity(run, Long.MAX_VALUE), run.effectiveScore());
	}

	@Test
	public void clearingStagesAddsRoomAndBossBonuses()
	{
		RogueScapeRun run = roomBossRun(RunMode.UNSPECIFIED);
		int before = run.effectiveScore();
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);
		run.session().recordCurrentStageLegalItemGain();
		loop.completeCurrentStage(1_000L);
		assertEquals(1, run.clearedRooms());
		assertTrue("clearing a room should raise the score", run.effectiveScore() > before);
		assertEquals(parity(run, Long.MAX_VALUE), run.effectiveScore());
	}

	@Test
	public void noTimeArgWithholdsSpeedrunBonus()
	{
		RogueScapeRun run = roomBossRun(RunMode.SEEDED_RACE); // -> SPEEDRUN (time bonus enabled)
		run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_LOOT);
		// No-arg score must NOT include the +50 time bonus (time unknown); a fast timed score must.
		int untimed = run.effectiveScore();
		int fast = run.effectiveScore(60L);
		assertEquals(untimed + 50, fast);
	}
}
