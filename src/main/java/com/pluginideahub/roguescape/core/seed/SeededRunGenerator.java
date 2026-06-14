package com.pluginideahub.roguescape.core.seed;

import com.pluginideahub.roguescape.core.reward.DeterministicRng;
import com.pluginideahub.roguescape.core.reward.RewardDrafter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stage 9 — deterministic challenge generator. Given a {@link ChallengeDefinition} (which
 * already pins the seed string and template pools), produces the same {@link GeneratedRunPlan}
 * for every caller.
 *
 * The generator never picks the same room twice in a row; bosses come after the rooms.
 */
public final class SeededRunGenerator
{
	private SeededRunGenerator() {}

	public static GeneratedRunPlan generate(ChallengeDefinition challenge)
	{
		if (challenge == null) throw new IllegalArgumentException("challenge required");
		DeterministicRng rng = new DeterministicRng(challenge.seed() + "|" + challenge.challengeId());
		List<GeneratedRunPlan.Stage> route = new ArrayList<>();
		Set<String> usedRooms = new LinkedHashSet<>();
		List<String> roomPool = new ArrayList<>(challenge.roomNamePool());
		for (int i = 0; i < challenge.routeLength(); i++)
		{
			String room = pickDistinct(roomPool, usedRooms, rng, "Room " + (i + 1));
			route.add(new GeneratedRunPlan.Stage("R" + (i + 1), room, false));
		}
		Set<String> usedBosses = new LinkedHashSet<>();
		List<String> bossPool = new ArrayList<>(challenge.bossNamePool());
		for (int i = 0; i < challenge.bossCount(); i++)
		{
			String boss = pickDistinct(bossPool, usedBosses, rng, "Boss " + (i + 1));
			route.add(new GeneratedRunPlan.Stage("B" + (i + 1), boss, true));
		}
		List<String> relicPool = new ArrayList<>(challenge.relicIdPool());
		RewardDrafter.shuffle(relicPool, rng);
		List<String> chosenRelics = new ArrayList<>(relicPool.subList(0, Math.min(2, relicPool.size())));
		return new GeneratedRunPlan(challenge, route, chosenRelics, new ArrayList<>(challenge.starterKit()));
	}

	private static String pickDistinct(List<String> pool, Set<String> used, DeterministicRng rng, String fallback)
	{
		if (pool == null || pool.isEmpty()) return fallback;
		for (int attempts = 0; attempts < Math.max(8, pool.size() * 4); attempts++)
		{
			String pick = pool.get(rng.nextInt(pool.size()));
			if (used.add(pick)) return pick;
		}
		// Pool exhausted — return any.
		String pick = pool.get(rng.nextInt(pool.size()));
		used.add(pick);
		return pick;
	}
}
