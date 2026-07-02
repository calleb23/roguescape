package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.briefing.RunBriefing;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Side-panel view model with tabs. RUN tab is clean and focused;
 * other tabs (ZONES, RELICS) expose builder and modifier details.
 */
public final class SidePanelViewModel
{
	private final PanelTab activeTab;
	private final boolean lobby;
	private final List<String> headerRows;
	private final List<String> ruleRows;
	private final List<String> statusRows;
	private final List<String> zoneRows;
	private final List<String> relicRows;
	private final List<String> routeRows;
	private final List<Chapter> chapters;

	/**
	 * One stage of the run as a journal chapter: an ordinal label, the place name, and
	 * whether it is the boss finale, already cleared (stamped), or the current bookmark.
	 * Pure data — the journal painters turn this into ink, stamps and ribbons.
	 */
	public static final class Chapter
	{
		private final String numeral;
		private final String name;
		private final boolean boss;
		private final boolean done;
		private final boolean current;

		public Chapter(String numeral, String name, boolean boss, boolean done, boolean current)
		{
			this.numeral = numeral == null ? "" : numeral;
			this.name = name == null ? "" : name;
			this.boss = boss;
			this.done = done;
			this.current = current;
		}

		public String numeral() { return numeral; }
		public String name() { return name; }
		public boolean isBoss() { return boss; }
		public boolean isDone() { return done; }
		public boolean isCurrent() { return current; }
	}

