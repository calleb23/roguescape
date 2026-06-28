package com.pluginideahub.roguescape.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.runelite.client.ui.PluginPanel;

/**
 * Stateless factory for the RogueScape side panel's leaf Swing widgets — styled rows, labels, boxes
 * and spacers built purely from their arguments plus {@link RogueScapeTheme}. Lifted out of
 * {@code RogueScapePanel} (which keeps thin delegating wrappers) so the widget construction is small,
 * reusable across the panel and the in-game builder window, and free of the panel's mutable state.
 */
public final class PanelWidgetFactory
{
	private PanelWidgetFactory() {}

	/** Minimal HTML-escape for text placed into Swing {@code <html>} labels. */
	public static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static JPanel vGap(int h)
	{
		JPanel p = new JPanel();
		p.setBackground(RogueScapeTheme.SECTION_BG);
		p.setPreferredSize(new Dimension(1, h));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		return p;
	}

	public static JLabel fieldLabel(String text)
	{
		JLabel lbl = new JLabel(text.toUpperCase());
		lbl.setForeground(RogueScapeTheme.ACCENT);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		return lbl;
	}

	public static JPanel mutedRow(String text)
	{
		JLabel lbl = new JLabel("<html><body style='width:160px'>" + escape(text) + "</body></html>");
		lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	/** A single detail line (e.g. "You CAN:", "  ✓ Fight monsters"). */
	public static JPanel detailRow(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));
		if (text.startsWith("You CAN") || text.startsWith("You CANNOT"))
		{
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
			lbl.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		}
		else if (text.contains("✓"))
		{
			lbl.setForeground(RogueScapeTheme.POSITIVE);
		}
		else if (text.contains("✗"))
		{
			lbl.setForeground(RogueScapeTheme.NEGATIVE);
		}
		else
		{
			lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		}

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	/** A label-left / value-right stat row for the LIVE RUN section. */
	public static JPanel statRow(String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

		JLabel l = new JLabel(label);
		l.setForeground(RogueScapeTheme.TEXT_MUTED);
		l.setFont(RogueScapeTheme.label(l.getFont()));
		row.add(l, BorderLayout.WEST);

		JLabel v = new JLabel(value);
		v.setForeground(valueColor);
		v.setFont(RogueScapeTheme.value(v.getFont()));
		row.add(v, BorderLayout.EAST);
		return row;
	}

	public static JPanel keyValueRow(String line)
	{
		int idx = line.indexOf(": ");
		if (idx <= 0)
		{
			return detailRow(line);
		}
		String label = line.substring(0, idx);
		String value = line.substring(idx + 2);
		String lower = value.toLowerCase();
		Color color = RogueScapeTheme.TEXT_PRIMARY;
		if (lower.contains("illegal"))
		{
			color = RogueScapeTheme.NEGATIVE;
		}
		else if (lower.equals("off") || lower.equals("allowed"))
		{
			color = RogueScapeTheme.TEXT_MUTED;
		}
		else if (lower.equals("on") || lower.equals("flagged"))
		{
			color = RogueScapeTheme.ACCENT;
		}
		return statRow(label, value, color);
	}

	public static JPanel routeRow(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));
		if (text.startsWith("✓"))
		{
			lbl.setForeground(RogueScapeTheme.POSITIVE);
		}
		else if (text.startsWith("▶"))
		{
			lbl.setForeground(RogueScapeTheme.ACCENT);
		}
		else
		{
			lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		}
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	public static JPanel hintBox(String[] lines)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER),
			BorderFactory.createEmptyBorder(5, 8, 5, 8)
		));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (String line : lines)
		{
			JLabel hl = new JLabel(line);
			hl.setForeground(RogueScapeTheme.TEXT_MUTED);
			hl.setFont(hl.getFont().deriveFont(Font.PLAIN, 10f));
			box.add(hl);
		}
		return box;
	}

	public static JPanel featureBox(String title, String[] lines)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER_BRIGHT),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel head = new JLabel(title);
		head.setForeground(RogueScapeTheme.GOLD);
		head.setFont(RogueScapeTheme.value(head.getFont()).deriveFont(Font.BOLD));
		box.add(head);
		box.add(Box.createVerticalStrut(3));
		for (String line : lines)
		{
			JLabel row = new JLabel("<html><body style='width:170px'>" + escape(line) + "</body></html>");
			row.setForeground(line.contains("[BOSS]") ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_PRIMARY);
			row.setFont(RogueScapeTheme.label(row.getFont()));
			box.add(row);
		}
		return box;
	}

	public static JScrollPane readOnlyArea(JTextArea area, Color fg, int height)
	{
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		area.setForeground(fg);
		area.setFont(RogueScapeTheme.label(area.getFont()));
		area.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(BorderFactory.createLineBorder(RogueScapeTheme.BORDER));
		scroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, height));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		return scroll;
	}
}
