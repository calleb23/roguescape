package com.pluginideahub.roguescape.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RogueScape Stage 1 pure run engine. Holds run identity, route, timeline, score, and recap.
 */
public class RogueScapeRunSession
{
	public enum RunEnding { BANK_USED, DEATH, EXTRA_INVENTORY, UNKNOWN_ITEM_SOURCE, MANUAL_FAIL, TIME_LIMIT }

	public enum ItemSource
	{
		STARTER_KIT,
		FOUND_DURING_RUN,
		BOUGHT_DURING_RUN,
		GATHERED_OR_CRAFTED
	}

	private static final class Reward
	{
		final String item;
		final int quantity;
		final ItemSource source;
		final String locationNote;
		final String role;
		final int points;
		Reward(String item, int quantity, ItemSource source, String locationNote, String role, int points)
		{
			this.item = item;
			this.quantity = quantity;
			this.source = source;
			this.locationNote = locationNote;
			this.role = role;
			this.points = points;
		}
	}

	private static final class Relic
	{
		final String name;
		final String effect;
		Relic(String name, String effect) { this.name = name; this.effect = effect; }
	}

	private static final class Violation
	{
		final String note;
		final RunEnding ending;
		Violation(String note, RunEnding ending) { this.note = note; this.ending = ending; }
	}

	private final String goal;
	private final String seed;
	private final String modeLabel;
	private final RunMode mode;
	private final RunPreset preset;

	private final List<String> starterKit = new ArrayList<>();
	private final RunRoute route = new RunRoute();
	private final List<Reward> rewards = new ArrayList<>();
	private final List<Relic> relics = new ArrayList<>();
	private final List<Violation> violations = new ArrayList<>();
	private final List<RunTimelineEvent> timeline = new ArrayList<>();

	private RunState state = RunState.ACTIVE;
	private RunEnding failedEnding;
	private RunCompletionReason completionReason;
	private String completionNote;

	private RogueScapeRunSession(String goal, String seed, String modeLabel, RunMode mode, RunPreset preset)
	{
		this.goal = goal;
		this.seed = seed;
		this.modeLabel = modeLabel;
		this.mode = mode != null ? mode : RunMode.UNSPECIFIED;
		this.preset = preset != null ? preset : RunPreset.UNSPECIFIED;
	}

	public static RogueScapeRunSession start(String goal)
	{
		return new RogueScapeRunSession(goal, null, null, RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED);
	}

	public static RogueScapeRunSession start(String goal, String seed, String mode)
	{
		return new RogueScapeRunSession(goal, seed, mode, RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED);
	}

	public static RogueScapeRunSession start(String goal, String seed, RunMode mode, RunPreset preset)
	{
		String label = mode != null ? mode.name() : null;
		return new RogueScapeRunSession(goal, seed, label, mode, preset);
	}

	// ---------- Run identity ----------

	public String goal() { return goal; }
	public String seed() { return seed; }
	public RunMode mode() { return mode; }
	public RunPreset preset() { return preset; }
	public RunState runState() { return state; }
	public RunCompletionReason completionReason() { return completionReason; }
	public RunEnding failureReason() { return failedEnding; }

	public RunRoute route() { return route; }

	public List<RunTimelineEvent> timeline()
	{
		return Collections.unmodifiableList(timeline);
	}

	// ---------- Stage 1 route API ----------

	public RunStage addStage(String id, RunStageType type, String name, String note)
	{
		return addStage(id, type, name, note, null, -1);
	}

	public RunStage addStage(String id, RunStageType type, String name, String note,
		String objectiveLabel, int requiredItemGains)
	{
		return addStage(id, type, name, note, objectiveLabel, null, requiredItemGains);
	}

	public RunStage addStage(String id, RunStageType type, String name, String note,
		String objectiveLabel, RunObjectiveKind objectiveKind, int requiredItemGains)
	{
		if (state != RunState.ACTIVE) return null;
		if (route.contains(id)) return route.stageById(id);
		RunStage stage = requiredItemGains >= 0
			? new RunStage(id, name, type, note, objectiveLabel, objectiveKind, requiredItemGains)
			: new RunStage(id, name, type, note);
		route.addStage(stage);
		recordTimeline(RunTimelineEvent.Type.STAGE_ADDED, id + ": " + name);
		return stage;
	}

	public void enterStage(String id)
	{
		if (state != RunState.ACTIVE) return;
		RunStage stage = route.stageById(id);
		if (stage == null) return;
		stage.markEntered();
		recordTimeline(RunTimelineEvent.Type.STAGE_ENTERED, id + ": " + stage.name());
	}

	public void clearStage(String id)
	{
		if (state != RunState.ACTIVE) return;
		RunStage stage = route.stageById(id);
		if (stage == null) return;
		stage.markCleared();
		recordTimeline(RunTimelineEvent.Type.STAGE_CLEARED, id + ": " + stage.name());
	}

	public void recordCurrentStageItemGain()
	{
		recordCurrentStageItemGain(null, null);
	}

