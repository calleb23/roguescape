package com.pluginideahub.roguescape.core.seed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 9 — output of {@link SeededRunGenerator}. Pure data describing the deterministic
 * route, relics, and starter kit derived from a challenge definition and seed.
 */
public final class GeneratedRunPlan
{
	public static final class Stage
	{
		public final String id;
		public final String name;
		public final boolean isBoss;
		Stage(String id, String name, boolean isBoss) { this.id = id; this.name = name; this.isBoss = isBoss; }
	}

	private final ChallengeDefinition challenge;
	private final List<Stage> route;
	private final List<String> relics;
	private final List<String> starterKit;

	GeneratedRunPlan(ChallengeDefinition challenge, List<Stage> route, List<String> relics, List<String> starterKit)
	{
		this.challenge = challenge;
		this.route = Collections.unmodifiableList(new ArrayList<>(route));
		this.relics = Collections.unmodifiableList(new ArrayList<>(relics));
		this.starterKit = Collections.unmodifiableList(new ArrayList<>(starterKit));
	}

	public ChallengeDefinition challenge() { return challenge; }
	public List<Stage> route() { return route; }
	public List<String> relics() { return relics; }
	public List<String> starterKit() { return starterKit; }
}
