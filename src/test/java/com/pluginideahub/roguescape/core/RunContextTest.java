package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RunContextTest
{
	private static RogueScapeRunLoop activeLoop()
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		return new RogueScapeRunLoop(run, 0L);
	}

	@Test
	public void lobbyHasNoRun()
	{
		RunContext ctx = RunContext.lobby();
		assertFalse(ctx.hasRun());
		assertNull(ctx.run());
		assertNull(ctx.session());
		assertNull(ctx.loop());
		assertEquals("", ctx.currentRegionId());
	}

	@Test
	public void activeExposesTheLiveTripleAndRegion()
	{
		RogueScapeRunLoop loop = activeLoop();
		RunContext ctx = RunContext.active(loop.run().session(), loop.run(), loop, "12850");
		assertTrue(ctx.hasRun());
		assertSame(loop, ctx.loop());
		assertSame(loop.run(), ctx.run());
		assertSame(loop.run().session(), ctx.session());
		assertEquals("12850", ctx.currentRegionId());
	}

	@Test
	public void aMissingHandleMeansNotHasRun()
	{
		RogueScapeRunLoop loop = activeLoop();
		RunContext ctx = RunContext.active(loop.run().session(), loop.run(), null, "12850");
		assertFalse(ctx.hasRun());
	}

	@Test
	public void nullRegionNormalizesToEmpty()
	{
		RunContext ctx = RunContext.active(null, null, null, null);
		assertEquals("", ctx.currentRegionId());
		assertFalse(ctx.hasRun());
	}
}
