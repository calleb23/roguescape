package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProvenanceSignalTrackerTest
{
	@Test
	public void menuClassifierFindsConservativeItemSources()
	{
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL,
			ProvenanceSignalTracker.classifyMenu("Withdraw-10", "Shark"));
		assertEquals(ProvenanceHint.OBSERVED_GE_COLLECT,
			ProvenanceSignalTracker.classifyMenu("Collect", "Grand Exchange offer"));
		assertEquals(ProvenanceHint.OBSERVED_TRADE,
			ProvenanceSignalTracker.classifyMenu("Trade with", "Zezima"));
		assertEquals(ProvenanceHint.OBSERVED_SHOP_PURCHASE,
			ProvenanceSignalTracker.classifyMenu("Buy 10", "Lobster"));
		assertEquals(ProvenanceHint.OBSERVED_GROUND_PICKUP,
			ProvenanceSignalTracker.classifyMenu("Take", "Coins"));
	}

	@Test
	public void chatClassifierFindsOnlyExplicitSources()
	{
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL,
			ProvenanceSignalTracker.classifyChat("You withdraw a shark from your bank."));
		assertEquals(ProvenanceHint.OBSERVED_SHOP_PURCHASE,
			ProvenanceSignalTracker.classifyChat("You buy 1 x Rope."));
		assertEquals(ProvenanceHint.OBSERVED_TRADE,
			ProvenanceSignalTracker.classifyChat("Accepted trade."));
		assertEquals(ProvenanceHint.UNKNOWN,
			ProvenanceSignalTracker.classifyChat("You eat the shark."));
	}

	@Test
	public void pendingHintConsumesOnceForNextInventoryDelta()
	{
		ProvenanceSignalTracker tracker = new ProvenanceSignalTracker();
		tracker.observeMenu("Withdraw-1", "Rune scimitar");
		assertTrue(tracker.hasPendingHint());
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, tracker.currentHint());
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, tracker.consumePendingHint());
		assertEquals(ProvenanceHint.UNKNOWN, tracker.consumePendingHint());
		assertFalse(tracker.hasPendingHint());
	}

	@Test
	public void pendingHintExpiresAfterTickWindow()
	{
		ProvenanceSignalTracker tracker = new ProvenanceSignalTracker();
		tracker.observeMenu("Take", "Bones");
		for (int i = 0; i < 4; i++)
		{
			tracker.onGameTick();
		}
		assertEquals(ProvenanceHint.UNKNOWN, tracker.currentHint());
		assertEquals(ProvenanceHint.UNKNOWN, tracker.consumePendingHint());
	}

	@Test
	public void deathMessagesAreDetectedThroughTags()
	{
		assertTrue(ProvenanceSignalTracker.isLikelyDeathMessage("<col=ff0000>Oh dear, you are dead!</col>"));
		assertTrue(ProvenanceSignalTracker.isLikelyDeathMessage("You have died."));
		assertFalse(ProvenanceSignalTracker.isLikelyDeathMessage("You feel refreshed."));
	}
}
