package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.legality.StrictnessMode;
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
	private final int legalCount;
	private final int illegalCount;
	private final int floorCurrent;
	private final int floorTotal;
	private final int bossesDefeated;
	private final int bossesTotal;

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
		this.legalCount = b.legalCount;
		this.illegalCount = b.illegalCount;
		this.floorCurrent = b.floorCurrent;
		this.floorTotal = b.floorTotal;
		this.bossesDefeated = b.bossesDefeated;
		this.bossesTotal = b.bossesTotal;
	}

	public PanelTab activeTab() { return activeTab; }
	public boolean isLobby() { return lobby; }
	public List<String> headerRows() { return headerRows; }
	public List<String> ruleRows() { return ruleRows; }
	public List<String> statusRows() { return statusRows; }
	public List<String> zoneRows() { return zoneRows; }
	public List<String> relicRows() { return relicRows; }
	public List<String> routeRows() { return routeRows; }
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
	public int legalCount() { return legalCount; }
	public int illegalCount() { return illegalCount; }
	public int floorCurrent() { return floorCurrent; }
	public int floorTotal() { return floorTotal; }
	public int bossesDefeated() { return bossesDefeated; }
	public int bossesTotal() { return bossesTotal; }

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
			.legalCount(run.legalCount())
			.illegalCount(run.illegalCount())
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
		}
		b.bossesDefeated(bossesCleared).bossesTotal(bossesTotal);

		// MODIFIERS section: the run's active rules (real state, not invented).
		b.modifier("Strictness: " + run.strictness());
		b.modifier("Bank unlocks: " + (run.bankUnlocked() ? "ON" : "off"));
		b.modifier("Prayer: " + (run.prayerUnlocked() ? "unlocked" : "locked"));
		b.modifier("Potions: " + (run.potionUnlocked() ? "unlocked" : "locked"));
		b.modifier("Trade: " + (run.tradeUnlocked() ? "unlocked" : "locked"));
		b.modifier("Pre-run supplies: " + (run.preRunSupplyExpected() ? "flagged" : "allowed"));
		if (loop.hasTimeLimit())
		{
			b.modifier("Time limit: " + loop.timeRemainingLabel() + " remaining");
		}
		List<Relic> held = run.heldRelics();
		if (!held.isEmpty())
		{
			b.modifier("Relics (" + held.size() + "):");
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
		b.progression("Rewards kept: " + s.legalRewardCount()
			+ (s.illegalRewardCount() > 0 ? " (illegal " + s.illegalRewardCount() + ")" : ""));
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
		if (runState == RunState.COMPLETE)
		{
			b.status("\u2713 RUN COMPLETE!");
			appendRecap(b, run, loop, roomsCleared, roomsTotal, bossesCleared, bossesTotal);
			b.action(PanelAction.RESET_RUN);
			return b.build();
		}
		if (runState == RunState.FAILED)
		{
			b.status("\u2717 RUN FAILED");
			appendRecap(b, run, loop, roomsCleared, roomsTotal, bossesCleared, bossesTotal);
			b.action(PanelAction.RESET_RUN);
			return b.build();
		}

		// Active run: phase-based status and actions
		RunPhase phase = loop.phase();
		b.status("CURRENT: " + phase.getDisplayName());
		// Relic category-limit breaches are flagged prominently in any active phase.
		for (com.pluginideahub.roguescape.core.reward.BankItemCategory cat : run.relicOverLimit())
		{
			b.status("✗ Over " + humanCategory(cat) + " limit! (relic)");
		}
		if (phase == RunPhase.TRAVEL_TO_STAGE)
		{
			String roomName = run.currentRoomName();
			com.pluginideahub.roguescape.core.RunStage stage = run.currentEnteredStage();
			com.pluginideahub.roguescape.core.region.StageRegionRule rule = run.currentStageRule();
			b.status("Travel to: " + (roomName == null ? "Unknown room" : roomName));
			if (stage != null)
			{
				b.status("Objective waits there: " + stage.objectiveProgressLabel());
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
			b.status("Region: " + (run.currentRegionId() == null || run.currentRegionId().isEmpty() ? "-" : run.currentRegionId()));
			if (loop.hasTimeLimit())
			{
				b.status("Room timer: " + loop.timeRemainingLabel());
			}
			if (stage != null)
			{
				b.status("Objective: " + stage.objectiveProgressLabel());
				if (stage.objectiveComplete())
				{
					b.status("Objective complete - claim your reward when ready.");
				}
				else if (stage.objectiveIsTrackable())
				{
					b.status("Complete Stage unlocks when this objective is done.");
				}
				else
				{
					b.status("Manual completion required once the objective is done.");
				}
			}
			b.status("");
			b.status("You CAN:");
			b.status("  \u2713 Fight monsters in this room");
			b.status("  \u2713 Pick up drops");
			b.status("  \u2713 Use resources found here");
			b.status("");
			b.status("You CANNOT:");
			if (!run.bankUnlocked())
			{
				b.status("  \u2717 Bank / deposit box");
			}
			if (!run.prayerUnlocked())
			{
				b.status("  \u2717 Prayers");
			}
			if (!run.potionUnlocked())
			{
				b.status("  \u2717 Potions");
			}
			if (run.strictness() == StrictnessMode.STRICT)
			{
				b.status("  \u2717 Leave the room region");
				b.status(run.tradeUnlocked() ? "  \u2717 GE" : "  \u2717 Trade / GE");
			}
			else
			{
				b.status(run.tradeUnlocked() ? "  \u2717 GE (moderate)" : "  \u2717 Trade / GE (moderate)");
			}
			b.status("  \u2717 Pick up ground items outside room");
			b.status("  \u2717 Walk while outside the active room");
			// Relic-imposed restrictions and caps surface here so the live rules reflect them.
			for (com.pluginideahub.roguescape.core.reward.BankItemCategory cat : run.relicRestrictedCategories())
			{
				b.status("  \u2717 " + humanCategory(cat) + " (relic)");
			}
			for (java.util.Map.Entry<com.pluginideahub.roguescape.core.reward.BankItemCategory, Integer> cap
				: run.relicCategoryLimits().entrySet())
			{
				b.status("  ! Max " + cap.getValue() + " " + humanCategory(cap.getKey()) + " (relic)");
			}
			if (loop.canCompleteCurrentStage())
			{
				b.action(PanelAction.COMPLETE_STAGE);
			}
		}
		else if (phase == RunPhase.BASE_REWARD)
		{
			boolean hasNextStage = hasUnclearedStage(s);
			b.status(hasNextStage
				? "Choose one reward before the next room."
				: "Choose one final reward before the run completes.");
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
		b.status("Legal/Illegal: " + run.legalCount() + "/" + run.illegalCount());
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
		b.status("Time: " + loop.runElapsedLabel());
		b.status("Score: " + run.effectiveScore());
		b.status("Rooms: " + roomsCleared + " / " + roomsTotal);
		b.status("Bosses: " + bossesCleared + " / " + bossesTotal);
		b.status("Items legal/illegal: " + run.legalCount() + " / " + run.illegalCount());
		List<Relic> held = run.heldRelics();
		if (!held.isEmpty())
		{
			b.status("");
			b.status("Relics (" + held.size() + "):");
			for (Relic relic : held)
			{
				b.status("  • " + relic.name());
			}
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
		private int legalCount;
		private int illegalCount;
		private int floorCurrent;
		private int floorTotal;
		private int bossesDefeated;
		private int bossesTotal;

		public Builder activeTab(PanelTab t) { this.activeTab = t; return this; }
		public Builder lobby(boolean v) { this.lobby = v; return this; }
		public Builder header(String s) { headerRows.add(s); return this; }
		public Builder rule(String s) { ruleRows.add(s); return this; }
		public Builder status(String s) { statusRows.add(s); return this; }
		public Builder zone(String s) { zoneRows.add(s); return this; }
		public Builder relic(String s) { relicRows.add(s); return this; }
		public Builder route(String s) { routeRows.add(s); return this; }
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
		public Builder legalCount(int v) { this.legalCount = v; return this; }
		public Builder illegalCount(int v) { this.illegalCount = v; return this; }
		public Builder floorCurrent(int v) { this.floorCurrent = v; return this; }
		public Builder floorTotal(int v) { this.floorTotal = v; return this; }
		public Builder bossesDefeated(int v) { this.bossesDefeated = v; return this; }
		public Builder bossesTotal(int v) { this.bossesTotal = v; return this; }
		public SidePanelViewModel build() { return new SidePanelViewModel(this); }
	}
}
