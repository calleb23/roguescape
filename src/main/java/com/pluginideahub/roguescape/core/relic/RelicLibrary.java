package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.restriction.CombatStyle;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import java.util.Arrays;
import java.util.List;

/**
 * The v1 relic pool (locked 2026-07-03): pure permits, full coverage. Law: <b>every restriction
 * in the vocabulary has a relic that eases it</b> — parameterised families (the three high
 * prayers, the three combat styles) get one relic per member, mirroring their restrictions.
 * Tier raises are upgrade-lane rewards, never relics (Armoury Key is retired); Deep Pockets
 * stays (slots are a relic). The old scoring pool is quarantined in {@link LegacyRelics}.
 */
public final class RelicLibrary
{
	private RelicLibrary() {}

	/** The whole draftable pool = the easers. */
	public static List<Relic> all()
	{
		return easers();
	}

	/** Every easer, in display order. */
	public static List<Relic> easers()
	{
		return Arrays.asList(
			breadOfTheWanderer(), alchemistsMercy(), keyToTheVault(), merchantsSeal(), tradersMark(),
			waystone(), whisperedFaith(), iconOfPiety(), iconOfRigour(), iconOfAugury(),
			bulwarkCharm(), fletchersQuiver(), runicFocus(), arcaneTome(),
			wayOfTheBlade(), wayOfTheBow(), wayOfTheWand(), deepPockets());
	}

	/** Eases FAMINE — food may be eaten again. */
	public static Relic breadOfTheWanderer()
	{
		return new Relic("bread-of-the-wanderer", "Bread of the Wanderer",
			"Food is permitted once more.",
			RelicEffect.ease(Restriction.FOOD));
	}

	/** Eases DRY_THROAT — potions may be drunk again. */
	public static Relic alchemistsMercy()
	{
		return new Relic("alchemists-mercy", "Alchemist's Mercy",
			"Potions are permitted once more.",
			RelicEffect.ease(Restriction.POTIONS));
	}

	/** Eases the bank lock. */
	public static Relic keyToTheVault()
	{
		return new Relic("key-to-the-vault", "Key to the Vault",
			"The bank is permitted once more.",
			RelicEffect.ease(Restriction.BANK));
	}

	/** Eases the Grand Exchange lock. */
	public static Relic merchantsSeal()
	{
		return new Relic("merchants-seal", "Merchant's Seal",
			"The Grand Exchange is permitted once more.",
			RelicEffect.ease(Restriction.GRAND_EXCHANGE));
	}

	/** Eases the trade lock. */
	public static Relic tradersMark()
	{
		return new Relic("traders-mark", "Trader's Mark",
			"Trading is permitted once more.",
			RelicEffect.ease(Restriction.TRADE));
	}

	/** Eases ANCHORED — teleports work again. */
	public static Relic waystone()
	{
		return new Relic("waystone", "Waystone",
			"Teleports are permitted once more.",
			RelicEffect.ease(Restriction.TELEPORTS));
	}

	/** Eases FAITHLESS — prayer returns. */
	public static Relic whisperedFaith()
	{
		return new Relic("whispered-faith", "Whispered Faith",
			"Prayer is permitted once more.",
			RelicEffect.ease(Restriction.PRAYER));
	}

	/** Eases the Piety lock. */
	public static Relic iconOfPiety()
	{
		return new Relic("icon-of-piety", "Icon of Piety",
			"Piety is permitted once more.",
			RelicEffect.ease(Restriction.PIETY));
	}

	/** Eases the Rigour lock. */
	public static Relic iconOfRigour()
	{
		return new Relic("icon-of-rigour", "Icon of Rigour",
			"Rigour is permitted once more.",
			RelicEffect.ease(Restriction.RIGOUR));
	}

	/** Eases the Augury lock. */
	public static Relic iconOfAugury()
	{
		return new Relic("icon-of-augury", "Icon of Augury",
			"Augury is permitted once more.",
			RelicEffect.ease(Restriction.AUGURY));
	}

	/** Eases BARE_FISTED — shields may be equipped. */
	public static Relic bulwarkCharm()
	{
		return new Relic("bulwark-charm", "Bulwark Charm",
			"Shields are permitted once more.",
			RelicEffect.ease(Restriction.SHIELD));
	}

	/** Eases QUIVERLESS — ammunition may be used. */
	public static Relic fletchersQuiver()
	{
		return new Relic("fletchers-quiver", "Fletcher's Quiver",
			"Ammunition is permitted once more.",
			RelicEffect.ease(Restriction.AMMO));
	}

	/** Eases RUNELESS — rune casting returns. */
	public static Relic runicFocus()
	{
		return new Relic("runic-focus", "Runic Focus",
			"Rune-cast magic is permitted once more.",
			RelicEffect.ease(Restriction.RUNES));
	}

	/** Eases BOUND_BOOK — every spellbook is permitted again. */
	public static Relic arcaneTome()
	{
		return new Relic("arcane-tome", "Arcane Tome",
			"Every spellbook is permitted once more.",
			RelicEffect.ease(Restriction.SPELLBOOK));
	}

	/** Permits melee under ONE_STYLE. */
	public static Relic wayOfTheBlade()
	{
		return new Relic("way-of-the-blade", "Way of the Blade",
			"Melee is permitted once more.",
			RelicEffect.permitCombatStyle(CombatStyle.MELEE));
	}

	/** Permits ranged under ONE_STYLE. */
	public static Relic wayOfTheBow()
	{
		return new Relic("way-of-the-bow", "Way of the Bow",
			"Ranged is permitted once more.",
			RelicEffect.permitCombatStyle(CombatStyle.RANGED));
	}

	/** Permits magic under ONE_STYLE. */
	public static Relic wayOfTheWand()
	{
		return new Relic("way-of-the-wand", "Way of the Wand",
			"Magic is permitted once more.",
			RelicEffect.permitCombatStyle(CombatStyle.MAGIC));
	}

	/** Loosens TIGHT_POCKETS — seven more inventory slots. */
	public static Relic deepPockets()
	{
		return new Relic("deep-pockets", "Deep Pockets",
			"Seven more inventory slots may be used.",
			RelicEffect.addInventorySlots(7));
	}
}