	/** Roman numeral for small chapter ordinals (1..~50); plain number beyond. */
	private static String roman(int n)
	{
		if (n <= 0 || n >= 400) return Integer.toString(n);
		int[] vals = {100, 90, 50, 40, 10, 9, 5, 4, 1};
		String[] syms = {"C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < vals.length; i++)
		{
			while (n >= vals[i])
			{
				sb.append(syms[i]);
				n -= vals[i];
			}
		}
		return sb.toString();
	}
	private final List<String> modifierRows;
	private final List<String> progressionRows;
	private final Set<PanelAction> enabledActions;

	// Typed live-run fields, consumed by the structured vertical sections.
	private final String goal;
	private final String stateLabel;
	private final String timerLabel;
	private final String phaseLabel;
	private final String roomName;
	private final String regionId;
	private final int score;
	private final int itemsCollected;
	private final int floorCurrent;
	private final int floorTotal;
	private final int bossesDefeated;
	private final int bossesTotal;
	private final String objective;
	private final boolean objectiveDone;
	private final String nextStage;
	private final boolean runOver;

	private SidePanelViewModel(Builder b)
	{
		this.activeTab = b.activeTab;
		this.lobby = b.lobby;
		this.headerRows = Collections.unmodifiableList(new ArrayList<>(b.headerRows));
		this.ruleRows = Collections.unmodifiableList(new ArrayList<>(b.ruleRows));
		this.statusRows = Collections.unmodifiableList(new ArrayList<>(b.statusRows));
		this.zoneRows = Collections.unmodifiableList(new ArrayList<>(b.zoneRows));
		this.relicRows = Collections.unmodifiableList(new ArrayList<>(b.relicRows));
		this.routeRows = Collections.unmodifiableList(new ArrayList<>(b.routeRows));
		this.chapters = Collections.unmodifiableList(new ArrayList<>(b.chapters));
		this.modifierRows = Collections.unmodifiableList(new ArrayList<>(b.modifierRows));
		this.progressionRows = Collections.unmodifiableList(new ArrayList<>(b.progressionRows));
		this.enabledActions = Collections.unmodifiableSet(EnumSet.copyOf(b.enabledActions));
		this.goal = b.goal;
		this.stateLabel = b.stateLabel;
		this.timerLabel = b.timerLabel;
		this.phaseLabel = b.phaseLabel;
		this.roomName = b.roomName;
		this.regionId = b.regionId;
		this.score = b.score;
		this.itemsCollected = b.itemsCollected;
		this.floorCurrent = b.floorCurrent;
		this.floorTotal = b.floorTotal;
		this.bossesDefeated = b.bossesDefeated;
		this.bossesTotal = b.bossesTotal;
		this.objective = b.objective;
		this.objectiveDone = b.objectiveDone;
		this.nextStage = b.nextStage;
		this.runOver = b.runOver;
	}

	public PanelTab activeTab() { return activeTab; }
	public boolean isLobby() { return lobby; }
	public List<String> headerRows() { return headerRows; }
	public List<String> ruleRows() { return ruleRows; }
	public List<String> statusRows() { return statusRows; }
	public List<String> zoneRows() { return zoneRows; }
	public List<String> relicRows() { return relicRows; }
	public List<String> routeRows() { return routeRows; }
	public List<Chapter> chapters() { return chapters; }
	public List<String> modifierRows() { return modifierRows; }
	public List<String> progressionRows() { return progressionRows; }
	public Set<PanelAction> enabledActions() { return enabledActions; }
	public boolean isActionEnabled(PanelAction a) { return enabledActions.contains(a); }

	public String goal() { return goal; }
	public String stateLabel() { return stateLabel; }
	public String timerLabel() { return timerLabel; }
	public String phaseLabel() { return phaseLabel; }
	public String roomName() { return roomName; }
	public String regionId() { return regionId; }
	public int score() { return score; }
	public int itemsCollected() { return itemsCollected; }
	public int floorCurrent() { return floorCurrent; }
	public int floorTotal() { return floorTotal; }
	public int bossesDefeated() { return bossesDefeated; }
	public int bossesTotal() { return bossesTotal; }
	public String objective() { return objective; }
	public boolean objectiveDone() { return objectiveDone; }
	public String nextStage() { return nextStage; }
	/** True when the run has ended (complete or failed) — the spread renders as The Final Page. */
	public boolean isRunOver() { return runOver; }

	/**
	 * The Contract (lobby) as a two-page spread. Left page = the choices: the mode contracts,
	 * the run title, and the Begin stamp. Right page = the context: the live briefing of the
	 * route the current selection will build (or why it could not be previewed). Pure core —
	 * the adapter supplies the selection state and the pre-built {@link RunBriefing}.
	 */
	public static JournalSpread contractSpread(RunMode mode, String runTitle, String seed,
		RunBriefing briefing, String briefingError)
	{
		List<JournalSpread.Block> left = new ArrayList<>();
		left.add(JournalSpread.Block.heading("Pick Your Contract"));
		List<JournalSpread.Choice> contracts = new ArrayList<>();
		contracts.add(new JournalSpread.Choice("Scavenger", "Earn power, room by room.", "Fresh source",
			JournalSpread.Tone.POSITIVE, mode == RunMode.FRESH_SOURCE || mode == RunMode.UNSPECIFIED, "scavenger"));
		contracts.add(new JournalSpread.Choice("Boss Ladder", "Short prep, boss loot.", "Climb",
			JournalSpread.Tone.GOLD, mode == RunMode.BANK_DRAFT, "rewarded"));
		contracts.add(new JournalSpread.Choice("Custom", "Draw your own route.", "Open builder",
			JournalSpread.Tone.MUTED, mode == RunMode.CUSTOM_CREATOR, "custom"));
		left.add(JournalSpread.Block.choices(contracts));
		left.add(JournalSpread.Block.gap());
		left.add(JournalSpread.Block.text("Run: " + (runTitle == null || runTitle.isEmpty() ? "(auto)" : runTitle),
			JournalSpread.Tone.INK));
		if (seed != null && !seed.trim().isEmpty())
		{
			left.add(JournalSpread.Block.note("Seed: " + seed.trim(), JournalSpread.Tone.MUTED));
		}
		left.add(JournalSpread.Block.gap());
		left.add(JournalSpread.Block.choices(Collections.singletonList(
			new JournalSpread.Choice("BEGIN THE RUN", "Sign and stamp the contract", "The route is drawn",
				JournalSpread.Tone.POSITIVE, false, "start-run"))));

		List<JournalSpread.Block> right = new ArrayList<>();
		if (briefing == null)
		{
			right.add(JournalSpread.Block.text(
				"Could not preview this route" + (briefingError == null || briefingError.isEmpty()
					? "." : ": " + briefingError), JournalSpread.Tone.NEGATIVE));
		}
		else
		{
			right.add(JournalSpread.Block.heading("The Route — " + briefing.roomCount() + " rooms"
				+ (briefing.bossCount() > 0 ? " + boss" : "")));
			if (!briefing.routeLocked())
			{
				right.add(JournalSpread.Block.note("Rooms re-roll on Begin. Set a seed to lock this route.",
					JournalSpread.Tone.MUTED));
			}
			for (RunBriefing.RoomLine room : briefing.rooms())
			{
				right.add(JournalSpread.Block.text(room.index() + ". " + room.name() + "  [" + room.kindLabel() + "]",
					room.bossStage() ? JournalSpread.Tone.NEGATIVE : JournalSpread.Tone.GOLD));
				right.add(JournalSpread.Block.note("   " + room.collectLabel() + " — clear by "
					+ room.gatingLabel(), JournalSpread.Tone.MUTED));
			}
			right.add(JournalSpread.Block.gap());
			right.add(JournalSpread.Block.heading("The Rules"));
			right.add(JournalSpread.Block.note("Loadout: " + briefing.loadoutLabel(), JournalSpread.Tone.INK));
			right.add(JournalSpread.Block.note(briefing.bankAccessLabel(), JournalSpread.Tone.INK));
			right.add(JournalSpread.Block.note(briefing.timeModelLabel(), JournalSpread.Tone.INK));
			right.add(JournalSpread.Block.note("Seed: " + briefing.seedLabel(), JournalSpread.Tone.MUTED));
			right.add(JournalSpread.Block.gap());
			right.add(JournalSpread.Block.note("WIN: " + briefing.winCondition(), JournalSpread.Tone.POSITIVE));
			for (String lose : briefing.loseConditions())
			{
				right.add(JournalSpread.Block.note("LOSE: " + lose, JournalSpread.Tone.NEGATIVE));
			}
		}

		return new JournalSpread("The Contract", "choose, sign, and stamp", left, right);
	}

	/**
	 * The Reward phase as a two-page spread. Left page = the choice (the reward cards — injected
	 * by the adapter, which owns their icons); right page = The Ledger: the hourglass, the score,
	 * and the relics already in pocket, so the pick is made in context.
	 */
	public JournalSpread rewardSpread(String chestTitle, String chestSubtitle)
	{
		List<JournalSpread.Block> right = new ArrayList<>();
		right.add(JournalSpread.Block.heading("The Ledger"));
		right.add(JournalSpread.Block.hourglass("The Hourglass", timerLabel));
		right.add(JournalSpread.Block.text("Score: " + score, JournalSpread.Tone.GOLD));
		right.add(JournalSpread.Block.gap());
		right.add(JournalSpread.Block.heading("Relics in pocket"));
		if (relicRows.isEmpty())
		{
			right.add(JournalSpread.Block.note("The pockets are empty.", JournalSpread.Tone.MUTED));
		}
		else
		{
			for (String row : relicRows)
			{
				right.add(JournalSpread.Block.note(row, JournalSpread.Tone.INK));
			}
		}
		return new JournalSpread(
			chestTitle == null || chestTitle.isEmpty() ? "The chest opens" : chestTitle,
			chestSubtitle == null ? "" : chestSubtitle,
			Collections.emptyList(), right);
	}

	/**
	 * The Run phase as a two-page journal spread. Left page = "what I have / my choices" (the
	 * current objective, what is next, and the rules of this place); right page = "the world /
	 * route / context" (The Record chapter list, The Hourglass, the running score). Derived only
	 * from this view model, so the per-phase split is unit-testable without RuneLite; the window
	 * overlay layer paints it.
	 */
	public JournalSpread runSpread()
	{
		String elapsed = timerLabel;
		Chapter current = null;
		for (Chapter c : chapters)
		{
			if (c.isCurrent())
			{
				current = c;
				break;
			}
		}

		String title;
		String subtitle;
		if (runOver || current == null)
		{
			// Terminal page (complete/failed): the recap reads as the journal's final entry.
			title = "The Final Page";
			subtitle = stateLabel + " — " + elapsed + " afoot";
		}
		else
		{
			title = "Chapter " + current.numeral() + " — " + current.name();
			subtitle = phaseLabel + " — " + elapsed + " afoot";
		}

		// Left page: the entry (objective + rules) for a live run, the recap when it is over.
		List<JournalSpread.Block> left = new ArrayList<>();
		if (runOver || current == null)
		{
			// The recap rows become the entry.
			for (String row : statusRows)
			{
				if (row == null || row.trim().isEmpty())
				{
					continue;
				}
				JournalSpread.Tone tone = row.contains("✓") ? JournalSpread.Tone.POSITIVE
					: row.contains("✗") ? JournalSpread.Tone.NEGATIVE : JournalSpread.Tone.INK;
				left.add(JournalSpread.Block.text(row, tone));
			}
		}
		else
		{
			if (!objective.isEmpty())
			{
				left.add(JournalSpread.Block.text(objective,
					objectiveDone ? JournalSpread.Tone.POSITIVE : JournalSpread.Tone.INK));
			}
			if (!nextStage.isEmpty())
			{
				left.add(JournalSpread.Block.text("Next: " + nextStage, JournalSpread.Tone.MUTED));
			}
			List<JournalSpread.Block> rules = ruleBlocks();
			if (!rules.isEmpty())
			{
				left.add(JournalSpread.Block.gap());
				left.add(JournalSpread.Block.heading("The rules of this place"));
				left.addAll(rules);
			}
		}

		// Right page: THE RECORD, the hourglass, and the running score.
		List<JournalSpread.Block> right = new ArrayList<>();
		if (!chapters.isEmpty())
		{
			right.add(JournalSpread.Block.heading("The Record"));
			right.add(JournalSpread.Block.chapters(chapters));
		}
		right.add(JournalSpread.Block.hourglass("The Hourglass", elapsed));
		right.add(JournalSpread.Block.text("Score: " + score, JournalSpread.Tone.GOLD));

		return new JournalSpread(title, subtitle, left, right);
	}

	/** Extracts the ✓ / ✗ / ! rule rows from the status rows as tone-coded margin notes. */
	private List<JournalSpread.Block> ruleBlocks()
	{
		List<JournalSpread.Block> rules = new ArrayList<>();
		for (String row : statusRows)
		{
			if (row == null)
			{
				continue;
			}
			String trimmed = row.trim();
			if (trimmed.startsWith("Permitted here") || trimmed.startsWith("Forbidden here"))
			{
				rules.add(JournalSpread.Block.note(trimmed, JournalSpread.Tone.INK));
			}
			else if (trimmed.startsWith("✓"))
			{
				rules.add(JournalSpread.Block.note(trimmed, JournalSpread.Tone.POSITIVE));
			}
			else if (trimmed.startsWith("✗") || trimmed.startsWith("!"))
			{
				rules.add(JournalSpread.Block.note(trimmed, JournalSpread.Tone.NEGATIVE));
			}
		}
		return rules;
	}

	public List<String> rowsFor(PanelTab tab)
	{
		switch (tab)
		{
			case RUN: return lobby ? headerRows : combine(headerRows, statusRows);
			case ZONES: return zoneRows;
			case RELICS: return relicRows;
			default: return Collections.emptyList();
		}
	}

	private static List<String> combine(List<String> a, List<String> b)
	{
		List<String> out = new ArrayList<>(a);
		out.addAll(b);
		return out;
	}

	// ---------- Factory methods ----------

	public static SidePanelViewModel lobby(RunMode mode, String goal, String rulesSummary)
	{
		Builder b = new Builder().activeTab(PanelTab.RUN).lobby(true);
		b.goal(goal == null || goal.isEmpty() ? "(none set)" : goal);
		b.stateLabel("LOBBY");
		b.header("═══ ROGUESCAPE ═══");
		b.header("");
		b.header("Mode: " + mode);
		b.header("Run: " + (goal == null || goal.isEmpty() ? "(auto)" : goal));
		b.header("");
		b.rule("Rules:");
		for (String line : rulesSummary.split("\n"))
		{
			b.rule("  " + line);
		}
		b.action(PanelAction.START_RUN);
		return b.build();
	}

	public static SidePanelViewModel active(RogueScapeRunLoop loop, PanelTab tab)
	{
		if (loop == null) throw new IllegalArgumentException("loop required");
		RogueScapeRun run = loop.run();
		RogueScapeRunSession s = run.session();
		Builder b = new Builder().activeTab(tab != null ? tab : PanelTab.RUN).lobby(false);

		// Typed live-run data for the structured sections.
		int floorTotal = s.route() == null ? 0 : s.route().size();
		int cleared = 0;
		if (s.route() != null)
		{
			for (com.pluginideahub.roguescape.core.RunStage stage : s.route().stages())
			{
				if (stage.isCleared()) cleared++;
			}
		}
		b.goal(s.goal() == null || s.goal().isEmpty() ? "(none)" : s.goal())
			.stateLabel(displayState(s.runState()))
			.timerLabel(loop.runElapsedLabel())
			.phaseLabel(loop.phase().getDisplayName())
			.roomName(run.currentRoomName() == null ? "" : run.currentRoomName())
			.regionId(run.currentRegionId() == null ? "" : run.currentRegionId())
			.score(run.effectiveScore())
			.itemsCollected(run.itemsCollected())
			.floorTotal(floorTotal)
			.floorCurrent(floorTotal == 0 ? 0 : Math.min(cleared + 1, floorTotal));

		// Route progress rows (ROUTE section) + room/boss tallies for PROGRESSION.
		int roomsTotal = 0;
		int roomsCleared = 0;
		int bossesTotal = 0;
		int bossesCleared = 0;
		if (s.route() != null)
		{
			for (com.pluginideahub.roguescape.core.RunStage stage : s.route().stages())
			{
				boolean boss = stage.type() == RunStageType.BOSS;
				if (boss)
				{
					bossesTotal++;
					if (stage.isCleared()) bossesCleared++;
				}
				else
				{
					roomsTotal++;
					if (stage.isCleared()) roomsCleared++;
				}
				String marker = stage.isCleared() ? "✓ " : (stage.isEntered() ? "▶ " : "• ");
				String tag = boss ? "[Boss] " : "";
				b.route(marker + tag + stage.name() + " - " + stage.objectiveProgressLabel());
			}
			// Structured chapters (CHAPTERS / RECORD) for the journal painters.
			List<com.pluginideahub.roguescape.core.RunStage> stages = s.route().stages();
			int currentIdx = -1;
			for (int i = 0; i < stages.size(); i++)
			{
				if (!stages.get(i).isCleared())
				{
					currentIdx = i;
					break;
				}
			}
			for (int i = 0; i < stages.size(); i++)
			{
				com.pluginideahub.roguescape.core.RunStage stage = stages.get(i);
				boolean boss = stage.type() == RunStageType.BOSS;
				boolean lastBoss = boss && i == stages.size() - 1;
				b.chapter(new Chapter(
					lastBoss ? "Final" : roman(i + 1),
					stage.name(),
					boss,
					stage.isCleared(),
					i == currentIdx));
			}
		}
		b.bossesDefeated(bossesCleared).bossesTotal(bossesTotal);

		// Typed entry/next for the journal spread (left page): the current objective and what is
		// coming next, taken straight from run state rather than re-parsed from status strings.
		com.pluginideahub.roguescape.core.RunStage enteredStage = run.currentEnteredStage();
		b.objective(enteredStage == null ? "" : enteredStage.objectiveProgressLabel());
		b.objectiveDone(enteredStage != null && enteredStage.objectiveComplete());
		String upcoming = "";
		if (s.route() != null)
		{
			for (com.pluginideahub.roguescape.core.RunStage st : s.route().stages())
			{
				if (!st.isCleared() && (enteredStage == null || !st.name().equals(enteredStage.name())))
				{
					upcoming = st.name();
					break;
				}
			}
		}
		b.nextStage(upcoming);

		// MODIFIERS section: the run's active rules (real state, not invented).
		b.modifier("Bank unlocks: " + (run.bankUnlocked() ? "ON" : "off"));
		b.modifier("Prayer: " + (run.prayerUnlocked() ? "unlocked" : "locked"));
		b.modifier("Potions: " + (run.potionUnlocked() ? "unlocked" : "locked"));
		b.modifier("Trade: " + (run.tradeUnlocked() ? "unlocked" : "locked"));
		if (loop.hasTimeLimit())
		{
			b.modifier("Time limit: " + loop.timeRemainingLabel() + " remaining");
		}
		List<Relic> held = run.heldRelics();
		if (!held.isEmpty())
		{
			b.modifier("Relics in pocket (" + held.size() + "):");
			for (Relic relic : held)
			{
				b.modifier("• " + relic.name());
				if (relic.description() != null && !relic.description().isEmpty())
				{
					// "~ " marks a wrapped description sub-line for the panel renderer.
					b.modifier("~ " + relic.description());
				}
			}
		}

		// PROGRESSION section.
		com.pluginideahub.roguescape.core.RunStage activeStage = run.currentEnteredStage();
		if (activeStage != null && !activeStage.isCleared())
		{
			b.progression("Current objective: " + activeStage.objectiveProgressLabel());
		}
		b.progression("Bosses defeated: " + bossesCleared + " / " + bossesTotal);
		b.progression("Rooms cleared: " + roomsCleared + " / " + roomsTotal);
		b.progression("Score: " + run.effectiveScore());
		b.progression("Items collected: " + s.itemsCollected());
		if (run.unlocks().isEmpty())
		{
			b.progression("Unlocks: none yet");
		}
		else
		{
			b.progression("Unlocks:");
			for (RunUnlock unlock : run.unlocks())
			{
				b.progression("- " + unlock.displayRow());
			}
		}
		b.progression("Violations: " + s.violationCount());
		appendRelicRows(b, run);

		// Header (all tabs)
		b.header("═══ ROGUESCAPE ═══");
		b.header("");
		b.header("RUN: " + (s.goal() == null || s.goal().isEmpty() ? "(auto)" : s.goal()));
		b.header("State: " + displayState(s.runState()));
		b.header("Timer: " + loop.runElapsedLabel());
		b.header("");

		// Finished-run states: show a recap and reset only.
		RunState runState = s.runState();
		b.runOver(runState == RunState.COMPLETE || runState == RunState.FAILED);
		if (runState == RunState.COMPLETE)
		{
			b.status("\u2713 The run is complete.");
			appendRecap(b, run, loop, roomsCleared, roomsTotal, bossesCleared, bossesTotal);
			b.action(PanelAction.EXPORT_RECAP);
			b.action(PanelAction.RESET_RUN);
			return b.build();
		}
		if (runState == RunState.FAILED)
		{
			b.status("\u2717 The run has failed.");
			appendRecap(b, run, loop, roomsCleared, roomsTotal, bossesCleared, bossesTotal);
			b.action(PanelAction.EXPORT_RECAP);
			b.action(PanelAction.RESET_RUN);
			return b.build();
		}

		// Active run: phase-based status and actions
		RunPhase phase = loop.phase();
		b.status("CURRENT: " + phase.getDisplayName());
		// Relic category-limit breaches are flagged prominently in any active phase.
		for (com.pluginideahub.roguescape.core.reward.BankItemCategory cat : run.relicOverLimit())
		{
			b.status("✗ Over the " + humanCategory(cat).toLowerCase() + " limit — a curse bites.");
		}
		if (phase == RunPhase.TRAVEL_TO_STAGE)
		{
			String roomName = run.currentRoomName();
			com.pluginideahub.roguescape.core.RunStage stage = run.currentEnteredStage();
			com.pluginideahub.roguescape.core.region.StageRegionRule rule = run.currentStageRule();
			b.status("Travel to " + (roomName == null ? "parts unknown" : roomName) + ".");
			if (stage != null)
			{
				b.status("The task waits there: " + stage.objectiveProgressLabel());
			}
			if (rule != null && rule.restrictsRegion())
			{
				b.status("Allowed regions: " + String.join(", ", rule.allowedRegionIds()));
			}
			else
			{
				b.status("Allowed region: unrestricted");
			}
			if (loop.hasTimeLimit())
			{
				b.status("Room timer: " + loop.timeRemainingLabel() + " once you enter");
			}
			b.status("");
			b.status(run.currentRegionLegal()
				? "You are in the room. The timer will arm on the next tick."
				: "Walk to the allowed room to start the timer.");
			b.status("Movement is allowed during Travel.");
			b.status("Pickups outside the target room are blocked.");
			if (!run.bankUnlocked() || !run.tradeUnlocked() || !run.prayerUnlocked() || !run.potionUnlocked())
			{
				b.status("Bank, trade, prayer, and potion rules still apply.");
			}
		}
		else if (phase == RunPhase.ROOM_ACTIVE || phase == RunPhase.BOSS_ACTIVE)
		{
			String roomName = run.currentRoomName();
			com.pluginideahub.roguescape.core.RunStage stage = run.currentEnteredStage();
			b.status("Room: " + (roomName == null ? "Unknown" : roomName));
			if (loop.hasTimeLimit())
			{
				b.status("Room timer: " + loop.timeRemainingLabel());
			}
			if (stage != null)
			{
				b.status("The task: " + stage.objectiveProgressLabel());
				if (stage.objectiveComplete())
				{
					b.status("The task is done — the chest can open.");
				}
				else if (stage.objectiveIsTrackable())
				{
					b.status("Finish the task to stamp this chapter.");
				}
				else
				{
					b.status("Manual completion required once the objective is done.");
				}
			}
			// Rules are collapsed to one glanceable line; the renderer folds it into a section.
			appendRoomRules(b, run);
			if (loop.canCompleteCurrentStage())
			{
				b.action(PanelAction.COMPLETE_STAGE);
			}
		}
		else if (phase == RunPhase.BASE_REWARD)
		{
			boolean hasNextStage = hasUnclearedStage(s);
			b.status(hasNextStage
				? "Choose one before the page turns."
				: "Choose one final reward — the journal is nearly full.");
			RewardDraft draft = loop.pendingRewardDraft();
			if (draft != null && draft.options() != null && !loop.baseRewardResolved())
			{
				int index = 1;
				for (RewardOption option : draft.options())
				{
					b.status(index + ") " + option.label() + " [" + option.chestType() + "]");
					if (index == 1) b.action(PanelAction.CHOOSE_REWARD_1);
					else if (index == 2) b.action(PanelAction.CHOOSE_REWARD_2);
					else if (index == 3) b.action(PanelAction.CHOOSE_REWARD_3);
					index++;
				}
				b.action(PanelAction.SKIP_REWARD);
			}
			else
			{
				b.status(hasNextStage
					? "Reward resolved. Move to the next room when ready."
					: "Final reward resolved. Finish the run when ready.");
				b.action(PanelAction.NEXT_STAGE);
			}
		}
		b.status("");
		b.status("Score: " + run.effectiveScore());
		b.status("Items collected: " + run.itemsCollected());
		b.action(PanelAction.FAIL_RUN);
		b.action(PanelAction.RESET_RUN);

		return b.build();
	}

	private static boolean hasUnclearedStage(RogueScapeRunSession session)
	{
		if (session == null || session.route() == null)
		{
			return false;
		}
		for (com.pluginideahub.roguescape.core.RunStage stage : session.route().stages())
		{
			if (!stage.isCleared())
			{
				return true;
			}
		}
		return false;
	}

	private static void appendRelicRows(Builder b, RogueScapeRun run)
	{
		List<Relic> held = run.heldRelics();
		if (held.isEmpty())
		{
			b.relic("No relic artifacts claimed yet.");
			return;
		}
		b.relic("Held artifacts: " + held.size());
		for (Relic relic : held)
		{
			b.relic("- " + relic.name());
			if (relic.description() != null && !relic.description().isEmpty())
			{
				b.relic("~ " + relic.description());
			}
		}
	}

	/** End-of-run recap rows shared by the COMPLETE and FAILED states. */
	private static void appendRecap(Builder b, RogueScapeRun run, RogueScapeRunLoop loop,
		int roomsCleared, int roomsTotal, int bossesCleared, int bossesTotal)
	{
		b.status("");
		b.status("Time afoot: " + loop.runElapsedLabel());
		b.status("Score: " + run.effectiveScore());
		b.status("Chapters: " + roomsCleared + " of " + roomsTotal);
		b.status("Bosses: " + bossesCleared + " of " + bossesTotal);
		b.status("Items collected: " + run.itemsCollected());
		List<Relic> held = run.heldRelics();
		if (!held.isEmpty())
		{
			b.status("");
			b.status("Relics in pocket (" + held.size() + "):");
			for (Relic relic : held)
			{
				b.status("  • " + relic.name());
			}
		}
	}

	/**
	 * Collapsed room rules: one glanceable line of what is blocked here, plus any active relic
	 * restrictions/caps. The window renderer folds these into a "rules of this place" section.
	 */
	private static void appendRoomRules(Builder b, RogueScapeRun run)
	{
		java.util.List<String> blocked = new java.util.ArrayList<>();
		if (!run.bankUnlocked()) blocked.add("bank");
		if (!run.tradeUnlocked()) blocked.add("trade/GE");
		if (!run.prayerUnlocked()) blocked.add("prayer");
		if (!run.potionUnlocked()) blocked.add("potions");
		b.status("✗ Stay in this room" + (blocked.isEmpty() ? "." : " — no " + String.join(", ", blocked) + "."));
		for (com.pluginideahub.roguescape.core.reward.BankItemCategory cat : run.relicRestrictedCategories())
		{
			b.status("✗ " + humanCategory(cat) + " forbidden (relic)");
		}
		for (java.util.Map.Entry<com.pluginideahub.roguescape.core.reward.BankItemCategory, Integer> cap
			: run.relicCategoryLimits().entrySet())
		{
			b.status("! Max " + cap.getValue() + " " + humanCategory(cap.getKey()) + " (relic)");
		}
	}

	/** Turns a category enum (e.g. MELEE_WEAPON) into a readable label ("Melee weapon"). */
	private static String humanCategory(com.pluginideahub.roguescape.core.reward.BankItemCategory cat)
	{
		String s = cat.name().toLowerCase().replace('_', ' ');
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String displayState(RunState state)
	{
		switch (state)
		{
			case ACTIVE: return "RUNNING";
			case COMPLETE: return "COMPLETED";
			case FAILED: return "FAILED";
			default: return state.name();
		}
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private PanelTab activeTab = PanelTab.RUN;
		private boolean lobby = true;
		private final List<String> headerRows = new ArrayList<>();
		private final List<String> ruleRows = new ArrayList<>();
		private final List<String> statusRows = new ArrayList<>();
		private final List<String> zoneRows = new ArrayList<>();
		private final List<String> relicRows = new ArrayList<>();
		private final List<String> routeRows = new ArrayList<>();
		private final List<Chapter> chapters = new ArrayList<>();
		private final List<String> modifierRows = new ArrayList<>();
		private final List<String> progressionRows = new ArrayList<>();
		private final Set<PanelAction> enabledActions = EnumSet.noneOf(PanelAction.class);

		private String goal = "";
		private String stateLabel = "";
		private String timerLabel = "";
		private String phaseLabel = "";
		private String roomName = "";
		private String regionId = "";
		private int score;
		private int itemsCollected;
		private int floorCurrent;
		private int floorTotal;
		private int bossesDefeated;
		private int bossesTotal;
		private String objective = "";
		private boolean objectiveDone;
		private String nextStage = "";
		private boolean runOver;

		public Builder activeTab(PanelTab t) { this.activeTab = t; return this; }
		public Builder lobby(boolean v) { this.lobby = v; return this; }
		public Builder header(String s) { headerRows.add(s); return this; }
		public Builder rule(String s) { ruleRows.add(s); return this; }
		public Builder status(String s) { statusRows.add(s); return this; }
		public Builder zone(String s) { zoneRows.add(s); return this; }
		public Builder relic(String s) { relicRows.add(s); return this; }
		public Builder route(String s) { routeRows.add(s); return this; }
		public Builder chapter(Chapter c) { if (c != null) chapters.add(c); return this; }
		public Builder modifier(String s) { modifierRows.add(s); return this; }
		public Builder progression(String s) { progressionRows.add(s); return this; }
		public Builder action(PanelAction a) { enabledActions.add(a); return this; }
		public Builder goal(String s) { this.goal = s == null ? "" : s; return this; }
		public Builder stateLabel(String s) { this.stateLabel = s == null ? "" : s; return this; }
		public Builder timerLabel(String s) { this.timerLabel = s == null ? "" : s; return this; }
		public Builder phaseLabel(String s) { this.phaseLabel = s == null ? "" : s; return this; }
		public Builder roomName(String s) { this.roomName = s == null ? "" : s; return this; }
		public Builder regionId(String s) { this.regionId = s == null ? "" : s; return this; }
		public Builder score(int v) { this.score = v; return this; }
		public Builder itemsCollected(int v) { this.itemsCollected = v; return this; }
		public Builder floorCurrent(int v) { this.floorCurrent = v; return this; }
		public Builder floorTotal(int v) { this.floorTotal = v; return this; }
		public Builder bossesDefeated(int v) { this.bossesDefeated = v; return this; }
		public Builder bossesTotal(int v) { this.bossesTotal = v; return this; }
		public Builder objective(String s) { this.objective = s == null ? "" : s; return this; }
		public Builder objectiveDone(boolean v) { this.objectiveDone = v; return this; }
		public Builder nextStage(String s) { this.nextStage = s == null ? "" : s; return this; }
		public Builder runOver(boolean v) { this.runOver = v; return this; }
		public SidePanelViewModel build() { return new SidePanelViewModel(this); }
	}
}
