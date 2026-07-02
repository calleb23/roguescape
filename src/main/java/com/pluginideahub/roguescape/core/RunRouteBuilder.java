package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.campaign.CampaignDefinition;
import com.pluginideahub.roguescape.core.campaign.CampaignLibrary;
import com.pluginideahub.roguescape.core.region.BossLibrary;
import com.pluginideahub.roguescape.core.region.RoomDefinition;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.RoomLibrary;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.reward.DeterministicRng;
import com.pluginideahub.roguescape.core.task.RoomTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 5 — populates a {@link RogueScapeRunSession}/{@link RogueScapeRun} pair with a
 * randomized (or seeded) route drawn from {@link RoomLibrary} and {@link BossLibrary}.
 *
 * Pure Java; no RuneLite types. Generated runs place rooms before bosses; explicit custom
 * routes preserve the player's mixed room/boss order. A seed string makes the draw deterministic
 * so speedruns / seeded races can share the same route.
 */
public final class RunRouteBuilder
{
	private RunRouteBuilder() {}

	public static void buildRoute(
		RunMode mode,
		RunPreset preset,
		String seedText,
		RogueScapeRunSession session,
		RogueScapeRun run)
	{
		if (session == null) throw new IllegalArgumentException("session required");
		if (run == null) throw new IllegalArgumentException("run required");
		if (session.route().size() > 0) throw new IllegalStateException("route already populated");

		if (mode != RunMode.SEEDED_RACE && buildCampaignRouteIfPresent(preset, session, run))
		{
			enterFirstStage(session);
			return;
		}

		int roomCount = roomCount(mode, preset);
		int bossCount = bossCount(mode, preset);

		long seed = (seedText != null && !seedText.isEmpty()) ? DeterministicRng.hash(seedText) : System.nanoTime();
		DeterministicRng random = new DeterministicRng(seed);

		List<RoomDefinition> roomPool = selectRoomsForMode(mode, roomCount, random);

		List<RoomDefinition> bossPool = new ArrayList<>(BossLibrary.all());
		random.shuffle(bossPool);

		for (RoomDefinition room : roomPool)
		{
			addRoom(room, session, run);
		}

		int bosses = Math.min(bossCount, bossPool.size());
		for (int i = 0; i < bosses; i++)
		{
			addBoss(bossPool.get(i), session, run);
		}

		enterFirstStage(session);
	}

	public static List<String> campaignPreviewRows(RunPreset preset)
	{
		CampaignDefinition campaign = CampaignLibrary.find(preset);
		List<String> rows = new ArrayList<>();
		if (campaign == null)
		{
			return rows;
		}
		Map<String, RoomDefinition> rooms = indexById(RoomLibrary.all());
		Map<String, RoomDefinition> bosses = indexById(BossLibrary.all());
		int step = 1;
		for (String id : campaign.roomIds())
		{
			RoomDefinition room = rooms.get(id);
			if (room != null)
			{
				rows.add(step++ + ". [" + room.kind().name() + "] " + room.name());
			}
		}
		for (String id : campaign.bossIds())
		{
			RoomDefinition boss = bosses.get(id);
			if (boss != null)
			{
				rows.add(step++ + ". [BOSS] " + boss.name());
			}
		}
		return rows;
	}

	/**
	 * Builds a route from an explicit, ordered list of room and boss IDs followed by an
	 * optional legacy boss ID. IDs are resolved against {@link RoomLibrary} and
	 * {@link BossLibrary}; unknown IDs are skipped. Use this for the handpicked-route flow; use
	 * {@link #buildRoute} for an auto-generated route.
	 */
	public static void buildExplicitRoute(
		List<String> roomIds,
		String bossId,
		RogueScapeRunSession session,
		RogueScapeRun run)
	{
		buildExplicitRoute(roomIds, Collections.emptyList(), bossId, session, run);
	}

	public static void buildExplicitRoute(
		List<String> roomIds,
		List<String> roomAllowances,
		String bossId,
		RogueScapeRunSession session,
		RogueScapeRun run)
	{
		if (session == null) throw new IllegalArgumentException("session required");
		if (run == null) throw new IllegalArgumentException("run required");
		if (session.route().size() > 0) throw new IllegalStateException("route already populated");

		Map<String, RoomDefinition> rooms = indexById(RoomLibrary.all());
		Map<String, RoomDefinition> bosses = indexById(BossLibrary.all());

		if (roomIds != null)
		{
			for (int i = 0; i < roomIds.size(); i++)
			{
				String id = roomIds.get(i);
				RoomDefinition def = rooms.get(id);
				if (def == null)
				{
					RoomDefinition boss = bosses.get(id);
					if (boss != null)
					{
						addBoss(boss, session, run);
					}
					continue;
				}
				RoomKind kind = explicitRoomKind(def, roomAllowances != null && i < roomAllowances.size()
					? roomAllowances.get(i) : null);
				addRoom(def, kind, session, run);
			}
		}

		if (bossId != null && !bossId.isEmpty())
		{
			RoomDefinition boss = bosses.get(bossId);
			if (boss != null)
			{
				addBoss(boss, session, run);
			}
		}

		enterFirstStage(session);
	}

