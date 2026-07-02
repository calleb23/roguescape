package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.reward.DeterministicRng;
import com.pluginideahub.roguescape.core.reward.RelicDraftGenerator;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import com.pluginideahub.roguescape.core.reward.SupplyDraftGenerator;
import com.pluginideahub.roguescape.core.reward.UnlockDraftGenerator;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.unlock.RunUnlockGenerator;

/**
 * Manual playable RogueScape loop: active stage -> base reward -> next stage.
 *
 * This is pure Java by design. RuneLite/UI code injects time and calls these
 * methods from explicit player controls; the core never reads system time.
 */
public final class RogueScapeRunLoop
{
	private final RogueScapeRun run;
	private final long startedAtMillis;
	private long nowMillis;
	private long phaseStartedAtMillis;
	private RunPhase phase;
	private RewardDraft pendingRewardDraft;
	private String completedStageId;
	private boolean baseRewardResolved;
	private long timeLimitMillis;
	private boolean baseRewardsEnabled = true;
	private boolean travelGatedStages;
	private boolean resolvingTimeLimit;

	public RogueScapeRunLoop(RogueScapeRun run, long startedAtMillis)
	{
		if (run == null) throw new IllegalArgumentException("run required");
		this.run = run;
		this.startedAtMillis = startedAtMillis;
		this.nowMillis = startedAtMillis;
		this.phaseStartedAtMillis = startedAtMillis;
		this.phase = derivePhase();
	}

	public RogueScapeRun run() { return run; }
	public RunPhase phase() { return phase; }
	public RewardDraft pendingRewardDraft() { return pendingRewardDraft; }
	public String completedStageId() { return completedStageId; }
	public long startedAtMillis() { return startedAtMillis; }
	public long nowMillis() { return nowMillis; }
	public long runElapsedMillis() { return RunTimer.elapsed(startedAtMillis, nowMillis); }
	public long phaseElapsedMillis() { return RunTimer.elapsed(phaseStartedAtMillis, nowMillis); }
	public String runElapsedLabel() { return RunTimer.format(runElapsedMillis()); }
	public String phaseElapsedLabel() { return RunTimer.format(phaseElapsedMillis()); }
	public boolean baseRewardResolved() { return baseRewardResolved; }
	public boolean baseRewardsEnabled() { return baseRewardsEnabled; }
	public boolean travelGatedStages() { return travelGatedStages; }
	public long timeLimitMillis() { return timeLimitMillis; }
	public boolean hasTimeLimit() { return timeLimitMillis > 0; }
	public long timeRemainingMillis()
	{
		if (!hasTimeLimit()) return 0L;
		if (travelGatedStages && !isTimedStageActive()) return timeLimitMillis;
		long elapsed = travelGatedStages && isTimedStageActive() ? phaseElapsedMillis() : runElapsedMillis();
		return Math.max(0L, timeLimitMillis - elapsed);
	}
	public String timeRemainingLabel()
	{
		return hasTimeLimit() ? RunTimer.format(timeRemainingMillis()) : "";
	}

	public RogueScapeRunLoop setTimeLimitMillis(long timeLimitMillis)
	{
		this.timeLimitMillis = Math.max(0L, timeLimitMillis);
		markNow(nowMillis);
		return this;
	}

	public RogueScapeRunLoop setBaseRewardsEnabled(boolean baseRewardsEnabled)
	{
		this.baseRewardsEnabled = baseRewardsEnabled;
		return this;
	}

	public RogueScapeRunLoop setTravelGatedStages(boolean travelGatedStages)
	{
		this.travelGatedStages = travelGatedStages;
		RunStage stage = currentActiveStage();
		if (stage != null && run.session().runState() == RunState.ACTIVE)
		{
			transition(travelGatedStages ? RunPhase.TRAVEL_TO_STAGE : phaseForStage(stage), nowMillis);
		}
		run.setRegionRestrictionArmed(!travelGatedStages || isTimedStageActive());
		return this;
	}

	public boolean canCompleteCurrentStage()
	{
		RunStage stage = currentActiveStage();
		if (stage == null) return false;
		if (phase != RunPhase.ROOM_ACTIVE && phase != RunPhase.BOSS_ACTIVE) return false;
		return !stage.objectiveIsTrackable() || stage.objectiveComplete();
	}

	public String completionBlockReason()
	{
		RunStage stage = currentActiveStage();
		if (stage == null) return "No active stage.";
		if (phase != RunPhase.ROOM_ACTIVE && phase != RunPhase.BOSS_ACTIVE)
		{
			return "Stage completion is not available during " + phase.getDisplayName() + ".";
		}
		if (stage.objectiveIsTrackable() && !stage.objectiveComplete())
		{
			return "Objective incomplete: " + stage.objectiveProgressLabel();
		}
		return "";
	}

