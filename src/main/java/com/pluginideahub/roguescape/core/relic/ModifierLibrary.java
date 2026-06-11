package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 5 — modifiers are lightweight relics offered as run curses/challenges. Unlike full
 * relics, modifiers carry only RESTRICTION and CATEGORY_LIMIT effects: they make runs harder
 * without granting scoring bonuses. They reuse the {@link Relic} model so the
 * {@link RelicEngine} can interpret them identically.
 */
public final class ModifierLibrary
{
	private ModifierLibrary() {}

	/** All modifiers/curses in the library, in display order. */
	public static List<Relic> all()
	{
		return Arrays.asList(
			noFood(), noPotions(), noTeleports(), noShields(), noMagic(), noRanged(), noMelee(),
			noArmour(), noRunes(), noJewelry(), twoFoodMax(), onePotionMax(), threeItemCap(),
			noAmmo(), noCapes(), noSkilling(), weaponsOnly(), oneWeaponOnly(), barefoot(),
			helmetless());
	}

	/** No Food — food items are forbidden. */
	public static Relic noFood()
	{
		return new Relic("mod-no-food", "No Food",
			"Curse: food items are forbidden.",
			RelicEffect.restriction(BankItemCategory.FOOD));
	}

	/** No Potions — potions are forbidden. */
	public static Relic noPotions()
	{
		return new Relic("mod-no-potions", "No Potions",
			"Curse: potions are forbidden.",
			RelicEffect.restriction(BankItemCategory.POTION));
	}

	/** No Teleports — teleports are forbidden. */
	public static Relic noTeleports()
	{
		return new Relic("mod-no-teleports", "No Teleports",
			"Curse: teleports are forbidden.",
			RelicEffect.restriction(BankItemCategory.TELEPORT));
	}

	/** No Shields — shields are forbidden. */
	public static Relic noShields()
	{
		return new Relic("mod-no-shields", "No Shields",
			"Curse: shields are forbidden.",
			RelicEffect.restriction(BankItemCategory.SHIELD));
	}

	/** No Magic — magic weapons are forbidden. */
	public static Relic noMagic()
	{
		return new Relic("mod-no-magic", "No Magic",
			"Curse: magic weapons are forbidden.",
			RelicEffect.restriction(BankItemCategory.MAGIC_WEAPON));
	}

	/** No Ranged — ranged weapons are forbidden. */
	public static Relic noRanged()
	{
		return new Relic("mod-no-ranged", "No Ranged",
			"Curse: ranged weapons are forbidden.",
			RelicEffect.restriction(BankItemCategory.RANGED_WEAPON));
	}

	/** No Melee — melee weapons are forbidden. */
	public static Relic noMelee()
	{
		return new Relic("mod-no-melee", "No Melee",
			"Curse: melee weapons are forbidden.",
			RelicEffect.restriction(BankItemCategory.MELEE_WEAPON));
	}

	/** No Armour — helmet, body, legs, boots, gloves all forbidden. */
	public static Relic noArmour()
	{
		return new Relic("mod-no-armour", "No Armour",
			"Curse: helmet, body, legs, boots, and gloves are all forbidden.",
			RelicEffect.restriction(BankItemCategory.HELMET),
			RelicEffect.restriction(BankItemCategory.BODY),
			RelicEffect.restriction(BankItemCategory.LEGS),
			RelicEffect.restriction(BankItemCategory.BOOTS),
			RelicEffect.restriction(BankItemCategory.GLOVES));
	}

	/** No Runes — runes are forbidden. */
	public static Relic noRunes()
	{
		return new Relic("mod-no-runes", "No Runes",
			"Curse: runes are forbidden.",
			RelicEffect.restriction(BankItemCategory.RUNE));
	}

	/** No Jewelry — rings and amulets are forbidden. */
	public static Relic noJewelry()
	{
		return new Relic("mod-no-jewelry", "No Jewelry",
			"Curse: rings and amulets are forbidden.",
			RelicEffect.restriction(BankItemCategory.RING),
			RelicEffect.restriction(BankItemCategory.NECK));
	}

	/** Two Food Max — food capped at 2. */
	public static Relic twoFoodMax()
	{
		return new Relic("mod-two-food-max", "Two Food Max",
			"Curse: at most two food items.",
			RelicEffect.categoryLimit(BankItemCategory.FOOD, 2));
	}

	/** One Potion Max — potions capped at 1. */
	public static Relic onePotionMax()
	{
		return new Relic("mod-one-potion-max", "One Potion Max",
			"Curse: at most one potion.",
			RelicEffect.categoryLimit(BankItemCategory.POTION, 1));
	}

	/** Three Item Cap — total item events capped at 3 (recap warns when exceeded). */
	public static Relic threeItemCap()
	{
		return new Relic("mod-three-item-cap", "Three Item Cap",
			"Curse: capped at three total item events.",
			RelicEffect.categoryLimit(BankItemCategory.UNKNOWN, 3));
	}

	/** No Ammo — ammunition is forbidden. */
	public static Relic noAmmo()
	{
		return new Relic("mod-no-ammo", "No Ammo",
			"Curse: ammunition is forbidden.",
			RelicEffect.restriction(BankItemCategory.AMMO));
	}

	/** No Capes — capes are forbidden. */
	public static Relic noCapes()
	{
		return new Relic("mod-no-capes", "No Capes",
			"Curse: capes are forbidden.",
			RelicEffect.restriction(BankItemCategory.CAPE));
	}

	/** No Skilling — skilling supplies are forbidden. */
	public static Relic noSkilling()
	{
		return new Relic("mod-no-skilling", "No Skilling",
			"Curse: skilling supplies are forbidden.",
			RelicEffect.restriction(BankItemCategory.SKILLING_SUPPLY));
	}

	/** Weapons Only — food, potion, rune, teleport, skilling supply, ammo all forbidden. */
	public static Relic weaponsOnly()
	{
		return new Relic("mod-weapons-only", "Weapons Only",
			"Curse: food, potions, runes, teleports, skilling supplies, and ammo are all forbidden.",
			RelicEffect.restriction(BankItemCategory.FOOD),
			RelicEffect.restriction(BankItemCategory.POTION),
			RelicEffect.restriction(BankItemCategory.RUNE),
			RelicEffect.restriction(BankItemCategory.TELEPORT),
			RelicEffect.restriction(BankItemCategory.SKILLING_SUPPLY),
			RelicEffect.restriction(BankItemCategory.AMMO));
	}

	/** One Weapon Only — each weapon style capped at 1. */
	public static Relic oneWeaponOnly()
	{
		return new Relic("mod-one-weapon-only", "One Weapon Only",
			"Curse: at most one melee, one ranged, and one magic weapon.",
			RelicEffect.categoryLimit(BankItemCategory.MELEE_WEAPON, 1),
			RelicEffect.categoryLimit(BankItemCategory.RANGED_WEAPON, 1),
			RelicEffect.categoryLimit(BankItemCategory.MAGIC_WEAPON, 1));
	}

	/** Barefoot — boots and gloves are forbidden. */
	public static Relic barefoot()
	{
		return new Relic("mod-barefoot", "Barefoot",
			"Curse: boots and gloves are forbidden.",
			RelicEffect.restriction(BankItemCategory.BOOTS),
			RelicEffect.restriction(BankItemCategory.GLOVES));
	}

	/** Helmetless — helmets are forbidden. */
	public static Relic helmetless()
	{
		return new Relic("mod-helmetless", "Helmetless",
			"Curse: helmets are forbidden.",
			RelicEffect.restriction(BankItemCategory.HELMET));
	}
}