	private static boolean buildCampaignRouteIfPresent(RunPreset preset, RogueScapeRunSession session, RogueScapeRun run)
	{
		CampaignDefinition campaign = CampaignLibrary.find(preset);
		if (campaign == null)
		{
			return false;
		}
		Map<String, RoomDefinition> rooms = indexById(RoomLibrary.all());
		Map<String, RoomDefinition> bosses = indexById(BossLibrary.all());
		for (String id : campaign.roomIds())
		{
			RoomDefinition room = rooms.get(id);
			if (room != null)
			{
				addRoom(room, session, run);
			}
		}
		for (String id : campaign.bossIds())
		{
			RoomDefinition boss = bosses.get(id);
			if (boss != null)
			{
				addBoss(boss, session, run);
			}
		}
		return session.route().size() > 0;
	}

	private static List<RoomDefinition> selectBalancedRooms(int roomCount, DeterministicRng random)
	{
		List<RoomDefinition> selected = new ArrayList<>();
		List<RoomKind> desired = desiredRoomKinds(roomCount);
		for (RoomKind kind : desired)
		{
			RoomDefinition room = pickRoom(kind, selected, random);
			if (room != null)
			{
				selected.add(room);
			}
		}
		while (selected.size() < roomCount)
		{
			RoomDefinition room = pickRoom(null, selected, random);
			if (room == null)
			{
				break;
			}
			selected.add(room);
		}
		return selected;
	}

	private static List<RoomDefinition> selectRoomsForMode(RunMode mode, int roomCount, DeterministicRng random)
	{
		if (roomCount <= 0)
		{
			// Boss Ladder: bosses only — no prep room stage.
			return new ArrayList<>();
		}
		if (mode != RunMode.BANK_DRAFT)
		{
			return selectBalancedRooms(roomCount, random);
		}

		List<RoomDefinition> selected = new ArrayList<>();
		RoomDefinition supply = pickRoom(RoomKind.SUPPLY, selected, random);
		if (supply != null)
		{
			selected.add(supply);
		}
		while (selected.size() < roomCount)
		{
			RoomDefinition combat = pickRoom(RoomKind.COMBAT, selected, random);
			if (combat == null)
			{
				break;
			}
			selected.add(combat);
		}
		while (selected.size() < roomCount)
		{
			RoomDefinition any = pickRoom(null, selected, random);
			if (any == null)
			{
				break;
			}
			selected.add(any);
		}
		return selected;
	}

	private static List<RoomKind> desiredRoomKinds(int roomCount)
	{
		List<RoomKind> order = new ArrayList<>();
		if (roomCount >= 1) order.add(RoomKind.REGION);
		if (roomCount >= 2) order.add(RoomKind.SUPPLY);
		if (roomCount >= 3) order.add(RoomKind.COMBAT);
		if (roomCount >= 4) order.add(RoomKind.ARMOUR);
		if (roomCount >= 5) order.add(RoomKind.SKILLING);
		while (order.size() < roomCount)
		{
			order.add(RoomKind.SHOP);
		}
		return order;
	}

