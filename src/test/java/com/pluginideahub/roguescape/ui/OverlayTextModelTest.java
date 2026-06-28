package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Characterizes {@link OverlayTextModel} — the active-run objective HUD construction lifted out of
 * {@code RogueScapePlugin.objectiveView()}/{@code targetRegionLabel()} behind the {@link RunContext}
 * seam. Pins the visibility gate, the region-composition join (travel vs. non-travel), the
 * next-stage collapse, the travel objective override, and the target-region label formatting.
 */
public class OverlayTextModelTest
{
	/** A fresh ACTIVE run sitting in the entered ROOM stage R1, with a BOSS stage B1 still ahead. */
	private static RogueScapeRunLoop loopWithRoomThenBoss()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Obj test", "seed", RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter room");
		base.addStage("B1", RunStageType.BOSS, "Obor", "boss room");
		base.enterStage("R1");
		return new RogueScapeRunLoop(RogueScapeRun.wrap(base), 1_000L);
	}

	private static RunContext active(RogueScapeRunLoop loop, String region)
	{
		return RunContext.active(loop.run().session(), loop.run(), loop, region);
	}

	private static Set<String> regions(String... ids)
	{
		return new LinkedHashSet<>(Arrays.asList(ids));
	}

	@Test
	public void noObjectiveViewWithoutAnActiveStage()
	{
		assertNull(OverlayTextModel.objectiveView(RunContext.lobby()));
		assertNull(OverlayTextModel.objectiveView(null));
	}

	@Test
	public void activeRoomStageProducesViewWithCollapsedNext()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		RogueScapeObjectiveOverlay.View view = OverlayTextModel.objectiveView(active(loop, "lumbridge"));

		assertNotNull(view);
		assertEquals("Lumbridge", view.stage);
		// next-uncleared resolves to the entered stage itself, so it collapses to empty
		assertEquals("", view.next);
		assertEquals(RunPhase.ROOM_ACTIVE.getDisplayName(), view.phase);
		// no time limit configured -> score text, not a timer
		assertEquals("Score " + loop.run().effectiveScore(), view.score);
		// 0 of 2 stages cleared
		assertEquals(0.0, view.progress, 0.0001);
		// non-travel phase keeps the stage's own objective label
		assertEquals(loop.run().currentEnteredStage().objectiveProgressLabel(), view.objective);
		assertEquals(loop.run().currentRegionLegal(), view.regionLegal);
	}

	@Test
	public void nonTravelPhaseJoinsRegionAndTargetWithSlash()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));

		RogueScapeObjectiveOverlay.View view = OverlayTextModel.objectiveView(active(loop, "varrock"));

		assertNotNull(view);
		assertEquals("varrock / lumbridge", view.region);
	}

	@Test
	public void travelPhaseOverridesObjectiveAndArrowsTowardTarget()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));
		loop.setTravelGatedStages(true); // moves the active stage into TRAVEL_TO_STAGE

		RogueScapeObjectiveOverlay.View view = OverlayTextModel.objectiveView(active(loop, "varrock"));

		assertNotNull(view);
		assertEquals(RunPhase.TRAVEL_TO_STAGE.getDisplayName(), view.phase);
		assertEquals("Travel to the allowed room region", view.objective);
		assertEquals("varrock -> lumbridge", view.region);
	}

	@Test
	public void unknownRegionWhenContextHasNoRegionId()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		RogueScapeObjectiveOverlay.View view = OverlayTextModel.objectiveView(active(loop, ""));

		assertNotNull(view);
		assertEquals("unknown", view.region);
	}

	@Test
	public void targetRegionLabelIsEmptyWithoutARestrictingRule()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		assertEquals("", OverlayTextModel.targetRegionLabel(active(loop, "")));
		assertEquals("", OverlayTextModel.targetRegionLabel(RunContext.lobby()));
		assertEquals("", OverlayTextModel.targetRegionLabel(null));
	}

	@Test
	public void targetRegionLabelFormatsSingleAndMultiRegion()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		RogueScapeRun run = loop.run();

		run.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));
		assertEquals("lumbridge", OverlayTextModel.targetRegionLabel(active(loop, "")));

		run.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge", "varrock", "draynor"), true));
		assertEquals("lumbridge +2", OverlayTextModel.targetRegionLabel(active(loop, "")));
	}

	@Test
	public void ruleWithNoAllowedRegionsProducesNoTargetLabel()
	{
		RogueScapeRunLoop loop = loopWithRoomThenBoss();
		// a rule with an empty region set does not restrict region -> empty label
		loop.run().setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions(), true));
		assertEquals("", OverlayTextModel.targetRegionLabel(active(loop, "")));
		assertFalse(loop.run().currentStageRule() == null);
	}
}
