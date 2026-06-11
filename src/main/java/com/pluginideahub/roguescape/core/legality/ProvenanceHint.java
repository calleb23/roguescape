package com.pluginideahub.roguescape.core.legality;

/**
 * Stage 2 — hint about how an item was observed entering inventory. Adapters set this from
 * RuneLite events; the legality classifier turns the hint into an {@link ItemLegality}
 * in the current run context (region, room kind, mode, unlocks).
 */
public enum ProvenanceHint
{
	UNKNOWN,
	OBSERVED_LOOT,
	OBSERVED_SHOP_PURCHASE,
	OBSERVED_GATHERED,
	OBSERVED_CRAFTED,
	OBSERVED_BANK_WITHDRAWAL,
	OBSERVED_TRADE,
	OBSERVED_GE_COLLECT,
	OBSERVED_GROUND_PICKUP,
	OBSERVED_QUEST_REWARD,
	OBSERVED_DIALOG_REWARD,
	MANUAL_DECLARED_LEGAL,
	MANUAL_DECLARED_ILLEGAL
}
