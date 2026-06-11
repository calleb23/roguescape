package com.pluginideahub.roguescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class RogueScapeCustomRoomEditorStateTest
{
	@Test
	public void hoverChangeFiresListenerOnce()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		state.setHoveredRegionId(12850);

		assertEquals(12850, state.getHoveredRegionId());
		assertEquals(1, fired.get());
	}

	@Test
	public void repeatedSameHoverDoesNotFireAgain()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		state.setHoveredRegionId(12850);
		state.setHoveredRegionId(12850);
		state.setHoveredRegionId(12850);

		assertEquals(1, fired.get());
	}

	@Test
	public void hoverResetToNoneFires()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		state.setHoveredRegionId(12850);

		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		state.setHoveredRegionId(-1);

		assertEquals(-1, state.getHoveredRegionId());
		assertEquals(1, fired.get());
	}

	@Test
	public void editingFlagChangeFires()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		state.setEditing(true);
		state.setEditing(true);
		state.setEditing(false);

		assertEquals(2, fired.get());
	}

	@Test
	public void toggleHoveredAddsRegionAndUpdatesSummary()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection("Thermy Zone"));
		state.setHoveredRegionId(12850);
		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		int toggled = state.toggleHovered();

		assertEquals(12850, toggled);
		assertTrue(state.selection().contains(12850));
		assertEquals(1, state.selection().size());
		assertEquals(12850, state.getLastToggledRegionId());
		assertEquals("Region 12850 added", state.getLastToggleSummary());
		assertEquals(1, fired.get());
	}

	@Test
	public void toggleHoveredRemovesIfAlreadySelected()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		state.setHoveredRegionId(12850);
		state.toggleHovered();
		assertTrue(state.selection().contains(12850));

		int toggled = state.toggleHovered();

		assertEquals(12850, toggled);
		assertFalse(state.selection().contains(12850));
		assertEquals("Region 12850 removed", state.getLastToggleSummary());
	}

	@Test
	public void toggleHoveredWithNoHoverIsNoop()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		AtomicInteger fired = new AtomicInteger();
		state.onChange(fired::incrementAndGet);

		int toggled = state.toggleHovered();

		assertEquals(-1, toggled);
		assertEquals(0, state.selection().size());
		assertEquals("", state.getLastToggleSummary());
		assertEquals(0, fired.get());
	}

	@Test
	public void hoverIdsDiffer()
	{
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(
			new RogueScapeCustomRoomSelection());
		state.setHoveredRegionId(12850);
		assertNotEquals(-1, state.getHoveredRegionId());
	}
}
