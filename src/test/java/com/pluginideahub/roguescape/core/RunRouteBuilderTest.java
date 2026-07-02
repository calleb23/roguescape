package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.region.BossLibrary;
import com.pluginideahub.roguescape.core.region.RoomDefinition;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.RoomLibrary;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RunRouteBuilderTest
{
	private static RogueScapeRunSession freshSession()
	{
		return RogueScapeRunSession.start("Test goal", "seed-default", RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED);
	}

	private static int countOfType(RogueScapeRunSession session, RunStageType type)
	{
		int n = 0;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == type) n++;
		}
		return n;
	}

	private static List<String> stageIds(RogueScapeRunSession session)
	{
		List<String> ids = new ArrayList<>();
		for (RunStage s : session.route().stages()) ids.add(s.id());
		return ids;
	}

	@Test
	public void noCuratedCampaignsSoPreviewRowsAreEmpty()
	{
		// Presets were removed; campaignPreviewRows resolves nothing until real runs are authored.
		assertTrue(RunRouteBuilder.campaignPreviewRows(RunPreset.UNSPECIFIED).isEmpty());
		assertTrue(RunRouteBuilder.campaignPreviewRows(null).isEmpty());
	}

	@Test
	public void generatedRoomsCarryTrackableObjectives()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("draynor-village"), "", session, run);

		RunStage stage = session.route().stages().get(0);
		assertTrue(stage.objectiveIsTrackable());
		assertEquals(RunObjectiveKind.SUPPLY_ITEMS, stage.objectiveKind());
		assertEquals(2, stage.requiredItemGains());
		assertTrue(stage.objectiveProgressLabel().contains("0 / 2"));

		run.moveToRegion("12338");
		run.applyItemDelta("Shark", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(1, stage.itemGains());
		assertFalse(stage.objectiveComplete());

		run.applyItemDelta("Lobster", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(2, stage.itemGains());
		assertTrue(stage.objectiveComplete());
	}

	@Test
	public void craftingObjectivesRequireCraftedOrGatheredSignal()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("edgeville"), "", session, run);
		RunStage stage = session.route().stages().get(0);

		assertEquals(RunObjectiveKind.SKILLING_RESOURCE, stage.objectiveKind());
		run.moveToRegion("12342");
		run.applyItemDelta("Rune scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(0, stage.itemGains());

		run.applyItemDelta("Iron scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_CRAFTED);
		assertEquals(1, stage.itemGains());

		// Crafting rooms carry a room task: crafting XP completes the objective.
		assertTrue(run.recordStatChanged("Smithing", 10));
		assertTrue(stage.objectiveComplete());
	}

	@Test
	public void supplyRoomsCountRawMaterialsAsSupplies()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("dwarven-mine"), "", session, run);
		RunStage stage = session.route().stages().get(0);

		assertEquals(RunObjectiveKind.SUPPLY_ITEMS, stage.objectiveKind());
		run.moveToRegion("12441");
		run.applyItemDelta("Rune scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(0, stage.itemGains());

		run.applyItemDelta("Iron ore", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_GATHERED);
		assertEquals(1, stage.itemGains());
	}

	@Test
	public void combatFlavouredRoomsAreWeaponRooms()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("edgeville-dungeon"), "", session, run);
		RunStage stage = session.route().stages().get(0);

		assertEquals(RunObjectiveKind.WEAPON_UPGRADE, stage.objectiveKind());
		run.moveToRegion("12442");
		run.applyItemDelta("Bones", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(0, stage.itemGains());

		run.applyItemDelta("Rune scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertTrue(stage.objectiveComplete());
	}

	@Test
	public void weaponObjectivesRequireWeaponCategory()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("barbarian-village"), "", session, run);
		RunStage stage = session.route().stages().get(0);

		assertEquals(RunObjectiveKind.WEAPON_UPGRADE, stage.objectiveKind());
		run.moveToRegion("12341");
		run.applyItemDelta("Shark", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(0, stage.itemGains());

		run.applyItemDelta("Iron scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertTrue(stage.objectiveComplete());
	}

	@Test
	public void armourObjectivesRequireArmourCategory()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("falador"), "", session, run);
		RunStage stage = session.route().stages().get(0);

		assertEquals(RunObjectiveKind.ARMOUR_UPGRADE, stage.objectiveKind());
		run.moveToRegion("11828");
		run.applyItemDelta("Iron scimitar", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertEquals(0, stage.itemGains());

		run.applyItemDelta("Iron platebody", 1, com.pluginideahub.roguescape.core.item.ProvenanceHint.OBSERVED_LOOT);
		assertTrue(stage.objectiveComplete());
	}

	@Test
	public void unspecifiedPresetDefaultsToThreeRoomsOneBoss()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "unspec", session, run);
		assertEquals(4, session.route().size());
		assertEquals(3, countOfType(session, RunStageType.ROOM));
		assertEquals(1, countOfType(session, RunStageType.BOSS));
	}

	@Test
	public void scavengerGeneratedRouteHasThreeRoomsAndOneBoss()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED, "scavenger", session, run);

		assertEquals(4, session.route().size());
		assertEquals(3, countOfType(session, RunStageType.ROOM));
		assertEquals(1, countOfType(session, RunStageType.BOSS));
	}

	@Test
	public void bossLadderRouteIsBossesOnly()
	{
		// Locked 2026-07-02: the Boss Ladder route is purely the boss line-up — no prep ROOM
		// stage; gearing up happens in the prep PHASE between bosses.
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED, "rewarded", session, run);

		assertEquals(3, session.route().size());
		assertEquals(0, countOfType(session, RunStageType.ROOM));
		assertEquals(3, countOfType(session, RunStageType.BOSS));
	}

	@Test
	public void testerGeneratedRoutesHavePlayableStageContracts()
	{
		assertPlayableGeneratedRoute(RunMode.FRESH_SOURCE, "scavenger-contract");
		assertPlayableGeneratedRoute(RunMode.BANK_DRAFT, "rewarded-contract");
	}

	@Test
	public void seededRaceUsesBalancedRandomRoomKinds()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);

		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, "balanced", session, run);

		assertHasRoomKind(run, session, RoomKind.WEAPON);
		assertHasRoomKind(run, session, RoomKind.SUPPLY);
		assertHasRoomKind(run, session, RoomKind.ARMOUR);
	}

	@Test
	public void sameSeedProducesIdenticalRoute()
	{
		RogueScapeRunSession sessionA = freshSession();
		RogueScapeRun runA = RogueScapeRun.wrap(sessionA);
		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, "race-seed-1", sessionA, runA);

		RogueScapeRunSession sessionB = freshSession();
		RogueScapeRun runB = RogueScapeRun.wrap(sessionB);
		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, "race-seed-1", sessionB, runB);

		assertEquals(stageIds(sessionA), stageIds(sessionB));
	}

	@Test
	public void differentSeedsProduceDifferentRoutes()
	{
		RogueScapeRunSession sessionA = freshSession();
		RogueScapeRun runA = RogueScapeRun.wrap(sessionA);
		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, "seed-alpha", sessionA, runA);

		RogueScapeRunSession sessionB = freshSession();
		RogueScapeRun runB = RogueScapeRun.wrap(sessionB);
		RunRouteBuilder.buildRoute(RunMode.SEEDED_RACE, RunPreset.UNSPECIFIED, "seed-omega", sessionB, runB);

		assertFalse("expected distinct routes for distinct seeds", stageIds(sessionA).equals(stageIds(sessionB)));
	}

	@Test
	public void buildPopulatesRegionRulesOnRun()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "rules", session, run);
		assertFalse(session.route().stages().isEmpty());
		for (RunStage s : session.route().stages())
		{
			assertTrue("rule missing for " + s.id(), run.regionPolicy().hasRule(s.id()));
		}
	}

	@Test
	public void bossStagesGetBossKindRegionRule()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "boss-rule", session, run);

		RunStage boss = null;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == RunStageType.BOSS) { boss = s; break; }
		}
		assertNotNull("expected at least one boss stage", boss);
		StageRegionRule rule = run.regionPolicy().ruleFor(boss.id());
		assertEquals(RoomKind.BOSS, rule.roomKind());
		assertFalse(rule.allowsRegionGain());
	}

	@Test
	public void roomStagesGetRoomKindRegionRule()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "room-rule", session, run);

		RunStage room = null;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == RunStageType.ROOM) { room = s; break; }
		}
		assertNotNull("expected at least one room stage", room);
		StageRegionRule rule = run.regionPolicy().ruleFor(room.id());
		assertTrue(rule.allowsRegionGain());
	}

	@Test
	public void firstStageIsEntered()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "enter-first", session, run);
		assertFalse(session.route().stages().isEmpty());
		assertTrue(session.route().stages().get(0).isEntered());
	}

	@Test
	public void doubleBuildThrows()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "first", session, run);
		try
		{
			RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "second", session, run);
			fail("expected IllegalStateException on double build");
		}
		catch (IllegalStateException expected)
		{
			// pass
		}
	}

	@Test
	public void nullSessionThrows()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		try
		{
			RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "x", null, run);
			fail("expected IllegalArgumentException for null session");
		}
		catch (IllegalArgumentException expected)
		{
			// pass
		}
	}

	private static List<RoomDefinition> nonBossRooms()
	{
		List<RoomDefinition> rooms = new ArrayList<>();
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() != RoomKind.BOSS) rooms.add(def);
		}
		return rooms;
	}

	private static void assertHasRoomKind(RogueScapeRun run, RogueScapeRunSession session, RoomKind kind)
	{
		for (RunStage stage : session.route().stages())
		{
			if (stage.type() == RunStageType.ROOM && run.regionPolicy().ruleFor(stage.id()).roomKind() == kind)
			{
				return;
			}
		}
		fail("expected route to contain room kind " + kind);
	}

	private static void assertPlayableGeneratedRoute(RunMode mode, String seed)
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(mode, RunPreset.UNSPECIFIED, seed, session, run);

		assertFalse("expected generated route", session.route().stages().isEmpty());
		assertTrue("first stage should be entered", session.route().stages().get(0).isEntered());
		for (RunStage stage : session.route().stages())
		{
			assertTrue("rule missing for " + stage.id(), run.regionPolicy().hasRule(stage.id()));
			assertNotNull("objective kind missing for " + stage.id(), stage.objectiveKind());
			assertFalse("objective label missing for " + stage.id(), stage.objectiveLabel().trim().isEmpty());
			if (stage.type() == RunStageType.BOSS)
			{
				assertEquals(RoomKind.BOSS, run.regionPolicy().ruleFor(stage.id()).roomKind());
				assertEquals(RunObjectiveKind.BOSS_DEFEAT, stage.objectiveKind());
			}
			else
			{
				assertTrue("room stages should allow region entry gains", run.regionPolicy().ruleFor(stage.id()).allowsRegionGain());
			}
		}
	}

	private static void assertCustomAllowance(RogueScapeRunSession session, RogueScapeRun run, int index,
		RoomKind expectedKind, RunObjectiveKind expectedObjective, int expectedRequiredGains)
	{
		RunStage stage = session.route().stages().get(index);
		assertEquals(expectedKind, run.regionPolicy().ruleFor(stage.id()).roomKind());
		assertEquals(expectedObjective, stage.objectiveKind());
		assertEquals(expectedRequiredGains, stage.requiredItemGains());
	}

	@Test
	public void explicitRouteHonorsOrderAndBoss()
	{
		List<RoomDefinition> rooms = nonBossRooms();
		String roomA = rooms.get(0).id();
		String roomB = rooms.get(1).id();
		String bossId = BossLibrary.all().get(0).id();

		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList(roomA, roomB), bossId, session, run);

		assertEquals(Arrays.asList(roomA, roomB, bossId), stageIds(session));
		assertEquals(2, countOfType(session, RunStageType.ROOM));
		assertEquals(1, countOfType(session, RunStageType.BOSS));
		assertTrue("first stage should be entered", session.route().stages().get(0).isEntered());
	}

	@Test
	public void explicitRouteUsesCustomAllowancesForObjectivesAndRules()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);

		RunRouteBuilder.buildExplicitRoute(
			Arrays.asList("lumbridge-swamp", "draynor-village", "edgeville"),
			Arrays.asList("Weapons", "Supply", "Shopping"),
			"",
			session,
			run);

		RunStage weapons = session.route().stages().get(0);
		RunStage supply = session.route().stages().get(1);
		RunStage shopping = session.route().stages().get(2);

		assertEquals(RoomKind.WEAPON, run.regionPolicy().ruleFor(weapons.id()).roomKind());
		assertEquals(RunObjectiveKind.WEAPON_UPGRADE, weapons.objectiveKind());
		assertEquals("Find a weapon upgrade in Lumbridge Swamp", weapons.objectiveLabel());

		assertEquals(RoomKind.SUPPLY, run.regionPolicy().ruleFor(supply.id()).roomKind());
		assertEquals(RunObjectiveKind.SUPPLY_ITEMS, supply.objectiveKind());
		assertEquals(2, supply.requiredItemGains());

		// Legacy "Shopping" allowance collapses into SUPPLY.
		assertEquals(RoomKind.SUPPLY, run.regionPolicy().ruleFor(shopping.id()).roomKind());
		assertEquals(RunObjectiveKind.SUPPLY_ITEMS, shopping.objectiveKind());
	}

	@Test
	public void explicitRouteMapsEveryCustomAllowanceToRuleAndObjective()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);

		RunRouteBuilder.buildExplicitRoute(
			Arrays.asList("lumbridge-swamp", "draynor-village", "barbarian-village",
				"edgeville", "varrock-west", "rimmington"),
			Arrays.asList("All", "Supply", "Weapon", "Shopping", "Armour", "Crafting"),
			"",
			session,
			run);

		// "All" falls back to the room's own kind (lumbridge-swamp is a supply room).
		assertCustomAllowance(session, run, 0, RoomKind.SUPPLY, RunObjectiveKind.SUPPLY_ITEMS, 2);
		assertCustomAllowance(session, run, 1, RoomKind.SUPPLY, RunObjectiveKind.SUPPLY_ITEMS, 2);
		assertCustomAllowance(session, run, 2, RoomKind.WEAPON, RunObjectiveKind.WEAPON_UPGRADE, 1);
		assertCustomAllowance(session, run, 3, RoomKind.SUPPLY, RunObjectiveKind.SUPPLY_ITEMS, 2);
		assertCustomAllowance(session, run, 4, RoomKind.ARMOUR, RunObjectiveKind.ARMOUR_UPGRADE, 1);
		assertCustomAllowance(session, run, 5, RoomKind.CRAFTING, RunObjectiveKind.SKILLING_RESOURCE, 1);
	}

	@Test
	public void explicitRouteKeepsBossesInMixedOrder()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);

		RunRouteBuilder.buildExplicitRoute(
			Arrays.asList("lumbridge-swamp", "boss-king-black-dragon", "edgeville"),
			Arrays.asList("Weapons", "Boss", "Shopping"),
			"",
			session,
			run);

		assertEquals("lumbridge-swamp", session.route().stages().get(0).id());
		assertEquals(RunStageType.BOSS, session.route().stages().get(1).type());
		assertEquals("edgeville", session.route().stages().get(2).id());
		assertTrue(session.route().stages().get(0).isEntered());
	}

	@Test
	public void explicitRouteSkipsUnknownIds()
	{
		String validRoom = nonBossRooms().get(0).id();

		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(Arrays.asList("no-such-room", validRoom), "no-such-boss", session, run);

		assertEquals(Arrays.asList(validRoom), stageIds(session));
	}

	@Test
	public void explicitRouteWithNoSelectionsProducesEmptyRoute()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildExplicitRoute(new ArrayList<>(), "", session, run);
		assertEquals(0, session.route().size());
	}

	@Test
	public void bossesAppearAfterRooms()
	{
		RogueScapeRunSession session = freshSession();
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.UNSPECIFIED, RunPreset.UNSPECIFIED, "order", session, run);

		boolean seenBoss = false;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == RunStageType.BOSS) seenBoss = true;
			else if (seenBoss) fail("room stage appeared after a boss stage: " + s.id());
		}
	}
}
