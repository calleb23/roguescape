package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The fight-gate loadout validation (MVP chunk 6, pure half). At PREP → FIGHT the adapter reads
 * the player's worn + carried items and asks this class whether the loadout obeys the run's
 * restrictions: gear-tier cap, forbidden consumable categories (food/potions/ammo/runes), the
 * shield lock, and the inventory limit. The gate either passes ({@link Result#clean()}) or lists
 * exactly what must be dropped before the fight may begin.
 */
public final class LoadoutCheck
{
	/**
	 * One item as the adapter sees it: a name, a requirement level if known, a category.
	 * The requirement level is the item's LANE requirement on the universal scale (locked
	 * 2026-07-03): equip level for weapon/armour, CREATION level (crafting/herblore/cooking)
	 * for jewellery/supplies — the adapter supplies whichever fits the category. Zero means
	 * unknown, which passes any cap.
	 */
	public static final class Item
	{
		private final String name;
		private final int equipLevelRequirement;
		private final BankItemCategory category;
		private final boolean worn;

		public Item(String name, int equipLevelRequirement, BankItemCategory category, boolean worn)
		{
			this.name = name == null ? "" : name;
			this.equipLevelRequirement = Math.max(0, equipLevelRequirement);
			this.category = category == null ? BankItemCategory.UNKNOWN : category;
			this.worn = worn;
		}

		public String name() { return name; }
		public int equipLevelRequirement() { return equipLevelRequirement; }
		public BankItemCategory category() { return category; }
		public boolean isWorn() { return worn; }
	}

	/** The gate's verdict: clean, or the violations that block the fight. */
	public static final class Result
	{
		private final List<String> violations;

		private Result(List<String> violations)
		{
			this.violations = Collections.unmodifiableList(new ArrayList<>(violations));
		}

		public static Result clean()
		{
			return new Result(Collections.emptyList());
		}

		public boolean passed() { return violations.isEmpty(); }
		public List<String> violations() { return violations; }
	}

	private LoadoutCheck()
	{
	}

	/** Validate the full worn + carried loadout against the run's restrictions. */
	public static Result validate(RunRestrictions restrictions, List<Item> loadout)
	{
		List<String> violations = new ArrayList<>();
		if (restrictions == null || loadout == null)
		{
			return Result.clean();
		}

		int slotsUsed = 0;
		for (Item item : loadout)
		{
			if (item == null)
			{
				continue;
			}
			if (!item.isWorn())
			{
				slotsUsed++;
			}

			// Lane caps: the item's requirement level must pass its lane's cap (equip req for
			// weapon/armour, creation req for jewellery/supplies — one universal scale).
			com.pluginideahub.roguescape.core.restriction.UpgradeLane lane =
				com.pluginideahub.roguescape.core.restriction.UpgradeLane.laneOf(item.category());
			if (lane != null)
			{
				int level = GearTiers.tierOf(item.name(), item.equipLevelRequirement());
				if (!restrictions.laneAllowed(lane, level))
				{
					violations.add(item.name() + " is above the " + lane.displayName() + " cap ("
						+ level + " > " + restrictions.laneCap(lane) + ")");
				}
			}
			else if (item.equipLevelRequirement() > 0)
			{
				// Equippable but unmapped category: hold it to the strictest active lane cap.
				int level = GearTiers.tierOf(item.name(), item.equipLevelRequirement());
				if (!restrictions.gearTierAllowed(level))
				{
					violations.add(item.name() + " is above the gear cap ("
						+ level + " > " + restrictions.strictestLaneCap() + ")");
				}
			}

			// Forbidden consumable categories (food, potions, ammo, runes).
			Restriction forbidden = restrictions.forbiddenBy(item.category());
			if (forbidden != null)
			{
				violations.add(item.name() + " is forbidden — " + forbidden.forbids() + " is not permitted");
			}

			// The shield lock.
			if (item.category() == BankItemCategory.SHIELD && restrictions.isRestricted(Restriction.SHIELD))
			{
				violations.add(item.name() + " is forbidden — shields are locked");
			}
		}

		if (!restrictions.inventorySizeAllowed(slotsUsed))
		{
			violations.add("Carrying " + slotsUsed + " items — the limit is " + restrictions.inventoryLimit());
		}

		return violations.isEmpty() ? Result.clean() : new Result(violations);
	}
}
