package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RestrictionOutcome;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Chunk 2 of the Boss Ladder MVP: relics are restriction-removers — each easer relic lifts one
 * restriction (or raises a cap) on the run's {@link RunRestrictions}.
 */
public class RelicEasingTest
{
	@Test
	public void everyEaserEasesExactlyOneThing()
	{
		for (Relic relic : RelicLibrary.easers())
		{
			assertEquals(relic.name() + " must ease exactly one thing", 1, relic.effects().size());
			RelicEffectKind kind = relic.effects().get(0).kind();
			assertTrue(relic.name() + " must be an easing effect",
				kind == RelicEffectKind.EASE_RESTRICTION
					|| kind == RelicEffectKind.PERMIT_COMBAT_STYLE
					|| kind == RelicEffectKind.ADD_INVENTORY_SLOTS);
		}
	}

	@Test
	public void breadOfTheWandererLiftsFamine()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE, EnumSet.of(Curse.FAMINE));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.FOOD));

		RelicEngine.applyEasing(RelicLibrary.breadOfTheWanderer(), r);
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.FOOD));
	}

	@Test
	public void laneRaisesClimbBandsNotArithmetic()
	{
		// Armoury Key is retired (locked 2026-07-03): tier raises are upgrade-lane rewards.
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW, EnumSet.noneOf(Curse.class));
		assertEquals(20, r.gearTierCap());
		r.raiseLane(com.pluginideahub.roguescape.core.restriction.UpgradeLane.ARMOUR);
		assertEquals(30, r.laneCap(com.pluginideahub.roguescape.core.restriction.UpgradeLane.ARMOUR));
		assertTrue(r.laneAllowed(com.pluginideahub.roguescape.core.restriction.UpgradeLane.ARMOUR, 30));
		assertFalse(r.laneAllowed(com.pluginideahub.roguescape.core.restriction.UpgradeLane.ARMOUR, 31));
		// The other lanes did not move.
		assertEquals(20, r.laneCap(com.pluginideahub.roguescape.core.restriction.UpgradeLane.WEAPON));
	}

	@Test
	public void deepPocketsAddsSlotsUnderTightPockets()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.TIGHT_POCKETS));
		assertEquals(Curse.TIGHT_POCKETS_SLOTS, r.inventoryLimit());
		RelicEngine.applyEasing(RelicLibrary.deepPockets(), r);
		assertEquals(Curse.TIGHT_POCKETS_SLOTS + 7, r.inventoryLimit());
	}

	@Test
	public void easingSomethingNotRestrictedIsHarmless()
	{
		RunRestrictions r = RunRestrictions.unrestricted();
		RelicEngine.applyEasing(RelicLibrary.waystone(), r);
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.TELEPORTS));
		assertTrue(r.isEmpty());
	}

	@Test
	public void permissionComboEnablesAStrategy()
	{
		// The design's synergy example: unlock Prayer + unlock Food -> you can tank a harder fight.
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE,
			EnumSet.of(Curse.FAMINE, Curse.FAITHLESS));
		assertEquals(RestrictionOutcome.BLOCK, r.decide(Restriction.FOOD));
		assertEquals(RestrictionOutcome.FAIL, r.decide(Restriction.PRAYER));

		RelicEngine.applyEasing(RelicLibrary.breadOfTheWanderer(), r);
		RelicEngine.applyEasing(RelicLibrary.whisperedFaith(), r);
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.FOOD));
		assertEquals(RestrictionOutcome.ALLOW, r.decide(Restriction.PRAYER));
	}
}
