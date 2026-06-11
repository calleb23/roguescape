package com.pluginideahub.roguescape.core.region;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class BossLibraryTest
{
	@Test
	public void allReturnsTwentyBosses()
	{
		List<RoomDefinition> bosses = BossLibrary.all();
		assertEquals(20, bosses.size());
	}

	@Test
	public void allBossesAreKindBoss()
	{
		for (RoomDefinition b : BossLibrary.all())
		{
			assertEquals("boss " + b.id() + " should be RoomKind.BOSS", RoomKind.BOSS, b.kind());
			assertFalse("boss " + b.id() + " should have region IDs", b.regionIds().isEmpty());
		}
	}

	@Test
	public void giantMoleHasExpectedFields()
	{
		RoomDefinition r = BossLibrary.giantMole();
		assertEquals("boss-giant-mole", r.id());
		assertEquals("Giant Mole", r.name());
		assertEquals(RoomKind.BOSS, r.kind());
		assertTrue(r.regionIds().contains("6993"));
	}

	@Test
	public void zulrahHasExpectedFields()
	{
		RoomDefinition r = BossLibrary.zulrah();
		assertEquals("boss-zulrah", r.id());
		assertEquals("Zulrah", r.name());
		assertEquals(RoomKind.BOSS, r.kind());
		assertTrue(r.regionIds().contains("9007"));
	}

	@Test
	public void corporealBeastHasExpectedFields()
	{
		RoomDefinition r = BossLibrary.corporealBeast();
		assertEquals("boss-corporeal-beast", r.id());
		assertEquals("Corporeal Beast", r.name());
		assertEquals(RoomKind.BOSS, r.kind());
		assertTrue(r.regionIds().contains("11842"));
	}
}
