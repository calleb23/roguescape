package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.legality.ItemLegality;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.EnumMap;
import org.junit.Test;

import static org.junit.Assert.*;

public class RelicEngineTest
{
	private static ItemEvent event(String name, ItemLegality legality, ProvenanceHint hint)
	{
		ItemDelta d = new ItemDelta(name.toLowerCase(), name, 1, "", hint);
		return new ItemEvent(d, legality, "R1");
	}

	@Test
	public void oneBankMercyConsumesOneIllegalBankWithdrawal()
	{
		RelicEngine engine = new RelicEngine().addRelic(RelicLibrary.oneBankMercy());
		assertEquals(1, engine.mercyChargesLeft("one-bank-mercy"));

		ItemEvent first = event("Shark", ItemLegality.ILLEGAL_BANK_WITHDRAWAL, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.SUSPICIOUS_UNKNOWN, engine.adjustLegality(first, BankItemCategory.FOOD));
		assertEquals(0, engine.mercyChargesLeft("one-bank-mercy"));

		ItemEvent second = event("Lobster", ItemLegality.ILLEGAL_BANK_WITHDRAWAL, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals("second illegal withdrawal still illegal", ItemLegality.ILLEGAL_BANK_WITHDRAWAL,
			engine.adjustLegality(second, BankItemCategory.FOOD));
	}

	@Test
	public void fourFoodLimitFlagsExcessFoodAsOverLimit()
	{
		RelicEngine engine = new RelicEngine().addRelic(RelicLibrary.fourFoodLimit());
		for (int i = 0; i < 4; i++) engine.recordItem(BankItemCategory.FOOD);
		assertTrue(engine.overLimit().isEmpty());
		engine.recordItem(BankItemCategory.FOOD);
		assertTrue(engine.overLimit().contains(BankItemCategory.FOOD));
	}

	@Test
	public void cursedBladesScoresMeleeBonusAndForbidsShields()
	{
		RelicEngine engine = new RelicEngine().addRelic(RelicLibrary.cursedBlades());

		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.MELEE_WEAPON, 3);
		counts.put(BankItemCategory.FOOD, 2);
		assertEquals(6, engine.scoreBonus(counts));

		ItemEvent shield = event("Rune kiteshield", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK, engine.adjustLegality(shield, BankItemCategory.SHIELD));

		ItemEvent whip = event("Abyssal whip", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, engine.adjustLegality(whip, BankItemCategory.MELEE_WEAPON));
	}

	@Test
	public void differentRelicSetsProduceDifferentRunBehaviour()
	{
		RelicEngine runA = new RelicEngine().addRelic(RelicLibrary.oneBankMercy());
		RelicEngine runB = new RelicEngine().addRelic(RelicLibrary.cursedBlades());

		// Same bank withdrawal in both runs.
		ItemEvent withdrawal = event("Shark", ItemLegality.ILLEGAL_BANK_WITHDRAWAL, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.SUSPICIOUS_UNKNOWN, runA.adjustLegality(withdrawal, BankItemCategory.FOOD));
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL, runB.adjustLegality(withdrawal, BankItemCategory.FOOD));

		// Same shield pickup in both runs.
		ItemEvent shield = event("Rune kiteshield", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, runA.adjustLegality(shield, BankItemCategory.SHIELD));
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK, runB.adjustLegality(shield, BankItemCategory.SHIELD));

		// Same melee scoring counts produce different bonuses.
		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.MELEE_WEAPON, 2);
		assertEquals(0, runA.scoreBonus(counts));
		assertEquals(4, runB.scoreBonus(counts));
	}

	@Test
	public void specificItemIdRestrictionApplies()
	{
		Relic banSharks = new Relic("ban-sharks", "Shark Allergy", "Sharks are forbidden.",
			RelicEffect.restrictionById("shark"));
		RelicEngine engine = new RelicEngine().addRelic(banSharks);
		ItemEvent shark = event("Shark", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK, engine.adjustLegality(shark, BankItemCategory.FOOD));
		ItemEvent lobster = event("Lobster", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, engine.adjustLegality(lobster, BankItemCategory.FOOD));
	}
}
