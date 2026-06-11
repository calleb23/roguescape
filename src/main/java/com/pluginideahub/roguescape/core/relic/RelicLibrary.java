package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 5 — canonical relics from the roadmap. Curated rather than exhaustive; the relic
 * library can be extended by content packs at later stages.
 */
public final class RelicLibrary
{
	private RelicLibrary() {}

	/** All relics in the library, in display order. */
	public static List<Relic> all()
	{
		return Arrays.asList(
			oneBankMercy(), fourFoodLimit(), cursedBlades(), glassCannonMode(), rangersPact(),
			skiller(), hoarder(), minimalist(), bloodMoney(), ironRations(), runePouch(),
			ancestralCurse(), shieldWall(), greedIsGood(), ammoCurse(), jewelryJunkie(),
			supplySurge(), capeCollection(), berserkersCurse(), gluttony());
	}

	/**
	 * One Bank Mercy — the run can survive exactly one bank withdrawal during the run.
	 * Without the relic, a bank withdrawal is illegal (per legality classifier rules); with
	 * the relic, the first such illegal bank withdrawal is downgraded to suspicious and
	 * the relic charge is consumed.
	 */
	public static Relic oneBankMercy()
	{
		return new Relic("one-bank-mercy", "One Bank Mercy",
			"Consume once to survive a single bank withdrawal you would otherwise fail.",
			RelicEffect.oneShotMercy());
	}

	/** Four-Food Limit — the player can carry at most four food items in total. */
	public static Relic fourFoodLimit()
	{
		return new Relic("four-food-limit", "Four-Food Limit",
			"You may stack at most four total food items during the run.",
			RelicEffect.categoryLimit(BankItemCategory.FOOD, 4));
	}

	/** Cursed Blades — melee weapon scoring +2 per item but you may use no shields. */
	public static Relic cursedBlades()
	{
		return new Relic("cursed-blades", "Cursed Blades",
			"Melee weapons score +2 each at recap; shields are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.MELEE_WEAPON, 2),
			RelicEffect.restriction(BankItemCategory.SHIELD));
	}

	/** Glass Cannon Mode — magic weapons score +3 each; all armour categories are forbidden. */
	public static Relic glassCannonMode()
	{
		return new Relic("glass-cannon-mode", "Glass Cannon Mode",
			"Magic weapons score +3 each at recap; all armour is forbidden.",
			RelicEffect.scoringBias(BankItemCategory.MAGIC_WEAPON, 3),
			RelicEffect.restriction(BankItemCategory.HELMET),
			RelicEffect.restriction(BankItemCategory.BODY),
			RelicEffect.restriction(BankItemCategory.LEGS),
			RelicEffect.restriction(BankItemCategory.BOOTS),
			RelicEffect.restriction(BankItemCategory.GLOVES),
			RelicEffect.restriction(BankItemCategory.SHIELD));
	}

	/** Ranger's Pact — ranged weapons score +2 each; melee weapons are forbidden. */
	public static Relic rangersPact()
	{
		return new Relic("rangers-pact", "Ranger's Pact",
			"Ranged weapons score +2 each at recap; melee weapons are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.RANGED_WEAPON, 2),
			RelicEffect.restriction(BankItemCategory.MELEE_WEAPON));
	}

	/** Skiller — skilling supplies score +3 each; all weapon categories are forbidden. */
	public static Relic skiller()
	{
		return new Relic("skiller", "Skiller",
			"Skilling supplies score +3 each at recap; weapons of any kind are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.SKILLING_SUPPLY, 3),
			RelicEffect.restriction(BankItemCategory.MELEE_WEAPON),
			RelicEffect.restriction(BankItemCategory.RANGED_WEAPON),
			RelicEffect.restriction(BankItemCategory.MAGIC_WEAPON));
	}

	/** Hoarder — every category scores +1 each at recap. */
	public static Relic hoarder()
	{
		return new Relic("hoarder", "Hoarder",
			"Every recorded item category scores +1 each at recap.",
			RelicEffect.scoringBias(BankItemCategory.MELEE_WEAPON, 1),
			RelicEffect.scoringBias(BankItemCategory.RANGED_WEAPON, 1),
			RelicEffect.scoringBias(BankItemCategory.MAGIC_WEAPON, 1),
			RelicEffect.scoringBias(BankItemCategory.SHIELD, 1),
			RelicEffect.scoringBias(BankItemCategory.HELMET, 1),
			RelicEffect.scoringBias(BankItemCategory.BODY, 1),
			RelicEffect.scoringBias(BankItemCategory.LEGS, 1),
			RelicEffect.scoringBias(BankItemCategory.BOOTS, 1),
			RelicEffect.scoringBias(BankItemCategory.GLOVES, 1),
			RelicEffect.scoringBias(BankItemCategory.CAPE, 1),
			RelicEffect.scoringBias(BankItemCategory.NECK, 1),
			RelicEffect.scoringBias(BankItemCategory.RING, 1),
			RelicEffect.scoringBias(BankItemCategory.AMMO, 1),
			RelicEffect.scoringBias(BankItemCategory.FOOD, 1),
			RelicEffect.scoringBias(BankItemCategory.POTION, 1),
			RelicEffect.scoringBias(BankItemCategory.RUNE, 1),
			RelicEffect.scoringBias(BankItemCategory.TELEPORT, 1),
			RelicEffect.scoringBias(BankItemCategory.SKILLING_SUPPLY, 1));
	}