	public void markNow(long nowMillis)
	{
		this.nowMillis = nowMillis;
		if (timeLimitMillis > 0
			&& run.session().runState() == RunState.ACTIVE
			&& !resolvingTimeLimit
			&& timeLimitExpired())
		{
			if (travelGatedStages && isTimedStageActive())
			{
				resolvingTimeLimit = true;
				try
				{
					completeCurrentStage(nowMillis, true, "Timer expired");
				}
				finally
				{
					resolvingTimeLimit = false;
				}
			}
			else
			{
				run.session().recordViolation("Time limit exceeded: " + RunTimer.format(timeLimitMillis),
					RogueScapeRunSession.RunEnding.TIME_LIMIT);
			}
		}
		if (run.session().runState() == RunState.FAILED && phase != RunPhase.RUN_FAILED)
		{
			transition(RunPhase.RUN_FAILED, nowMillis);
		}
		else if (run.session().runState() == RunState.COMPLETE && phase != RunPhase.RUN_COMPLETE)
		{
			transition(RunPhase.RUN_COMPLETE, nowMillis);
		}
	}

	public void completeCurrentStage(long nowMillis)
	{
		completeCurrentStage(nowMillis, false, "Completed");
	}

	public void forceCompleteCurrentStage(long nowMillis)
	{
		completeCurrentStage(nowMillis, true, "Force completed");
	}

	private void completeCurrentStage(long nowMillis, boolean force, String completionVerb)
	{
		markNow(nowMillis);
		if (phase != RunPhase.ROOM_ACTIVE && phase != RunPhase.BOSS_ACTIVE) return;
		RunStage stage = currentActiveStage();
		if (stage == null) return;
		if (!force && !canCompleteCurrentStage())
		{
			run.session().recordRunLoopNote("Completion blocked: " + completionBlockReason());
			return;
		}

		run.session().clearStage(stage.id());
		run.grantUnlock(RunUnlockGenerator.forClearedStage(stage, run.regionPolicy().ruleFor(stage.id())));
		completedStageId = stage.id();
		run.session().recordRunLoopNote((completionVerb == null || completionVerb.isEmpty()
			? (force ? "Force completed" : "Completed") : completionVerb)
			+ " " + stage.id() + " at " + RunTimer.format(runElapsedMillis()));

		if (nextUnclearedStage() == null)
		{
			if (baseRewardsEnabled)
			{
				baseRewardResolved = false;
				offerBaseRewardIfNeeded(stage);
				transition(RunPhase.BASE_REWARD, nowMillis);
				return;
			}
			run.session().completeRun("Route complete", RunCompletionReason.GOAL_COMPLETE);
			transition(RunPhase.RUN_COMPLETE, nowMillis);
			return;
		}

		if (baseRewardsEnabled)
		{
			baseRewardResolved = false;
			offerBaseRewardIfNeeded(stage);
			transition(RunPhase.BASE_REWARD, nowMillis);
			return;
		}

		RunStage next = nextUnclearedStage();
		if (next != null)
		{
			run.session().enterStage(next.id());
			run.session().recordRunLoopNote("Started " + next.id() + " at " + RunTimer.format(runElapsedMillis()));
			baseRewardResolved = true;
			transition(travelGatedStages ? RunPhase.TRAVEL_TO_STAGE : phaseForStage(next), nowMillis);
		}
	}

	public RewardDraft offerBaseRewardIfNeeded(RunStage completedStage)
	{
		if (pendingRewardDraft != null && !pendingRewardDraft.isSelected() && !pendingRewardDraft.isRejected())
		{
			return pendingRewardDraft;
		}
		String stageId = completedStage != null ? completedStage.id() : completedStageId;
		int index = run.drafts().size();
		String draftId = "BASE-" + (stageId == null ? "STAGE" : stageId) + "-" + (index + 1);
		long seed = draftSeed(run.session().seed(), stageId, index);
		RoomKind kind = completedStage == null ? RoomKind.REGION : run.regionPolicy().ruleFor(completedStage.id()).roomKind();
		if (kind == RoomKind.SUPPLY)
		{
			pendingRewardDraft = SupplyDraftGenerator.supplyDraft(draftId, stageId, seed);
		}
		else if (kind == RoomKind.REGION || kind == RoomKind.BOSS)
		{
			pendingRewardDraft = RelicDraftGenerator.relicDraft(draftId, stageId, seed, 3);
		}
		else
		{
			pendingRewardDraft = UnlockDraftGenerator.unlockDraft(draftId, completedStage, seed);
		}
		run.addRewardDraft(pendingRewardDraft);
		return pendingRewardDraft;
	}

