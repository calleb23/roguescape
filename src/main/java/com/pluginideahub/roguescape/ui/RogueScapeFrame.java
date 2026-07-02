package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * Shared "dungeon panel" skin for RogueScape's custom in-game windows — one place to paint the
 * background and the ornate frame so the window overlay and the reward window read identically and
 * retune together.
 *
 * <p>Pure {@link Graphics2D} (no image assets), but layered to actually read as a framed panel:
 * a vertical background gradient with an edge vignette for depth, then a chiselled gold frame with
 * a top-left highlight / bottom-right shadow bevel and L-shaped corner brackets. If/when designed
 * PNG art (9-slice frame + stone texture) is bundled, only this class changes.
 */
public final class RogueScapeFrame
{
	private RogueScapeFrame()
	{
	}

	static final Color SHADOW = new Color(0x08, 0x06, 0x04);

	/** Fills an aged-paper page (cached texture blit). */
	public static void background(Graphics2D g, int x, int y, int w, int h)
	{
		g.drawImage(RogueScapePaper.sheet(w, h), x, y, x + w, y + h, 0, 0, w, h, null);
	}

	private static void backgroundPattern(Graphics2D g, int x, int y, int w, int h)
	{
		Color line = new Color(255, 255, 255, 10);
		Color shadow = new Color(0, 0, 0, 22);
		int tile = 34;

		g.setColor(line);
		for (int px = x + tile; px < x + w; px += tile)
		{
			g.drawLine(px, y + 4, px, y + h - 5);
		}
		for (int py = y + tile; py < y + h; py += tile)
		{
			g.drawLine(x + 4, py, x + w - 5, py);
		}

		// Offset short strokes make the field feel like worn stone instead of a perfect grid.
		g.setColor(shadow);
		for (int py = y + 18; py < y + h; py += tile)
		{
			for (int px = x + 12; px < x + w; px += tile * 2)
			{
				g.drawLine(px, py, Math.min(x + w - 6, px + 16), py + 8);
			}
		}

		g.setColor(new Color(RogueScapeTheme.ACCENT.getRed(), RogueScapeTheme.ACCENT.getGreen(),
			RogueScapeTheme.ACCENT.getBlue(), 8));
		for (int py = y + 26; py < y + h; py += tile * 2)
		{
			g.drawLine(x + 12, py, x + w - 13, py - 10);
		}
	}

	/** Burnt page edge with a plain wood rim — the journal cover seen edge-on. */
	public static void frame(Graphics2D g, int x, int y, int w, int h)
	{
		g.setColor(SHADOW);
		g.drawRect(x, y, w - 1, h - 1);
		g.setColor(RogueScapeTheme.EDGE);
		g.drawRect(x + 1, y + 1, w - 3, h - 3);
		RogueScapePaper.burntEdge(g, x + 2, y + 2, w - 4, h - 4);
	}

