package com.pluginideahub.roguescape.core.enforcement;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import com.pluginideahub.roguescape.core.unlock.RunUnlockType;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RogueScapeEnforcementRulesTest
{
	private static Set<String> regions(String... ids)
	{
		Set<String> s = new LinkedHashSet<>();
		for (String id : ids) s.add(id);
		return s;
	}

	private static RogueScapeRun runWithEnteredStage(String stageId, StageRegionRule rule, boolean bankAllowed)
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Test");
		session.addStage(stageId, RunStageType.ROOM, "Test stage", "");
		RogueScapeRun run = RogueScapeRun.wrap(session).setBankAccessAllowed(bankAllowed);
		if (rule != null)
		{
			run.setRegionRule(stageId, rule);
		}
		session.enterStage(stageId);
		return run;
	}

	@Test
	public void forRunBlocksBankWhenNotAllowed()
	{
		RogueScapeRun run = runWithEnteredStage("R1", null, false);
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertTrue(rules.blockBank());
	}

	@Test
	public void forRunAllowsBankWhenAllowed()
	{
		RogueScapeRun run = runWithEnteredStage("R1", null, true);
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertFalse(rules.blockBank());
	}

	@Test
	public void forRunAlwaysBlocksTradeAndGE()
	{
		RogueScapeRun bankClosed = runWithEnteredStage("R1", null, false);
		RogueScapeEnforcementRules rulesA = RogueScapeEnforcementRules.forRun(bankClosed);
		assertTrue(rulesA.blockTrade());
		assertTrue(rulesA.blockGrandExchange());

		RogueScapeRun bankOpen = runWithEnteredStage("R1", null, true);
		RogueScapeEnforcementRules rulesB = RogueScapeEnforcementRules.forRun(bankOpen);
		assertTrue(rulesB.blockTrade());
		assertTrue(rulesB.blockGrandExchange());
	}

	@Test
	public void forRunUnlocksTradePrayerAndPotions()
	{
		RogueScapeRun run = runWithEnteredStage("R1", null, false)
			.grantUnlock(new RunUnlock(RunUnlockType.TRADE, "Trade unlocked", "R0", "Shop"))
			.grantUnlock(new RunUnlock(RunUnlockType.PRAYER, "Prayer unlocked", "R0", "Combat"))
			.grantUnlock(new RunUnlock(RunUnlockType.POTION, "Potions unlocked", "R0", "Supply"));

		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);

		assertFalse(rules.blockTrade());
		assertFalse(rules.blockPrayer());
		assertFalse(rules.blockPotions());
		assertTrue(rules.blockGrandExchange());
	}

	@Test
	public void forRunBlocksGroundPickupWhenRegionRestricted()
	{
		StageRegionRule rule = new StageRegionRule(RoomKind.SUPPLY, regions("lumbridge"), true);
		RogueScapeRun run = runWithEnteredStage("R1", rule, false);
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertTrue(rules.blockGroundPickup());
		assertTrue(rules.blockWalkOutsideRoom());
		assertTrue(rules.warnLeaveRoom());
	}

	@Test
	public void forRunBlocksPickupButNotWalkDuringTravelBeforeRegionLockArms()
	{
		StageRegionRule rule = new StageRegionRule(RoomKind.SUPPLY, regions("lumbridge"), true);
		RogueScapeRun run = runWithEnteredStage("R1", rule, false)
			.setRegionRestrictionArmed(false);

		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);

		assertTrue(rules.blockGroundPickup());
		assertFalse(rules.blockWalkOutsideRoom());
		assertFalse(rules.warnLeaveRoom());
	}

	@Test
	public void forRunDoesNotBlockGroundPickupWhenNoRegionRule()
	{
		RogueScapeRun run = runWithEnteredStage("R1", null, false);
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertFalse(rules.blockGroundPickup());
		assertFalse(rules.warnLeaveRoom());
	}
}
