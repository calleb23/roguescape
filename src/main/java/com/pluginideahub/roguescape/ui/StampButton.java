package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/**
 * A button painted as a tilted rubber stamp: a rough double border in the role's ink, a faint
 * inked fill that deepens on hover, punched paper grain, and centered ink text. Used for the
 * journal's loud actions — BEGIN THE RUN, ABANDON RUN, TAKE IT.
 */
public class StampButton extends JButton
{
	private final Color ink;
	private final double tiltDeg;
	private boolean hover;

	public StampButton(String text, Color ink, double tiltDeg)
	{
		super(text);
		this.ink = ink;
		this.tiltDeg = tiltDeg;
		setOpaque(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setForeground(ink);
		setFont(new Font(Font.SERIF, Font.BOLD, 15));
		setAlignmentX(LEFT_ALIGNMENT);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
		setPreferredSize(new Dimension(160, 38));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addChangeListener(e ->
		{
			boolean h = getModel().isRollover();
			if (h != hover)
			{
				hover = h;
				repaint();
			}
		});
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();
		int pad = 3;
		g.rotate(Math.toRadians(tiltDeg), w / 2.0, h / 2.0);
		RogueScapePaper.stamp(g, pad, pad, w - pad * 2, h - pad * 2, ink, hover);

		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		String text = getText();
		g.setColor(ink);
		g.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2 + fm.getAscent() / 2 - 2);
		g.dispose();
	}
}
