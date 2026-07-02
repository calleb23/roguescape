package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.item.StarterKit;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The plugin no longer classifies item provenance as permitted/forbidden — disallowed actions are
 * blocked at the menu instead. These tests cover what the run actually tracks now: items collected,
 * objective/boss completion, and region awareness.
 */
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
	public void runCollectsItemsAcrossRoomsAndBossThenCompletes()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Defeat Obor", "seed-2-3", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");
		base.addStage("R2", RunStageType.ROOM, "Barbarian Village", "food check");
		base.addStage("B1", RunStageType.BOSS, "Obor", "first boss");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.declareStarterKit(kitOf("Bronze dagger"))
			.setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, regions("lumbridge"), true))
			.setRegionRule("R2", new StageRegionRule(RoomKind.SUPPLY, regions("barbarian_village"), true))
			.setRegionRule("B1", new StageRegionRule(RoomKind.BOSS, regions("obor_lair"), true));

		base.enterStage("R1");
		run.moveToRegion("lumbridge");
		run.applyItemDelta("Rat tail", 1, ProvenanceHint.OBSERVED_LOOT);
		base.clearStage("R1");

		base.enterStage("R2");
		run.moveToRegion("barbarian_village");
		run.applyItemDelta("Trout", 2, ProvenanceHint.OBSERVED_GATHERED);
		base.clearStage("R2");

		base.enterStage("B1");
		run.moveToRegion("obor_lair");
		run.applyItemDelta("Obor key", 1, ProvenanceHint.OBSERVED_LOOT);
		assertTrue("boss loot should complete the boss objective", base.currentOpenStage().objectiveComplete());
		base.clearStage("B1");
		base.completeRun("Obor down", RunCompletionReason.GOAL_COMPLETE);

		assertEquals(RunState.COMPLETE, base.runState());
		assertEquals(3, run.itemsCollected());
	}

	@Test
	public void regionTrackingReflectsCurrentRegionWithoutFailingTheRun()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Stay in Lumbridge", "seed-region", RunMode.REGION_CRAWL, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter zone");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, regions("lumbridge"), true));

		base.enterStage("R1");
		run.moveToRegion("lumbridge");
		assertTrue(run.currentRegionAllowed());

		// Leaving the room's region is surfaced for the UI/enforcement but never fails the run.
		run.moveToRegion("karamja");
		assertFalse(run.currentRegionAllowed());
		assertEquals(RunState.ACTIVE, base.runState());
	}

	@Test
	public void itemsCountTowardTheCurrentStageAndAreRecorded()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Mixed sources");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");

		run.applyItemDelta("Tinderbox", 1, ProvenanceHint.OBSERVED_SHOP_PURCHASE);
		run.applyItemDelta("Oak logs", 5, ProvenanceHint.OBSERVED_GATHERED);
		run.applyItemDelta("Bones", 3, ProvenanceHint.OBSERVED_LOOT);

		assertEquals(3, run.itemsCollected());
		assertEquals(3, run.collectedItems().size());
		assertEquals("Tinderbox", run.collectedItems().get(0).itemName());
	}
}
