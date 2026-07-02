package com.pluginideahub.roguescape.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;
import net.runelite.client.ui.PluginPanel;

/**
 * A run-mode choice painted as a signed contract on the journal page: a slightly tilted paper
 * card pinned with a tack, a colored wax seal, an ink title and italic subtitle, and — when
 * this is the signed contract — a red ink ring around the seal plus a tilted CHOSEN stamp.
 *
 * <p>Stays a {@link JButton} so the existing click/action wiring and the layout tests
 * ({@code roguescape.modeTile.*} + doClick) keep working; only the paint is custom.
 */
public class ContractCard extends JButton
{
	private static final int HEIGHT = 50;

	private final String contractTitle;
	private final String subtitle;
	private final Color seal;
	private final double tiltDeg;
	private boolean signed;

	public ContractCard(String title, String subtitle, Color seal, double tiltDeg)
	{
		this.contractTitle = title;
		this.subtitle = subtitle;
		this.seal = seal;
		this.tiltDeg = tiltDeg;

		setOpaque(false);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setAlignmentX(LEFT_ALIGNMENT);
		Dimension d = new Dimension(PluginPanel.PANEL_WIDTH - 24, HEIGHT);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(title + " — " + subtitle);
	}

	/** Marks this as the chosen contract (ring + CHOSEN stamp). */
	public void setSigned(boolean signed)
	{
		if (this.signed != signed)
		{
			this.signed = signed;
			repaint();
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();
		int pad = 4;
		int x = pad;
		int y = pad;
		int cw = w - pad * 2;
		int ch = h - pad * 2;

		// A tiny tilt sells the "pinned paper" feel; the pad keeps corners from clipping.
		g.rotate(Math.toRadians(tiltDeg), w / 2.0, h / 2.0);

		RogueScapePaper.card(g, x, y, cw, ch, signed);

		// Tack pin at the top edge.
		g.setColor(RogueScapeTheme.INK_FADED);
		g.fillOval(x + cw / 2 - 3, y - 2, 7, 7);
		g.setColor(new Color(255, 255, 255, 80));
		g.fillOval(x + cw / 2 - 2, y - 1, 3, 3);

		// Wax seal on the left; signing draws a red ink ring around it.
		int sealCx = x + 22;
		int sealCy = y + ch / 2;
		RogueScapePaper.waxSeal(g, sealCx, sealCy, 12, seal);
		if (signed)
		{
			g.setColor(RogueScapeTheme.STAMP);
			g.setStroke(new BasicStroke(2.2f));
			g.drawOval(sealCx - 16, sealCy - 16, 32, 32);
		}

		// Title + subtitle in ink.
		g.setFont(new Font(Font.SERIF, Font.BOLD, 14));
		g.setColor(RogueScapeTheme.INK);
		g.drawString(contractTitle, x + 44, y + 21);
		g.setFont(new Font(Font.SERIF, Font.ITALIC, 10));
		g.setColor(RogueScapeTheme.INK_FADED);
		g.drawString(subtitle, x + 42, y + 37);

		// CHOSEN stamp, tilted, top-right.
		if (signed)
		{
			Graphics2D s = (Graphics2D) g.create();
			s.rotate(Math.toRadians(-8), x + cw - 32, y + 14);
			s.setFont(new Font(Font.SERIF, Font.BOLD, 10));
			s.setColor(RogueScapeTheme.STAMP);
			s.drawString("CHOSEN", x + cw - 52, y + 17);
			s.dispose();
		}

		g.dispose();
	}
}
