package com.pluginideahub.roguescape.core.reward;

import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link RewardDraft} whose options are relics/modifiers drawn from the catalog.
 *
 * <p>The draw is deterministic for a given seed so seeded races present the same relic choices.
 * Pure Java; no RuneLite types.
 */
public final class RelicDraftGenerator
{
	private RelicDraftGenerator() {}

	/**
	 * Produces a relic reward draft with up to {@code optionCount} distinct options drawn from
	 * the easers that would actually loosen {@code current} (the no-filler law, locked
	 * 2026-07-03), deterministically ordered by {@code seed}. Returns {@code null} when nothing
	 * useful remains — the caller skips the reward phase honestly instead of padding.
	 */
	public static RewardDraft usefulRelicDraft(String draftId, String stageId, long seed, int optionCount,
		com.pluginideahub.roguescape.core.restriction.RunRestrictions current)
	{
		List<Relic> pool = new ArrayList<>(
			com.pluginideahub.roguescape.core.ladder.LadderRewardDrafter.usefulEasers(current));
		if (pool.isEmpty())
		{
			return null;
		}
		new DeterministicRng(seed).shuffle(pool);

		int n = Math.max(1, Math.min(optionCount, pool.size()));
		List<RewardOption> options = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
		{
			options.add(RewardOption.ofRelic(draftId + "-" + (i + 1), pool.get(i)));
		}
		return new RewardDraft(draftId, stageId, ChestType.RELIC, options);
	}

	/**
	 * Legacy unfiltered draw over the relic + modifier catalog. Kept for seeded-race
	 * compatibility paths that predate the no-filler law; new callers use
	 * {@link #usefulRelicDraft}.
	 */
	public static RewardDraft relicDraft(String draftId, String stageId, long seed, int optionCount)
	{
		List<Relic> pool = new ArrayList<>();
		pool.addAll(RelicLibrary.all());
		pool.addAll(ModifierLibrary.all());
		new DeterministicRng(seed).shuffle(pool);

		int n = Math.max(1, Math.min(optionCount, pool.size()));
		List<RewardOption> options = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
		{
			options.add(RewardOption.ofRelic(draftId + "-" + (i + 1), pool.get(i)));
		}
		return new RewardDraft(draftId, stageId, ChestType.RELIC, options);
	}
}
