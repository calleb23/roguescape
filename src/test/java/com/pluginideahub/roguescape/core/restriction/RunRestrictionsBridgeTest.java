package com.pluginideahub.roguescape.core.restriction;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.enforcement.RogueScapeEnforcementRules;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Chunk 7 (bridge): a live run's rules are now expressed as RunRestrictions — the single verdict
 * brain — and the menu-enforcement booleans derive from it, preserving the old behaviour.
 */
public class RunRestrictionsBridgeTest
{
	private static RogueScapeRun runInRoom()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Bridge run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, Collections.singleton("lumbridge"), true));
		return run;
	}

	@Test
	public void lockedRunRestrictsTheClassicSet()
	{
		RunRestrictions r = runInRoom().currentRestrictions();
		assertTrue(r.isRestricted(Restriction.BANK));
		assertTrue(r.isRestricted(Restriction.TRADE));
		assertTrue(r.isRestricted(Restriction.GRAND_EXCHANGE));
		assertTrue(r.isRestricted(Restriction.PRAYER));
		assertTrue(r.isRestricted(Restriction.POTIONS));
		assertTrue(r.isRestricted(Restriction.LEAVE_REGION));
		assertTrue(r.isRestricted(Restriction.GROUND_PICKUP_OUTSIDE_ROOM));
	}

	@Test
	public void unlocksEaseTheDerivedRestrictions()
	{
		RogueScapeRun run = runInRoom();
		run.grantUnlock(new com.pluginideahub.roguescape.core.unlock.RunUnlock(
			com.pluginideahub.roguescape.core.unlock.RunUnlockType.PRAYER, "Prayer unlocked", "R1", "Combat"));
		RunRestrictions r = run.currentRestrictions();
		assertFalse(r.isRestricted(Restriction.PRAYER));
		assertTrue(r.isRestricted(Restriction.POTIONS));
	}

	@Test
	public void enforcementRulesMatchTheOldDerivation()
	{
		RogueScapeRun run = runInRoom();
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertEquals(!run.bankUnlocked(), rules.blockBank());
		assertEquals(!run.tradeUnlocked(), rules.blockTrade());
		assertTrue(rules.blockGrandExchange());
		assertEquals(!run.prayerUnlocked(), rules.blockPrayer());
		assertEquals(!run.potionUnlocked(), rules.blockPotions());
		assertTrue(rules.blockGroundPickup());
		assertTrue(rules.warnLeaveRoom());
	}

	@Test
	public void relicCurseSurfacesInTheDerivedRestrictions()
	{
		RogueScapeRun run = runInRoom();
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.ModifierLibrary.noFood());
		assertTrue(run.currentRestrictions().isRestricted(Restriction.FOOD));
	}
}
