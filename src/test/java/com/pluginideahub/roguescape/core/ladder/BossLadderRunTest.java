package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RestrictionOutcome;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Chunk 3 of the Boss Ladder MVP: the PREP → FIGHT → REWARD state machine over RunRestrictions.
 */
public class BossLadderRunTest
{
	private static BossLadderRun ladder()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW,
			EnumSet.of(Curse.SEALED_BANK, Curse.FAMINE));
		return new BossLadderRun(Arrays.asList("Giant Mole", "Scorpia", "Vorkath"), r);
	}

	@Test
	public void climbsPrepFightRewardPerBossThenCompletes()
	{
		BossLadderRun run = ladder();
		assertEquals(LadderPhase.PREP, run.phase());
		assertEquals("Giant Mole", run.currentBoss());

		run.beginFight();
		assertEquals(LadderPhase.FIGHT, run.phase());
		run.recordBossKill();
		assertEquals(LadderPhase.REWARD, run.phase());
		assertEquals(1, run.bossesDefeated());

		run.skipReward();
		assertEquals(LadderPhase.PREP, run.phase());
		assertEquals("Scorpia", run.currentBoss());

		run.beginFight();
		run.recordBossKill();
		run.chooseReward(RelicLibrary.breadOfTheWanderer());

		run.beginFight();
		run.recordBossKill();
		run.skipReward();
		assertEquals(LadderPhase.COMPLETE, run.phase());
		assertTrue(run.isOver());
		assertEquals(3, run.bossesDefeated());
	}

	@Test
	public void prepTemporarilyAllowsBankAndGe()
	{
		BossLadderRun run = ladder();
		// Sealed Bank is active, but PREP grants the allowance without mutating the restrictions.
		assertEquals(RestrictionOutcome.ALLOW, run.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.ALLOW, run.decide(Restriction.GRAND_EXCHANGE));
		assertTrue(run.prepAllowanceActive(Restriction.BANK));
		assertTrue(run.restrictions().isRestricted(Restriction.BANK));
		// Non-economy restrictions still bite during prep.
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.FOOD));
	}

	@Test
	public void fightRelocksTheEconomy()
	{
		BossLadderRun run = ladder();
		run.beginFight();
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.GRAND_EXCHANGE));
		assertFalse(run.prepAllowanceActive(Restriction.BANK));
	}

	@Test
	public void rewardEasesTheRestrictionsForTheRestOfTheClimb()
	{
		BossLadderRun run = ladder();
		run.beginFight();
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.FOOD));
		run.recordBossKill();
		run.chooseReward(RelicLibrary.breadOfTheWanderer());

		// Next boss's prep — food is now permitted for good.
		assertEquals(LadderPhase.PREP, run.phase());
		assertEquals(RestrictionOutcome.ALLOW, run.decide(Restriction.FOOD));
		assertEquals(1, run.earnedRelics().size());
	}

	@Test
	public void breakingARestrictionEndsTheRun()
	{
		BossLadderRun run = ladder();
		run.beginFight();
		run.fail("Ate food under Famine");
		assertEquals(LadderPhase.FAILED, run.phase());
		assertTrue(run.isOver());
		assertEquals("Ate food under Famine", run.failReason());
	}

	@Test
	public void transitionsOutOfOrderThrow()
	{
		BossLadderRun run = ladder();
		try
		{
			run.recordBossKill();
			fail("kill before fight must throw");
		}
		catch (IllegalStateException expected)
		{
		}
		run.beginFight();
		try
		{
			run.beginFight();
			fail("double fight gate must throw");
		}
		catch (IllegalStateException expected)
		{
		}
	}
}
