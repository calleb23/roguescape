package com.pluginideahub.roguescape.core.unlock;

import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RunUnlockGeneratorTest
{
	private static RunStage roomStage()
	{
		return new RunStage("R1", "Lumbridge Swamp", RunStageType.ROOM, "");
	}

	private static StageRegionRule rule(RoomKind kind)
	{
		return new StageRegionRule(kind, Collections.emptySet(), true);
	}

	@Test
	public void bossStagesGrantNoUnlock()
	{
		RunStage boss = new RunStage("B1", "Giant Mole", RunStageType.BOSS, "");
		assertNull(RunUnlockGenerator.forClearedStage(boss, rule(RoomKind.BOSS)));
		assertNull(RunUnlockGenerator.forClearedStage(null, rule(RoomKind.SUPPLY)));
	}

	@Test
	public void roomKindMapsToUnlockType()
	{
		assertEquals(RunUnlockType.POTION, RunUnlockGenerator.forClearedStage(roomStage(), rule(RoomKind.SUPPLY)).type());
		assertEquals(RunUnlockType.BANK, RunUnlockGenerator.forClearedStage(roomStage(), rule(RoomKind.CRAFTING)).type());
		assertEquals(RunUnlockType.PRAYER, RunUnlockGenerator.forClearedStage(roomStage(), rule(RoomKind.WEAPON)).type());
		assertEquals(RunUnlockType.INVENTORY, RunUnlockGenerator.forClearedStage(roomStage(), rule(RoomKind.ARMOUR)).type());
	}

	@Test
	public void missingRuleDefaultsToInventoryUnlock()
	{
		RunUnlock unlock = RunUnlockGenerator.forClearedStage(roomStage(), null);
		assertEquals(RunUnlockType.INVENTORY, unlock.type());
		assertEquals("R1", unlock.sourceStageId());
		assertEquals("Lumbridge Swamp", unlock.sourceStageName());
		assertEquals("Inventory expansion unlocked from Lumbridge Swamp", unlock.displayRow());
	}
}
