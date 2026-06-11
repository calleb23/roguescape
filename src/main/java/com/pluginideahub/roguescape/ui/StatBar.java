package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import net.runelite.client.ui.PluginPanel;

/**
 * A labeled progress/stat bar painted to match the asset sheet's HP/Prayer/Synergy/Progress
 * bars: a dark rounded track, a colored fill, and centered overlay text. Pure code rendering;
 * no image assets.
 */
public class StatBar extends JComponent
{
	private static final int HEIGHT = 16;
	private static final int ARC = 6;

	private final Color fillColor;
	private double fraction;
	private String text = "";

	public StatBar(Color fillColor)
	{
		this.fillColor = fillColor;
		setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, HEIGHT));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
		setAlignmentX(LEFT_ALIGNMENT);
	}

	/** Sets fill fraction (clamped 0..1) and the centered overlay text. */
	public void setValue(double fraction, String text)
	{
		this.fraction = Math.max(0.0, Math.min(1.0, fraction));
		this.text = text == null ? "" : text;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		try
		{
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int w = getWidth();
			int h = getHeight();

			// Track
			g2.setColor(RogueScapeTheme.BAR_TRACK);
			g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

			// Fill
			int fillW = (int) Math.round((w - 2) * fraction);
			if (fillW > 0)
			{
				g2.setColor(fillColor);
				g2.fillRoundRect(1, 1, fillW, h - 3, ARC, ARC);
				// subtle top highlight
				g2.setColor(RogueScapeTheme.lighten(fillColor, 30));
				g2.fillRoundRect(1, 1, fillW, Math.max(2, (h - 3) / 2), ARC, ARC);
			}

			// Border
			g2.setColor(RogueScapeTheme.BORDER);
			g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

			// Text
			if (!text.isEmpty())
			{
				g2.setFont(RogueScapeTheme.value(getFont() == null ? g2.getFont() : getFont()));
				int tw = g2.getFontMetrics().stringWidth(text);
				int tx = (w - tw) / 2;
				int ty = (h + g2.getFontMetrics().getAscent()) / 2 - 1;
				g2.setColor(Color.BLACK);
				g2.drawString(text, tx + 1, ty + 1);
				g2.setColor(RogueScapeTheme.TEXT_PRIMARY);
				g2.drawString(text, tx, ty);
			}
		}
		finally
		{
			g2.dispose();
		}
	}
}
