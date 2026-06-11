package com.pluginideahub.roguescape.core.relic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RelicCatalogTest
{
	@Test
	public void relicCatalogIsComplete()
	{
		assertCatalog(RelicLibrary.all(), 20);
	}

	@Test
	public void modifierCatalogIsComplete()
	{
		assertCatalog(ModifierLibrary.all(), 20);
	}

	@Test
	public void combinedCatalogAndLookup()
	{
		assertEquals(40, RelicCatalog.all().size());
		assertNotNull(RelicCatalog.byId("mod-no-food"));
		assertEquals("No Food", RelicCatalog.byId("mod-no-food").name());
		assertNotNull(RelicCatalog.byId("gluttony"));
		assertNull(RelicCatalog.byId("does-not-exist"));
		assertNull(RelicCatalog.byId(null));
	}

	private static void assertCatalog(List<Relic> list, int expectedSize)
	{
		assertEquals(expectedSize, list.size());
		Set<String> ids = new HashSet<>();
		for (Relic relic : list)
		{
			assertNotNull(relic);
			assertNotNull("relic id required", relic.relicId());
			assertFalse("name must not be blank", relic.name() == null || relic.name().trim().isEmpty());
			assertFalse("description must not be blank",
				relic.description() == null || relic.description().trim().isEmpty());
			assertTrue("duplicate relic id: " + relic.relicId(), ids.add(relic.relicId()));
		}
	}
}
