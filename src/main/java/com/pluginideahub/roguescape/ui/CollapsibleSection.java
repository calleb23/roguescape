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
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		add(buildHeader(), BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setOpaque(false);
		content.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		add(content, BorderLayout.CENTER);

		applyCollapsedState();
	}

	private JPanel buildHeader()
	{
		// Journal style: a centered "— TITLE —" ink line over transparent paper.
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 8));
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel titleLabel = new JLabel("— " + title.toUpperCase() + " —", JLabel.CENTER);
		titleLabel.setForeground(RogueScapeTheme.INK);
		titleLabel.setFont(RogueScapeTheme.sectionTitle(titleLabel.getFont()));
		header.add(titleLabel, BorderLayout.CENTER);

		toggle.setForeground(RogueScapeTheme.INK_FADED);
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
	}

	@Override
	public Dimension getMaximumSize()
	{
		// Width tracks the sidebar; height stays content-driven so a stretching BoxLayout
		// gives spare vertical space to the trailing glue instead of opening gaps between
		// sections (or inside collapsed ones).
		return new Dimension(PluginPanel.PANEL_WIDTH, getPreferredSize().height);
	}
}
