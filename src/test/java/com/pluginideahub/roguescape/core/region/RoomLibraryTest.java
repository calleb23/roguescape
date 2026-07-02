package com.pluginideahub.roguescape.core.region;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class RoomLibraryTest
{
	@Test
	public void allReturnsThirtyRooms()
	{
		List<RoomDefinition> rooms = RoomLibrary.all();
		assertEquals(30, rooms.size());
	}

	@Test
	public void lumbridgeSwampHasIdNameAndRegionIds()
	{
		RoomDefinition r = RoomLibrary.lumbridgeSwamp();
		assertEquals("lumbridge-swamp", r.id());
		assertEquals("Lumbridge Swamp", r.name());
		assertEquals(RoomKind.SUPPLY, r.kind());
		assertTrue(r.regionIds().contains("12851"));
		assertTrue(r.regionIds().contains("12850"));
	}

	@Test
	public void grandExchangeIsASupplyRoomWithASingleRegion()
	{
		RoomDefinition r = RoomLibrary.grandExchange();
		assertEquals("grand-exchange", r.id());
		assertEquals("Grand Exchange", r.name());
		assertEquals(RoomKind.SUPPLY, r.kind());
		assertEquals(1, r.regionIds().size());
		assertTrue(r.regionIds().contains("12598"));
	}

	@Test
	public void dwarvenMineIsASupplyRoomWithNonEmptyRegionIds()
	{
		RoomDefinition r = RoomLibrary.dwarvenMine();
		assertEquals("dwarven-mine", r.id());
		assertEquals("Dwarven Mine", r.name());
		assertEquals(RoomKind.SUPPLY, r.kind());
		assertFalse(r.regionIds().isEmpty());
		assertTrue(r.regionIds().contains("12441"));
	}

	@Test
	public void allRoomsHaveUniqueIdsAndRegions()
	{
		Set<String> ids = new HashSet<>();
		for (RoomDefinition room : RoomLibrary.all())
		{
			assertTrue("duplicate room id: " + room.id(), ids.add(room.id()));
			assertFalse("room should have at least one region: " + room.id(), room.regionIds().isEmpty());
		}
	}

	@Test
	public void contentBankHasUsefulRoomKindSpread()
	{
		Map<RoomKind, Integer> counts = new EnumMap<>(RoomKind.class);
		for (RoomDefinition room : RoomLibrary.all())
		{
			counts.put(room.kind(), counts.getOrDefault(room.kind(), 0) + 1);
		}

		assertAtLeast(counts, RoomKind.WEAPON, 4);
		assertAtLeast(counts, RoomKind.ARMOUR, 4);
		assertAtLeast(counts, RoomKind.SUPPLY, 10);
		assertAtLeast(counts, RoomKind.CRAFTING, 3);
		assertAtLeast(counts, RoomKind.BOSS, 2);
	}

	private static void assertAtLeast(Map<RoomKind, Integer> counts, RoomKind kind, int expected)
	{
		assertTrue(kind + " count", counts.getOrDefault(kind, 0) >= expected);
	}
}
