package com.pluginideahub.roguescape.core.recap;

import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.reward.BankItem;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 8 — pure-Java recap of a completed/failed run. Snapshots every field needed for the
 * markdown/JSON export and for {@link RunHistory} comparisons. Recap is immutable once
 * built.
 */
public final class RunRecap
{
	private final String goal;
	private final String seed;
	private final RunState state;
	private final String completionNote;
	private final int score;
	private final int legalCount;
	private final int suspiciousCount;
	private final int illegalCount;
	private final List<String> stageRows;
	private final List<String> itemRows;
	private final List<String> relicRows;
	private final List<String> unlockRows;
	private final long durationMillis;

	private RunRecap(Builder b)
	{
		this.goal = b.goal;
		this.seed = b.seed;
		this.state = b.state;
		this.completionNote = b.completionNote;
		this.score = b.score;
		this.legalCount = b.legalCount;
		this.suspiciousCount = b.suspiciousCount;
		this.illegalCount = b.illegalCount;
		this.stageRows = Collections.unmodifiableList(new ArrayList<>(b.stageRows));
		this.itemRows = Collections.unmodifiableList(new ArrayList<>(b.itemRows));
		this.relicRows = Collections.unmodifiableList(new ArrayList<>(b.relicRows));
		this.unlockRows = Collections.unmodifiableList(new ArrayList<>(b.unlockRows));
		this.durationMillis = b.durationMillis;
	}

	public String goal() { return goal; }
	public String seed() { return seed; }
	public RunState state() { return state; }
	public String completionNote() { return completionNote; }
	public int score() { return score; }
	public int legalCount() { return legalCount; }
	public int suspiciousCount() { return suspiciousCount; }
	public int illegalCount() { return illegalCount; }
	public List<String> stageRows() { return stageRows; }
	public List<String> itemRows() { return itemRows; }
	public List<String> relicRows() { return relicRows; }
	public List<String> unlockRows() { return unlockRows; }
	public long durationMillis() { return durationMillis; }

	public static RunRecap snapshot(RogueScapeRun run, RelicEngine engine, long durationMillis)
	{
		RogueScapeRunSession s = run.session();
		Builder b = new Builder()
			.goal(s.goal())
			.seed(s.seed())
			.state(s.runState())
			.completionNote(extractCompletionNote(s))
			// Use the run's effectiveScore so the structured recap matches the live panel/overlay
			// score (which includes relic bonuses and cleared room/boss points), not the raw
			// legal-item tally.
			.score(run.effectiveScore())
			.legalCount(run.legalCount())
			.suspiciousCount(run.suspiciousCount())
			.illegalCount(run.illegalCount())
			.durationMillis(durationMillis);
		for (RunStage stage : s.route().stages())
		{
			String state = stage.isCleared() ? "cleared" : stage.isEntered() ? "entered" : "pending";
			b.stageRow(stage.id() + " | " + stage.type() + " | " + stage.name() + " | " + state);
		}
		for (ItemEvent e : run.itemEvents())
		{
			b.itemRow(e.delta().itemName() + " x" + e.delta().quantity() + " [" + e.legality() + "]");
		}
		if (engine != null)
		{
			for (Relic r : engine.relics()) b.relicRow(r.relicId() + ": " + r.name());
			for (String note : engine.consumedNotes()) b.relicRow("consumed: " + note);
		}
		for (String id : run.bankPool().unlockedIds())
		{
			BankItem item = run.bankPool().get(id);
			if (item != null) b.unlockRow(item.itemName() + " [" + item.category() + "]");
		}
		for (RunUnlock unlock : run.unlocks())
		{
			b.unlockRow(unlock.displayRow());
		}
		// Reward draft summary appended to item rows so the markdown stays simple.
		for (RewardDraft draft : run.drafts())
		{
			b.itemRow("draft " + draft.draftId() + " -> "
				+ (draft.isSelected() ? draft.selected().label() : draft.isRejected() ? "rejected" : "no-pick"));
		}
		return b.build();
	}

	private static String extractCompletionNote(RogueScapeRunSession s)
	{
		// Stage-1 doesn't expose its private completionNote field directly; instead reuse the
		// completion reason and failure ending where possible.
		if (s.runState() == RunState.COMPLETE) return s.completionReason() == null ? "complete" : s.completionReason().name();
		if (s.runState() == RunState.FAILED) return s.failureReason() == null ? "failed" : s.failureReason().name();
		return "active";
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private String goal = "";
		private String seed = "";
		private RunState state = RunState.ACTIVE;
		private String completionNote = "";
		private int score;
		private int legalCount;
		private int suspiciousCount;
		private int illegalCount;
		private final List<String> stageRows = new ArrayList<>();
		private final List<String> itemRows = new ArrayList<>();
		private final List<String> relicRows = new ArrayList<>();
		private final List<String> unlockRows = new ArrayList<>();
		private long durationMillis;

		public Builder goal(String v) { this.goal = v == null ? "" : v; return this; }
		public Builder seed(String v) { this.seed = v == null ? "" : v; return this; }
		public Builder state(RunState v) { this.state = v == null ? RunState.ACTIVE : v; return this; }
		public Builder completionNote(String v) { this.completionNote = v == null ? "" : v; return this; }
		public Builder score(int v) { this.score = v; return this; }
		public Builder legalCount(int v) { this.legalCount = v; return this; }
		public Builder suspiciousCount(int v) { this.suspiciousCount = v; return this; }
		public Builder illegalCount(int v) { this.illegalCount = v; return this; }
		public Builder durationMillis(long v) { this.durationMillis = Math.max(0, v); return this; }
		public Builder stageRow(String s) { stageRows.add(s); return this; }
		public Builder itemRow(String s) { itemRows.add(s); return this; }
		public Builder relicRow(String s) { relicRows.add(s); return this; }
		public Builder unlockRow(String s) { unlockRows.add(s); return this; }

		public RunRecap build() { return new RunRecap(this); }
	}
}
