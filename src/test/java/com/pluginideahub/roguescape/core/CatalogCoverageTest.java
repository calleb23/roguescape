package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.ladder.LadderRewardCard;
import com.pluginideahub.roguescape.core.ladder.LadderRewardDrafter;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.GradeBands;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The catalog laws (locked 2026-07-03), held as executable invariants:
 * every curse can be fully eased by the relic pool, the curse list is exactly 12,
 * and the band ladder climbs 1→5→10→20→30→40→50→60→70 then frees the lane.
 */
public class CatalogCoverageTest
{
	@Test
	public void curseListIsExactlyTwelve()
	{
		assertEquals(12, Curse.values().length);
	}

	@Test
	public void everyCurseCanBeFullyEasedByThePool()
	{
		for (Curse curse : Curse.values())
		{
			RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(curse));
			assertFalse(curse + " must restrict something", r.isEmpty());

			// Apply every useful easer until the pool runs dry (bounded: the pool must shrink).
			for (int guard = 0; guard < 32; guard++)
			{
				List<Relic> useful = LadderRewardDrafter.usefulEasers(r);
				if (useful.isEmpty())
				{
					break;
				}
				RelicEngine.applyEasing(useful.get(0), r);
			}
			assertTrue(curse + " must be fully easeable by the relic pool, still active: "
				+ r.active(), r.isEmpty());
		}
	}

	@Test
	public void aFullRunFromStartTierNoneCanEarnTotalFreedom()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE, EnumSet.allOf(Curse.class));
		for (int guard = 0; guard < 128; guard++)
		{
			List<LadderRewardCard> pool = LadderRewardDrafter.pool(r);
			if (pool.isEmpty())
			{
				break;
			}
			LadderRewardCard card = pool.get(0);
			if (card.isRaise())
			{
				r.raiseLane(card.lane());
			}
			else
			{
				RelicEngine.applyEasing(card.relic(), r);
			}
		}
		assertTrue("the mixed pool must be able to free the whole run, still active: "
			+ r.active(), r.isEmpty());
	}

	@Test
	public void bandLadderClimbsTheLockedSteps()
	{
		int[] expected = {1, 5, 10, 20, 30, 40, 50, 60, 70};
		assertArrayEquals(expected, GradeBands.BANDS);
		assertEquals(5, GradeBands.next(1));
		assertEquals(20, GradeBands.next(10));
		assertEquals(70, GradeBands.next(65));
		assertEquals(RunRestrictions.UNCAPPED, GradeBands.next(70));
		assertEquals("rune-grade", GradeBands.name(40));
		assertEquals("unrestricted", GradeBands.name(RunRestrictions.UNCAPPED));
	}
}
