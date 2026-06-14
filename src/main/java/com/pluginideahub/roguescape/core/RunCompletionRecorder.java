package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.race.Leaderboard;
import com.pluginideahub.roguescape.core.race.LeaderboardEntry;
import com.pluginideahub.roguescape.core.race.RaceUploadSink;
import com.pluginideahub.roguescape.core.recap.RunHistory;
import com.pluginideahub.roguescape.core.recap.RunRecap;

/**
 * Pure, testable run-end recorder: snapshots a finished run into {@link RunRecap}, appends it to a
 * local {@link RunHistory}, and produces a {@link LeaderboardEntry} (pushed through the supplied
 * {@link RaceUploadSink}, which is {@link RaceUploadSink#NOOP} until an online sink exists).
 *
 * <p>This is the core of the run-completion wiring; the plugin only owns a guarded one-shot tick
 * edge that delegates here, so the run-correctness logic stays unit-tested.
 */
public final class RunCompletionRecorder
{
	private static final String FALLBACK_PLAYER = "You";
	private static final String LOCAL_EVENT = "local";

	private RunCompletionRecorder() {}

	public static LeaderboardEntry record(RogueScapeRun run, long durationMillis, String playerName,
		long submittedAtEpochMillis, RunHistory history, RaceUploadSink sink)
	{
		if (run == null || history == null)
		{
			throw new IllegalArgumentException("run and history required");
		}
		RunRecap recap = RunRecap.snapshot(run, run.relicEngine(), durationMillis);
		history.add(recap);
		String player = playerName == null || playerName.trim().isEmpty() ? FALLBACK_PLAYER : playerName.trim();
		String eventId = recap.seed() == null || recap.seed().isEmpty() ? LOCAL_EVENT : recap.seed();
		LeaderboardEntry entry = Leaderboard.fromRecap(player, eventId, recap, submittedAtEpochMillis);
		if (sink != null)
		{
			sink.upload(entry);
		}
		return entry;
	}
}
