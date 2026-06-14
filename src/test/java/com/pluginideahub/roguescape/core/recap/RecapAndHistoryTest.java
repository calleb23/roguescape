package com.pluginideahub.roguescape.core.recap;

import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RunCompletionReason;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import org.junit.Test;

import static org.junit.Assert.*;

public class RecapAndHistoryTest
{
	private static RogueScapeRun simpleRun(String goal, String seed)
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(goal, seed, RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		return RogueScapeRun.wrap(base);
	}

	@Test
	public void completedRunProducesMarkdownAndJsonRecap()
	{
		RogueScapeRun run = simpleRun("Mark recap", "seed-md");
		run.session().enterStage("R1");
		run.applyItemDelta("Bronze dagger", 1, ProvenanceHint.OBSERVED_LOOT);
		run.session().clearStage("R1");
		run.session().completeRun("Done", RunCompletionReason.GOAL_COMPLETE);

		RelicEngine engine = new RelicEngine().addRelic(RelicLibrary.fourFoodLimit());
		RunRecap recap = RunRecap.snapshot(run, engine, 12_345L);
		assertEquals(RunState.COMPLETE, recap.state());
		assertEquals(1, recap.legalCount());
		// The structured recap score must match the live effectiveScore (W2 consolidation).
		assertEquals(run.effectiveScore(), recap.score());

		String md = RecapExport.toMarkdown(recap);
		assertTrue(md.contains("# RogueScape Recap"));
		assertTrue(md.contains("Goal: Mark recap"));
		assertTrue(md.contains("Bronze dagger"));
		assertTrue(md.contains("Four-Food Limit"));

		String json = RecapExport.toJson(recap);
		assertTrue(json.startsWith("{"));
		assertTrue(json.endsWith("}"));
		assertTrue(json.contains("\"goal\":\"Mark recap\""));
		assertTrue(json.contains("\"seed\":\"seed-md\""));
		assertTrue(json.contains("\"state\":\"COMPLETE\""));
		assertTrue(json.contains("\"items\":["));
	}

	@Test
	public void failedRunProducesRecapWithFailureReason()
	{
		RogueScapeRun run = simpleRun("Fail recap", "seed-fail");
		run.session().enterStage("R1");
		run.session().recordViolation("Bad item", RogueScapeRunSession.RunEnding.UNKNOWN_ITEM_SOURCE);

		RunRecap recap = RunRecap.snapshot(run, null, 5_000L);
		assertEquals(RunState.FAILED, recap.state());
		String md = RecapExport.toMarkdown(recap);
		assertTrue(md.contains("UNKNOWN_ITEM_SOURCE"));
	}

	@Test
	public void runHistoryRanksCompletedRunsAhead()
	{
		RunHistory history = new RunHistory();
		RunRecap a = RunRecap.builder().goal("g").seed("seed-A").state(RunState.COMPLETE).score(10).durationMillis(60_000).build();
		RunRecap b = RunRecap.builder().goal("g").seed("seed-A").state(RunState.COMPLETE).score(20).durationMillis(50_000).build();
		RunRecap c = RunRecap.builder().goal("g").seed("seed-A").state(RunState.FAILED).score(99).durationMillis(1_000).build();
		history.add(a).add(b).add(c);

		RunRecap pb = history.personalBest("seed-A");
		assertNotNull(pb);
		assertEquals(20, pb.score());
		assertEquals(50_000L, pb.durationMillis());

		// Completed beats failed regardless of score.
		assertEquals(3, history.entriesForSeed("seed-A").size());
		assertEquals(RunState.COMPLETE, history.entriesForSeed("seed-A").get(0).state());
	}

	@Test
	public void historyPersonalBestsBySeedReturnsMapKeyedBySeed()
	{
		RunHistory history = new RunHistory();
		history.add(RunRecap.builder().goal("g").seed("seed-A").state(RunState.COMPLETE).score(5).durationMillis(100).build());
		history.add(RunRecap.builder().goal("g").seed("seed-B").state(RunState.COMPLETE).score(15).durationMillis(80).build());
		assertEquals(2, history.personalBestsBySeed().size());
		assertEquals(15, history.personalBestsBySeed().get("seed-B").score());
	}

	@Test
	public void jsonEscapesSpecialCharacters()
	{
		RunRecap recap = RunRecap.builder().goal("a \" \\ b").seed("c\nd").state(RunState.COMPLETE).build();
		String json = RecapExport.toJson(recap);
		assertTrue(json.contains("\"goal\":\"a \\\" \\\\ b\""));
		assertTrue(json.contains("\"seed\":\"c\\nd\""));
	}
}
