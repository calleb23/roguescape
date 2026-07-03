package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import com.pluginideahub.roguescape.core.restriction.UpgradeLane;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The mixed-pool reward draft (locked 2026-07-03): one seeded shuffle over remaining lane raises
 * + easers useful against the live restrictions; every card would actually do something; drafts
 * shrink honestly when the pool runs short.
 */
public class LadderRewardDrafterTest
{
	@Test
	public void onlyOffersCardsThatWouldChangeSomething()
	{
		// Famine + a tier cap on all four lanes are in force.
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW, EnumSet.of(Curse.FAMINE));
		List<LadderRewardCard> pool = LadderRewardDrafter.pool(r);

		List<String> relicIds = pool.stream().filter(c -> !c.isRaise())
			.map(c -> c.relic().relicId()).collect(Collectors.toList());
		List<UpgradeLane> raises = pool.stream().filter(LadderRewardCard::isRaise)
			.map(LadderRewardCard::lane).collect(Collectors.toList());

		assertTrue(relicIds.contains("bread-of-the-wanderer")); // eases FOOD
		assertFalse(relicIds.contains("alchemists-mercy"));     // potions were never forbidden
		assertFalse(relicIds.contains("key-to-the-vault"));     // bank was never sealed
		assertFalse(relicIds.contains("deep-pockets"));         // no inventory limit in force
		// All four capped lanes offer their raise.
		assertEquals(4, raises.size());
		assertTrue(raises.containsAll(EnumSet.allOf(UpgradeLane.class)));
	}

	@Test
	public void raisesClimbTheBandLadderAndFreeTheLane()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.HIGH, EnumSet.noneOf(Curse.class));
		assertEquals(60, r.laneCap(UpgradeLane.WEAPON));

		r.raiseLane(UpgradeLane.WEAPON);
		assertEquals(70, r.laneCap(UpgradeLane.WEAPON));

		r.raiseLane(UpgradeLane.WEAPON); // past the top band: the lane frees entirely
		assertEquals(RunRestrictions.UNCAPPED, r.laneCap(UpgradeLane.WEAPON));

		// The freed lane no longer offers a raise; the other three still do.
		List<LadderRewardCard> pool = LadderRewardDrafter.pool(r);
		List<UpgradeLane> raises = pool.stream().filter(LadderRewardCard::isRaise)
			.map(LadderRewardCard::lane).collect(Collectors.toList());
		assertFalse(raises.contains(UpgradeLane.WEAPON));
		assertEquals(3, raises.size());
	}

	@Test
	public void draftIsDeterministicUnderASeed()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE,
			EnumSet.of(Curse.FAMINE, Curse.SEALED_BANK, Curse.ANCHORED, Curse.FAITHLESS));
		List<LadderRewardCard> a = LadderRewardDrafter.draft(r, 42L, LadderRewardDrafter.DRAFT_SIZE);
		List<LadderRewardCard> b = LadderRewardDrafter.draft(r, 42L, LadderRewardDrafter.DRAFT_SIZE);
		List<LadderRewardCard> c = LadderRewardDrafter.draft(r, 43L, LadderRewardDrafter.DRAFT_SIZE);

		assertEquals(LadderRewardDrafter.DRAFT_SIZE, a.size());
		assertEquals(titles(a), titles(b));
		// A different seed is allowed to differ (and with this pool it does).
		assertNotEquals(titles(a), titles(c));
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

	@Test
	public void oneStyleOffersOnlyTheMissingStyles()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.ONE_STYLE));
		List<String> ids = LadderRewardDrafter.usefulEasers(r).stream()
			.map(Relic::relicId).collect(Collectors.toList());
		assertFalse("melee is already permitted", ids.contains("way-of-the-blade"));
		assertTrue(ids.contains("way-of-the-bow"));
		assertTrue(ids.contains("way-of-the-wand"));
	}

	private static List<String> titles(List<LadderRewardCard> cards)
	{
		return cards.stream().map(LadderRewardCard::title).collect(Collectors.toList());
	}
}
