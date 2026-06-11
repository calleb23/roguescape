package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScoringRulesTest
{
	@Test
	public void balancedPresetCombinesItemsRoomsAndBosses()
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.BALANCED);
		// 3 legal*1 + 1 illegal*-3 + 2 rooms*5 + 1 boss*15 + relic 0 = 3 - 3 + 10 + 15 = 25
		int score = rules.calculateScore(3, 1, 2, 1, 600L, 0);
		assertEquals(25, score);
	}

	@Test
	public void speedrunPresetAddsTimeBonusUnderThreshold()
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.SPEEDRUN);
		int scoreFast = rules.calculateScore(0, 0, 0, 0, 1000L, 0);
		int scoreSlow = rules.calculateScore(0, 0, 0, 0, 2000L, 0);
		assertEquals("under-threshold run gets the +50 time bonus", 50, scoreFast);
		assertEquals("over-threshold run gets no time bonus", 0, scoreSlow);
	}

	@Test
	public void creatorChaosMultipliesRelicBonus()
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.CREATOR_CHAOS);
		// 1 legal*2 + 0 illegal + 0 rooms + 0 bosses + relic 10 * 2.0 = 2 + 20 = 22
		int score = rules.calculateScore(1, 0, 0, 0, 0L, 10);
		assertEquals(22, score);
	}

	@Test
	public void balancedPresetExposesParameterDefaults()
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.BALANCED);
		assertEquals(1, rules.basePointsPerLegalItem());
		assertEquals(5, rules.bonusPerClearedRoom());
		assertEquals(15, rules.bonusPerClearedBoss());
		assertFalse(rules.timeBonusEnabled());
		assertEquals(-3, rules.illegalPenaltyPerItem());
		assertEquals(1.0, rules.relicBonusMultiplier(), 0.0001);
	}

	@Test
	public void speedrunHalvesRelicBonus()
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.SPEEDRUN);
		// 0 items, 0 rooms/bosses, 4000 seconds (no time bonus), relic 10 * 0.5 = 5
		int score = rules.calculateScore(0, 0, 0, 0, 4000L, 10);
		assertEquals(5, score);
	}
}
