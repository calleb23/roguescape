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

	private static RogueScapeRun bossActiveRun()
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		s.addStage("B1", RunStageType.BOSS, "Obor", "boss room");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		s.clearStage("R1");
		s.enterStage("B1");
		return run;
	}

	private static RunContext ctx(RogueScapeRun run)
	{
		return RunContext.active(run.session(), run, null, "");
	}

	@Test
	public void deathMessageFailsRunAndSignals()
	{
		RogueScapeRun run = activeRun();
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret(
			"Oh dear, you are dead!", ctx(run), null);
		assertEquals("death observed", r.signal());
		assertTrue(r.refresh());
		assertEquals(RunState.FAILED, run.session().runState());
	}

	@Test
	public void benignMessageLeavesRunActive()
	{
		RogueScapeRun run = activeRun();
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret("Hello there", ctx(run), null);
		assertEquals(RunState.ACTIVE, run.session().runState());
		assertFalse(r.refresh());
		assertNull(r.signal());
	}

	@Test
	public void bossKillChatOnBossStageRecordsDefeatSignal()
	{
		RogueScapeRun run = bossActiveRun();
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret(
			"Obor defeated! Your kill count is: 5", ctx(run), null);
		assertTrue(r.refresh());
		assertEquals("boss defeated: Obor", r.signal());
		assertTrue(run.currentEnteredStage().objectiveComplete());
	}

	@Test
	public void nullContextIsSafe()
	{
		ChatEventInterpreter.Result r = ChatEventInterpreter.interpret("hi", null, null);
		assertFalse(r.refresh());
		assertNull(r.signal());
	}
}
