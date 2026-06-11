package com.pluginideahub.roguescape.ui;

import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RogueScapeJournalTest
{
	@Test
	public void describeIdDecodesGroupAndChild()
	{
		// 41222145 == group 629, child 1 (InterfaceID.SideJournal.TABS)
		assertEquals("629:1", RogueScapeJournalWidgetProbe.describeId(41222145));
		assertEquals("629:0", RogueScapeJournalWidgetProbe.describeId(41222144));
	}

	@Test
	public void probeIsNullSafeWithoutClient()
	{
		RogueScapeJournalWidgetProbe probe = new RogueScapeJournalWidgetProbe(null);
		assertFalse(probe.probe().available());

		List<String> dump = probe.dumpLines();
		assertFalse(dump.isEmpty());
		assertTrue(dump.get(0).toLowerCase().contains("unavailable"));
	}
}
