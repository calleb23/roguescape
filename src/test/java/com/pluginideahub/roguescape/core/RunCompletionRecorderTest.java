package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.race.LeaderboardEntry;
import com.pluginideahub.roguescape.core.race.RaceUploadSink;
import com.pluginideahub.roguescape.core.recap.RunHistory;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class RunCompletionRecorderTest
{
	private static RogueScapeRun completedRun(String seed)
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Goal", seed, RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");
		run.applyItemDelta("Bronze dagger", 1, ProvenanceHint.OBSERVED_LOOT);
		base.clearStage("R1");
		base.completeRun("done", RunCompletionReason.GOAL_COMPLETE);
		return run;
	}

	@Test
	public void recordAppendsHistoryBuildsEntryAndInvokesSink()
	{
		RunHistory history = new RunHistory();
		List<LeaderboardEntry> uploaded = new ArrayList<>();
		LeaderboardEntry entry = RunCompletionRecorder.record(
			completedRun("seed-A"), 12_345L, "Zezima", 1_000L, history, uploaded::add);

		assertEquals(1, history.size());
		assertEquals("Zezima", entry.playerName());
		assertEquals("seed-A", entry.seed());
		assertEquals(RunState.COMPLETE, entry.state());
		assertEquals(12_345L, entry.durationMillis());
		assertEquals(1, uploaded.size());
		assertSame(entry, uploaded.get(0));
		// The recorded recap is the personal best for its seed.
		assertEquals(entry.score(), history.personalBest("seed-A").score());
	}

	@Test
	public void blankPlayerFallsBackAndBlankSeedUsesLocalEvent()
	{
		RunHistory history = new RunHistory();
		LeaderboardEntry entry = RunCompletionRecorder.record(
			completedRun(""), 5_000L, "  ", 0L, history, RaceUploadSink.NOOP);

		assertEquals("You", entry.playerName());
		assertEquals("local", entry.eventId());
	}
}
