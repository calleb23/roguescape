package com.pluginideahub.roguescape.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.PluginPanel;

/**
 * A titled section with a clickable header that collapses/expands its content.
 *
 * <p>This is the reusable building block for the vertical RogueScape side panel:
 * every section (RUN CONTROL, LIVE RUN, BUILD, ...) is one of these. Callers add
 * their widgets to {@link #content()} and never touch the header chrome.
 */
public class CollapsibleSection extends JPanel
{
	private final JPanel content = new JPanel();
	private final JLabel toggle = new JLabel();
	private final String title;
	private boolean collapsed;

	public CollapsibleSection(String title)
	{
		this(title, false);
	}

	public CollapsibleSection(String title, boolean collapsed)
	{
		this.title = title;
		this.collapsed = collapsed;

		setLayout(new BorderLayout());
		setBackground(RogueScapeTheme.PANEL_BG);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		add(buildHeader(), BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(RogueScapeTheme.SECTION_BG);
		content.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 1, 1, 1, RogueScapeTheme.BORDER),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));
		add(content, BorderLayout.CENTER);

		applyCollapsedState();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		header.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER),
			BorderFactory.createEmptyBorder(4, 8, 4, 8)
		));
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel titleLabel = new JLabel("✦ " + title.toUpperCase());
		titleLabel.setForeground(RogueScapeTheme.ACCENT);
		titleLabel.setFont(RogueScapeTheme.sectionTitle(titleLabel.getFont()));
		header.add(titleLabel, BorderLayout.WEST);

		toggle.setForeground(RogueScapeTheme.TEXT_MUTED);
		toggle.setFont(RogueScapeTheme.sectionTitle(toggle.getFont()));
		header.add(toggle, BorderLayout.EAST);

		header.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				setCollapsed(!collapsed);
			}
		});
		return header;
	}

	/** Container callers add their section widgets to. */
	public JPanel content()
	{
		return content;
	}

	public boolean isCollapsed()
	{
		return collapsed;
	}

	public void setCollapsed(boolean collapsed)
	{
		this.collapsed = collapsed;
		applyCollapsedState();
		revalidate();
		repaint();
	}

	/** Clears all widgets from the content area. */
	public void clearContent()
	{
		content.removeAll();
	}

	private void applyCollapsedState()
	{
		content.setVisible(!collapsed);
		toggle.setText(collapsed ? "▸" : "▾");
		// Section width should track the sidebar; height is content-driven.
		setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, Integer.MAX_VALUE));
	}
}
