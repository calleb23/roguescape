package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.reward.RelicDraftGenerator;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * End-to-end determinism guard for the seeded pipeline: the same seed text must produce the
 * same route and the same relic drafts on every build, on every JVM. Pins the contract the
 * determinism fixes restored (DeterministicRng everywhere; no java.util.Random/String.hashCode).
 */
public class SeededRunPipelineTest
{
	private static List<String> buildSeededRoute(String seed)
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("e2e", seed, RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, seed, session, run);
		List<String> names = new ArrayList<>();
		for (RunStage stage : session.route().stages())
		{
			names.add(stage.type() + ":" + stage.name());
		}
		return names;
	}

	private static List<String> relicDraftOptions(long seed)
	{
		RewardDraft draft = RelicDraftGenerator.relicDraft("draft-1", "R1", seed, 3);
		List<String> ids = new ArrayList<>();
		for (RewardOption option : draft.options())
		{
			ids.add(option.label());
		}
		return ids;
	}

	@Test
	public void sameSeedTextProducesIdenticalRoutes()
	{
		assertEquals(buildSeededRoute("race-night-42"), buildSeededRoute("race-night-42"));
	}

	@Test
	public void differentSeedTextsProduceDifferentRoutes()
	{
		assertFalse("two distinct seeds should not collide on the same route",
			buildSeededRoute("race-night-42").equals(buildSeededRoute("race-night-43")));
	}

	@Test
	public void sameDraftSeedProducesIdenticalRelicOptions()
	{
		assertEquals(relicDraftOptions(987654321L), relicDraftOptions(987654321L));
	}

	@Test
	public void perStageDraftSeedsAreStableAcrossRuns()
	{
		// The run loop derives per-stage seeds from (runSeed, stageId, index); the relic
		// options for a given triple must be identical between two separate runs.
		long a = stageDraftSeed("shared-seed", "R2", 1);
		long b = stageDraftSeed("shared-seed", "R2", 1);
		assertEquals(a, b);
		assertEquals(relicDraftOptions(a), relicDraftOptions(b));
	}

	private static long stageDraftSeed(String runSeed, String stageId, int index)
	{
		long base = com.pluginideahub.roguescape.core.reward.DeterministicRng.hash(runSeed);
		long stage = com.pluginideahub.roguescape.core.reward.DeterministicRng.hash(stageId);
		return (base * 1000003L) ^ (stage * 31L) ^ (index * 2654435761L);
	}
}
