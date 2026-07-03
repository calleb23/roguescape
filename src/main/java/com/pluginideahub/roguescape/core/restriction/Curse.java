package com.pluginideahub.roguescape.core.restriction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The curse catalog — the unified, stackable restriction toolkit. A curse is <em>setup-only</em>:
 * it adds restrictions when the run is assembled and never changes mid-run (relics may later ease
 * what a curse imposed). Each curse scores; score values are data, deferred to playtest.
 *
 * <p>Everything here must be expressible as "add a restriction" — that is the whole design space
 * (see docs/plans/roguescape-gameplay-design.md).
 */
public enum Curse
{
	FAMINE("Famine", "Food is forbidden.", 10, Restriction.FOOD),
	DRY_THROAT("Dry Throat", "Potions are forbidden.", 10, Restriction.POTIONS),
	SEALED_BANK("Sealed Bank", "No bank, trade, or Grand Exchange.", 15,
		Restriction.BANK, Restriction.TRADE, Restriction.GRAND_EXCHANGE),
	ANCHORED("Anchored", "Teleports are forbidden.", 10, Restriction.TELEPORTS),
	FAITHLESS("Faithless", "Prayer is forbidden.", 15, Restriction.PRAYER),
	BARE_FISTED("Bare-fisted", "Shields are forbidden.", 5, Restriction.SHIELD),
	QUIVERLESS("Quiverless", "Ammunition is forbidden.", 10, Restriction.AMMO),
	RUNELESS("Runeless", "Rune-cast magic is forbidden.", 10, Restriction.RUNES),
	TIGHT_POCKETS("Tight Pockets", "Only 14 inventory slots may be used.", 10)
	{
		@Override
		public void apply(RunRestrictions restrictions)
		{
			restrictions.restrictInventory(TIGHT_POCKETS_SLOTS);
		}
	},
	GODLESS("Godless", "Piety, Rigour, and Augury are forbidden.", 10,
		Restriction.PIETY, Restriction.RIGOUR, Restriction.AUGURY),
	BOUND_BOOK("Bound Book", "Only the standard spellbook is permitted.", 10)
	{
		@Override
		public void apply(RunRestrictions restrictions)
		{
			restrictions.restrictSpellbook(Spellbook.STANDARD);
		}
	},
	ONE_STYLE("One Style", "Only one combat style may be used.", 15)
	{
		@Override
		public void apply(RunRestrictions restrictions)
		{
			// v1: melee is the default permitted style; picking the style at setup is a
			// Contract-UI feature (the curse stays one entry, the parameter arrives later).
			restrictions.restrictCombatStyles(CombatStyle.MELEE);
		}
	};

	/** The slot allowance Tight Pockets leaves open (half an inventory). Playtest data. */
	public static final int TIGHT_POCKETS_SLOTS = 14;

	private final String displayName;
	private final String description;
	private final int scoreBonus;
	private final List<Restriction> adds;

	Curse(String displayName, String description, int scoreBonus, Restriction... adds)
	{
		this.displayName = displayName;
		this.description = description;
		this.scoreBonus = scoreBonus;
		this.adds = Collections.unmodifiableList(Arrays.asList(adds));
	}

	public String displayName()
	{
		return displayName;
	}

	public String description()
	{
		return description;
	}

	/** Score awarded for finishing a run with this curse active. Playtest data. */
	public int scoreBonus()
	{
		return scoreBonus;
	}

	/** The plain restrictions this curse adds (empty for parameterised curses). */
	public List<Restriction> restrictions()
	{
		return adds;
	}

	/** Adds this curse's restrictions to the run. */
	public void apply(RunRestrictions restrictions)
	{
		for (Restriction r : adds)
		{
			restrictions.restrict(r);
		}
	}
}
