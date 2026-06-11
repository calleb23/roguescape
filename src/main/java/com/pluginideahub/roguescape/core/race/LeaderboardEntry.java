package com.pluginideahub.roguescape.core.race;

import com.pluginideahub.roguescape.core.RunState;

/**
 * Stage 11 — leaderboard row for a single player attempt on a race seed.
 *
 * Immutable; constructed locally from {@link com.pluginideahub.roguescape.core.recap.RunRecap}
 * or received from imported recap exports (own or peer).
 */
public final class LeaderboardEntry
{
	private final String playerName;
	private final String eventId;
	private final String seed;
	private final RunState state;
	private final int score;
	private final long durationMillis;
	private final int legalCount;
	private final int illegalCount;
	private final long submittedAtEpochMillis;

	public LeaderboardEntry(String playerName, String eventId, String seed, RunState state,
		int score, long durationMillis, int legalCount, int illegalCount, long submittedAtEpochMillis)
	{
		if (playerName == null || playerName.isEmpty()) throw new IllegalArgumentException("playerName required");
		this.playerName = playerName;
		this.eventId = eventId == null ? "" : eventId;
		this.seed = seed == null ? "" : seed;
		this.state = state == null ? RunState.ACTIVE : state;
		this.score = score;
		this.durationMillis = Math.max(0, durationMillis);
		this.legalCount = legalCount;
		this.illegalCount = illegalCount;
		this.submittedAtEpochMillis = submittedAtEpochMillis;
	}

	public String playerName() { return playerName; }
	public String eventId() { return eventId; }
	public String seed() { return seed; }
	public RunState state() { return state; }
	public int score() { return score; }
	public long durationMillis() { return durationMillis; }
	public int legalCount() { return legalCount; }
	public int illegalCount() { return illegalCount; }
	public long submittedAtEpochMillis() { return submittedAtEpochMillis; }
}
