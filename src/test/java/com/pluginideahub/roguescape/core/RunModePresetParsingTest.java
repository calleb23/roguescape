package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RunModePresetParsingTest
{
	@Test
	public void parseModeAcceptsExactName()
	{
		assertEquals(RunMode.BANK_DRAFT, ModePresetParser.parseMode("BANK_DRAFT"));
	}

	@Test
	public void parseModeAcceptsLowerCase()
	{
		assertEquals(RunMode.BANK_DRAFT, ModePresetParser.parseMode("bank_draft"));
	}

	@Test
	public void parseModeAcceptsMixedCaseWithWhitespace()
	{
		assertEquals(RunMode.SEEDED_RACE, ModePresetParser.parseMode("  Seeded_Race  "));
	}

	@Test
	public void parseModeReturnsUnspecifiedOnBadInput()
	{
		assertEquals(RunMode.UNSPECIFIED, ModePresetParser.parseMode("not_a_mode"));
	}

	@Test
	public void parseModeReturnsUnspecifiedOnNull()
	{
		assertEquals(RunMode.UNSPECIFIED, ModePresetParser.parseMode(null));
	}

	@Test
	public void parseModeReturnsUnspecifiedOnEmpty()
	{
		assertEquals(RunMode.UNSPECIFIED, ModePresetParser.parseMode(""));
	}

	@Test
	public void parsePresetAcceptsExactName()
	{
		assertEquals(RunPreset.MAX_MAIN_DRAFT, ModePresetParser.parsePreset("MAX_MAIN_DRAFT"));
	}

	@Test
	public void parsePresetAcceptsLowerCase()
	{
		assertEquals(RunPreset.GOBLIN_RAT, ModePresetParser.parsePreset("goblin_rat"));
	}

	@Test
	public void parsePresetReturnsUnspecifiedOnBadInput()
	{
		assertEquals(RunPreset.UNSPECIFIED, ModePresetParser.parsePreset("super_preset"));
	}

	@Test
	public void parsePresetReturnsUnspecifiedOnNullOrEmpty()
	{
		assertEquals(RunPreset.UNSPECIFIED, ModePresetParser.parsePreset(null));
		assertEquals(RunPreset.UNSPECIFIED, ModePresetParser.parsePreset(""));
	}
}
