package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes {@link ZoneBuilderSection} — the stateful custom-zone builder card lifted out of
 * {@code RogueScapePanel}. Pins the initial name seeding, the status/region readout + button
 * enablement driven by {@link #update()}, the editing toggle, and the save flow (name sync + request).
 */
public class ZoneBuilderSectionTest
{
	private static <T extends Component> List<T> collect(Component root, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		collect(root, type, out);
		return out;
	}

	private static <T extends Component> void collect(Component c, Class<T> type, List<T> out)
	{
		if (type.isInstance(c))
		{
			out.add(type.cast(c));
		}
		if (c instanceof Container)
		{
			for (Component child : ((Container) c).getComponents())
			{
				collect(child, type, out);
			}
		}
	}

	private static JButton button(Component root, String text)
	{
		for (JButton b : collect(root, JButton.class))
		{
			if (text.equals(b.getText()))
			{
				return b;
			}
		}
		throw new AssertionError("button not found: " + text);
	}

	private static RogueScapeCustomRoomEditorState stateWith(int... regionIds)
	{
		RogueScapeCustomRoomSelection sel = new RogueScapeCustomRoomSelection("My Zone");
		for (int id : regionIds)
		{
			sel.addRegion(id);
		}
		return new RogueScapeCustomRoomEditorState(sel);
	}

	@Test
	public void seedsTheNameFieldFromTheSelection()
	{
		RogueScapeCustomRoomSelection sel = new RogueScapeCustomRoomSelection("Prebuilt Zone");
		ZoneBuilderSection section = new ZoneBuilderSection(new RogueScapeCustomRoomEditorState(sel), () -> { }, () -> { });
		JPanel tab = section.buildTab();
		assertEquals("Prebuilt Zone", collect(tab, JTextField.class).get(0).getText());
	}

	@Test
	public void updateReflectsSelectionCountAndRegionIds()
	{
		RogueScapeCustomRoomEditorState state = stateWith(12850, 12851);
		ZoneBuilderSection section = new ZoneBuilderSection(state, () -> { }, () -> { });
		JPanel tab = section.buildTab();
		section.update();

		List<JTextArea> areas = collect(tab, JTextArea.class); // [status, regions], in build order
		assertEquals(2, areas.size());
		assertTrue(areas.get(0).getText().contains("Selected: 2 region(s)"));
		assertTrue(areas.get(1).getText().contains("12850"));
		assertTrue(areas.get(1).getText().contains("12851"));

		assertTrue(button(tab, "Clear regions").isEnabled());
		assertTrue(button(tab, "Use zone for current run").isEnabled());
	}

	@Test
	public void emptySelectionDisablesClearAndUse()
	{
		ZoneBuilderSection section = new ZoneBuilderSection(stateWith(), () -> { }, () -> { });
		JPanel tab = section.buildTab();
		section.update();

		assertFalse(button(tab, "Clear regions").isEnabled());
		assertFalse(button(tab, "Use zone for current run").isEnabled());
	}

	@Test
	public void toggleButtonFlipsEditingStateAndLabel()
	{
		RogueScapeCustomRoomEditorState state = stateWith();
		ZoneBuilderSection section = new ZoneBuilderSection(state, () -> { }, () -> { });
		JPanel tab = section.buildTab();

		JButton toggle = button(tab, "Start adding regions");
		assertFalse(state.isEditing());
		toggle.doClick();
		assertTrue(state.isEditing());
		assertEquals("Stop adding regions", toggle.getText());
	}

	@Test
	public void saveSyncsZoneNameAndFiresSaveRequest()
	{
		RogueScapeCustomRoomEditorState state = stateWith(12850);
		boolean[] saved = {false};
		ZoneBuilderSection section = new ZoneBuilderSection(state, () -> saved[0] = true, () -> { });
		JPanel tab = section.buildTab();

		collect(tab, JTextField.class).get(0).setText("Castle Wars");
		button(tab, "Save Zone").doClick();

		assertTrue("save request should fire", saved[0]);
		assertEquals("Castle Wars", state.selection().getName());
		// the listener also records a status note via markChanged and refreshes the readout (update ran)
		assertTrue(collect(tab, JTextArea.class).get(0).getText().contains("saved"));
	}
}
