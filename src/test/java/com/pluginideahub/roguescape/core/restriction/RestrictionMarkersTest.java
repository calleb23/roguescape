package com.pluginideahub.roguescape.core.restriction;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RestrictionMarkersTest
{
	// A sample inventory: shark(food) . bronze sword(weapon) . empty . lobster(food) . prayer potion.
	private static List<BankItemCategory> sampleInventory()
	{
		return Arrays.asList(
			BankItemCategory.FOOD,          // slot 0
			BankItemCategory.MELEE_WEAPON,  // slot 1
			BankItemCategory.UNKNOWN,       // slot 2 (empty)
			BankItemCategory.FOOD,          // slot 3
			BankItemCategory.POTION);       // slot 4
	}

	@Test
	public void noFoodCrossesEveryFoodSlotAndNothingElse()
	{
		RunRestrictions r = new RunRestrictions().restrict(Restriction.FOOD);

		List<RestrictionMarker> markers = RestrictionMarkers.forInventory(sampleInventory(), r);

		assertEquals(2, markers.size());
		assertEquals(new RestrictionMarker(0, Restriction.FOOD), markers.get(0));
		assertEquals(new RestrictionMarker(3, Restriction.FOOD), markers.get(1));
	}

	@Test
	public void noFoodBlocksEatingButAllowsOtherCategories()
	{
		RunRestrictions r = new RunRestrictions().restrict(Restriction.FOOD);

		assertEquals(RestrictionOutcome.BLOCK, r.decideItemUse(BankItemCategory.FOOD));
		assertEquals(RestrictionOutcome.ALLOW, r.decideItemUse(BankItemCategory.POTION));
		assertEquals(Restriction.FOOD, r.forbiddenBy(BankItemCategory.FOOD));
		assertNull(r.forbiddenBy(BankItemCategory.MELEE_WEAPON));
	}

	@Test
	public void relicThatPermitsFoodClearsTheCrossesAndTheBlock()
	{
		RunRestrictions r = new RunRestrictions().restrict(Restriction.FOOD);
		assertEquals(2, RestrictionMarkers.forInventory(sampleInventory(), r).size());

		r.permit(Restriction.FOOD);

		assertTrue(RestrictionMarkers.forInventory(sampleInventory(), r).isEmpty());
		assertEquals(RestrictionOutcome.ALLOW, r.decideItemUse(BankItemCategory.FOOD));
	}

	@Test
	public void theSameCodePathCoversPotionsAmmoAndRunes()
	{
		RunRestrictions r = new RunRestrictions()
			.restrict(Restriction.POTIONS)
			.restrict(Restriction.AMMO)
			.restrict(Restriction.RUNES);

		assertEquals(Restriction.POTIONS, r.forbiddenBy(BankItemCategory.POTION));
		assertEquals(Restriction.AMMO, r.forbiddenBy(BankItemCategory.AMMO));
		assertEquals(Restriction.RUNES, r.forbiddenBy(BankItemCategory.RUNE));
		// Food isn't restricted here, so it stays usable.
		assertNull(r.forbiddenBy(BankItemCategory.FOOD));
	}

	@Test
	public void noRestrictionsMeansNoMarkers()
	{
		List<RestrictionMarker> markers = RestrictionMarkers.forInventory(sampleInventory(),
			RunRestrictions.unrestricted());
		assertTrue(markers.isEmpty());
	}
}