	private static RoomDefinition pickRoom(RoomKind kind, List<RoomDefinition> selected, DeterministicRng random)
	{
		List<RoomDefinition> candidates = new ArrayList<>();
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() == RoomKind.BOSS) continue;
			if (kind != null && def.kind() != kind) continue;
			boolean used = false;
			for (RoomDefinition existing : selected)
			{
				if (existing.id().equals(def.id()))
				{
					used = true;
					break;
				}
			}
			if (!used)
			{
				candidates.add(def);
			}
		}
		if (candidates.isEmpty())
		{
			return null;
		}
		return candidates.get(random.nextInt(candidates.size()));
	}

	private static void addRoom(RoomDefinition room, RogueScapeRunSession session, RogueScapeRun run)
	{
		addRoom(room, room.kind(), session, run);
	}

	private static void addRoom(RoomDefinition room, RoomKind roomKind, RogueScapeRunSession session, RogueScapeRun run)
	{
		String stageId = room.id();
		RoomKind kind = roomKind != null ? roomKind : room.kind();
		RunStage stage = session.addStage(stageId, RunStageType.ROOM, room.name(), "Room: " + room.name(),
			objectiveLabel(room, kind), objectiveKind(kind), requiredItemGains(kind));
		if (kind == RoomKind.SKILLING)
		{
			stage.setRoomTask(new RoomTask("Gain skilling XP in " + room.name(), "", 1));
		}
		run.setRegionRule(stageId, new StageRegionRule(kind, room.regionIds(), true));
	}

	private static void addBoss(RoomDefinition boss, RogueScapeRunSession session, RogueScapeRun run)
	{
		String stageId = boss.id();
		session.addStage(stageId, RunStageType.BOSS, boss.name(), "Boss: " + boss.name(),
			"Defeat " + boss.name() + ", then complete the stage", RunObjectiveKind.BOSS_DEFEAT, 0);
		run.setRegionRule(stageId, new StageRegionRule(RoomKind.BOSS, boss.regionIds(), false));
	}

	private static void enterFirstStage(RogueScapeRunSession session)
	{
		List<RunStage> stages = session.route().stages();
		if (!stages.isEmpty())
		{
			session.enterStage(stages.get(0).id());
		}
	}

	private static Map<String, RoomDefinition> indexById(List<RoomDefinition> defs)
	{
		Map<String, RoomDefinition> byId = new LinkedHashMap<>();
		for (RoomDefinition def : defs)
		{
			byId.put(def.id(), def);
		}
		return byId;
	}

	private static int roomCount(RunMode mode, RunPreset preset)
	{
		if (mode == RunMode.CUSTOM_CREATOR) return 3;
		// Boss Ladder is bosses only — no prep ROOM stage (the prep PHASE between bosses
		// handles gearing up). Locked 2026-07-02.
		if (mode == RunMode.BANK_DRAFT) return 0;
		return 3;
	}

	private static int bossCount(RunMode mode, RunPreset preset)
	{
		if (mode == RunMode.CUSTOM_CREATOR) return 1;
		if (mode == RunMode.BANK_DRAFT) return 3;
		return 1;
	}

	private static String objectiveLabel(RoomDefinition room)
	{
		return objectiveLabel(room, room.kind());
	}

	private static String objectiveLabel(RoomDefinition room, RoomKind kind)
	{
		RoomKind k = kind != null ? kind : room.kind();
		switch (k)
		{
			case SHOP:
				return "Buy or obtain one item in " + room.name();
			case COMBAT:
				return "Win one combat drop in " + room.name();
			case SUPPLY:
				return "Collect two supplies in " + room.name();
			case SKILLING:
				return "Gather two resources in " + room.name();
			case WEAPON:
				return "Find a weapon upgrade in " + room.name();
			case ARMOUR:
				return "Find an armour upgrade in " + room.name();
			case REGION:
			default:
				return "Find one upgrade in " + room.name();
		}
	}

	private static int requiredItemGains(RoomDefinition room)
	{
		return requiredItemGains(room.kind());
	}

	private static int requiredItemGains(RoomKind kind)
	{
		switch (kind != null ? kind : RoomKind.REGION)
		{
			case SUPPLY:
			case SKILLING:
				return 2;
			case BOSS:
				return 0;
			case REGION:
			case COMBAT:
			case SHOP:
			case WEAPON:
			case ARMOUR:
			default:
				return 1;
		}
	}

	private static RunObjectiveKind objectiveKind(RoomDefinition room)
	{
		return objectiveKind(room.kind());
	}

	private static RunObjectiveKind objectiveKind(RoomKind kind)
	{
		switch (kind != null ? kind : RoomKind.REGION)
		{
			case SHOP:
				return RunObjectiveKind.SHOP_PURCHASE;
			case COMBAT:
				return RunObjectiveKind.COMBAT_DROP;
			case SUPPLY:
				return RunObjectiveKind.SUPPLY_ITEMS;
			case SKILLING:
				return RunObjectiveKind.SKILLING_RESOURCE;
			case WEAPON:
				return RunObjectiveKind.WEAPON_UPGRADE;
			case ARMOUR:
				return RunObjectiveKind.ARMOUR_UPGRADE;
			case BOSS:
				return RunObjectiveKind.BOSS_DEFEAT;
			case REGION:
			default:
				return RunObjectiveKind.ANY_ITEM;
		}
	}

	private static RoomKind explicitRoomKind(RoomDefinition def, String allowance)
	{
		String value = allowance == null ? "" : allowance.trim().toLowerCase();
		if ("supply".equals(value)) return RoomKind.SUPPLY;
		if ("armour".equals(value) || "armor".equals(value)) return RoomKind.ARMOUR;
		if ("weapons".equals(value) || "weapon".equals(value)) return RoomKind.WEAPON;
		if ("skilling".equals(value)) return RoomKind.SKILLING;
		if ("shopping".equals(value) || "shop".equals(value)) return RoomKind.SHOP;
		if ("all".equals(value)) return RoomKind.REGION;
		return def == null ? RoomKind.REGION : def.kind();
	}
}
