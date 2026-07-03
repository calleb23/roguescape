package com.pluginideahub.roguescape.core.restriction;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;

/**
 * The four upgrade lanes (locked 2026-07-03). A lane cap is a LEVEL on one universal scale:
 * Weapon and Armour grade by <em>equip</em> requirement; Jewellery and Supplies grade by the
 * level required to <em>create</em> them (crafting / herblore / cooking). Lane raises are
 * upgrades — never relics.
 */
public enum UpgradeLane
{
	WEAPON("Weapon"),
	ARMOUR("Armour"),
	JEWELLERY("Jewellery"),
	SUPPLIES("Supplies");

	private final String displayName;

	UpgradeLane(String displayName)
	{
		this.displayName = displayName;
	}

	public String displayName()
	{
		return displayName;
	}

	/** The lane an item category is capped by, or {@code null} when no lane governs it. */
	public static UpgradeLane laneOf(BankItemCategory category)
	{
		if (category == null)
		{
			return null;
		}
		switch (category)
		{
			case MELEE_WEAPON:
			case RANGED_WEAPON:
			case MAGIC_WEAPON:
				return WEAPON;
			case SHIELD:
			case HELMET:
			case BODY:
			case LEGS:
			case BOOTS:
			case GLOVES:
			case CAPE:
				return ARMOUR;
			case RING:
			case NECK:
				return JEWELLERY;
			case FOOD:
			case POTION:
				return SUPPLIES;
			default:
				return null;
		}
	}
}