	public void recordCurrentStageItemGain(
		com.pluginideahub.roguescape.core.reward.BankItemCategory category,
		com.pluginideahub.roguescape.core.item.ProvenanceHint hint)
	{
		if (state != RunState.ACTIVE) return;
		RunStage stage = currentOpenStage();
		if (stage == null) return;
		boolean wasComplete = stage.objectiveComplete();
		stage.recordItemGain(category, hint);
		if (!wasComplete && stage.objectiveComplete())
		{
			recordRunLoopNote("Objective ready: " + stage.name() + " - " + stage.objectiveProgressLabel());
		}
	}

	public boolean recordCurrentStageBossDefeat(String source)
	{
		if (state != RunState.ACTIVE) return false;
		RunStage stage = currentOpenStage();
		if (stage == null || stage.type() != RunStageType.BOSS) return false;
		boolean wasComplete = stage.objectiveComplete();
		stage.recordBossDefeat();
		if (!wasComplete && stage.objectiveComplete())
		{
			String detail = source == null || source.trim().isEmpty() ? "" : " via " + source.trim();
			recordRunLoopNote("Boss objective ready: " + stage.name() + detail);
		}
		return stage.objectiveComplete();
	}

	public boolean recordCurrentStageStatChanged(String skillName)
	{
		if (state != RunState.ACTIVE) return false;
		RunStage stage = currentOpenStage();
		if (stage == null) return false;
		boolean wasComplete = stage.objectiveComplete();
		boolean accepted = stage.recordStatChanged(skillName);
		if (accepted && !wasComplete && stage.objectiveComplete())
		{
			recordRunLoopNote("Room task ready: " + stage.name() + " - " + stage.objectiveProgressLabel());
		}
		return accepted;
	}

	public RunStage currentOpenStage()
	{
		RunStage last = null;
		for (RunStage stage : route.stages())
		{
			if (stage.isEntered() && !stage.isCleared()) last = stage;
		}
		return last;
	}

	// ---------- Legacy helpers (backward compatible) ----------

	public void declareStarterKitItem(String itemName)
	{
		if (state != RunState.ACTIVE) return;
		starterKit.add(itemName);
		recordTimeline(RunTimelineEvent.Type.STARTER_KIT_DECLARED, itemName);
	}

	public void enterRoom(String roomName, String note)
	{
		if (state != RunState.ACTIVE) return;
		int roomIndex = 1;
		for (RunStage s : route.stages())
		{
			if (s.type() == RunStageType.ROOM) roomIndex++;
		}
		String id = "R" + (roomIndex);
		// The id may collide if user mixed addStage("R1", ...) earlier; bump until free.
		while (route.contains(id))
		{
			roomIndex++;
			id = "R" + roomIndex;
		}
		RunStage stage = new RunStage(id, roomName, RunStageType.ROOM, note);
		route.addStage(stage);
		stage.markEntered();
		recordTimeline(RunTimelineEvent.Type.STAGE_ADDED, id + ": " + roomName);
		recordTimeline(RunTimelineEvent.Type.STAGE_ENTERED, id + ": " + roomName);
	}

	/** Records an observed inventory gain. Items are never judged — they simply count toward the run. */
	public void observeItemGain(String itemName, int quantity, ItemSource source, String locationNote, String role, int points)
	{
		if (state != RunState.ACTIVE) return;
		rewards.add(new Reward(itemName, quantity, source, locationNote, role, points));
		recordTimeline(RunTimelineEvent.Type.ITEM_GAINED,
			itemName + (quantity > 1 ? " x" + quantity : "") + " [" + source.name() + "]");
	}

	/** Backward-compatible convenience: records a FOUND_DURING_RUN reward in the current room. */
	public void keepReward(String itemName, String role, int points)
	{
		observeItemGain(itemName, 1, ItemSource.FOUND_DURING_RUN, currentRoomName(), role, points);
	}

	public void addRelic(String name, String effect)
	{
		if (state != RunState.ACTIVE) return;
		relics.add(new Relic(name, effect));
		recordTimeline(RunTimelineEvent.Type.RELIC_ADDED, name);
	}

	public void completeRun(String note)
	{
		completeRun(note, RunCompletionReason.MANUAL_SUCCESS);
	}

	public void completeRun(String note, RunCompletionReason reason)
	{
		if (state != RunState.ACTIVE) return;
		this.completionNote = note;
		this.completionReason = reason != null ? reason : RunCompletionReason.MANUAL_SUCCESS;
		state = RunState.COMPLETE;
		recordTimeline(RunTimelineEvent.Type.RUN_COMPLETED,
			this.completionReason.name() + (note != null && !note.isEmpty() ? ": " + note : ""));
	}

	public void recordViolation(String note, RunEnding ending)
	{
		if (state != RunState.ACTIVE) return;
		violations.add(new Violation(note, ending));
		this.failedEnding = ending;
		state = RunState.FAILED;
		recordTimeline(RunTimelineEvent.Type.VIOLATION, ending.name() + ": " + note);
		recordTimeline(RunTimelineEvent.Type.RUN_FAILED, ending.name());
	}

