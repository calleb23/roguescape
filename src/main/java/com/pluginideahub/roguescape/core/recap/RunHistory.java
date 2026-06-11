package com.pluginideahub.roguescape.core.recap;

import com.pluginideahub.roguescape.core.RunState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 8 — local history of completed/failed runs, with personal-best comparison helpers.
 *
 * Stored entries are full {@link RunRecap} snapshots so the history is self-contained for
 * exports and the Stage-11 race seam can use them as comparable rows.
 */
public final class RunHistory
{
	private final List<RunRecap> entries = new ArrayList<>();

	public RunHistory add(RunRecap recap)
	{
		if (recap != null) entries.add(recap);
		return this;
	}

	public List<RunRecap> entries() { return Collections.unmodifiableList(entries); }

	public int size() { return entries.size(); }

	/** Returns all entries for a seed (best by score first, then by fastest duration). */
	public List<RunRecap> entriesForSeed(String seed)
	{
		List<RunRecap> matches = new ArrayList<>();
		for (RunRecap r : entries) if (seed != null && seed.equals(r.seed())) matches.add(r);
		matches.sort(personalBestOrder());
		return matches;
	}

	/** Returns the personal best for a seed, or null if no completed entry exists. */
	public RunRecap personalBest(String seed)
	{
		RunRecap best = null;
		for (RunRecap r : entriesForSeed(seed))
		{
			if (r.state() != RunState.COMPLETE) continue;
			if (best == null) { best = r; continue; }
			if (compareForBest(r, best) < 0) best = r;
		}
		return best;
	}

	public Comparator<RunRecap> personalBestOrder()
	{
		return (a, b) -> compareForBest(a, b);
	}

	/** Negative if a is better than b. Completed runs always rank above failed runs. */
	public static int compareForBest(RunRecap a, RunRecap b)
	{
		if (a == b) return 0;
		boolean aDone = a.state() == RunState.COMPLETE;
		boolean bDone = b.state() == RunState.COMPLETE;
		if (aDone != bDone) return aDone ? -1 : 1;
		int byScore = Integer.compare(b.score(), a.score());
		if (byScore != 0) return byScore;
		int byTime = Long.compare(a.durationMillis(), b.durationMillis());
		if (byTime != 0) return byTime;
		int byLegal = Integer.compare(b.legalCount(), a.legalCount());
		if (byLegal != 0) return byLegal;
		return Integer.compare(a.illegalCount(), b.illegalCount());
	}

	public Map<String, RunRecap> personalBestsBySeed()
	{
		Map<String, RunRecap> out = new LinkedHashMap<>();
		for (RunRecap r : entries)
		{
			String seed = r.seed();
			if (seed == null) continue;
			RunRecap pb = personalBest(seed);
			if (pb != null) out.put(seed, pb);
		}
		return out;
	}
}
