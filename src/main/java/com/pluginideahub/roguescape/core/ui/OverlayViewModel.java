package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 7 — minimal overlay/HUD model: current goal, current room, score, relic count, and
 * pending warnings (region/legality). Pure data — the RuneLite overlay renders these fields.
 */
public final class OverlayViewModel
{
	private final String goal;
	private final String currentRoom;
	private final RunState state;
	private final int score;
	private final int relicCount;
	private final int legalCount;
	private final int suspiciousCount;
	private final int illegalCount;
	private final boolean currentRegionLegal;
	private final List<String> warnings;

	private OverlayViewModel(Builder b)
	{
		this.goal = b.goal;
		this.currentRoom = b.currentRoom;
		this.state = b.state;
		this.score = b.score;
		this.relicCount = b.relicCount;
		this.legalCount = b.legalCount;
		this.suspiciousCount = b.suspiciousCount;
		this.illegalCount = b.illegalCount;
		this.currentRegionLegal = b.currentRegionLegal;
		this.warnings = Collections.unmodifiableList(new ArrayList<>(b.warnings));
	}

	public String goal() { return goal; }
	public String currentRoom() { return currentRoom; }
	public RunState state() { return state; }
	public int score() { return score; }
	public int relicCount() { return relicCount; }
	public int legalCount() { return legalCount; }
	public int suspiciousCount() { return suspiciousCount; }
	public int illegalCount() { return illegalCount; }
	public boolean currentRegionLegal() { return currentRegionLegal; }
	public List<String> warnings() { return warnings; }

	public static OverlayViewModel from(RogueScapeRun run)
	{
		RogueScapeRunSession s = run.session();
		RunStage current = run.currentEnteredStage();
		Builder b = new Builder()
			.goal(s.goal())
			.currentRoom(current == null ? "" : current.name())
			.state(s.runState())
			.score(s.runScore())
			.relicCount(s.relicCount())
			.legalCount(run.legalCount())
			.suspiciousCount(run.suspiciousCount())
			.illegalCount(run.illegalCount())
			.currentRegionLegal(run.currentRegionLegal());
		if (!run.currentRegionLegal())
		{
			b.warning("Outside legal region: " + run.currentRegionId());
		}
		if (run.illegalCount() > 0) b.warning(run.illegalCount() + " illegal item(s) observed");
		if (run.suspiciousCount() > 0) b.warning(run.suspiciousCount() + " suspicious item(s)");
		return b.build();
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private String goal = "";
		private String currentRoom = "";
		private RunState state = RunState.ACTIVE;
		private int score;
		private int relicCount;
		private int legalCount;
		private int suspiciousCount;
		private int illegalCount;
		private boolean currentRegionLegal = true;
		private final List<String> warnings = new ArrayList<>();

		public Builder goal(String g) { this.goal = g == null ? "" : g; return this; }
		public Builder currentRoom(String r) { this.currentRoom = r == null ? "" : r; return this; }
		public Builder state(RunState s) { this.state = s == null ? RunState.ACTIVE : s; return this; }
		public Builder score(int v) { this.score = v; return this; }
		public Builder relicCount(int v) { this.relicCount = v; return this; }
		public Builder legalCount(int v) { this.legalCount = v; return this; }
		public Builder suspiciousCount(int v) { this.suspiciousCount = v; return this; }
		public Builder illegalCount(int v) { this.illegalCount = v; return this; }
		public Builder currentRegionLegal(boolean b) { this.currentRegionLegal = b; return this; }
		public Builder warning(String w) { if (w != null && !w.isEmpty()) warnings.add(w); return this; }

		public OverlayViewModel build() { return new OverlayViewModel(this); }
	}
}
