package com.pluginideahub.roguescape.core.region;

/**
 * Stage 3 — finer-grained room category. The Stage-1 {@code RunStageType} distinguishes
 * ROOM vs BOSS; this enum classifies ROOM-style stages further so reward/legality rules
 * can vary by room kind.
 */
public enum RoomKind
{
	REGION,
	COMBAT,
	SUPPLY,
	WEAPON,
	ARMOUR,
	SHOP,
	SKILLING,
	BOSS,
	CHOICE_CHEST
}
