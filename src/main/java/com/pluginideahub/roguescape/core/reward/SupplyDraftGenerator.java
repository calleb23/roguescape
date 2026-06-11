package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.List;

public final class SupplyDraftGenerator
{
	private SupplyDraftGenerator() {}

	public static RewardDraft supplyDraft(String draftId, String stageId, long seed)
	{
		List<RewardOption> pool = new ArrayList<>();
		pool.add(new RewardOption("supply-food", "Claim 5 emergency food", ChestType.FOOD, null));
		pool.add(new RewardOption("supply-potion", "Claim 2 basic potions", ChestType.POTION, null));
		pool.add(new RewardOption("supply-ammo", "Claim ammo and runes bundle", ChestType.AMMO, null));
		pool.add(new RewardOption("supply-teleport", "Claim one panic teleport", ChestType.UTILITY, null));
		return RewardDrafter.draftFromOptions(draftId, stageId, ChestType.SUPPLY, pool, new DeterministicRng(String.valueOf(seed)));
	}
}
