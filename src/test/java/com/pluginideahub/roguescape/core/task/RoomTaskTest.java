package com.pluginideahub.roguescape.core.task;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RoomTaskTest
{
	@Test
	public void blankLabelFallsBackToDefault()
	{
		RoomTask task = new RoomTask("  ", "mining", 3);
		assertEquals("Gain XP in this room", task.label());
	}

	@Test
	public void requiredEventsClampToAtLeastOne()
	{
		RoomTask task = new RoomTask("Chop logs", "woodcutting", 0);
		assertEquals(1, task.xpEventsRequired());
		assertFalse(task.complete());
		assertTrue(task.recordStatChanged("Woodcutting"));
		assertTrue(task.complete());
	}

	@Test
	public void skillFilterIgnoresOtherSkills()
	{
		RoomTask task = new RoomTask("Mine ore", "mining", 2);
		assertFalse(task.recordStatChanged("fishing"));
		assertEquals(0, task.xpEvents());
		assertTrue(task.recordStatChanged(" MINING "));
		assertEquals(1, task.xpEvents());
	}

	@Test
	public void emptySkillFilterAcceptsAnySkill()
	{
		RoomTask task = new RoomTask("Gain any XP", null, 2);
		assertTrue(task.recordStatChanged("agility"));
		assertTrue(task.recordStatChanged(null));
		assertTrue(task.complete());
	}

	@Test
	public void progressStopsCountingAtRequirement()
	{
		RoomTask task = new RoomTask("Train", "magic", 2);
		task.recordStatChanged("magic");
		task.recordStatChanged("magic");
		task.recordStatChanged("magic");
		assertEquals(2, task.xpEvents());
		assertEquals("Train (2 / 2)", task.progressLabel());
	}
}
