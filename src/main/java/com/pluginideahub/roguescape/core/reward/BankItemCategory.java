package com.pluginideahub.roguescape.core.reward;

/**
 * Stage 4 — classification of items eligible for the bank-draft pool. The categories are
 * coarse on purpose: the first-pass classifier is heuristic and a richer classification
 * (e.g. damage-tier weapons) lives behind the same enum without breaking callers.
 */
public enum BankItemCategory
{
	MELEE_WEAPON,
	RANGED_WEAPON,
	MAGIC_WEAPON,
	SHIELD,
	HELMET,
	BODY,
	LEGS,
	BOOTS,
	GLOVES,
	CAPE,
	NECK,
	RING,
	AMMO,
	FOOD,
	POTION,
	RUNE,
	TELEPORT,
	SKILLING_SUPPLY,
	JUNK,
	UNKNOWN
}
