package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChatEventInterpreterTest
{
	private static RogueScapeRun activeRun()
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		return run;
	}

	@Test
	public void deathMessageFailsRunAndSignals()
	{
		RogueScapeRun run = activeRun();
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret(
			"Oh dear, you are dead!", run, run.session(), null);
		assertEquals("death observed", r.signal());
		assertTrue(r.refresh());
		assertEquals(RunState.FAILED, run.session().runState());
	}

	@Test
	public void benignMessageLeavesRunActive()
	{
		RogueScapeRun run = activeRun();
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret("Hello there", run, run.session(), null);
		assertEquals(RunState.ACTIVE, run.session().runState());
		assertFalse(r.refresh());
		assertNull(r.signal());
	}

	@Test
	public void nullArgumentsAreSafe()
	{
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret("hi", null, null, null);
		assertFalse(r.refresh());
		assertNull(r.signal());
	}
}
