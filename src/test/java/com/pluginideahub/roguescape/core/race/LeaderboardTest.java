package com.pluginideahub.roguescape.core.race;

import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.recap.RunRecap;
import com.pluginideahub.roguescape.core.seed.ChallengeCodec;
import com.pluginideahub.roguescape.core.seed.ChallengeDefinition;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class LeaderboardTest
{
	private static RunRecap recap(String seed, RunState state, int score, long ms, int legal, int illegal)
	{
		return RunRecap.builder()
			.goal("Weekly race")
			.seed(seed)
			.state(state)
			.score(score)
			.durationMillis(ms)
			.legalCount(legal)
			.illegalCount(illegal)
			.build();
	}

	@Test
	public void sortsCompletedRunsAheadAndByScoreThenTime()
	{
		Leaderboard board = new Leaderboard("evt-1", "seed-1");
		board.add(Leaderboard.fromRecap("Caleb", "evt-1", recap("seed-1", RunState.COMPLETE, 20, 120_000L, 12, 0), 1L));
		board.add(Leaderboard.fromRecap("Friend", "evt-1", recap("seed-1", RunState.COMPLETE, 30, 200_000L, 14, 0), 2L));
		board.add(Leaderboard.fromRecap("FailGuy", "evt-1", recap("seed-1", RunState.FAILED, 99, 1_000L, 0, 1), 3L));

		List<LeaderboardEntry> ranked = board.ranked();
		assertEquals("Friend", ranked.get(0).playerName());
		assertEquals("Caleb", ranked.get(1).playerName());
		assertEquals("FailGuy", ranked.get(2).playerName());
	}

	@Test
	public void recapImportFromRemotePeerWorks()
	{
		// Simulate a peer that exported recap fields and shipped them across.
		RunRecap peer = RunRecap.builder().goal("Race").seed("seed-2").state(RunState.COMPLETE)
			.score(50).durationMillis(60_000L).legalCount(20).illegalCount(0).build();
		LeaderboardEntry peerEntry = Leaderboard.fromRecap("Remote", "evt-2", peer, 100L);
		assertEquals(50, peerEntry.score());
		assertEquals("seed-2", peerEntry.seed());
		assertEquals(RunState.COMPLETE, peerEntry.state());
	}

	@Test
	public void weeklyEventCarriesChallengeDefinition()
	{
		ChallengeDefinition challenge = ChallengeDefinition.builder()
			.challengeId("wk-1")
			.seed("seed-week")
			.goal("Friday weekly")
			.routeLength(3)
			.bossCount(1)
			.roomNamePool("A", "B", "C", "D")
			.bossNamePool("X")
			.build();
		WeeklyEvent event = new WeeklyEvent("week-2026-22", "Week 22", "2026-05-25", "2026-06-01", challenge);
		assertEquals("week-2026-22", event.eventId());
		assertEquals("seed-week", event.challenge().seed());

		// Codec round-trip on the underlying challenge ensures the event JSON-ish payload
		// can survive plugin <-> companion app transport.
		ChallengeDefinition decoded = ChallengeCodec.decode(ChallengeCodec.encode(challenge));
		assertEquals(challenge.seed(), decoded.seed());
		assertEquals(challenge.roomNamePool(), decoded.roomNamePool());
	}

	@Test
	public void uploadSinkInterfaceCallsThroughNoopByDefault()
	{
		// Acts as a contract test that the interface exists and can be used as a seam.
		final int[] count = new int[]{0};
		RaceUploadSink sink = entry -> count[0]++;
		LeaderboardEntry entry = Leaderboard.fromRecap("Local",
			"evt-x",
			RunRecap.builder().goal("g").seed("seed-X").state(RunState.COMPLETE).score(1).durationMillis(10).build(),
			0L);
		sink.upload(entry);
		assertEquals(1, count[0]);
		RaceUploadSink.NOOP.upload(entry); // does nothing, doesn't throw
	}
}
