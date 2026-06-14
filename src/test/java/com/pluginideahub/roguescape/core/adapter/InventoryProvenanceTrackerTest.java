package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
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

	private static RunContext enteredRoomCtx()
	{
		RogueScapeRunSession s = RogueScapeRunSession.start("g", "", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		s.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(s);
		s.enterStage("R1");
		return RunContext.active(s, run, null, "12850");
	}

	private static InventorySnapshot snap(String id, int qty)
	{
		Map<String, Integer> m = new LinkedHashMap<>();
		m.put(id, qty);
		return new InventorySnapshot(m);
	}

	@Test
	public void positiveGainResolvesNameConsumesHintOnceAndAppliesToRun()
	{
		RunContext ctx = enteredRoomCtx();
		ProvenanceSignalTracker signals = new ProvenanceSignalTracker();
		signals.observe(ProvenanceHint.OBSERVED_BANK_WITHDRAWAL, "withdraw");

		InventoryProvenanceTracker.Result r = InventoryProvenanceTracker.apply(
			ctx, signals, new InventorySnapshot(), snap("995", 100), NAMER);

		assertTrue(r.changed());
		assertEquals("Coins x100", r.latestObservedItem());
		assertEquals("OBSERVED_BANK_WITHDRAWAL", r.latestProvenanceSignal());
		assertEquals(1, ctx.run().itemEvents().size());
		assertFalse("hint must be consumed exactly once", signals.hasPendingHint());
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
