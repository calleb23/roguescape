package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 7 — minimal overlay/HUD model: current goal, current room, score, relic count, items
 * collected, and pending warnings. Pure data — the RuneLite overlay renders these fields.
 */
public final class OverlayViewModel
{
	private final String goal;
	private final String currentRoom;
	private final RunState state;
	private final int score;
	private final int relicCount;
	private final int itemsCollected;
	private final boolean currentRegionAllowed;
	private final List<String> warnings;

	private OverlayViewModel(Builder b)
	{
		this.goal = b.goal;
		this.currentRoom = b.currentRoom;
		this.state = b.state;
		this.score = b.score;
		this.relicCount = b.relicCount;
		this.itemsCollected = b.itemsCollected;
		this.currentRegionAllowed = b.currentRegionAllowed;
		this.warnings = Collections.unmodifiableList(new ArrayList<>(b.warnings));
	}

	public String goal() { return goal; }
	public String currentRoom() { return currentRoom; }
	public RunState state() { return state; }
	public int score() { return score; }
	public int relicCount() { return relicCount; }
	public int itemsCollected() { return itemsCollected; }
	public boolean currentRegionAllowed() { return currentRegionAllowed; }
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
			.itemsCollected(run.itemsCollected())
			.currentRegionAllowed(run.currentRegionAllowed());
		if (!run.currentRegionAllowed())
		{
			b.warning("Outside this room's region: " + run.currentRegionId());
		}
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
		private int itemsCollected;
		private boolean currentRegionAllowed = true;
		private final List<String> warnings = new ArrayList<>();

		public Builder goal(String g) { this.goal = g == null ? "" : g; return this; }
		public Builder currentRoom(String r) { this.currentRoom = r == null ? "" : r; return this; }
		public Builder state(RunState s) { this.state = s == null ? RunState.ACTIVE : s; return this; }
		public Builder score(int v) { this.score = v; return this; }
		public Builder relicCount(int v) { this.relicCount = v; return this; }
		public Builder itemsCollected(int v) { this.itemsCollected = v; return this; }
		public Builder currentRegionAllowed(boolean b) { this.currentRegionAllowed = b; return this; }
		public Builder warning(String w) { if (w != null && !w.isEmpty()) warnings.add(w); return this; }

		public OverlayViewModel build() { return new OverlayViewModel(this); }
	}
}