	public RewardOption chooseReward(String optionId, long nowMillis)
	{
		markNow(nowMillis);
		if (phase != RunPhase.BASE_REWARD) throw new IllegalStateException("reward choice only allowed at base");
		RewardDraft draft = requirePendingDraft();
		RewardOption selected = draft.select(optionId);
		baseRewardResolved = true;
		if (selected.isRelic())
		{
			run.chooseRelic(selected.relic());
		}
		if (selected.isUnlock())
		{
			run.grantUnlock(selected.unlock());
		}
		if (selected.isBankUnlock())
		{
			run.bankPool().markUnlocked(selected.bankItem());
		}
		run.session().recordRunLoopNote("Selected base reward: " + selected.label());
		return selected;
	}

	public void skipReward(long nowMillis)
	{
		markNow(nowMillis);
		if (phase != RunPhase.BASE_REWARD) throw new IllegalStateException("reward skip only allowed at base");
		RewardDraft draft = requirePendingDraft();
		draft.reject();
		baseRewardResolved = true;
		run.session().recordRunLoopNote("Skipped base reward");
	}

	public void startNextStage(long nowMillis)
	{
		markNow(nowMillis);
		if (phase != RunPhase.BASE_REWARD) return;
		if (!baseRewardResolved && pendingRewardDraft != null && !pendingRewardDraft.isSelected() && !pendingRewardDraft.isRejected()) return;

		RunStage next = nextUnclearedStage();
		if (next == null)
		{
			run.session().completeRun("Route complete", RunCompletionReason.GOAL_COMPLETE);
			transition(RunPhase.RUN_COMPLETE, nowMillis);
			return;
		}

		run.session().enterStage(next.id());
		run.session().recordRunLoopNote("Started " + next.id() + " at " + RunTimer.format(runElapsedMillis()));
		baseRewardResolved = false;
		transition(travelGatedStages ? RunPhase.TRAVEL_TO_STAGE : phaseForStage(next), nowMillis);
	}

	public boolean notifyRegionChanged(long nowMillis)
	{
		markNow(nowMillis);
		if (!travelGatedStages || phase != RunPhase.TRAVEL_TO_STAGE || run.session().runState() != RunState.ACTIVE)
		{
			return false;
		}
		RunStage stage = currentActiveStage();
		if (stage == null || !run.currentRegionLegal())
		{
			return false;
		}
		transition(phaseForStage(stage), nowMillis);
		run.setRegionRestrictionArmed(true);
		run.session().recordRunLoopNote("Entered " + stage.name() + "; room timer started at "
			+ RunTimer.format(runElapsedMillis()));
		return true;
	}

	/** Deterministic per-stage draft seed so seeded races present identical relic choices. */
	private static long draftSeed(String runSeed, String stageId, int index)
	{
		long base = DeterministicRng.hash(runSeed);
		long stage = DeterministicRng.hash(stageId);
		return (base * 1000003L) ^ (stage * 31L) ^ (index * 2654435761L);
	}

	private RewardDraft requirePendingDraft()
	{
		if (pendingRewardDraft == null)
		{
			throw new IllegalStateException("no pending reward draft");
		}
		return pendingRewardDraft;
	}

	private RunPhase derivePhase()
	{
		RunState state = run.session().runState();
		if (state == RunState.FAILED) return RunPhase.RUN_FAILED;
		if (state == RunState.COMPLETE) return RunPhase.RUN_COMPLETE;
		RunStage stage = currentActiveStage();
		return stage == null ? RunPhase.ROOM_ACTIVE : phaseForStage(stage);
	}

	private RunStage currentActiveStage()
	{
		for (RunStage stage : run.session().route().stages())
		{
			if (stage.isEntered() && !stage.isCleared()) return stage;
		}
		return null;
	}

	private RunStage nextUnclearedStage()
	{
		for (RunStage stage : run.session().route().stages())
		{
			if (!stage.isCleared()) return stage;
		}
		return null;
	}

	private static RunPhase phaseForStage(RunStage stage)
	{
		return stage.type() == RunStageType.BOSS ? RunPhase.BOSS_ACTIVE : RunPhase.ROOM_ACTIVE;
	}

	private boolean isTimedStageActive()
	{
		return phase == RunPhase.ROOM_ACTIVE || phase == RunPhase.BOSS_ACTIVE;
	}

	private boolean timeLimitExpired()
	{
		if (!hasTimeLimit()) return false;
		if (travelGatedStages)
		{
			return isTimedStageActive() && phaseElapsedMillis() >= timeLimitMillis;
		}
		return runElapsedMillis() >= timeLimitMillis;
	}

	private void transition(RunPhase nextPhase, long nowMillis)
	{
		if (this.phase != nextPhase)
		{
			this.phase = nextPhase;
			this.phaseStartedAtMillis = nowMillis;
		}
		if (travelGatedStages)
		{
			run.setRegionRestrictionArmed(nextPhase == RunPhase.ROOM_ACTIVE || nextPhase == RunPhase.BOSS_ACTIVE);
		}
		this.nowMillis = nowMillis;
	}
}
