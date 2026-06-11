package com.pluginideahub.roguescape.core.legality;

import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class LegalityClassifierTest
{
	private static ItemDelta delta(String name, ProvenanceHint hint)
	{
		return new ItemDelta(name.toLowerCase(), name, 1, "", hint);
	}

	private static ItemDelta delta(String name, int qty, ProvenanceHint hint)
	{
		return new ItemDelta(name.toLowerCase(), name, qty, "", hint);
	}

	private static StarterKit kit(String... names)
	{
		Map<String, Integer> m = new LinkedHashMap<>();
		for (String n : names) m.put(n.toLowerCase(), 1);
		return new StarterKit(m);
	}

	@Test
	public void starterKitItemIsLegalStarterKit()
	{
		LegalityContext ctx = LegalityContext.builder()
			.starterKit(kit("Bronze dagger"))
			.build();
		ItemLegality result = LegalityClassifier.classify(
			delta("Bronze dagger", ProvenanceHint.UNKNOWN), ctx);
		assertEquals(ItemLegality.LEGAL_STARTER_KIT, result);
	}

	@Test
	public void observedLootInsideAllowedRegionIsLegalRegionGain()
	{
		Set<String> allowed = new HashSet<>();
		allowed.add("lumbridge");
		StageRegionRule rule = new StageRegionRule(RoomKind.REGION, allowed, true);
		LegalityContext ctx = LegalityContext.builder()
			.stageRule(rule)
			.currentRegionId("lumbridge")
			.build();
		ItemLegality result = LegalityClassifier.classify(
			delta("Bronze dagger", ProvenanceHint.OBSERVED_LOOT), ctx);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, result);
	}

	@Test
	public void observedLootOutsideAllowedRegionIsIllegalOutOfRegion()
	{
		Set<String> allowed = new HashSet<>();
		allowed.add("lumbridge");
		StageRegionRule rule = new StageRegionRule(RoomKind.REGION, allowed, true);
		LegalityContext ctx = LegalityContext.builder()
			.stageRule(rule)
			.currentRegionId("karamja")
			.build();
		ItemLegality result = LegalityClassifier.classify(
			delta("Bronze dagger", ProvenanceHint.OBSERVED_LOOT), ctx);
		assertEquals(ItemLegality.ILLEGAL_OUT_OF_REGION, result);
	}

	@Test
	public void unrestrictedStageAllowsLootAnywhere()
	{
		LegalityContext ctx = LegalityContext.builder()
			.stageRule(StageRegionRule.UNRESTRICTED)
			.currentRegionId("anywhere")
			.build();
		assertEquals(ItemLegality.LEGAL_REGION_GAIN,
			LegalityClassifier.classify(delta("Trout", ProvenanceHint.OBSERVED_LOOT), ctx));
	}

	@Test
	public void preRunSupplyDetectedWhenContextFlagsAndItemNotInKit()
	{
		LegalityContext ctx = LegalityContext.builder()
			.starterKit(kit("Bronze dagger"))
			.preRunSupplyExpected(true)
			.build();
		ItemLegality result = LegalityClassifier.classify(
			delta("Shark", 5, ProvenanceHint.UNKNOWN), ctx);
		assertEquals(ItemLegality.ILLEGAL_PRE_RUN_SUPPLY, result);
	}

	@Test
	public void tradeIsAlwaysIllegalTrade()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.ILLEGAL_TRADE,
			LegalityClassifier.classify(delta("Bond", ProvenanceHint.OBSERVED_TRADE), ctx));
	}

	@Test
	public void geCollectionIsAlwaysIllegalGE()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.ILLEGAL_GE,
			LegalityClassifier.classify(delta("Whip", ProvenanceHint.OBSERVED_GE_COLLECT), ctx));
	}

	@Test
	public void bankWithdrawalIllegalWhenBankAccessNotAllowed()
	{
		LegalityContext ctx = LegalityContext.builder()
			.bankAccessAllowed(false)
			.build();
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL,
			LegalityClassifier.classify(delta("Whip", ProvenanceHint.OBSERVED_BANK_WITHDRAWAL), ctx));
	}

	@Test
	public void bankWithdrawalLegalWhenUnlocked()
	{
		LegalityContext ctx = LegalityContext.builder()
			.bankAccessAllowed(true)
			.unlockedBankItem(true)
			.build();
		assertEquals(ItemLegality.LEGAL_BANK_UNLOCK,
			LegalityClassifier.classify(delta("Whip", ProvenanceHint.OBSERVED_BANK_WITHDRAWAL), ctx));
	}

	@Test
	public void bankWithdrawalIllegalWhenLockedEvenIfAccessAllowed()
	{
		LegalityContext ctx = LegalityContext.builder()
			.bankAccessAllowed(true)
			.unlockedBankItem(false)
			.build();
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL,
			LegalityClassifier.classify(delta("Whip", ProvenanceHint.OBSERVED_BANK_WITHDRAWAL), ctx));
	}

	@Test
	public void shopPurchaseIsLegalShopPurchase()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.LEGAL_SHOP_PURCHASE,
			LegalityClassifier.classify(delta("Tinderbox", ProvenanceHint.OBSERVED_SHOP_PURCHASE), ctx));
	}

	@Test
	public void gatheredOrCraftedIsLegalGatheredOrCrafted()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.LEGAL_GATHERED_OR_CRAFTED,
			LegalityClassifier.classify(delta("Oak logs", ProvenanceHint.OBSERVED_GATHERED), ctx));
		assertEquals(ItemLegality.LEGAL_GATHERED_OR_CRAFTED,
			LegalityClassifier.classify(delta("Air rune", ProvenanceHint.OBSERVED_CRAFTED), ctx));
	}

	@Test
	public void groundPickupIsSuspiciousUnknown()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.SUSPICIOUS_UNKNOWN,
			LegalityClassifier.classify(delta("Coins", ProvenanceHint.OBSERVED_GROUND_PICKUP), ctx));
	}

	@Test
	public void unknownProvenanceIsSuspiciousUnknown()
	{
		LegalityContext ctx = LegalityContext.builder().build();
		assertEquals(ItemLegality.SUSPICIOUS_UNKNOWN,
			LegalityClassifier.classify(delta("Mystery", ProvenanceHint.UNKNOWN), ctx));
	}

	@Test
	public void manualDeclaredLegalAndIllegalWin()
	{
		LegalityContext ctx = LegalityContext.builder()
			.stageRule(new StageRegionRule(RoomKind.REGION, Collections.singleton("lumbridge"), true))
			.currentRegionId("karamja")
			.build();
		assertEquals(ItemLegality.LEGAL_MANUAL_APPROVAL,
			LegalityClassifier.classify(delta("Special", ProvenanceHint.MANUAL_DECLARED_LEGAL), ctx));
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK,
			LegalityClassifier.classify(delta("Cheats", ProvenanceHint.MANUAL_DECLARED_ILLEGAL), ctx));
	}

	@Test
	public void roomRewardStageMapsLootToLegalRoomReward()
	{
		StageRegionRule rule = new StageRegionRule(RoomKind.CHOICE_CHEST, Collections.emptySet(), false);
		LegalityContext ctx = LegalityContext.builder()
			.stageRule(rule)
			.build();
		assertEquals(ItemLegality.LEGAL_ROOM_REWARD,
			LegalityClassifier.classify(delta("Chest pick", ProvenanceHint.OBSERVED_DIALOG_REWARD), ctx));
	}

	@Test
	public void itemLegalityCategoryFlagsBehaveCorrectly()
	{
		assertTrue(ItemLegality.LEGAL_REGION_GAIN.isLegal());
		assertTrue(ItemLegality.SUSPICIOUS_UNKNOWN.isSuspicious());
		assertTrue(ItemLegality.ILLEGAL_BANK_WITHDRAWAL.isIllegal());
		assertFalse(ItemLegality.LEGAL_REGION_GAIN.isIllegal());
		assertFalse(ItemLegality.ILLEGAL_TRADE.isLegal());
		assertFalse(ItemLegality.SUSPICIOUS_UNKNOWN.isLegal());
	}
}
