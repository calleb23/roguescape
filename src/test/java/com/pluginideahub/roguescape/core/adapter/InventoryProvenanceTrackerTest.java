package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntFunction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventoryProvenanceTrackerTest
{
	private static final IntFunction<String> NAMER = id -> id == 995 ? "Coins" : String.valueOf(id);

	private static RunContext enteredRoomCtx(String region)
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		return RunContext.active(s, run, null, region);
	}

	private static RunContext enteredRoomCtx()
	{
		return enteredRoomCtx("12850");
	}

	private static InventorySnapshot snap(String id, int qty)
	{
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put(id, qty);
		return new InventorySnapshot(m);
	}

	@Test
	public void positiveGainResolvesNameStampsRegionAndHintConsumesHintOnce()
	{
		RunContext ctx = enteredRoomCtx();
		ProvenanceSignalTracker signals = new ProvenanceSignalTracker();
		signals.observe(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, "withdraw");

		InventoryProvenanceTracker.Result r = InventoryProvenanceTracker.apply(
			ctx, signals, new InventorySnapshot(), snap("995", 100), NAMER);

		assertTrue(r.changed());
		assertEquals("Coins x100", r.latestObservedItem());
		assertEquals("OBSERVED_BANK_WITHDRAWAL", r.latestProvenanceSignal());
		assertFalse("hint must be consumed exactly once", signals.hasPendingHint());

		// The name/region/hint annotation migrated out of the deleted plugin helpers must land on the
		// applied delta, not just the Result status strings.
		assertEquals(1, ctx.run().itemEvents().size());
		ItemDelta applied = ctx.run().itemEvents().get(0).delta();
		assertEquals("Coins", applied.itemName());
		assertEquals("region 12850", applied.locationNote());
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, applied.provenanceHint());
	}

	@Test
	public void blankRegionStampsTheUnknownRegionFallbackNote()
	{
		RunContext ctx = enteredRoomCtx("");
		InventoryProvenanceTracker.apply(ctx, new ProvenanceSignalTracker(), new InventorySnapshot(), snap("995", 100), NAMER);

		assertEquals("unknown region", ctx.run().itemEvents().get(0).delta().locationNote());
	}

	@Test
	public void reapplyingTheSameSnapshotYieldsNoFurtherGains()
	{
		RunContext ctx = enteredRoomCtx();
		ProvenanceSignalTracker signals = new ProvenanceSignalTracker();
		InventorySnapshot after = snap("995", 100);
		InventoryProvenanceTracker.apply(ctx, signals, new InventorySnapshot(), after, NAMER);

		InventoryProvenanceTracker.Result second = InventoryProvenanceTracker.apply(ctx, signals, after, after, NAMER);

		assertFalse(second.changed());
		assertEquals(1, ctx.run().itemEvents().size());
	}

	@Test
	public void noPositiveDeltaLeavesThePendingHintIntact()
	{
		RunContext ctx = enteredRoomCtx();
		ProvenanceSignalTracker signals = new ProvenanceSignalTracker();
		signals.observe(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, "withdraw");
		InventorySnapshot same = snap("995", 100);

		InventoryProvenanceTracker.Result r = InventoryProvenanceTracker.apply(ctx, signals, same, same, NAMER);

		assertFalse(r.changed());
		assertTrue("hint only consumed on an actual gain", signals.hasPendingHint());
		assertEquals(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, signals.currentHint());
	}

	@Test
	public void nonNumericIdKeepsTheProvidedNameWithoutResolving()
	{
		RunContext ctx = enteredRoomCtx();
		InventoryProvenanceTracker.Result r = InventoryProvenanceTracker.apply(
			ctx, new ProvenanceSignalTracker(), new InventorySnapshot(), snap("coins", 5), NAMER);

		assertTrue(r.changed());
		assertEquals("coins x5", r.latestObservedItem());
		assertEquals("unknown source", r.latestProvenanceSignal());
	}

	@Test
	public void lobbyContextAppliesNothing()
	{
		ProvenanceSignalTracker signals = new ProvenanceSignalTracker();
		signals.observe(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, "withdraw");

		InventoryProvenanceTracker.Result r = InventoryProvenanceTracker.apply(
			RunContext.lobby(), signals, new InventorySnapshot(), snap("995", 100), NAMER);

		assertFalse(r.changed());
		assertTrue(signals.hasPendingHint());
	}
}
