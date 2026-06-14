package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes the enforcement gate the plugin's {@code enforcementActive()} delegates to, and the
 * region-legality composition behind a blocked click — both now driven through {@link RunContext}.
 */
public class MenuEnforcementControllerTest
{
	private static RogueScapeRunLoop loopWithTwoStages()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Enf test", "seed", RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter room");
		base.addStage("B1", RunStageType.BOSS, "Obor", "boss room");
		base.enterStage("R1");
		return new RogueScapeRunLoop(RogueScapeRun.wrap(base), 1_000L);
	}

	private static RunContext ctx(RogueScapeRunLoop loop)
	{
		return RunContext.active(loop.run().session(), loop.run(), loop, "");
	}

	private static Set<String> regions(String... ids)
	{
		return new LinkedHashSet<>(Arrays.asList(ids));
	}

	@Test
	public void lobbyIsNotEnforcement()
	{
		assertFalse(MenuEnforcementController.isActive(RunContext.lobby()));
	}

	@Test
	public void activeRoomPhaseIsEnforcement()
	{
		assertTrue(MenuEnforcementController.isActive(ctx(loopWithTwoStages())));
	}

	@Test
	public void baseRewardPhaseIsNotEnforcement()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.run().session().recordCurrentStageLegalItemGain();
		loop.completeCurrentStage(61_000L);
		assertFalse(MenuEnforcementController.isActive(ctx(loop)));
	}

	@Test
	public void failedRunIsNotEnforcement()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		loop.run().session().recordViolation("Manual fail", RogueScapeRunSession.RunEnding.MANUAL_FAIL);
		loop.markNow(2_000L);
		assertFalse(MenuEnforcementController.isActive(ctx(loop)));
	}

	@Test
	public void blocksGainActionsOutsideTheLegalRegionButAllowsInside()
	{
		RogueScapeRunLoop loop = loopWithTwoStages();
		RogueScapeRun run = loop.run();
		run.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));
		RunContext ctx = RunContext.active(run.session(), run, loop, "");

		run.moveToRegion("karamja");
		assertTrue(MenuEnforcementController.shouldBlockClick("Take", "Bones", ctx));

		run.moveToRegion("lumbridge");
		assertFalse(MenuEnforcementController.shouldBlockClick("Take", "Bones", ctx));
	}
}
