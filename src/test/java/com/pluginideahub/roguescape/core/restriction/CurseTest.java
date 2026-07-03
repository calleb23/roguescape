package com.pluginideahub.roguescape.core.restriction;

import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Chunk 1 of the Boss Ladder MVP: curses are setup-only restriction bundles, and a run's starting
 * {@link RunRestrictions} is assembled from the chosen start tier + curses.
 */
public class CurseTest
{
	@Test
	public void everyCurseAddsAtLeastOneRestriction()
	{
		for (Curse curse : Curse.values())
		{
			RunRestrictions r = RunRestrictions.unrestricted();
			curse.apply(r);
			assertFalse(curse.name() + " must restrict something", r.isEmpty());
			assertFalse(curse.displayName().isEmpty());
			assertFalse(curse.description().isEmpty());
			assertTrue(curse.name() + " must score", curse.scoreBonus() > 0);
		}
	}

	@Test
	public void startingAssemblesTierPlusCurses()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW,
			Arrays.asList(Curse.FAMINE, Curse.SEALED_BANK));

		// The tier's gear cap is in force.
		assertTrue(r.isRestricted(Restriction.GEAR_TIER_CAP));
		assertEquals(20, r.gearTierCap());
		assertTrue(r.gearTierAllowed(20));
		assertFalse(r.gearTierAllowed(21));

		// The curses' restrictions are in force.
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.FOOD));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.TRADE));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.GRAND_EXCHANGE));

		// Unchosen things stay allowed.
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.POTIONS));
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.TELEPORTS));
	}

	@Test
	public void tightPocketsIsParameterised()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.TIGHT_POCKETS));
		assertTrue(r.isRestricted(Restriction.INVENTORY_LIMIT));
		assertEquals(Curse.TIGHT_POCKETS_SLOTS, r.inventoryLimit());
		assertTrue(r.inventorySizeAllowed(14));
		assertFalse(r.inventorySizeAllowed(15));
	}

	@Test
	public void noneTierIsTheFullShackle()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE, EnumSet.noneOf(Curse.class));
		assertEquals(1, r.gearTierCap());
		assertTrue(r.gearTierAllowed(1));
		assertFalse(r.gearTierAllowed(2));
	}

	@Test
	public void relicsEaseWhatCursesImposed()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE, EnumSet.of(Curse.FAMINE));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.FOOD));
		r.permit(Restriction.FOOD);
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.FOOD));
		// Tier raises are upgrade-lane rewards now (locked 2026-07-03): band 1 -> 5.
		r.raiseLane(UpgradeLane.WEAPON);
		assertEquals(5, r.laneCap(UpgradeLane.WEAPON));
	}
}
