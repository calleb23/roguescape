package com.pluginideahub.roguescape.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes {@link CustomRunSpec} — the Swing-free run-builder state model lifted out of
 * {@code RogueScapePanel}. This first slice covers the scalar constraints: defaults, the cycle/toggle
 * mutators, and the null/blank-guarded mode/loadout setters.
 */
public class CustomRunSpecTest
{
	@Test
	public void hasTheExpectedDefaults()
	{
		CustomRunSpec spec = new CustomRunSpec();
		assertEquals("Scavenger", spec.customBuilderGameMode());
		assertEquals("Naked", spec.customBuilderLoadout());
		assertEquals("Balanced", spec.customStrictness());
		assertFalse(spec.customBankUnlocks());
		assertEquals(0, spec.customTimeLimitMinutes());
		assertEquals(0, spec.customBossLimit());
	}

	@Test
	public void cyclesStrictnessBalancedTrustStrictAndBack()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.cycleCustomStrictness();
		assertEquals("Trust", spec.customStrictness());
		spec.cycleCustomStrictness();
		assertEquals("Strict", spec.customStrictness());
		spec.cycleCustomStrictness();
		assertEquals("Balanced", spec.customStrictness());
	}

	@Test
	public void togglesBankUnlocks()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.toggleCustomBankUnlocks();
		assertTrue(spec.customBankUnlocks());
		spec.toggleCustomBankUnlocks();
		assertFalse(spec.customBankUnlocks());
	}

	@Test
	public void cyclesTimeLimitThroughThirtySixtyNinetyAndBackToZero()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.cycleCustomTimeLimit();
		assertEquals(30, spec.customTimeLimitMinutes());
		spec.cycleCustomTimeLimit();
		assertEquals(60, spec.customTimeLimitMinutes());
		spec.cycleCustomTimeLimit();
		assertEquals(90, spec.customTimeLimitMinutes());
		spec.cycleCustomTimeLimit();
		assertEquals(0, spec.customTimeLimitMinutes());
	}

	@Test
	public void cyclesBossLimitThroughOneTwoThreeAndBackToZero()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.cycleCustomBossLimit();
		assertEquals(1, spec.customBossLimit());
		spec.cycleCustomBossLimit();
		assertEquals(2, spec.customBossLimit());
		spec.cycleCustomBossLimit();
		assertEquals(3, spec.customBossLimit());
		spec.cycleCustomBossLimit();
		assertEquals(0, spec.customBossLimit());
	}

	@Test
	public void modeAndLoadoutSettersTrimAndIgnoreBlanks()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.setCustomBuilderGameMode("  Rewarded  ");
		assertEquals("Rewarded", spec.customBuilderGameMode());
		spec.setCustomBuilderGameMode("   ");
		assertEquals("Rewarded", spec.customBuilderGameMode()); // blank ignored
		spec.setCustomBuilderGameMode(null);
		assertEquals("Rewarded", spec.customBuilderGameMode()); // null ignored

		spec.setCustomBuilderLoadout("  Mid Gear  ");
		assertEquals("Mid Gear", spec.customBuilderLoadout());
		spec.setCustomBuilderLoadout("");
		assertEquals("Mid Gear", spec.customBuilderLoadout());
	}

	@Test
	public void plainSettersAssignDirectly()
	{
		// these are the unguarded setters applyCustomSeed drives after its own validation/parsing;
		// unlike the mode/loadout setters, setCustomStrictness intentionally does no validation
		CustomRunSpec spec = new CustomRunSpec();
		spec.setCustomStrictness("Whatever");
		assertEquals("Whatever", spec.customStrictness());
		spec.setCustomBankUnlocks(true);
		assertTrue(spec.customBankUnlocks());
		spec.setCustomTimeLimitMinutes(45);
		assertEquals(45, spec.customTimeLimitMinutes());
		spec.setCustomBossLimit(2);
		assertEquals(2, spec.customBossLimit());
	}
}
