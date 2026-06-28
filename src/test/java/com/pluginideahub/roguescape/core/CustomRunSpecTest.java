package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
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

	@Test
	public void togglesModifiersByIndexAndTracksSelectedIndexes()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.toggleModifierIndex(0);
		assertEquals(1, spec.selectedModifierIds().size());
		assertTrue(spec.selectedModifierIndexes().contains(0));
		assertEquals(ModifierLibrary.all().get(0).relicId(), spec.selectedModifierIds().get(0));

		spec.toggleModifierIndex(0); // toggling again removes it
		assertTrue(spec.selectedModifierIds().isEmpty());
	}

	@Test
	public void applyModifierIdsFromCsvKeepsKnownUniqueIdsOnly()
	{
		CustomRunSpec spec = new CustomRunSpec();
		String id0 = ModifierLibrary.all().get(0).relicId();
		spec.applyModifierIdsFromCsv(id0 + "," + id0 + ",totally-bogus-id");
		assertEquals(1, spec.selectedModifierIds().size()); // dedup + unknown dropped
		assertEquals(id0, spec.selectedModifierIds().get(0));

		spec.applyModifierIdsFromCsv("   "); // blank clears
		assertTrue(spec.selectedModifierIds().isEmpty());
	}

	@Test
	public void addModifierIdIfAbsentReportsWhetherItAdded()
	{
		CustomRunSpec spec = new CustomRunSpec();
		assertTrue(spec.addModifierIdIfAbsent("x"));
		assertFalse(spec.addModifierIdIfAbsent("x")); // already present
		spec.clearModifiers();
		assertTrue(spec.selectedModifierIds().isEmpty());
	}

	@Test
	public void modifierPagingClampsToBounds()
	{
		CustomRunSpec spec = new CustomRunSpec();
		int last = Math.max(0, ModifierLibrary.all().size() - 1);
		spec.pageCustomModifierIndex(1000);
		assertEquals(last, spec.customModifierPageStart());
		spec.pageCustomModifierIndex(-1000);
		assertEquals(0, spec.customModifierPageStart());
	}

	@Test
	public void emptySeedPreviewHasTheCanonicalDefaults()
	{
		assertEquals("mode=Scavenger;loadout=Naked;rooms=;mods=;strictness=Balanced;bank=off;time=none;bosscap=none",
			new CustomRunSpec().customSeedPreview());
	}

	@Test
	public void addsCursorRoomOnceAndTracksSelectedRow()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.selectCustomRoomIndex(0);
		assertTrue(spec.addSelectedCustomRoom());
		assertEquals(1, spec.selectedRoomIds().size());
		assertEquals(0, spec.selectedRouteIndex());
		assertFalse(spec.addSelectedCustomRoom()); // same room id -> dedup, no add
		assertEquals(1, spec.selectedRoomIds().size());
	}

	@Test
	public void bossLimitStopsExtraBosses()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.setCustomBossLimit(1);
		spec.selectCustomBossIndex(0);
		assertTrue(spec.addSelectedCustomBoss());
		spec.selectCustomBossIndex(1);
		assertFalse(spec.addSelectedCustomBoss()); // cap reached
		assertEquals(1, spec.selectedRoomIds().size());
		assertFalse(spec.selectedBossId().isEmpty());
	}

	@Test
	public void routeRowMovesAreNoOpsAtBoundaries()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.selectCustomRoomIndex(0);
		spec.addSelectedCustomRoom();
		spec.selectCustomRoomIndex(1);
		spec.addSelectedCustomRoom();
		assertEquals(2, spec.selectedRoomIds().size());

		spec.selectRouteRow(0);
		assertFalse(spec.moveSelectedRouteUp());   // already top
		assertTrue(spec.moveSelectedRouteDown());  // 0 -> 1
		assertEquals(1, spec.selectedRouteIndex());
		assertFalse(spec.moveSelectedRouteDown()); // already bottom
	}

	@Test
	public void applyRouteFromSeedKeepsKnownIdsAndDropsUnknown()
	{
		CustomRunSpec spec = new CustomRunSpec();
		spec.selectCustomRoomIndex(0);
		spec.addSelectedCustomRoom();
		String realId = spec.selectedRoomIds().get(0);
		spec.clearRoute();
		assertTrue(spec.selectedRoomIds().isEmpty());

		spec.applyRouteFromSeed(realId + ":Weapons,bogus-room-id:All", null);
		assertEquals(1, spec.selectedRoomIds().size());
		assertEquals(realId, spec.selectedRoomIds().get(0));
		assertEquals("Weapons", spec.selectedRoomAllowances().get(0));
	}
}
