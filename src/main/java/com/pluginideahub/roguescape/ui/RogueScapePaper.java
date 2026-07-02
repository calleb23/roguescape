package com.pluginideahub.roguescape.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Painting toolkit for the "adventurer's journal" look: aged paper, ink rules, wax seals,
 * rubber stamps, ribbon bookmarks. All texture work is cached so per-frame painting is a
 * plain image blit plus a few strokes.
 */
public final class RogueScapePaper
{
	private RogueScapePaper()
	{
	}

	/** Deterministic grain so the page doesn't shimmer between repaints. */
	private static final long GRAIN_SEED = 7L;

	private static BufferedImage cachedSheet;
	private static int cachedW;
	private static int cachedH;

	/** Cached aged-paper sheet of at least the requested size (regenerated only on growth). */
	public static BufferedImage sheet(int w, int h)
	{
		if (cachedSheet == null || cachedW < w || cachedH < h)
		{
			cachedW = Math.max(w, cachedW);
			cachedH = Math.max(h, cachedH);
			cachedSheet = paintSheet(cachedW, cachedH);
		}
		return cachedSheet;
	}

	private static BufferedImage paintSheet(int w, int h)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		Random rng = new Random(GRAIN_SEED);
		g.setColor(RogueScapeTheme.PAPER);
		g.fillRect(0, 0, w, h);
		// fibre grain
		for (int i = 0; i < w * h / 45; i++)
		{
			int px = rng.nextInt(w), py = rng.nextInt(h);
			g.setColor(new Color(0x6E5A3E & 0xFFFFFF | (10 + rng.nextInt(26)) << 24, true));
			g.fillRect(px, py, 1 + rng.nextInt(2), 1);
		}
		// water blotches
		for (int i = 0; i < Math.max(4, w * h / 26000); i++)
		{
			int sx = rng.nextInt(w), sy = rng.nextInt(h), sr = 22 + rng.nextInt(52);
			g.setColor(new Color(0x8A744E & 0xFFFFFF | (10 + rng.nextInt(14)) << 24, true));
			g.fillOval(sx - sr, sy - sr / 2, sr * 2, sr);
		}
		// foxing spots
		for (int i = 0; i < Math.max(8, w * h / 2600); i++)
		{
			int sx = rng.nextInt(w), sy = rng.nextInt(h), sr = 2 + rng.nextInt(4);
			g.setColor(new Color(0x7A5230 & 0xFFFFFF | (26 + rng.nextInt(40)) << 24, true));
			g.fillOval(sx, sy, sr, sr);
		}
		g.dispose();
		return img;
	}

	/** Blits aged paper over the region and burns the edges. */
	public static void page(Graphics2D g, int x, int y, int w, int h)
	{
		g.drawImage(sheet(w, h), x, y, x + w, y + h, 0, 0, w, h, null);
		burntEdge(g, x, y, w, h);
	}

	/** Uneven darkened border with ragged nicks, deterministic per geometry. */
	public static void burntEdge(Graphics2D g, int x, int y, int w, int h)
	{
		Random rng = new Random(GRAIN_SEED ^ (w * 31L + h));
		for (int i = 0; i < 3; i++)
		{
			g.setStroke(new BasicStroke(3.5f - i));
			g.setColor(new Color(0x5C4326 & 0xFFFFFF | (46 - i * 14) << 24, true));
			g.drawRoundRect(x + i * 2, y + i * 2, w - i * 4 - 1, h - i * 4 - 1, 7, 7);
		}
		g.setColor(RogueScapeTheme.EDGE);
		for (int i = 0; i < Math.max(8, (w + h) / 60); i++)
		{
			boolean vert = rng.nextBoolean();
			int ex = vert ? (rng.nextBoolean() ? x : x + w - 2) : x + rng.nextInt(Math.max(1, w));
			int ey = vert ? y + rng.nextInt(Math.max(1, h)) : (rng.nextBoolean() ? y : y + h - 2);
			g.fillRect(ex, ey, vert ? 2 : 3 + rng.nextInt(4), vert ? 3 + rng.nextInt(4) : 2);
		}
		g.setStroke(new BasicStroke(1f));
	}

	/** A lighter inner card of paper (contracts, loot cards, pinned notes). */
	public static void card(Graphics2D g, int x, int y, int w, int h, boolean chosen)
	{
		g.setColor(new Color(0, 0, 0, 50));
		g.fillRect(x + 2, y + 3, w, h);
		g.setColor(RogueScapeTheme.PAPER_CARD);
		g.fillRect(x, y, w, h);
		g.setStroke(new BasicStroke(chosen ? 2.2f : 1.2f));
		g.setColor(chosen ? RogueScapeTheme.STAMP : RogueScapeTheme.EDGE);
		g.drawRect(x, y, w, h);
		g.setStroke(new BasicStroke(1f));
	}

	/** Horizontal ink rule with a small flourish at the middle. */
	public static void inkRule(Graphics2D g, int x, int y, int w)
	{
		g.setStroke(new BasicStroke(1.3f));
		g.setColor(new Color(0x70332414, true));
		g.drawLine(x, y, x + w, y);
		g.drawArc(x + w / 2 - 8, y - 3, 16, 6, 0, 180);
		g.setStroke(new BasicStroke(1f));
	}

	/** Dotted ledger leader line. */
	public static void leader(Graphics2D g, int x1, int y, int x2)
	{
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0));
		g.setColor(new Color(0x40332414, true));
		g.drawLine(x1, y, x2, y);
		g.setStroke(old);
	}

	/** Wax seal blob with rim and embossed cross. */
	public static void waxSeal(Graphics2D g, int cx, int cy, int r, Color wax)
	{
		Random rng = new Random(GRAIN_SEED ^ (cx * 31L + cy));
		g.setColor(new Color(0, 0, 0, 60));
		g.fillOval(cx - r + 1, cy - r + 2, r * 2, r * 2);
		g.setColor(wax);
		g.fillOval(cx - r, cy - r, r * 2, r * 2);
		for (int i = 0; i < 5; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			g.fillOval((int) (cx + Math.cos(a) * r * 0.8) - 3, (int) (cy + Math.sin(a) * r * 0.8) - 3, 7, 7);
		}
		g.setColor(wax.darker());
		g.drawOval(cx - r + 3, cy - r + 3, (r - 3) * 2, (r - 3) * 2);
		g.setColor(new Color(255, 255, 255, 70));
		int sr = Math.max(2, r - 6);
		for (int i = 0; i < 4; i++)
		{
			double a = Math.PI / 4 + i * Math.PI / 2;
			g.drawLine(cx, cy, (int) (cx + Math.cos(a) * sr), (int) (cy + Math.sin(a) * sr));
		}
	}

	/** A wax seal rendered to an image (for Swing icons). */
	public static BufferedImage waxSealImage(int size, Color wax)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		waxSeal(g, size / 2, size / 2, size / 2 - 3, wax);
		g.dispose();
		return img;
	}

	/** Rubber-stamp framed button face: translucent fill, rough double border, punched grain. */
	public static void stamp(Graphics2D g, int x, int y, int w, int h, Color ink, boolean hover)
	{
		Random rng = new Random(GRAIN_SEED ^ (x * 31L + y));
		g.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), hover ? 48 : 28));
		g.fillRoundRect(x, y, w, h, 10, 10);
		g.setStroke(new BasicStroke(2.4f));
		g.setColor(ink);
		g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
		g.setStroke(new BasicStroke(1.1f));
		g.drawRoundRect(x + 4, y + 4, w - 9, h - 9, 7, 7);
		g.setColor(new Color(RogueScapeTheme.PAPER.getRed(), RogueScapeTheme.PAPER.getGreen(), RogueScapeTheme.PAPER.getBlue(), 90));
		for (int i = 0; i < (w * h) / 110; i++)
		{
			g.fillRect(x + 2 + rng.nextInt(Math.max(1, w - 4)), y + 2 + rng.nextInt(Math.max(1, h - 4)), 1, 1);
		}
		g.setStroke(new BasicStroke(1f));
	}

	/** Red ribbon bookmark triangle pointing down at (cx, top..top+len). */
	public static void ribbon(Graphics2D g, int cx, int top, int width, int len)
	{
		int hw = width / 2;
		Polygon p = new Polygon(
			new int[]{cx - hw, cx + hw, cx + hw, cx, cx - hw},
			new int[]{top, top, top + len, top + len - width / 2, top + len},
			5);
		g.setColor(RogueScapeTheme.RIBBON);
		g.fillPolygon(p);
		g.setColor(new Color(0, 0, 0, 60));
		g.drawPolygon(p);
	}

	/** Tilted round arrival stamp reading {@code word}. */
	public static void clearStamp(Graphics2D g, int cx, int cy, int r, String word)
	{
		Graphics2D c = (Graphics2D) g.create();
		c.rotate(Math.toRadians(-14), cx, cy);
		c.setColor(new Color(0x70B03224, true));
		c.setStroke(new BasicStroke(2f));
		c.drawOval(cx - r, cy - r, r * 2, r * 2);
		c.setFont(new Font(Font.SERIF, Font.BOLD, Math.max(7, r - 3)));
		c.setColor(new Color(0xB0B03224, true));
		FontMetrics fm = c.getFontMetrics();
		c.drawString(word, cx - fm.stringWidth(word) / 2, cy + fm.getAscent() / 2 - 1);
		c.dispose();
	}

	/** Score as gate-five tally marks (four uprights + a diagonal), inked at (x, y). */
	public static void tally(Graphics2D g, int x, int y, int count)
	{
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(RogueScapeTheme.INK);
		int groups = count / 5;
		int rest = count % 5;
		int gx = x;
		for (int i = 0; i < groups; i++)
		{
			for (int j = 0; j < 4; j++)
			{
				g.drawLine(gx + j * 5, y, gx + j * 5, y + 14);
			}
			g.drawLine(gx - 3, y + 12, gx + 18, y + 2);
			gx += 28;
		}
		for (int j = 0; j < rest; j++)
		{
			g.drawLine(gx + j * 5, y, gx + j * 5, y + 14);
		}
		g.setStroke(new BasicStroke(1f));
	}

	/** Stitched paper relic pocket; filled pockets hold a wax seal, empty ones an inked "?". */
	public static void pocket(Graphics2D g, int x, int y, int w, int h, Color seal)
	{
		g.setColor(RogueScapeTheme.PAPER_DARK);
		g.fillRoundRect(x, y, w, h, 10, 10);
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 3f}, 0));
		g.setColor(RogueScapeTheme.INK_FADED);
		g.drawRoundRect(x + 3, y + 3, w - 6, h - 6, 8, 8);
		g.setStroke(old);
		if (seal != null)
		{
			waxSeal(g, x + w / 2, y + h / 2, Math.min(w, h) / 2 - 6, seal);
		}
		else
		{
			g.setFont(new Font(Font.SERIF, Font.ITALIC, 18));
			g.setColor(new Color(0x40332414, true));
			FontMetrics fm = g.getFontMetrics();
			g.drawString("?", x + w / 2 - fm.stringWidth("?") / 2, y + h / 2 + 6);
		}
	}

	/** Empty dotted stamp slot awaiting ink. */
	public static void stampSlot(Graphics2D g, int cx, int cy, int r, boolean boss)
	{
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0));
		g.setColor(boss ? new Color(0x80B03224, true) : new Color(0x806E5A3E, true));
		g.drawOval(cx - r, cy - r, r * 2, r * 2);
		g.setStroke(old);
	}
}
