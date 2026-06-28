package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.region.StageRegionRule;

/**
 * Pure-logic builder for the active-run objective HUD. Reads the run-state triple through a
 * {@link RunContext} snapshot and produces the {@link RogueScapeObjectiveOverlay.View} the overlay
 * renders, so the construction can be unit-tested without instantiating the {@code @Inject}-heavy
 * plugin. Lives in {@code ui} (not {@code core}) because it returns the sibling overlay's View type;
 * it holds no RuneLite/Swing types and no mutable state.
 */
public final class OverlayTextModel
{
	private OverlayTextModel() {}

	/**
	 * The objective-overlay view for the live run, or {@code null} when there is no active stage to
	 * show (lobby, non-ACTIVE run, or no entered stage) — the overlay treats {@code null} as "hide".
	 */
	public static RogueScapeObjectiveOverlay.View objectiveView(RunContext ctx)
	{
		if (ctx == null || !ctx.hasRun() || ctx.session().runState() != RunState.ACTIVE)
		{
			return null;
		}
		RogueScapeRun run = ctx.run();
		RogueScapeRunSession session = ctx.session();
		RogueScapeRunLoop loop = ctx.loop();
		RunStage stage = run.currentEnteredStage();
		if (stage == null)
		{
			return null;
		}
		int total = countStages(session, false);
		int cleared = countStages(session, true);
		double progress = total > 0 ? (double) cleared / total : 0.0;
		String currentRegionId = ctx.currentRegionId();
		String region = currentRegionId.isEmpty() ? "unknown" : currentRegionId;
		String target = targetRegionLabel(ctx);
		if (!target.isEmpty())
		{
			region = loop.phase() == RunPhase.TRAVEL_TO_STAGE
				? region + " -> " + target
				: region + " / " + target;
		}
		String next = nextUnclearedStageName(session);
		if (next.equals(stage.name()))
		{
			next = "";
		}
		String objective = stage.objectiveProgressLabel();
		String score = "Score " + run.effectiveScore();
		if (loop.phase() == RunPhase.TRAVEL_TO_STAGE)
		{
			objective = "Travel to the allowed room region";
			score = loop.hasTimeLimit() ? "Timer " + loop.timeRemainingLabel() : score;
		}
		else if (loop.hasTimeLimit())
		{
			score = "Timer " + loop.timeRemainingLabel();
		}
		return new RogueScapeObjectiveOverlay.View(
			stage.name(),
			objective,
			next,
			region,
			loop.phase().getDisplayName(),
			score,
			progress,
			stage.objectiveComplete(),
			run.currentRegionLegal());
	}

	/**
	 * Short label for the current stage's allowed region(s): {@code ""} when the stage does not
	 * restrict region, the single region id when there is one, or {@code "first +N"} for several.
	 */
	public static String targetRegionLabel(RunContext ctx)
	{
		RogueScapeRun run = ctx == null ? null : ctx.run();
		if (run == null || run.currentStageRule() == null)
		{
			return "";
		}
		StageRegionRule rule = run.currentStageRule();
		if (!rule.restrictsRegion() || rule.allowedRegionIds().isEmpty())
		{
			return "";
		}
		int count = rule.allowedRegionIds().size();
		String first = rule.allowedRegionIds().iterator().next();
		return count == 1 ? first : first + " +" + (count - 1);
	}

	private static int countStages(RogueScapeRunSession session, boolean clearedOnly)
	{
		if (session == null)
		{
			return 0;
		}
		int n = 0;
		for (RunStage stage : session.route().stages())
		{
			if (!clearedOnly || stage.isCleared())
			{
				n++;
			}
		}
		return n;
	}

	private static String nextUnclearedStageName(RogueScapeRunSession session)
	{
		if (session == null)
		{
			return "";
		}
		for (RunStage stage : session.route().stages())
		{
			if (!stage.isCleared())
			{
				return stage.name();
			}
		}
		return "";
	}
}