	/** Minimalist — total item events capped at 12 (recap warns when exceeded). */
	public static Relic minimalist()
	{
		return new Relic("minimalist", "Minimalist",
			"Capped at twelve total item events; carrying more flags the recap.",
			RelicEffect.categoryLimit(BankItemCategory.UNKNOWN, 12));
	}

	/** Blood Money — potions score +2 each; food is forbidden. */
	public static Relic bloodMoney()
	{
		return new Relic("blood-money", "Blood Money",
			"Potions score +2 each at recap; food is forbidden.",
			RelicEffect.scoringBias(BankItemCategory.POTION, 2),
			RelicEffect.restriction(BankItemCategory.FOOD));
	}

	/** Iron Rations — food limit 2; potions score +1 each. */
	public static Relic ironRations()
	{
		return new Relic("iron-rations", "Iron Rations",
			"Food capped at two; potions score +1 each at recap.",
			RelicEffect.categoryLimit(BankItemCategory.FOOD, 2),
			RelicEffect.scoringBias(BankItemCategory.POTION, 1));
	}

	/** Rune Pouch — runes score +2 each; teleports are forbidden. */
	public static Relic runePouch()
	{
		return new Relic("rune-pouch", "Rune Pouch",
			"Runes score +2 each at recap; teleports are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.RUNE, 2),
			RelicEffect.restriction(BankItemCategory.TELEPORT));
	}

	/** Ancestral Curse — magic weapons score +4 each; all ranged and melee weapons forbidden. */
	public static Relic ancestralCurse()
	{
		return new Relic("ancestral-curse", "Ancestral Curse",
			"Magic weapons score +4 each at recap; ranged and melee weapons are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.MAGIC_WEAPON, 4),
			RelicEffect.restriction(BankItemCategory.RANGED_WEAPON),
			RelicEffect.restriction(BankItemCategory.MELEE_WEAPON));
	}

	/** Shield Wall — shields score +3 each; capes are forbidden. */
	public static Relic shieldWall()
	{
		return new Relic("shield-wall", "Shield Wall",
			"Shields score +3 each at recap; capes are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.SHIELD, 3),
			RelicEffect.restriction(BankItemCategory.CAPE));
	}

	/** Greed Is Good — global +1 per scored item signal. */
	public static Relic greedIsGood()
	{
		return new Relic("greed-is-good", "Greed Is Good",
			"Every scoring item gains +1 extra recap value.",
			RelicEffect.scoringBias(BankItemCategory.UNKNOWN, 1));
	}

	/** Ammo Curse — ammo scores +2 each; ranged weapons are forbidden. */
	public static Relic ammoCurse()
	{
		return new Relic("ammo-curse", "Ammo Curse",
			"Ammunition scores +2 each at recap; ranged weapons are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.AMMO, 2),
			RelicEffect.restriction(BankItemCategory.RANGED_WEAPON));
	}

	/** Jewelry Junkie — rings and necks each score +3; boots are forbidden. */
	public static Relic jewelryJunkie()
	{
		return new Relic("jewelry-junkie", "Jewelry Junkie",
			"Rings and amulets each score +3 at recap; boots are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.RING, 3),
			RelicEffect.scoringBias(BankItemCategory.NECK, 3),
			RelicEffect.restriction(BankItemCategory.BOOTS));
	}

	/** Supply Surge — skilling supplies and runes each score +2. */
	public static Relic supplySurge()
	{
		return new Relic("supply-surge", "Supply Surge",
			"Skilling supplies and runes each score +2 at recap.",
			RelicEffect.scoringBias(BankItemCategory.SKILLING_SUPPLY, 2),
			RelicEffect.scoringBias(BankItemCategory.RUNE, 2));
	}

	/** Cape Collection — capes score +4 each; boots and gloves are forbidden. */
	public static Relic capeCollection()
	{
		return new Relic("cape-collection", "Cape Collection",
			"Capes score +4 each at recap; boots and gloves are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.CAPE, 4),
			RelicEffect.restriction(BankItemCategory.BOOTS),
			RelicEffect.restriction(BankItemCategory.GLOVES));
	}

	/** Berserker's Curse — melee weapons score +4 each; all non-melee armour is forbidden. */
	public static Relic berserkersCurse()
	{
		return new Relic("berserkers-curse", "Berserker's Curse",
			"Melee weapons score +4 each at recap; helmet, body, legs, boots, gloves, and shield are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.MELEE_WEAPON, 4),
			RelicEffect.restriction(BankItemCategory.HELMET),
			RelicEffect.restriction(BankItemCategory.BODY),
			RelicEffect.restriction(BankItemCategory.LEGS),
			RelicEffect.restriction(BankItemCategory.BOOTS),
			RelicEffect.restriction(BankItemCategory.GLOVES),
			RelicEffect.restriction(BankItemCategory.SHIELD));
	}

	/** Gluttony — food scores +3 each; potions are forbidden. */
	public static Relic gluttony()
	{
		return new Relic("gluttony", "Gluttony",
			"Food scores +3 each at recap; potions are forbidden.",
			RelicEffect.scoringBias(BankItemCategory.FOOD, 3),
			RelicEffect.restriction(BankItemCategory.POTION));
	}
}
