package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.legality.ItemLegality;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.legality.StarterKit;
import com.pluginideahub.roguescape.core.legality.StrictnessMode;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class RogueScapeRunTest
{
	private static StarterKit kitOf(String... names)
	{
		Map<String, Integer> m = new LinkedHashMap<>();
		for (String n : names) m.put(n.toLowerCase(), 1);
		return new StarterKit(m);
	}

	private static Set<String> regions(String... ids)
	{
		Set<String> s = new LinkedHashSet<>();
		for (String id : ids) s.add(id);
		return s;
	}

	@Test
	public void r1ToR2ToB1RunIsLegalInsideAllowedRegions()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Defeat Obor", "seed-2-3", RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		base.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		base.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.declareStarterKit(kitOf("Bronze dagger"))
			.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true))
			.setRegionRule("R2", new StageRegionRule(RoomKind.SUPPLY, regions("barbarian_village"), true))
			.setRegionRule("B1", new StageRegionRule(RoomKind.BOSS, regions("obor_lair"), true));

		base.enterStage("R1");
		run.moveToRegion("lumbridge");
		ItemEvent kitEvent = run.applyItemDelta("Bronze dagger", 1, ProvenanceHint.UNKNOWN);
		assertEquals(ItemLegality.LEGAL_STARTER_KIT, kitEvent.legality());

		ItemEvent r1Loot = run.applyItemDelta("Rat tail", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, r1Loot.legality());
		base.clearStage("R1");

		base.enterStage("R2");
		run.moveToRegion("barbarian_village");
		ItemEvent r2Catch = run.applyItemDelta("Trout", 2, ProvenanceHint.OBSERVED_GATHERED);
		assertEquals(ItemLegality.LEGAL_GATHERED_OR_CRAFTED, r2Catch.legality());
		base.clearStage("R2");

		base.enterStage("B1");
		run.moveToRegion("obor_lair");
		ItemEvent bossDrop = run.applyItemDelta("Obor key", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, bossDrop.legality());
		assertTrue(base.currentOpenStage().objectiveComplete());
		base.clearStage("B1");
		base.completeRun("Obor down", RunCompletionReason.GOAL_COMPLETE);

		assertEquals(RunState.COMPLETE, base.runState());
		assertEquals(4, run.legalCount());
		assertEquals(0, run.suspiciousCount());
		assertEquals(0, run.illegalCount());
	}

	@Test
	public void leavingLegalRegionInStrictModeFailsRun()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Stay in Lumbridge", "seed-strict",
			RunMode.REGION_CRAWL, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setStrictness(StrictnessMode.STRICT)
			.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));

		base.enterStage("R1");
		run.moveToRegion("karamja");

		assertEquals(RunState.FAILED, base.runState());
		assertFalse(run.currentRegionLegal());
	}

	@Test
	public void itemGainedOutsideAllowedRegionIsIllegal()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Region test");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setStrictness(StrictnessMode.BALANCED)
			.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true));

		base.enterStage("R1");
		run.moveToRegion("karamja");
		// Balanced mode does not fail just on region change, but item gain should be illegal.
		ItemEvent e = run.applyItemDelta("Banana", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.ILLEGAL_OUT_OF_REGION, e.legality());
		assertEquals(1, run.illegalCount());
		assertEquals(RunState.FAILED, base.runState());
	}

	@Test
	public void preRunSupplyDetectedWhenSnapshotShowsExtraItemsAtStart()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Pre-run supply test");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		Map<String, Integer> startQs = new LinkedHashMap<>();
		startQs.put("bronze dagger", 1);
		startQs.put("shark", 5);

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.declareStarterKit(kitOf("Bronze dagger"))
			.setStartSnapshot(new InventorySnapshot(startQs))
			.setPreRunSupplyExpected(true);

		base.enterStage("R1");
		// Simulate the adapter feeding "Shark" delta in run-start phase.
		ItemEvent e = run.applyItemDelta("Shark", 5, ProvenanceHint.UNKNOWN);
		assertEquals(ItemLegality.ILLEGAL_PRE_RUN_SUPPLY, e.legality());
		assertEquals(1, run.illegalCount());
	}

	@Test
	public void suspiciousItemFailsInStrictModeButFlagsInBalanced()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Suspicious strict test");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun strict = RogueScapeRun.wrap(base).setStrictness(StrictnessMode.STRICT);
		base.enterStage("R1");
		strict.applyItemDelta("Mystery coin", 1, ProvenanceHint.OBSERVED_GROUND_PICKUP);
		assertEquals(RunState.FAILED, base.runState());

		RogueScapeRunSession base2 = RogueScapeRunSession.start("Suspicious balanced test");
		base2.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun balanced = RogueScapeRun.wrap(base2).setStrictness(StrictnessMode.BALANCED);
		base2.enterStage("R1");
		balanced.applyItemDelta("Mystery coin", 1, ProvenanceHint.OBSERVED_GROUND_PICKUP);
		assertEquals(RunState.ACTIVE, base2.runState());
		assertEquals(1, balanced.suspiciousCount());
	}

	@Test
	public void shopPurchaseLootGatheredAllScoreAsLegal()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Mixed legal sources");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");

		run.applyItemDelta("Tinderbox", 1, ProvenanceHint.OBSERVED_SHOP_PURCHASE);
		run.applyItemDelta("Oak logs", 5, ProvenanceHint.OBSERVED_GATHERED);
		run.applyItemDelta("Bones", 3, ProvenanceHint.OBSERVED_LOOT);

		assertEquals(3, run.legalCount());
		assertEquals(0, run.suspiciousCount());
		assertEquals(0, run.illegalCount());
	}

	@Test
	public void clearedStageBeforeAdvancingStillReportsLegality()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Cleared but not advanced");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("R2", RunStageType.ROOM, "Karamja", "");
		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setRegionRule("R1", new StageRegionRule(RoomKind.REGION, regions("lumbridge"), true))
			.setRegionRule("R2", new StageRegionRule(RoomKind.REGION, regions("karamja"), true));

		base.enterStage("R1");
		base.clearStage("R1");
		run.moveToRegion("lumbridge");
		// After clearing R1 and before entering R2, the most-recent entered stage is R1.
		ItemEvent e = run.applyItemDelta("Bronze pickaxe", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(ItemLegality.LEGAL_REGION_GAIN, e.legality());
		assertEquals("R1", e.stageId());
	}
}
