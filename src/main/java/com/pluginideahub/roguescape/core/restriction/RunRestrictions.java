package com.pluginideahub.roguescape.core.restriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The live set of restrictions in force for a run — RogueScape's "what am I allowed to do right now"
 * state, and the spine of the subtractive design.
 *
 * <p>A run is set up with some restrictions active (loadout + curses). As the player earns relics,
 * those relics <em>ease</em> restrictions: {@link #permit} removes one, and the parameterised easers
 * ({@link #raiseGearTierCap}, {@link #addInventorySlots}, {@link #setAllowedSpellbook},
 * {@link #permitCombatStyle}) loosen the capped ones. The "build" is simply the shape of what has
 * been un-shackled.
 *
 * <p>Pure Java; the adapter asks {@link #decide} for plain restrictions and the {@code *Allowed}
 * queries for the parameterised ones, then blocks the action or fails the run accordingly.
 */
public final class RunRestrictions
{
	public static final int UNCAPPED = -1;

	private final EnumSet<Restriction> active = EnumSet.noneOf(Restriction.class);
	private int gearTierCap = UNCAPPED;
	private int inventoryLimit = UNCAPPED;
	private Spellbook allowedSpellbook;
	private final EnumSet<CombatStyle> allowedStyles = EnumSet.allOf(CombatStyle.class);

	public RunRestrictions() {}

	/** A run with nothing forbidden. */
	public static RunRestrictions unrestricted()
	{
		return new RunRestrictions();
	}

	// ---------- Setup (curses / loadout add restrictions) ----------

	public RunRestrictions restrict(Restriction restriction)
	{
		if (restriction != null)
		{
			active.add(restriction);
		}
		return this;
	}

	public RunRestrictions restrictGearTier(int cap)
	{
		this.gearTierCap = Math.max(0, cap);
		active.add(Restriction.GEAR_TIER_CAP);
		return this;
	}

	public RunRestrictions restrictInventory(int limit)
	{
		this.inventoryLimit = Math.max(0, limit);
		active.add(Restriction.INVENTORY_LIMIT);
		return this;
	}

	public RunRestrictions restrictSpellbook(Spellbook allowed)
	{
		this.allowedSpellbook = allowed;
		active.add(Restriction.SPELLBOOK);
		return this;
	}

	public RunRestrictions restrictCombatStyles(CombatStyle... allowed)
	{
		allowedStyles.clear();
		if (allowed != null)
		{
			for (CombatStyle s : allowed)
			{
				if (s != null) allowedStyles.add(s);
			}
		}
		active.add(Restriction.COMBAT_STYLE);
		return this;
	}

	// ---------- Easing (relics remove / loosen restrictions) ----------

	/** Fully lift a restriction — the canonical relic effect. */
	public RunRestrictions permit(Restriction restriction)
	{
		if (restriction != null)
		{
			active.remove(restriction);
			if (restriction == Restriction.GEAR_TIER_CAP) gearTierCap = UNCAPPED;
			if (restriction == Restriction.INVENTORY_LIMIT) inventoryLimit = UNCAPPED;
			if (restriction == Restriction.SPELLBOOK) allowedSpellbook = null;
			if (restriction == Restriction.COMBAT_STYLE) allowedStyles.addAll(EnumSet.allOf(CombatStyle.class));
		}
		return this;
	}

	/** Raise the gear-tier cap (relic). No effect if the cap isn't in force. */
	public RunRestrictions raiseGearTierCap(int by)
	{
		if (active.contains(Restriction.GEAR_TIER_CAP) && gearTierCap != UNCAPPED)
		{
			gearTierCap += Math.max(0, by);
		}
		return this;
	}

	/** Grant more inventory slots (relic). No effect if no limit is in force. */
	public RunRestrictions addInventorySlots(int slots)
	{
		if (active.contains(Restriction.INVENTORY_LIMIT) && inventoryLimit != UNCAPPED)
		{
			inventoryLimit += Math.max(0, slots);
		}
		return this;
	}

	/** Change which spellbook is permitted (shrine/relic). Keeps the lock, swaps the allowance. */
	public RunRestrictions setAllowedSpellbook(Spellbook allowed)
	{
		if (active.contains(Restriction.SPELLBOOK))
		{
			this.allowedSpellbook = allowed;
		}
		return this;
	}

	/** Add a permitted combat style (relic/shrine). Lifts the lock entirely once all are allowed. */
	public RunRestrictions permitCombatStyle(CombatStyle style)
	{
		if (style != null && active.contains(Restriction.COMBAT_STYLE))
		{
			allowedStyles.add(style);
			if (allowedStyles.containsAll(EnumSet.allOf(CombatStyle.class)))
			{
				active.remove(Restriction.COMBAT_STYLE);
			}
		}
		return this;
	}

	// ---------- Queries ----------

	public boolean isRestricted(Restriction restriction)
	{
		return restriction != null && active.contains(restriction);
	}

	/** The verdict for a plain (non-parameterised) attempted action. */
	public RestrictionOutcome decide(Restriction attempted)
	{
		if (attempted == null || !active.contains(attempted))
		{
			return RestrictionOutcome.ALLOW;
		}
		return attempted.enforcement() == Restriction.Enforcement.BLOCK
			? RestrictionOutcome.BLOCK
			: RestrictionOutcome.FAIL;
	}

	public int gearTierCap() { return gearTierCap; }
	public int inventoryLimit() { return inventoryLimit; }
	public Spellbook allowedSpellbook() { return allowedSpellbook; }
	public Set<CombatStyle> allowedStyles() { return Collections.unmodifiableSet(EnumSet.copyOf(allowedStyles)); }

	public boolean gearTierAllowed(int tier)
	{
		return gearTierCap == UNCAPPED || tier <= gearTierCap;
	}

	public boolean inventorySizeAllowed(int slotsUsed)
	{
		return inventoryLimit == UNCAPPED || slotsUsed <= inventoryLimit;
	}

	public boolean spellbookAllowed(Spellbook book)
	{
		return !active.contains(Restriction.SPELLBOOK) || allowedSpellbook == null || book == allowedSpellbook;
	}

	public boolean combatStyleAllowed(CombatStyle style)
	{
		return !active.contains(Restriction.COMBAT_STYLE) || allowedStyles.contains(style);
	}

	// ---------- Item-category restrictions (food/potions/ammo/runes) ----------

	/**
	 * The active restriction that forbids carrying/using an item of this category right now, or
	 * {@code null} if the category is fine. Drives both the red-X overlay and the menu block.
	 */
	public Restriction forbiddenBy(com.pluginideahub.roguescape.core.reward.BankItemCategory category)
	{
		Restriction mapped = categoryRestriction(category);
		return mapped != null && active.contains(mapped) ? mapped : null;
	}

	/** The verdict for using an item of this category (e.g. eating food) under the current rules. */
	public RestrictionOutcome decideItemUse(com.pluginideahub.roguescape.core.reward.BankItemCategory category)
	{
		Restriction forbidden = forbiddenBy(category);
		return forbidden == null ? RestrictionOutcome.ALLOW : decide(forbidden);
	}

	private static Restriction categoryRestriction(com.pluginideahub.roguescape.core.reward.BankItemCategory category)
	{
		if (category == null) return null;
		switch (category)
		{
			case FOOD: return Restriction.FOOD;
			case POTION: return Restriction.POTIONS;
			case RUNE: return Restriction.RUNES;
			case AMMO: return Restriction.AMMO;
			default: return null;
		}
	}

	/** Active restrictions, ordered by family then name — for the briefing / custom editor. */
	public List<Restriction> active()
	{
		List<Restriction> out = new ArrayList<>(active);
		out.sort(Comparator.comparing(Restriction::family).thenComparing(Restriction::name));
		return Collections.unmodifiableList(out);
	}

	public boolean isEmpty() { return active.isEmpty(); }
	public int count() { return active.size(); }
}
