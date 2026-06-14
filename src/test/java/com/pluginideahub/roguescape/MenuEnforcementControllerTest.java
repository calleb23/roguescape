package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MenuEnforcementControllerTest
{
	@Test
	public void inactiveForNullState()
	{
		assertFalse(MenuEnforcementController.isActive(null, null, null));
	}

	@Test
	public void activeDuringAnEnteredRun()
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);
		assertTrue(MenuEnforcementController.isActive(run, loop, s));
	}
}
