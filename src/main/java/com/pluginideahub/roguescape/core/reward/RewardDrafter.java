package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4 — deterministic 3-choice draft generator.
 *
 * Given a chest type, an eligible item pool, and an RNG, returns three distinct options.
 * If the pool has fewer than three eligible items, the draft is short by the same amount
 * (callers should consult {@link BankDraftPool#canRoll} before drafting). Already-unlocked
 * items are filtered out so a draft never re-offers an existing unlock.
 */
public final class RewardDrafter
{
	private RewardDrafter() {}

	public static RewardDraft draftBankUnlock(String draftId, String stageId, ChestType chestType,
		BankDraftPool pool, DeterministicRng rng)
	{
		if (pool == null) throw new IllegalArgumentException("pool required");
		if (rng == null) throw new IllegalArgumentException("rng required");
		List<BankItem> eligible = new ArrayList<>();
		for (BankItem item : pool.eligibleFor(chestType))
		{
			if (!pool.isUnlocked(item.itemId())) eligible.add(item);
		}
		shuffle(eligible, rng);
		List<BankItem> picks = eligible.subList(0, Math.min(3, eligible.size()));
		List<RewardOption> options = new ArrayList<>();
		for (BankItem item : picks)
		{
			options.add(new RewardOption(item.itemId(), item.itemName(), ChestType.BANK_UNLOCK, item));
		}
		if (options.isEmpty())
		{
			// fall back to placeholder so callers don't NPE; the draft will be marked
			// rejected and effectively skipped by the run.
			options.add(new RewardOption("empty-pool", "(no eligible items)", chestType, null));
			RewardDraft draft = new RewardDraft(draftId, stageId, chestType, options);
			draft.reject();
			return draft;
		}
		return new RewardDraft(draftId, stageId, ChestType.BANK_UNLOCK, options);
	}

	public static RewardDraft draftFromOptions(String draftId, String stageId, ChestType chestType,
		List<RewardOption> pool, DeterministicRng rng)
	{
		if (pool == null) throw new IllegalArgumentException("pool required");
		if (rng == null) throw new IllegalArgumentException("rng required");
		List<RewardOption> copy = new ArrayList<>(pool);
		shuffle(copy, rng);
		List<RewardOption> picks = new ArrayList<>(copy.subList(0, Math.min(3, copy.size())));
		if (picks.isEmpty()) throw new IllegalArgumentException("option pool empty");
		return new RewardDraft(draftId, stageId, chestType, picks);
	}

	static <T> void shuffle(List<T> list, DeterministicRng rng)
	{
		rng.shuffle(list);
	}
}
