package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.*;

/** Chunk 5 of the Boss Ladder MVP: gear tier = equip-level requirement, with a name fallback. */
public class GearTiersTest
{
	@Test
	public void knownEquipRequirementWinsOverTheName()
	{
		assertEquals(75, GearTiers.tierOf("Armadyl godsword", 75));
	}

	@Test
	public void nameFallbackPlacesTheClassicMaterials()
	{
		assertEquals(1, GearTiers.tierFromName("Bronze scimitar"));
		assertEquals(5, GearTiers.tierFromName("Steel platebody"));
		assertEquals(20, GearTiers.tierFromName("Mithril chainbody"));
		assertEquals(30, GearTiers.tierFromName("Adamant platelegs"));
		assertEquals(40, GearTiers.tierFromName("Rune scimitar"));
		assertEquals(60, GearTiers.tierFromName("Dragon dagger"));
	}

	@Test
	public void unknownItemsAreTierZeroAndAlwaysAllowed()
	{
		assertEquals(GearTiers.UNKNOWN_TIER, GearTiers.tierFromName("Lobster"));
		RunRestrictions r = RunRestrictions.starting(StartTier.NONE, Collections.emptySet());
		assertTrue(r.gearTierAllowed(GearTiers.tierFromName("Lobster")));
	}

	@Test
	public void capChecksArePureAgainstRestrictions()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.MEDIUM, Collections.emptySet());
		assertTrue(r.gearTierAllowed(GearTiers.tierOf("Rune scimitar", 0)));      // 40 <= 40
		assertFalse(r.gearTierAllowed(GearTiers.tierOf("Dragon dagger", 0)));     // 60 > 40
		assertFalse(r.gearTierAllowed(GearTiers.tierOf("Abyssal whip", 70)));     // real req wins
	}
}
