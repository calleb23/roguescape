package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.PluginPanel;

/**
 * Builds the read-only "Relics" side-panel section: the static relic + modifier catalog in a scroll
 * list. Stateless and driven entirely by {@link RelicLibrary}/{@link ModifierLibrary}, so it carries
 * no panel state — lifted out of {@code RogueScapePanel}, building its leaf rows via
 * {@link PanelWidgetFactory}.
 */
public final class RelicCatalogSection
{
	private RelicCatalogSection() {}

	public static CollapsibleSection build()
	{
		CollapsibleSection section = new CollapsibleSection("Relics", true);
		JPanel c = section.content();
		c.add(PanelWidgetFactory.mutedRow("The relic & modifier catalog. Relics grant scoring bonuses; modifiers are curses that only restrict."));
		c.add(PanelWidgetFactory.vGap(6));

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(RogueScapeTheme.SECTION_BG);

		list.add(catalogHeader("RELICS (" + RelicLibrary.all().size() + ")"));
		for (Relic r : RelicLibrary.all())
		{
			list.add(catalogEntry(r));
		}
		list.add(PanelWidgetFactory.vGap(8));
		list.add(catalogHeader("MODIFIERS / CURSES (" + ModifierLibrary.all().size() + ")"));
		for (Relic r : ModifierLibrary.all())
		{
			list.add(catalogEntry(r));
		}

		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createLineBorder(RogueScapeTheme.BORDER));
		scroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, 280));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		scroll.getViewport().setBackground(RogueScapeTheme.SECTION_BG);
		scroll.getVerticalScrollBar().setUnitIncrement(12);
		c.add(scroll);
		return section;
	}

	private static JLabel catalogHeader(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setForeground(RogueScapeTheme.GOLD);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return lbl;
	}

	private static JPanel catalogEntry(Relic relic)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(RogueScapeTheme.SECTION_BG);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));

		JLabel name = new JLabel(relic.name());
		name.setForeground(RogueScapeTheme.ACCENT);
		name.setFont(RogueScapeTheme.value(name.getFont()));
		name.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.add(name);

		JLabel desc = new JLabel("<html><body style='width:165px'>" + PanelWidgetFactory.escape(relic.description()) + "</body></html>");
		desc.setForeground(RogueScapeTheme.TEXT_MUTED);
		desc.setFont(RogueScapeTheme.small(desc.getFont()));
		desc.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.add(desc);
		return p;
	}
}