	/** Small rune-like ornaments for testing a more magical frame silhouette. */
	public static void edgeOrnaments(Graphics2D g, int x, int y, int w, int h)
	{
		int x1 = x + w - 1;
		int y1 = y + h - 1;
		int corner = 10;
		Color glow = new Color(RogueScapeTheme.ACCENT.getRed(), RogueScapeTheme.ACCENT.getGreen(),
			RogueScapeTheme.ACCENT.getBlue(), 70);

		// Tiny glow pips near each corner to make the border feel built, not just outlined.
		g.setColor(glow);
		g.fillOval(x + corner - 9, y + corner - 9, 18, 18);
		g.fillOval(x1 - corner - 9, y + corner - 9, 18, 18);
		g.fillOval(x + corner - 9, y1 - corner - 9, 18, 18);
		g.fillOval(x1 - corner - 9, y1 - corner - 9, 18, 18);

		// Corner gems, pulled just inside the frame so they work on overlays and previews.
		drawDiamond(g, x + corner, y + corner, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
		drawDiamond(g, x1 - corner, y + corner, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
		drawDiamond(g, x + corner, y1 - corner, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);
		drawDiamond(g, x1 - corner, y1 - corner, 7, RogueScapeTheme.ACCENT_DIM, RogueScapeTheme.ACCENT);

		// Mid-edge notches: subtle gold diamonds connected by short chiselled ticks.
		drawEdgeNotch(g, x + w / 2, y + 3, true);
		drawEdgeNotch(g, x + w / 2, y1 - 3, true);
		drawEdgeNotch(g, x + 3, y + h / 2, false);
		drawEdgeNotch(g, x1 - 3, y + h / 2, false);
	}

	/** Bright gold L-brackets hugging each corner — the ornate accent that flat borders lack. */
	private static void cornerBrackets(Graphics2D g, int x, int y, int w, int h)
	{
		int s = 9;
		int x1 = x + w - 1;
		int y1 = y + h - 1;
		g.setColor(RogueScapeTheme.GOLD);
		// Top-left.
		g.drawLine(x + 1, y + 1, x + s, y + 1);
		g.drawLine(x + 1, y + 1, x + 1, y + s);
		// Top-right.
		g.drawLine(x1 - s, y + 1, x1 - 1, y + 1);
		g.drawLine(x1 - 1, y + 1, x1 - 1, y + s);
		// Bottom-left.
		g.drawLine(x + 1, y1 - s, x + 1, y1 - 1);
		g.drawLine(x + 1, y1 - 1, x + s, y1 - 1);
		// Bottom-right.
		g.drawLine(x1 - s, y1 - 1, x1 - 1, y1 - 1);
		g.drawLine(x1 - 1, y1 - s, x1 - 1, y1 - 1);
	}

	private static void drawEdgeNotch(Graphics2D g, int cx, int cy, boolean horizontal)
	{
		drawDiamond(g, cx, cy, 4, RogueScapeTheme.GOLD_DIM, RogueScapeTheme.GOLD);
		g.setColor(RogueScapeTheme.BORDER_BRIGHT);
		if (horizontal)
		{
			g.drawLine(cx - 24, cy, cx - 9, cy);
			g.drawLine(cx + 9, cy, cx + 24, cy);
		}
		else
		{
			g.drawLine(cx, cy - 24, cx, cy - 9);
			g.drawLine(cx, cy + 9, cx, cy + 24);
		}
	}

	private static void drawDiamond(Graphics2D g, int cx, int cy, int r, Color outer, Color inner)
	{
		g.setColor(new Color(inner.getRed(), inner.getGreen(), inner.getBlue(), 55));
		g.fillOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);

		Polygon p = new Polygon();
		p.addPoint(cx, cy - r);
		p.addPoint(cx + r, cy);
		p.addPoint(cx, cy + r);
		p.addPoint(cx - r, cy);
		g.setColor(outer);
		g.fillPolygon(p);

		int ir = Math.max(1, r - 3);
		Polygon q = new Polygon();
		q.addPoint(cx, cy - ir);
		q.addPoint(cx + ir, cy);
		q.addPoint(cx, cy + ir);
		q.addPoint(cx - ir, cy);
		g.setColor(inner);
		g.fillPolygon(q);
	}

	/** Header gradient bar with a gold base rule + a shadow line beneath it. */
	public static void headerBar(Graphics2D g, int x, int y, int w, int h)
	{
		g.setPaint(new GradientPaint(0, y, lighten(RogueScapeTheme.SECTION_HEADER_BG, 10),
			0, y + h, RogueScapeTheme.PANEL_BG));
		g.fillRect(x, y, w, h);
		g.setColor(RogueScapeTheme.BORDER_BRIGHT);
		g.drawLine(x, y + h, x + w - 1, y + h);
		g.setColor(SHADOW);
		g.drawLine(x, y + h + 1, x + w - 1, y + h + 1);
	}

	public static Color darken(Color c, int amount)
	{
		return new Color(
			Math.max(0, c.getRed() - amount),
			Math.max(0, c.getGreen() - amount),
			Math.max(0, c.getBlue() - amount));
	}

	public static Color lighten(Color c, int amount)
	{
		return RogueScapeTheme.lighten(c, amount);
	}
}
