package com.pluginideahub.roguescape.core.item;

/**
 * Hint about how an item was observed entering inventory. Adapters set this from RuneLite
 * events; the run uses it to decide which room objectives an item gain counts toward
 * (e.g. a combat drop vs. a shop purchase vs. a gathered resource).
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
	MANUAL_DECLARED_PERMITTED,
	MANUAL_DECLARED_FORBIDDEN
}
