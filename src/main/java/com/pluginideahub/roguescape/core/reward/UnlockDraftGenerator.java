package com.pluginideahub.roguescape.core.reward;

import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import com.pluginideahub.roguescape.core.unlock.RunUnlockType;
import java.util.ArrayList;
import java.util.List;

public final class UnlockDraftGenerator
{
	private UnlockDraftGenerator() {}

	public static RewardDraft unlockDraft(String draftId, RunStage stage, long seed)
	{
		String stageId = stage == null ? "" : stage.id();
		String stageName = stage == null ? "" : stage.name();
		List<RewardOption> pool = new ArrayList<>();
		pool.add(RewardOption.ofUnlock(draftId + "-prayer",
			new RunUnlock(RunUnlockType.PRAYER, "Unlock prayers", stageId, stageName)));
		pool.add(RewardOption.ofUnlock(draftId + "-potion",
			new RunUnlock(RunUnlockType.POTION, "Unlock potions", stageId, stageName)));
		pool.add(RewardOption.ofUnlock(draftId + "-bank",
			new RunUnlock(RunUnlockType.BANK, "Unlock bank withdrawals", stageId, stageName)));
		pool.add(RewardOption.ofUnlock(draftId + "-trade",
			new RunUnlock(RunUnlockType.TRADE, "Unlock trading", stageId, stageName)));
		pool.add(RewardOption.ofUnlock(draftId + "-inventory",
			new RunUnlock(RunUnlockType.INVENTORY, "Unlock extra inventory space", stageId, stageName)));
		return RewardDrafter.draftFromOptions(draftId, stageId, ChestType.UNLOCK, pool, new DeterministicRng(String.valueOf(seed)));
	}
}
