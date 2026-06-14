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
	 * the combined relic + modifier catalog, deterministically ordered by {@code seed}.
	 */
	public static RewardDraft relicDraft(String draftId, String stageId, long seed, int optionCount)
	{
		List<Relic> pool = new ArrayList<>();
		pool.addAll(RelicLibrary.all());
		pool.addAll(ModifierLibrary.all());
		RewardDrafter.shuffle(pool, new DeterministicRng(seed));

		int n = Math.max(1, Math.min(optionCount, pool.size()));
		List<RewardOption> options = new ArrayList<>(n);
		for (int i = 0; i < n; i++)
		{
			options.add(RewardOption.ofRelic(draftId + "-" + (i + 1), pool.get(i)));
		}
		return new RewardDraft(draftId, stageId, ChestType.RELIC, options);
	}
}
