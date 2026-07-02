package com.pluginideahub.roguescape.core.ladder;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The gear-tier model (MVP chunk 5). A tier IS an equip-level requirement — "no armour above
 * 60 Defence req" is one tiny general rule. When the adapter knows an item's real requirement it
 * passes that straight through; when it doesn't, {@link #tierFromName} falls back to material
 * name-matching (the locked design's fallback for the few awkward tiers).
 */
public final class GearTiers
{
	/** Tier for an item whose material we cannot place — allowed under any cap. */
	public static final int UNKNOWN_TIER = 0;

	/** Material keyword → equip-level requirement, checked in order (longest names first). */
	private static final Map<String, Integer> MATERIALS = new LinkedHashMap<>();

	static
	{
		// Order matters: "black dragon" must not hit "black" (specific entries first).
		MATERIALS.put("3rd age", 65);
		MATERIALS.put("dragon", 60);
		MATERIALS.put("granite", 50);
		MATERIALS.put("rune", 40);
		MATERIALS.put("adamant", 30);
		MATERIALS.put("mithril", 20);
		MATERIALS.put("black", 10);
		MATERIALS.put("white", 10);
		MATERIALS.put("steel", 5);
		MATERIALS.put("iron", 1);
		MATERIALS.put("bronze", 1);
		MATERIALS.put("leather", 1);
	}

	private GearTiers()
	{
	}

	/** The tier of an item with a known equip-level requirement — the requirement itself. */
	public static int tierOfEquipLevel(int equipLevelRequirement)
	{
		return Math.max(0, equipLevelRequirement);
	}

	/** Material-name fallback: the tier implied by an item's name, or {@link #UNKNOWN_TIER}. */
	public static int tierFromName(String itemName)
	{
		if (itemName == null || itemName.isEmpty())
		{
			return UNKNOWN_TIER;
		}
		String lower = itemName.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> material : MATERIALS.entrySet())
		{
			if (lower.contains(material.getKey()))
			{
				return material.getValue();
			}
		}
		return UNKNOWN_TIER;
	}

	/**
	 * The best tier estimate for an item: the real equip requirement when the adapter has one
	 * ({@code > 0}), else the name fallback.
	 */
	public static int tierOf(String itemName, int equipLevelRequirement)
	{
		return equipLevelRequirement > 0 ? equipLevelRequirement : tierFromName(itemName);
	}
}
