package com.pluginideahub.roguescape.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.pluginideahub.roguescape.core.item.InventorySnapshot;
import com.pluginideahub.roguescape.core.region.RoomKind;
import java.util.Arrays;
import org.junit.Test;

public class RogueScapeCustomRunFactoryTest
{
	@Test
	public void customBuilderSelectionsProducePlayableRun()
	{
		RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
			RogueScapeCustomRunFactory.Config.builder()
				.goal("Custom flow")
				.seed("custom-seed")
				.customMode("Rewarded")
				.loadout("Mid Gear")
				.roomIds(Arrays.asList("lumbridge-swamp", "boss-king-black-dragon", "edgeville"))
				.roomAllowances(Arrays.asList("Weapons", "Boss", "Shopping"))
				.modifierIds(Arrays.asList("mod-no-food", "mod-no-teleports"))
				.bankUnlocks(true)
				.timeLimitMinutes(30)
				.startedAtMillis(1_000L)
				.startSnapshot(new InventorySnapshot()));

		RogueScapeRunSession session = started.session();
		RogueScapeRun run = started.run();
		RogueScapeRunLoop loop = started.loop();

		assertEquals(RunMode.CUSTOM_CREATOR, session.mode());
		assertEquals("custom-seed", session.seed());
		assertEquals(3, session.route().size());
		assertEquals(RunPhase.TRAVEL_TO_STAGE, loop.phase());
		assertTrue(loop.travelGatedStages());
		assertTrue(loop.baseRewardsEnabled());
		assertEquals(30 * 60_000L, loop.timeLimitMillis());
		assertTrue(run.bankAccessAllowed());
		assertFalse(run.starterKit().isEmpty());
		assertEquals(2, run.heldRelics().size());

		RunStage first = session.route().stages().get(0);
		RunStage second = session.route().stages().get(1);
		RunStage third = session.route().stages().get(2);
		assertEquals(RoomKind.WEAPON, run.regionPolicy().ruleFor(first.id()).roomKind());
		assertEquals(RunObjectiveKind.WEAPON_UPGRADE, first.objectiveKind());
		assertEquals(RunStageType.BOSS, second.type());
		assertEquals(RoomKind.SHOP, run.regionPolicy().ruleFor(third.id()).roomKind());
		assertEquals(RunObjectiveKind.SHOP_PURCHASE, third.objectiveKind());

		run.moveToRegion("12850");
		assertTrue(loop.notifyRegionChanged(1_500L));
		assertEquals(RunPhase.ROOM_ACTIVE, loop.phase());
	}

	@Test
	public void scavengerCustomModeKeepsRoomRewardsEnabled()
	{
		RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
			RogueScapeCustomRunFactory.Config.builder()
				.customMode("Scavenger")
				.roomIds(Arrays.asList("lumbridge-swamp", "edgeville"))
				.roomAllowances(Arrays.asList("All", "Shopping"))
				.startedAtMillis(0L));

		assertTrue(started.loop().baseRewardsEnabled());
	}

	@Test
	public void rewardedCustomModeUsesNormalRewardStops()
	{
		RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
			RogueScapeCustomRunFactory.Config.builder()
				.customMode("Rewarded")
				.roomIds(Arrays.asList("draynor-village", "boss-king-black-dragon"))
				.roomAllowances(Arrays.asList("Supply", "Boss"))
				.startedAtMillis(0L));

		assertTrue(started.loop().baseRewardsEnabled());
		assertEquals(2, started.session().route().size());
	}

	@Test
	public void emptyCustomRouteFallsBackToGeneratedRoute()
	{
		RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
			RogueScapeCustomRunFactory.Config.builder()
				.goal("Fallback")
				.seed("fallback-seed")
				.startedAtMillis(0L));

		assertTrue(started.session().route().size() > 0);
		assertEquals(RunPhase.TRAVEL_TO_STAGE, started.loop().phase());
	}
}
