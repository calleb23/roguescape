package com.pluginideahub.roguescape.core.reward;

import org.junit.Test;

import static org.junit.Assert.*;

public class BankItemClassifierTest
{
	@Test
	public void recognisesMeleeWeapons()
	{
		assertEquals(BankItemCategory.MELEE_WEAPON, BankItemClassifier.guessCategory("Abyssal whip"));
		assertEquals(BankItemCategory.MELEE_WEAPON, BankItemClassifier.guessCategory("Rune scimitar"));
		assertEquals(BankItemCategory.MELEE_WEAPON, BankItemClassifier.guessCategory("Dragon dagger"));
	}

	@Test
	public void recognisesRangedAndMagicWeapons()
	{
		assertEquals(BankItemCategory.RANGED_WEAPON, BankItemClassifier.guessCategory("Toxic blowpipe"));
		assertEquals(BankItemCategory.RANGED_WEAPON, BankItemClassifier.guessCategory("Magic shortbow"));
		assertEquals(BankItemCategory.MAGIC_WEAPON, BankItemClassifier.guessCategory("Trident of the seas"));
		assertEquals(BankItemCategory.MAGIC_WEAPON, BankItemClassifier.guessCategory("Master wand"));
	}

	@Test
	public void recognisesFoodAndPotions()
	{
		assertEquals(BankItemCategory.FOOD, BankItemClassifier.guessCategory("Shark"));
		assertEquals(BankItemCategory.FOOD, BankItemClassifier.guessCategory("Anglerfish"));
		assertEquals(BankItemCategory.POTION, BankItemClassifier.guessCategory("Super combat potion(4)"));
		assertEquals(BankItemCategory.POTION, BankItemClassifier.guessCategory("Saradomin brew(4)"));
	}

	@Test
	public void recognisesAmmoAndRunes()
	{
		assertEquals(BankItemCategory.AMMO, BankItemClassifier.guessCategory("Dragon arrow"));
		assertEquals(BankItemCategory.AMMO, BankItemClassifier.guessCategory("Diamond bolts (e)"));
		assertEquals(BankItemCategory.RUNE, BankItemClassifier.guessCategory("Death rune"));
		assertEquals(BankItemCategory.RUNE, BankItemClassifier.guessCategory("Blood runes"));
	}

	@Test
	public void recognisesArmourSlots()
	{
		assertEquals(BankItemCategory.HELMET, BankItemClassifier.guessCategory("Rune full helm"));
		assertEquals(BankItemCategory.BODY, BankItemClassifier.guessCategory("Rune platebody"));
		assertEquals(BankItemCategory.LEGS, BankItemClassifier.guessCategory("Rune platelegs"));
		assertEquals(BankItemCategory.BOOTS, BankItemClassifier.guessCategory("Primordial boots"));
		assertEquals(BankItemCategory.GLOVES, BankItemClassifier.guessCategory("Mithril gloves"));
		assertEquals(BankItemCategory.CAPE, BankItemClassifier.guessCategory("Fire cape"));
		assertEquals(BankItemCategory.SHIELD, BankItemClassifier.guessCategory("Dragonfire shield"));
	}

	@Test
	public void teleportAndJewelryCategories()
	{
		assertEquals(BankItemCategory.TELEPORT, BankItemClassifier.guessCategory("Varrock teleport"));
		assertEquals(BankItemCategory.NECK, BankItemClassifier.guessCategory("Amulet of fury"));
		assertEquals(BankItemCategory.RING, BankItemClassifier.guessCategory("Berserker ring"));
	}

	@Test
	public void unknownAndJunkFallback()
	{
		assertEquals(BankItemCategory.JUNK, BankItemClassifier.guessCategory("Spinach roll placeholder"));
		assertEquals(BankItemCategory.UNKNOWN, BankItemClassifier.guessCategory("Whatever random thing"));
	}

	@Test
	public void classifyAssignsValueTierFromPrice()
	{
		BankItem cheap = BankItemClassifier.classify("Shark", 1_000);
		BankItem mid = BankItemClassifier.classify("Bandos chestplate", 25_000_000);
		BankItem high = BankItemClassifier.classify("Twisted bow", 1_200_000_000);
		assertEquals(ValueTier.TIER_1, cheap.valueTier());
		assertEquals(ValueTier.TIER_4, mid.valueTier());
		assertEquals(ValueTier.TIER_6, high.valueTier());
	}

	@Test
	public void fairnessPolicyFiltersByTierAndExclusions()
	{
		BankItem cheap = BankItemClassifier.classify("Trout", 100);
		BankItem expensive = BankItemClassifier.classify("Twisted bow", 1_200_000_000);
		FairnessPolicy band = FairnessPolicy.builder().valueBand(ValueTier.TIER_2, ValueTier.TIER_5).build();
		assertFalse("tier-1 below band is rejected", band.accepts(cheap));
		assertFalse("tier-6 above band is rejected", band.accepts(expensive));

		FairnessPolicy excludeJunk = FairnessPolicy.builder().excludeCategory(BankItemCategory.JUNK).build();
		BankItem junk = BankItemClassifier.classify("Spinach roll placeholder", 0);
		assertFalse(excludeJunk.accepts(junk));

		FairnessPolicy excludeId = FairnessPolicy.builder().exclude("shark").build();
		BankItem shark = BankItemClassifier.classify("Shark", 100);
		assertFalse(excludeId.accepts(shark));
	}
}
