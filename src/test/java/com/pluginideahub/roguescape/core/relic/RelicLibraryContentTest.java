package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.legality.ItemLegality;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class RelicLibraryContentTest
{
	private static ItemEvent event(String name, ItemLegality legality, ProvenanceHint hint)
	{
		ItemDelta d = new ItemDelta(name.toLowerCase(), name, 1, "", hint);
		return new ItemEvent(d, legality, "R1");
	}

	private static Set<BankItemCategory> restrictedCategories(Relic relic)
	{
		Set<BankItemCategory> out = new HashSet<>();
		for (RelicEffect e : relic.effects())
		{
			if (e.kind() == RelicEffectKind.RESTRICTION)
			{
				out.addAll(e.categories());
			}
		}
		return out;
	}

	@Test
	public void glassCannonModeBoostsMagicAndRestrictsAllArmour()
	{
		Relic relic = RelicLibrary.glassCannonMode();
		assertEquals("glass-cannon-mode", relic.relicId());

		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.MAGIC_WEAPON, 2);
		RelicEngine engine = new RelicEngine().addRelic(relic);
		assertEquals(6, engine.scoreBonus(counts));

		Set<BankItemCategory> restricted = restrictedCategories(relic);
		assertTrue(restricted.contains(BankItemCategory.HELMET));
		assertTrue(restricted.contains(BankItemCategory.BODY));
		assertTrue(restricted.contains(BankItemCategory.LEGS));
		assertTrue(restricted.contains(BankItemCategory.BOOTS));
		assertTrue(restricted.contains(BankItemCategory.GLOVES));
		assertTrue(restricted.contains(BankItemCategory.SHIELD));
	}

	@Test
	public void rangersPactBoostsRangedAndRestrictsMelee()
	{
		Relic relic = RelicLibrary.rangersPact();
		RelicEngine engine = new RelicEngine().addRelic(relic);
		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.RANGED_WEAPON, 4);
		assertEquals(8, engine.scoreBonus(counts));

		ItemEvent whip = event("Abyssal whip", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK, engine.adjustLegality(whip, BankItemCategory.MELEE_WEAPON));
	}

	@Test
	public void minimalistUsesUnknownCategoryLimitAsTotalItemCap()
	{
		Relic relic = RelicLibrary.minimalist();
		assertEquals(1, relic.effects().size());
		RelicEffect effect = relic.effects().get(0);
		assertEquals(RelicEffectKind.CATEGORY_LIMIT, effect.kind());
		assertTrue(effect.categories().contains(BankItemCategory.UNKNOWN));
		assertEquals(12, effect.magnitude());
	}

	@Test
	public void runePouchBoostsRunesAndForbidsTeleports()
	{
		Relic relic = RelicLibrary.runePouch();
		RelicEngine engine = new RelicEngine().addRelic(relic);
		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.RUNE, 5);
		assertEquals(10, engine.scoreBonus(counts));
		ItemEvent tele = event("Varrock teleport", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.ILLEGAL_MANUAL_MARK, engine.adjustLegality(tele, BankItemCategory.TELEPORT));
	}

	@Test
	public void jewelryJunkieBoostsRingAndNeckAndRestrictsBoots()
	{
		Relic relic = RelicLibrary.jewelryJunkie();
		RelicEngine engine = new RelicEngine().addRelic(relic);
		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.RING, 2);
		counts.put(BankItemCategory.NECK, 1);
		assertEquals(9, engine.scoreBonus(counts));
		assertTrue(restrictedCategories(relic).contains(BankItemCategory.BOOTS));
	}

	@Test
	public void hoarderAddsOneBiasPerCategory()
	{
		Relic relic = RelicLibrary.hoarder();
		RelicEngine engine = new RelicEngine().addRelic(relic);
		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.FOOD, 3);
		counts.put(BankItemCategory.RUNE, 2);
		counts.put(BankItemCategory.NECK, 4);
		assertEquals(9, engine.scoreBonus(counts));
	}

	@Test
	public void rangersPactAndGlassCannonCombineToRestrictMelee()
	{
		RelicEngine engine = new RelicEngine()
			.addRelic(RelicLibrary.rangersPact())
			.addRelic(RelicLibrary.glassCannonMode());

		ItemEvent melee = event("Dragon scimitar", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals("melee forbidden by rangersPact",
			ItemLegality.ILLEGAL_MANUAL_MARK,
			engine.adjustLegality(melee, BankItemCategory.MELEE_WEAPON));

		ItemEvent helm = event("Rune full helm", ItemLegality.LEGAL_REGION_GAIN, ProvenanceHint.OBSERVED_LOOT);
		assertEquals("helmet forbidden by glassCannonMode",
			ItemLegality.ILLEGAL_MANUAL_MARK,
			engine.adjustLegality(helm, BankItemCategory.HELMET));

		EnumMap<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		counts.put(BankItemCategory.RANGED_WEAPON, 1);
		counts.put(BankItemCategory.MAGIC_WEAPON, 1);
		assertEquals("rangers +2 plus glass-cannon +3", 5, engine.scoreBonus(counts));
	}
}
