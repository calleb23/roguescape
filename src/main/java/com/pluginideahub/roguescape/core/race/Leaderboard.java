package com.pluginideahub.roguescape.core.race;

import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.recap.RunRecap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 11 — local leaderboard. Holds entries from multiple players for one seed/event and
 * sorts by the same comparator used in {@link com.pluginideahub.roguescape.core.recap.RunHistory}.
 *
 * The online-board seam is intentionally a small static "uploadStub" — concrete network
 * implementations will live behind {@link RaceUploadSink}.
 */
public final class Leaderboard
{
	private final String eventId;
	private final String seed;
	private final List<LeaderboardEntry> entries = new ArrayList<>();

	public Leaderboard(String eventId, String seed)
	{
		this.eventId = eventId == null ? "" : eventId;
		this.seed = seed == null ? "" : seed;
	}

	public String eventId() { return eventId; }
	public String seed() { return seed; }

	public Leaderboard add(LeaderboardEntry entry)
	{
		if (entry != null) entries.add(entry);
		return this;
	}

	public List<LeaderboardEntry> ranked()
	{
		List<LeaderboardEntry> copy = new ArrayList<>(entries);
		copy.sort((a, b) -> {
			boolean aDone = a.state() == RunState.COMPLETE;
			boolean bDone = b.state() == RunState.COMPLETE;
			if (aDone != bDone) return aDone ? -1 : 1;
			int byScore = Integer.compare(b.score(), a.score());
			if (byScore != 0) return byScore;
			int byTime = Long.compare(a.durationMillis(), b.durationMillis());
			if (byTime != 0) return byTime;
			return Integer.compare(b.itemsCollected(), a.itemsCollected());
		});
		return Collections.unmodifiableList(copy);
	}

	public int size() { return entries.size(); }

	public static LeaderboardEntry fromRecap(String player, String eventId, RunRecap recap, long submittedAtEpochMillis)
	{
		return new LeaderboardEntry(player, eventId, recap.seed(), recap.state(), recap.score(),
			recap.durationMillis(), recap.itemsCollected(), submittedAtEpochMillis);
	}
}
