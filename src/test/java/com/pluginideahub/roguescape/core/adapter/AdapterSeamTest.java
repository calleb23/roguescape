package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class AdapterSeamTest
{
	private static Map<String, Integer> qs(Object... kv)
	{
		Map<String, Integer> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2)
		{
			m.put((String) kv[i], (Integer) kv[i + 1]);
		}
		return m;
	}

	@Test
	public void positiveDeltasOnlyIncludesGains()
	{
		InventorySnapshot before = new InventorySnapshot(qs("bronze dagger", 1, "shark", 3));
		InventorySnapshot after = new InventorySnapshot(qs("bronze dagger", 1, "shark", 5, "rat tail", 1));
		List<ItemDelta> deltas = InventoryDiff.positiveDeltas(before, after, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(2, deltas.size());
		// Find each by id to avoid relying on map iteration order.
		ItemDelta sharkDelta = deltas.stream().filter(d -> d.itemId().equals("shark")).findFirst().orElseThrow(AssertionError::new);
		assertEquals(2, sharkDelta.quantity());
		ItemDelta ratTailDelta = deltas.stream().filter(d -> d.itemId().equals("rat tail")).findFirst().orElseThrow(AssertionError::new);
		assertEquals(1, ratTailDelta.quantity());
		assertEquals(ProvenanceHint.OBSERVED_LOOT, sharkDelta.provenanceHint());
	}

	@Test
	public void negativeDeltasAreIgnored()
	{
		InventorySnapshot before = new InventorySnapshot(qs("shark", 5));
		InventorySnapshot after = new InventorySnapshot(qs("shark", 2));
		List<ItemDelta> deltas = InventoryDiff.positiveDeltas(before, after, ProvenanceHint.UNKNOWN);
		assertTrue(deltas.isEmpty());
	}

	@Test
	public void regionTrackerReportsRegionChanges()
	{
		RegionTracker tracker = new RegionTracker()
			.map(12850, "lumbridge")
			.map(11062, "karamja");
		assertTrue(tracker.observe(12850));
		assertEquals("lumbridge", tracker.currentRegionId());
		assertFalse("same region — not a change", tracker.observe(12850));
		assertTrue(tracker.observe(11062));
		assertEquals("karamja", tracker.currentRegionId());
	}

	@Test
	public void unknownRegionCodesProduceEmptyId()
	{
		RegionTracker tracker = new RegionTracker().map(12850, "lumbridge");
		// Move into a known region first so unknown is observably "different".
		assertTrue(tracker.observe(12850));
		assertEquals("lumbridge", tracker.currentRegionId());
		// Stepping into an unknown code clears the current region.
		assertTrue(tracker.observe(99999));
		assertEquals("", tracker.currentRegionId());
	}

	@Test
	public void translatorBuildsItemDeltaWithCorrectHint()
	{
		Map<String, String> attrs = new LinkedHashMap<>();
		attrs.put("itemName", "Shark");
		attrs.put("quantity", "2");
		ObservedEvent withdraw = new ObservedEvent(ObservedEventKind.BANK_WITHDRAWAL, "bank-lumbridge", 100L, attrs);
		ItemDelta delta = AdapterTranslator.toItemDelta(withdraw);
		assertNotNull(delta);
		assertEquals("Shark", delta.itemName());
		assertEquals(2, delta.quantity());
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, delta.provenanceHint());

		ObservedEvent trade = new ObservedEvent(ObservedEventKind.TRADE_ACCEPTED, "varrock", 110L, attrs);
		assertEquals(ProvenanceHint.OBSERVED_TRADE, AdapterTranslator.toItemDelta(trade).provenanceHint());

		ObservedEvent ge = new ObservedEvent(ObservedEventKind.GE_COLLECTED, "ge", 120L, attrs);
		assertEquals(ProvenanceHint.OBSERVED_GE_COLLECT, AdapterTranslator.toItemDelta(ge).provenanceHint());

		ObservedEvent shop = new ObservedEvent(ObservedEventKind.SHOP_PURCHASE, "lumby_shop", 130L, attrs);
		assertEquals(ProvenanceHint.OBSERVED_SHOP_PURCHASE, AdapterTranslator.toItemDelta(shop).provenanceHint());
	}

	@Test
	public void translatorReturnsNullForNonItemEvents()
	{
		ObservedEvent tick = new ObservedEvent(ObservedEventKind.GAME_TICK, "lumbridge", 1L, new LinkedHashMap<>());
		assertNull(AdapterTranslator.toItemDelta(tick));
		ObservedEvent death = new ObservedEvent(ObservedEventKind.DEATH, "wilderness", 2L, new LinkedHashMap<>());
		assertNull(AdapterTranslator.toItemDelta(death));
	}
}
