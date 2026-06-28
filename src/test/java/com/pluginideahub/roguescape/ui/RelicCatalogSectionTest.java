package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes {@link RelicCatalogSection} — the read-only relic/modifier catalog section lifted out
 * of {@code RogueScapePanel}. Pins that it builds a section, shows the correct count headers, hosts the
 * list in a scroll pane, and renders an entry for every relic and modifier from the libraries.
 */
public class RelicCatalogSectionTest
{
	private static CollapsibleSection expandedSection()
	{
		CollapsibleSection section = RelicCatalogSection.build();
		section.setCollapsed(false);
		return section;
	}

	private static List<String> labelTexts(Component root)
	{
		List<JLabel> labels = new ArrayList<>();
		collect(root, JLabel.class, labels);
		List<String> texts = new ArrayList<>();
		for (JLabel l : labels)
		{
			texts.add(l.getText());
		}
		return texts;
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

	@Test
	public void buildsASectionWithBothCountHeaders()
	{
		CollapsibleSection section = expandedSection();
		assertNotNull(section);

		List<String> texts = labelTexts(section);
		assertTrue(texts.contains("RELICS (" + RelicLibrary.all().size() + ")"));
		assertTrue(texts.contains("MODIFIERS / CURSES (" + ModifierLibrary.all().size() + ")"));
	}

	@Test
	public void rendersAnEntryForEveryRelicAndModifier()
	{
		List<String> texts = labelTexts(expandedSection());

		for (Relic r : RelicLibrary.all())
		{
			assertTrue("missing relic name: " + r.name(), texts.contains(r.name()));
		}
		for (Relic r : ModifierLibrary.all())
		{
			assertTrue("missing modifier name: " + r.name(), texts.contains(r.name()));
		}
	}

	@Test
	public void hostsTheCatalogInsideAScrollPane()
	{
		List<JScrollPane> scrolls = new ArrayList<>();
		collect(expandedSection(), JScrollPane.class, scrolls);
		assertFalse(scrolls.isEmpty());

		// the catalog list (with its count headers) must live INSIDE the scroll pane's viewport,
		// not be added to the section directly
		List<String> scrollLabels = labelTexts(scrolls.get(0).getViewport());
		assertTrue(scrollLabels.contains("RELICS (" + RelicLibrary.all().size() + ")"));
	}
}
