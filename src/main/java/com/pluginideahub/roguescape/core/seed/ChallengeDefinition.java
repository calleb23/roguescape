package com.pluginideahub.roguescape.core.seed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stage 9 — pure-data challenge definition. Carries the goal text, mode label, seed string,
 * starter kit declaration, and the pools the seeded generator may draw from. Round-trips to
 * a minimal JSON-ish text form for sharing.
 */
public final class ChallengeDefinition
{
	private final String challengeId;
	private final String goal;
	private final String seed;
	private final String mode;
	private final int routeLength;
	private final int bossCount;
	private final List<String> roomNamePool;
	private final List<String> bossNamePool;
	private final List<String> relicIdPool;
	private final List<String> starterKit;

	private ChallengeDefinition(Builder b)
	{
		this.challengeId = Objects.requireNonNull(b.challengeId, "challengeId");
		this.goal = b.goal == null ? "" : b.goal;
		this.seed = b.seed == null ? "" : b.seed;
		this.mode = b.mode == null ? "" : b.mode;
		this.routeLength = Math.max(1, b.routeLength);
		this.bossCount = Math.max(0, b.bossCount);
		this.roomNamePool = Collections.unmodifiableList(new ArrayList<>(b.roomNamePool));
		this.bossNamePool = Collections.unmodifiableList(new ArrayList<>(b.bossNamePool));
		this.relicIdPool = Collections.unmodifiableList(new ArrayList<>(b.relicIdPool));
		this.starterKit = Collections.unmodifiableList(new ArrayList<>(b.starterKit));
	}

	public String challengeId() { return challengeId; }
	public String goal() { return goal; }
	public String seed() { return seed; }
	public String mode() { return mode; }
	public int routeLength() { return routeLength; }
	public int bossCount() { return bossCount; }
	public List<String> roomNamePool() { return roomNamePool; }
	public List<String> bossNamePool() { return bossNamePool; }
	public List<String> relicIdPool() { return relicIdPool; }
	public List<String> starterKit() { return starterKit; }

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private String challengeId;
		private String goal;
		private String seed;
		private String mode;
		private int routeLength = 3;
		private int bossCount = 1;
		private final List<String> roomNamePool = new ArrayList<>();
		private final List<String> bossNamePool = new ArrayList<>();
		private final List<String> relicIdPool = new ArrayList<>();
		private final List<String> starterKit = new ArrayList<>();

		public Builder challengeId(String v) { this.challengeId = v; return this; }
		public Builder goal(String v) { this.goal = v; return this; }
		public Builder seed(String v) { this.seed = v; return this; }
		public Builder mode(String v) { this.mode = v; return this; }
		public Builder routeLength(int v) { this.routeLength = v; return this; }
		public Builder bossCount(int v) { this.bossCount = v; return this; }
		public Builder roomNamePool(List<String> v) { this.roomNamePool.clear(); if (v != null) this.roomNamePool.addAll(v); return this; }
		public Builder roomNamePool(String... v) { return roomNamePool(Arrays.asList(v)); }
		public Builder bossNamePool(List<String> v) { this.bossNamePool.clear(); if (v != null) this.bossNamePool.addAll(v); return this; }
		public Builder bossNamePool(String... v) { return bossNamePool(Arrays.asList(v)); }
		public Builder relicIdPool(List<String> v) { this.relicIdPool.clear(); if (v != null) this.relicIdPool.addAll(v); return this; }
		public Builder relicIdPool(String... v) { return relicIdPool(Arrays.asList(v)); }
		public Builder starterKit(List<String> v) { this.starterKit.clear(); if (v != null) this.starterKit.addAll(v); return this; }
		public Builder starterKit(String... v) { return starterKit(Arrays.asList(v)); }
		public ChallengeDefinition build() { return new ChallengeDefinition(this); }
	}
}
