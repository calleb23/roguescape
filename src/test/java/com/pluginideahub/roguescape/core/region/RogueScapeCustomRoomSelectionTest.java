package com.pluginideahub.roguescape.core.region;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class RogueScapeCustomRoomSelectionTest
{
	@Test
	public void defaultsToNamedCustomRoom()
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection();

		assertEquals(RogueScapeCustomRoomSelection.DEFAULT_NAME, selection.getName());
		assertTrue(selection.isEmpty());
		assertEquals("", selection.toCsv());
	}

	@Test
	public void preservesSelectionOrderAndIgnoresDuplicates()
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection("Thermy West Entrance");

		selection.addRegion(12345);
		selection.addRegion(54321);
		selection.addRegion(12345);

		assertEquals("Thermy West Entrance", selection.getName());
		assertEquals("12345,54321", selection.toCsv());
	}

	@Test
	public void togglesAndUndoLastChange()
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection();

		assertTrue(selection.toggleRegion(100));
		assertTrue(selection.contains(100));
		assertFalse(selection.toggleRegion(100));
		assertFalse(selection.contains(100));
		assertTrue(selection.undoLastToggle());
		assertTrue(selection.contains(100));
	}

	@Test
	public void parsesCsvDefensively()
	{
		RogueScapeCustomRoomSelection selection = RogueScapeCustomRoomSelection.fromCsv(
			"Castle Wars Thermy",
			" 12850, nope, -1, 12851, 999999, 12850 "
		);

		assertEquals("Castle Wars Thermy", selection.getName());
		assertEquals("12850,12851", selection.toCsv());
	}

	@Test
	public void canReplaceAndBulkAddRegions()
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection();

		assertEquals(2, selection.addRegions(Arrays.asList(1, 2, 2, -5, null)));
		assertEquals("1,2", selection.toCsv());

		selection.replaceFromCsv("3,4,bad,3");

		assertEquals("3,4", selection.toCsv());
	}
}
