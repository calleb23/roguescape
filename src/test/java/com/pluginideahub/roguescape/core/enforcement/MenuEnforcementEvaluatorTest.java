package com.pluginideahub.roguescape.core.enforcement;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MenuEnforcementEvaluatorTest
{
	private static RogueScapeEnforcementRules rules(boolean bank, boolean trade, boolean ge, boolean pickup, boolean warn)
	{
		return new RogueScapeEnforcementRules()
			.setBlockBank(bank)
			.setBlockTrade(trade)
			.setBlockGrandExchange(ge)
			.setBlockGroundPickup(pickup)
			.setWarnLeaveRoom(warn);
	}

	@Test
	public void bankMenuBlockedWhenBankNotAllowed()
	{
		RogueScapeEnforcementRules rules = rules(true, false, false, false, false);
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Bank", "", rules, true));
	}

	@Test
	public void bankMenuAllowedWhenBankAllowed()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, false, false);
		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Bank", "", rules, true));
	}

	@Test
	public void tradeBlockedDuringRun()
	{
		RogueScapeEnforcementRules rules = rules(false, true, false, false, false);
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Trade with", "Player", rules, true));
	}

	@Test
	public void prayerAndPotionsBlockedUntilUnlocked()
	{
		RogueScapeEnforcementRules rules = new RogueScapeEnforcementRules()
			.setBlockPrayer(true)
			.setBlockPotions(true);

		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Activate", "Protect from Magic", rules, true));
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Drink", "Prayer potion(4)", rules, true));
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Drink", "Saradomin brew(4)", rules, true));
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Drink", "Super restore(4)", rules, true));
	}

	@Test
	public void geExchangeBlocked()
	{
		RogueScapeEnforcementRules rules = rules(false, false, true, false, false);
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Exchange", "", rules, true));
	}

	@Test
	public void takeBlockedOutsideRegionWhenRestricted()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, true, true);
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Take", "Bones", rules, false));
	}

	@Test
	public void takeBlockedOutsideRegionEvenWhenWalkIsStillAllowed()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, true, false)
			.setBlockWalkOutsideRoom(false);

		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Take", "Bones", rules, false));
		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Walk here", "", rules, false));
	}

	@Test
	public void takeAllowedInsideRegion()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, true, true);
		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Take", "Bones", rules, true));
	}

	@Test
	public void walkBlockedOutsideRoomWhenRoomLockArmed()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, true, true)
			.setBlockWalkOutsideRoom(true);

		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Walk here", "", rules, false));
	}

	@Test
	public void walkAllowedInsideRoomWhenRoomLockArmed()
	{
		RogueScapeEnforcementRules rules = rules(false, false, false, true, true)
			.setBlockWalkOutsideRoom(true);

		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Walk here", "", rules, true));
	}

	@Test
	public void normalAttackAlwaysAllowed()
	{
		RogueScapeEnforcementRules rules = rules(true, true, true, true, true);
		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Attack", "Goblin", rules, false));
	}

	@Test
	public void bankTargetContainsBankerBlocked()
	{
		RogueScapeEnforcementRules rules = rules(true, false, false, false, false);
		assertEquals(MenuEnforcementDecision.BLOCK,
			MenuEnforcementEvaluator.evaluate("Talk-to", "Banker", rules, true));
	}

	@Test
	public void forRunWithBankAllowedDoesNotBlockBank()
	{
		RogueScapeRunSession session = RogueScapeRunSession.start("Test");
		RogueScapeRun run = RogueScapeRun.wrap(session).setBankAccessAllowed(true);
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(run);
		assertEquals(MenuEnforcementDecision.ALLOW,
			MenuEnforcementEvaluator.evaluate("Bank", "", rules, true));
	}
}