	public void recordRunLoopNote(String note)
	{
		if (note == null || note.trim().isEmpty()) return;
		recordTimeline(RunTimelineEvent.Type.RUN_LOOP_NOTE, note);
	}

	private void recordTimeline(RunTimelineEvent.Type type, String description)
	{
		timeline.add(new RunTimelineEvent(timeline.size(), type, description));
	}

	// ---------- Legacy accessors ----------

	public String state()
	{
		return state.name();
	}

	public int roomCount()
	{
		int count = 0;
		for (RunStage s : route.stages())
		{
			if (s.type() == RunStageType.ROOM) count++;
		}
		return count;
	}

	public int rewardCount()
	{
		return rewards.size();
	}

	/** Total items collected during the run (no legality judgment — every gain counts). */
	public int itemsCollected()
	{
		return rewards.size();
	}

	public int relicCount()
	{
		return relics.size();
	}

	public int runScore()
	{
		int total = 0;
		for (Reward r : rewards) total += r.points;
		return total;
	}

	public String currentRoomName()
	{
		String name = "";
		for (RunStage s : route.stages())
		{
			if (s.type() == RunStageType.ROOM && s.isEntered()) name = s.name();
		}
		return name;
	}

	public int violationCount()
	{
		return violations.size();
	}

	// ---------- Reporting ----------

	public String overlaySummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("RogueScape | ").append(goal).append(" | ");
		if (modeLabel != null) sb.append("[").append(modeLabel).append("] | ");
		else if (mode != RunMode.UNSPECIFIED) sb.append("[").append(mode.name()).append("] | ");
		if (preset != RunPreset.UNSPECIFIED) sb.append("{").append(preset.name()).append("} | ");
		sb.append("Rooms: ").append(roomCount());
		sb.append(" | Items: ").append(itemsCollected());
		sb.append(" | Relics: ").append(relics.size());
		sb.append(" | Score: ").append(runScore());
		sb.append(" | ").append(state.name());
		if (state == RunState.FAILED && failedEnding != null)
		{
			sb.append(" | Failed: ").append(failedEnding.name());
		}
		else if (state == RunState.COMPLETE && completionReason != null)
		{
			sb.append(" | Complete: ").append(completionReason.name());
		}
		return sb.toString();
	}

	public String recapMarkdown()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("# RogueScape Run: ").append(goal).append("\n\n");
		sb.append("**State:** ").append(state.name()).append("\n\n");
		if (seed != null) sb.append("**Seed:** ").append(seed).append("\n\n");
		if (modeLabel != null) sb.append("**Mode:** ").append(modeLabel).append("\n\n");
		else if (mode != RunMode.UNSPECIFIED) sb.append("**Mode:** ").append(mode.name()).append("\n\n");
		if (preset != RunPreset.UNSPECIFIED) sb.append("**Preset:** ").append(preset.name()).append("\n\n");
		sb.append("**Score:** ").append(runScore()).append("\n\n");

		if (!starterKit.isEmpty())
		{
			sb.append("## Starter Kit\n");
			for (String item : starterKit) sb.append("- ").append(item).append("\n");
			sb.append("\n");
		}

		sb.append("## Route\n");
		for (RunStage s : route.stages())
		{
			sb.append("- **").append(s.id()).append("** [")
				.append(s.type().name()).append("] ")
				.append(s.name()).append(" — ").append(s.note());
			if (s.isCleared()) sb.append(" (cleared)");
			else if (s.isEntered()) sb.append(" (entered)");
			sb.append("\n");
		}
		sb.append("\n");

		sb.append("## Rewards\n");
		for (Reward r : rewards)
		{
			sb.append("- ").append(r.item);
			if (r.quantity > 1) sb.append(" x").append(r.quantity);
			sb.append(" [").append(r.source.name()).append("]");
			sb.append(" — ").append(r.role).append(" (+").append(r.points).append(")");
			if (r.locationNote != null && !r.locationNote.isEmpty())
			{
				sb.append(" @ ").append(r.locationNote);
			}
			sb.append("\n");
		}
		sb.append("\n");

		sb.append("## Relics\n");
		for (Relic r : relics)
		{
			sb.append("- ").append(r.name).append(": ").append(r.effect).append("\n");
		}
		sb.append("\n");

		if (!violations.isEmpty())
		{
			sb.append("## Violations\n");
			for (Violation v : violations)
			{
				sb.append("- [").append(v.ending.name()).append("] ").append(v.note).append("\n");
			}
			sb.append("\n");
		}

		if (state == RunState.FAILED && failedEnding != null)
		{
			sb.append("## Fail Reason\n").append(failedEnding.name()).append("\n\n");
		}

		if (state == RunState.COMPLETE && completionReason != null)
		{
			sb.append("## Completion\n");
			sb.append("**Reason:** ").append(completionReason.name()).append("\n");
			if (completionNote != null) sb.append(completionNote).append("\n");
			sb.append("\n");
		}
		else if (completionNote != null)
		{
			sb.append("## Completion\n").append(completionNote).append("\n");
		}

		return sb.toString();
	}
}
