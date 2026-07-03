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
 * ({@link #raiseLane}, {@link #addInventorySlots}, {@link #setAllowedSpellbook},
 * {@link #permitCombatStyle}) loosen the capped ones. The "build" is simply the shape of what has
 * been un-shackled.
 *
 * <p>Pure Java; the adapter asks {@link #decide} for plain restrictions and the {@code *Allowed}
 * queries for the parameterised ones, then blocks the action or fails the run accordingly.
 */
public final class RunRestrictions
{
	public static final int UNCAPPED = -1;

	/** A full OSRS inventory; a limit grown back to this lifts the INVENTORY_LIMIT restriction. */
	public static final int FULL_INVENTORY = 28;

	private final EnumSet<Restriction> active = EnumSet.noneOf(Restriction.class);
	// Per-lane caps (locked 2026-07-03): one level cap per upgrade lane, raised band by band.
	private final java.util.EnumMap<UpgradeLane, Integer> laneCaps = new java.util.EnumMap<>(UpgradeLane.class);
	private int inventoryLimit = UNCAPPED;
	private Spellbook allowedSpellbook;
	private final EnumSet<CombatStyle> allowedStyles = EnumSet.allOf(CombatStyle.class);

	public RunRestrictions() {}

	/** A run with nothing forbidden. */
	public static RunRestrictions unrestricted()
	{
		return new RunRestrictions();
	}

	/**
	 * The starting shackles: a run's initial restrictions assembled from the chosen start tier
	 * (the gear-tier cap you begin under) plus every chosen curse. This is the setup step of the
	 * subtractive design — everything after it is relics easing what was assembled here.
	 */
	public static RunRestrictions starting(StartTier tier, java.util.Collection<Curse> curses)
	{
		RunRestrictions r = new RunRestrictions();
		if (tier != null)
		{
			tier.apply(r);
		}
		if (curses != null)
		{
			for (Curse curse : curses)
			{
				if (curse != null)
				{
					curse.apply(r);
				}
			}
		}
		return r;
	}

	// ---------- Setup (curses / loadout add restrictions) ----------

	public RunRestrictions restrict(Restriction restriction)
	{
		if (restriction == Restriction.GEAR_TIER_CAP && laneCaps.isEmpty())
		{
			// The flag without caps would be a stuck state nothing can ease — give the caps a
			// concrete value (the lowest band) so raises exist.
			return restrictGearTier(GradeBands.BANDS[0]);
		}
		if (restriction != null)
		{
			active.add(restriction);
		}
		return this;
	}

	/** Caps ALL four lanes at once — StartTier's one knob ("same band, all lanes"). */
	public RunRestrictions restrictGearTier(int cap)
	{
		for (UpgradeLane lane : UpgradeLane.values())
		{
			laneCaps.put(lane, Math.max(0, cap));
		}
		active.add(Restriction.GEAR_TIER_CAP);
		return this;
	}

	/** Caps one lane (custom setups / future asymmetric starts). */
	public RunRestrictions restrictLane(UpgradeLane lane, int cap)
	{
		if (lane != null)
		{
			laneCaps.put(lane, Math.max(0, cap));
			active.add(Restriction.GEAR_TIER_CAP);
		}
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
		if (allowedStyles.containsAll(EnumSet.allOf(CombatStyle.class)))
		{
			// Every style allowed = no lock; setting the flag would be a stuck state nothing
			// in the pool could ever clear.
			return this;
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
			if (restriction == Restriction.GEAR_TIER_CAP) laneCaps.clear();
			if (restriction == Restriction.INVENTORY_LIMIT) inventoryLimit = UNCAPPED;
			if (restriction == Restriction.SPELLBOOK) allowedSpellbook = null;
			if (restriction == Restriction.COMBAT_STYLE) allowedStyles.addAll(EnumSet.allOf(CombatStyle.class));
		}
		return this;
	}

	/**
	 * Raise one lane to its next band (the upgrade-lane reward). Raising past the top band frees
	 * the lane; once every lane is free the GEAR_TIER_CAP restriction lifts entirely.
	 */
	public RunRestrictions raiseLane(UpgradeLane lane)
	{
		Integer cap = lane == null ? null : laneCaps.get(lane);
		if (cap != null)
		{
			int next = GradeBands.next(cap);
			if (next == UNCAPPED)
			{
				laneCaps.remove(lane);
			}
			else
			{
				laneCaps.put(lane, next);
			}
			if (laneCaps.isEmpty())
			{
				active.remove(Restriction.GEAR_TIER_CAP);
			}
		}
		return this;
	}

	/** Grant more inventory slots (relic); reaching the full inventory lifts the limit entirely. */
	public RunRestrictions addInventorySlots(int slots)
	{
		if (active.contains(Restriction.INVENTORY_LIMIT) && inventoryLimit != UNCAPPED)
		{
			inventoryLimit += Math.max(0, slots);
			if (inventoryLimit >= FULL_INVENTORY)
			{
				permit(Restriction.INVENTORY_LIMIT);
			}
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

	/** The cap on one lane, or {@link #UNCAPPED} when that lane is free. */
	public int laneCap(UpgradeLane lane)
	{
		Integer cap = lane == null ? null : laneCaps.get(lane);
		return cap == null ? UNCAPPED : cap;
	}

	/** The loosest still-active lane cap — a one-number summary for display, {@link #UNCAPPED} when all free. */
	public int gearTierCap()
	{
		int max = UNCAPPED;
		for (Integer cap : laneCaps.values())
		{
			if (cap != null && cap > max)
			{
				max = cap;
			}
		}
		return max;
	}

	/** The tightest still-active lane cap — the one an unmapped item is held to, {@link #UNCAPPED} when all free. */
	public int strictestLaneCap()
	{
		int min = UNCAPPED;
		for (Integer cap : laneCaps.values())
		{
			if (cap != null && (min == UNCAPPED || cap < min))
			{
				min = cap;
			}
		}
		return min;
	}

	public int inventoryLimit() { return inventoryLimit; }
	public Spellbook allowedSpellbook() { return allowedSpellbook; }
	public Set<CombatStyle> allowedStyles() { return Collections.unmodifiableSet(EnumSet.copyOf(allowedStyles)); }

	/** Whether a requirement level passes one lane's cap. */
	public boolean laneAllowed(UpgradeLane lane, int level)
	{
		int cap = laneCap(lane);
		return cap == UNCAPPED || level <= cap;
	}

	/** Legacy single-cap query: the level must pass EVERY active lane (used where the lane is unknown). */
	public boolean gearTierAllowed(int tier)
	{
		for (UpgradeLane lane : UpgradeLane.values())
		{
			if (!laneAllowed(lane, tier))
			{
				return false;
			}
		}
		return true;
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
