package com.pluginideahub.roguescape.core.restriction;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RunRestrictionsTest
{
	@Test
	public void unrestrictedAllowsEverything()
	{
		RunRestrictions r = RunRestrictions.unrestricted();
		assertTrue(r.isEmpty());
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.PRAYER));
	}

	@Test
	public void blockModeRestrictionsBlockAndFailModeRestrictionsFail()
	{
		RunRestrictions r = new RunRestrictions()
			.restrict(Restriction.BANK)      // BLOCK family
			.restrict(Restriction.PRAYER);   // FAIL family

		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.FAIL, r.decide(Restriction.PRAYER));
		// A restriction that isn't active is allowed.
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.TRADE));
	}

	@Test
	public void enforcementModesMatchTheCatalog()
	{
		assertEquals(Restriction.Enforcement.BLOCK, Restriction.BANK.enforcement());
		assertEquals(Restriction.Enforcement.BLOCK, Restriction.FOOD.enforcement());
		assertEquals(Restriction.Enforcement.FAIL, Restriction.PRAYER.enforcement());
		assertEquals(Restriction.Enforcement.FAIL, Restriction.LEAVE_REGION.enforcement());
	}

	@Test
	public void permitLiftsARestriction_theCanonicalRelic()
	{
		RunRestrictions r = new RunRestrictions().restrict(Restriction.FOOD);
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.FOOD));

		r.permit(Restriction.FOOD);
		assertFalse(r.isRestricted(Restriction.FOOD));
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.FOOD));
	}

	@Test
	public void gearTierCapBlocksAboveTheCapAndRelicsRaiseIt()
	{
		RunRestrictions r = new RunRestrictions().restrictGearTier(40);
		assertTrue(r.gearTierAllowed(40));
		assertFalse(r.gearTierAllowed(50));

		r.raiseGearTierCap(20);
		assertTrue(r.gearTierAllowed(60));

		r.permit(Restriction.GEAR_TIER_CAP);
		assertTrue("lifting the cap allows any tier", r.gearTierAllowed(99));
	}

	@Test
	public void inventoryLimitBlocksOverLimitAndRelicsAddSlots()
	{
		RunRestrictions r = new RunRestrictions().restrictInventory(6);
		assertTrue(r.inventorySizeAllowed(6));
		assertFalse(r.inventorySizeAllowed(7));

		r.addInventorySlots(4);
		assertTrue(r.inventorySizeAllowed(10));
	}

	@Test
	public void spellbookLockAllowsOnlyTheAllowedBookUntilSwappedOrLifted()
	{
		RunRestrictions r = new RunRestrictions().restrictSpellbook(Spellbook.STANDARD);
		assertTrue(r.spellbookAllowed(Spellbook.STANDARD));
		assertFalse(r.spellbookAllowed(Spellbook.ANCIENT));

		// A magic altar swaps which book is permitted.
		r.setAllowedSpellbook(Spellbook.ANCIENT);
		assertTrue(r.spellbookAllowed(Spellbook.ANCIENT));
		assertFalse(r.spellbookAllowed(Spellbook.STANDARD));

		// A relic lifting the lock allows any book.
		r.permit(Restriction.SPELLBOOK);
		assertTrue(r.spellbookAllowed(Spellbook.LUNAR));
	}

	@Test
	public void combatStyleLockNarrowsThenWidensBackToUnlocked()
	{
		RunRestrictions r = new RunRestrictions().restrictCombatStyles(CombatStyle.MELEE);
		assertTrue(r.combatStyleAllowed(CombatStyle.MELEE));
		assertFalse(r.combatStyleAllowed(CombatStyle.RANGED));

		r.permitCombatStyle(CombatStyle.RANGED);
		assertTrue(r.combatStyleAllowed(CombatStyle.RANGED));
		assertTrue("still locked while a style is missing", r.isRestricted(Restriction.COMBAT_STYLE));

		// Permitting the final style lifts the lock entirely.
		r.permitCombatStyle(CombatStyle.MAGIC);
		assertFalse(r.isRestricted(Restriction.COMBAT_STYLE));
		assertTrue(r.combatStyleAllowed(CombatStyle.MAGIC));
	}

	@Test
	public void activeListIsOrderedByFamilyAndCounts()
	{
		RunRestrictions r = new RunRestrictions()
			.restrict(Restriction.PRAYER)   // COMBAT
			.restrict(Restriction.BANK);    // ECONOMY

		assertEquals(2, r.count());
		// ECONOMY sorts before COMBAT.
		assertEquals(Restriction.BANK, r.active().get(0));
		assertEquals(Restriction.PRAYER, r.active().get(1));
	}
}
