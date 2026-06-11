package com.pluginideahub.roguescape.core.race;

/**
 * Stage 11 — interface for future online race-leaderboard sinks.
 *
 * The plugin core does not depend on a concrete network implementation. A noop default
 * is provided for tests; a Stage 12 web-app sink can implement this without changing
 * core code.
 */
public interface RaceUploadSink
{
	void upload(LeaderboardEntry entry);

	RaceUploadSink NOOP = entry -> {};
}
