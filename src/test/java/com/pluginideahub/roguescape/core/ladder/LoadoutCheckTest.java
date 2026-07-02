package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.restriction.Curse;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.StartTier;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;

/** Chunk 6 (pure half): the PREP → FIGHT loadout gate. */
public class LoadoutCheckTest
{
	@Test
	public void cleanLoadoutPasses()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.MEDIUM, EnumSet.of(Curse.FAMINE));
		LoadoutCheck.Result result = LoadoutCheck.validate(r, Arrays.asList(
			new LoadoutCheck.Item("Rune scimitar", 40, BankItemCategory.MELEE_WEAPON, true),
			new LoadoutCheck.Item("Adamant platebody", 30, BankItemCategory.BODY, true),
			new LoadoutCheck.Item("Prayer potion(4)", 0, BankItemCategory.POTION, false)));
		assertTrue(result.violations().toString(), result.passed());
	}

	@Test
	public void overTierGearBlocksTheGate()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.MEDIUM, EnumSet.noneOf(Curse.class));
		LoadoutCheck.Result result = LoadoutCheck.validate(r, Arrays.asList(
			new LoadoutCheck.Item("Dragon dagger", 60, BankItemCategory.MELEE_WEAPON, true)));
		assertFalse(result.passed());
		assertTrue(result.violations().get(0).contains("gear-tier cap"));
	}

	@Test
	public void forbiddenFoodBlocksTheGate()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.FAMINE));
		LoadoutCheck.Result result = LoadoutCheck.validate(r, Arrays.asList(
			new LoadoutCheck.Item("Shark", 0, BankItemCategory.FOOD, false)));
		assertFalse(result.passed());
		assertTrue(result.violations().get(0).toLowerCase().contains("shark"));
	}

	@Test
	public void shieldLockAndInventoryLimitAreChecked()
	{
		RunRestrictions r = RunRestrictions.starting(null,
			EnumSet.of(Curse.BARE_FISTED, Curse.TIGHT_POCKETS));
		java.util.List<LoadoutCheck.Item> loadout = new java.util.ArrayList<>();
		loadout.add(new LoadoutCheck.Item("Bronze kiteshield", 1, BankItemCategory.SHIELD, true));
		for (int i = 0; i < 15; i++)
		{
			loadout.add(new LoadoutCheck.Item("Lobster " + i, 0, BankItemCategory.UNKNOWN, false));
		}
		LoadoutCheck.Result result = LoadoutCheck.validate(r, loadout);
		assertFalse(result.passed());
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("shields are locked")));
		assertTrue(result.violations().stream().anyMatch(v -> v.contains("limit is 14")));
	}

	@Test
	public void easedRestrictionsStopBlockingTheGate()
	{
		RunRestrictions r = RunRestrictions.starting(null, EnumSet.of(Curse.FAMINE));
		r.permit(com.pluginideahub.roguescape.core.restriction.Restriction.FOOD);
		LoadoutCheck.Result result = LoadoutCheck.validate(r, Arrays.asList(
			new LoadoutCheck.Item("Shark", 0, BankItemCategory.FOOD, false)));
		assertTrue(result.passed());
	}
}
