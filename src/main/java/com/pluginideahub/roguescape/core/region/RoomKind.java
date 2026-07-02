package com.pluginideahub.roguescape.core.region;

/**
 * Stage 3 — finer-grained room category. The Stage-1 {@code RunStageType} distinguishes
 * ROOM vs BOSS; this enum classifies ROOM-style stages further so reward/restriction rules
 * can vary by room kind. Locked to four collection categories plus BOSS.
 */
public enum RoomKind
{
	/** A weapon you pick up and wield. */
	WEAPON,
	/** Armour you wear. */
	ARMOUR,
	/** Consumables (food/potions/runes/ammo) and raw crafting materials (bars/ores). */
	SUPPLY,
	/** An item you make and then wear or wield (smith at an anvil, etc.). */
	CRAFTING,
	/** Boss stage. */
	BOSS
}
