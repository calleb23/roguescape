package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BossKillChatMatcherTest
{
	@Test
	public void matchesCommonBossKillMessages()
	{
		assertTrue(BossKillChatMatcher.matches("Giant Mole",
			"<col=ff0000>Giant Mole kill count: 7</col>"));
		assertTrue(BossKillChatMatcher.matches("King Black Dragon",
			"King Black Dragon duration: 01:42.60"));
		assertTrue(BossKillChatMatcher.matches("General Graardor",
			"You have killed General Graardor 12 times."));
		assertTrue(BossKillChatMatcher.matches("Vorkath",
			"Vorkath has been defeated."));
	}

	@Test
	public void rejectsUnrelatedOrNonKillMessages()
	{
		assertFalse(BossKillChatMatcher.matches("Giant Mole", "You killed a goblin."));
		assertFalse(BossKillChatMatcher.matches("Giant Mole", "Giant Mole is nearby."));
		assertFalse(BossKillChatMatcher.matches("", "kill count: 1"));
		assertFalse(BossKillChatMatcher.matches("Giant Mole", null));
	}
}
