package com.pluginideahub.roguescape.core;

import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunSeedCodecTest
{
	@Test
	public void parseFieldsSplitsSemicolonKeyValues()
	{
		Map<String, String> f = RunSeedCodec.parseFields("mode=Custom; Time=10m; boss=2");
		assertEquals("Custom", f.get("mode"));
		assertEquals("10m", f.get("time")); // keys are lowercased
		assertEquals("2", f.get("boss"));
		assertTrue(RunSeedCodec.parseFields("").isEmpty());
		assertTrue(RunSeedCodec.parseFields(null).isEmpty());
	}

	@Test
	public void parseTimeMinutesHandlesSuffixAndSentinels()
	{
		assertEquals(10, RunSeedCodec.parseTimeMinutes("10m"));
		assertEquals(10, RunSeedCodec.parseTimeMinutes(" 10 "));
		assertEquals(0, RunSeedCodec.parseTimeMinutes("none"));
		assertEquals(0, RunSeedCodec.parseTimeMinutes(null));
		assertEquals(0, RunSeedCodec.parseTimeMinutes("abc"));
	}

	@Test
	public void parseBossLimitClampsToThree()
	{
		assertEquals(2, RunSeedCodec.parseBossLimit("2"));
		assertEquals(3, RunSeedCodec.parseBossLimit("9"));
		assertEquals(0, RunSeedCodec.parseBossLimit("off"));
		assertEquals(0, RunSeedCodec.parseBossLimit(null));
	}
}
