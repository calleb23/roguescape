package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Chunk 4 of the Boss Ladder MVP: the reward draft only offers easers that would actually loosen
 * the current restrictions, and is deterministic under a seed.
 */
public class LadderRewardDrafterTest
{
	@Test
	public void onlyOffersCardsThatWouldEaseSomething()
	{
		// Only Famine + a tier cap are in force.
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW, EnumSet.of(Curse.FAMINE));
		List<Relic> useful = LadderRewardDrafter.usefulEasers(r);
		List<String> ids = useful.stream().map(Relic::relicId).collect(Collectors.toList());

		assertTrue(ids.contains("bread-of-the-wanderer")); // eases FOOD
		assertTrue(ids.contains("armoury-key"));           // raises the tier cap
		assertFalse(ids.contains("alchemists-mercy"));     // potions were never forbidden
		assertFalse(ids.contains("key-to-the-vault"));     // bank was never sealed
		assertFalse(ids.contains("deep-pockets"));         // no inventory limit in force
	}

	@Test
	public void draftIsDeterministicUnderASeed()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE,
			EnumSet.of(Curse.FAMINE, Curse.SEALED_BANK, Curse.ANCHORED, Curse.FAITHLESS));
		List<Relic> a = LadderRewardDrafter.draft(r, 42L, LadderRewardDrafter.DRAFT_SIZE);
		List<Relic> b = LadderRewardDrafter.draft(r, 42L, LadderRewardDrafter.DRAFT_SIZE);
		List<Relic> c = LadderRewardDrafter.draft(r, 43L, LadderRewardDrafter.DRAFT_SIZE);

		assertEquals(LadderRewardDrafter.DRAFT_SIZE, a.size());
		assertEquals(
			a.stream().map(Relic::relicId).collect(Collectors.toList()),
			b.stream().map(Relic::relicId).collect(Collectors.toList()));
		// A different seed is allowed to differ (and with this pool it does).
		assertNotEquals(
			a.stream().map(Relic::relicId).collect(Collectors.toList()),
			c.stream().map(Relic::relicId).collect(Collectors.toList()));
	}

	@Test
	public void pickingACardStopsItBeingOffered()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.FAMINE));
		List<Relic> before = LadderRewardDrafter.usefulEasers(r);
		assertEquals(1, before.size());

		RelicEngine.applyEasing(before.get(0), r);
		assertTrue("once eased, the card is no longer useful",
			LadderRewardDrafter.usefulEasers(r).isEmpty());
	}

	@Test
	public void fullyEasedRunDraftsNothing()
	{
		assertTrue(LadderRewardDrafter.draft(RunRestrictions.unrestricted(), 7L, 3).isEmpty());
	}
}
