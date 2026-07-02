package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.api.Client;
import net.runelite.client.util.ImageUtil;

/**
 * Generates the widget window's "adventurer's journal" skin at runtime and registers it as
 * client sprite overrides. Everything is painted with the same {@link RogueScapePaper} toolkit
 * the Graphics2D book uses, so the widget window can never drift from the painted look — there
 * are no bundled art assets to fall out of date.
 *
 * <p>Sprite ids are negative (custom-override range) so they can't collide with cache sprites.
 * {@link #register(Client)} must run on the client thread; {@link #unregister(Client)} removes
 * the overrides again on plugin shutdown.
 */
public final class RogueScapeWidgetSkin
{
	private RogueScapeWidgetSkin()
	{
	}

	/** The full open-book spread background (paper, burnt edges, spine, page stacks). */
	public static final int SPRITE_BOOK_BG = -21870;
	public static final int SPRITE_SEAL_RED = -21871;
	public static final int SPRITE_SEAL_GREEN = -21872;
	public static final int SPRITE_SEAL_BLUE = -21873;
	public static final int SPRITE_SEAL_GOLD = -21874;
	public static final int SPRITE_RIBBON = -21875;

	/** Book spread size — matches the painted book window (680x430). */
	public static final int BOOK_W = 680;
	public static final int BOOK_H = 430;
	/** Where the two-page chrome starts/stops vertically (below the masthead, above the margin). */
	public static final int PAGE_TOP = 34;
	public static final int PAGE_BOTTOM = BOOK_H - 14;

	private static final int SEAL_SIZE = 26;

	/** Registers every skin sprite as an override. Client thread only. */
	public static void register(Client client)
	{
		put(client, SPRITE_BOOK_BG, bookBackground(BOOK_W, BOOK_H));
		put(client, SPRITE_SEAL_RED, RogueScapePaper.waxSealImage(SEAL_SIZE, RogueScapeTheme.WAX_RED));
		put(client, SPRITE_SEAL_GREEN, RogueScapePaper.waxSealImage(SEAL_SIZE, RogueScapeTheme.WAX_GREEN));
		put(client, SPRITE_SEAL_BLUE, RogueScapePaper.waxSealImage(SEAL_SIZE, RogueScapeTheme.WAX_BLUE));
		put(client, SPRITE_SEAL_GOLD, RogueScapePaper.waxSealImage(SEAL_SIZE, RogueScapeTheme.WAX_GOLD));
		put(client, SPRITE_RIBBON, ribbonImage(16, 34));
	}

	/** Removes the overrides again (plugin shutdown). Client thread only. */
	public static void unregister(Client client)
	{
		client.getSpriteOverrides().remove(SPRITE_BOOK_BG);
		client.getSpriteOverrides().remove(SPRITE_SEAL_RED);
		client.getSpriteOverrides().remove(SPRITE_SEAL_GREEN);
		client.getSpriteOverrides().remove(SPRITE_SEAL_BLUE);
		client.getSpriteOverrides().remove(SPRITE_SEAL_GOLD);
		client.getSpriteOverrides().remove(SPRITE_RIBBON);
	}

	/** The seal sprite id for a wax color, defaulting to gold for unknown colors. */
	public static int sealSprite(Color wax)
	{
		if (RogueScapeTheme.WAX_RED.equals(wax) || RogueScapeTheme.STAMP.equals(wax) || RogueScapeTheme.RIBBON.equals(wax))
		{
			return SPRITE_SEAL_RED;
		}
		if (RogueScapeTheme.WAX_GREEN.equals(wax) || RogueScapeTheme.POSITIVE.equals(wax))
		{
			return SPRITE_SEAL_GREEN;
		}
		if (RogueScapeTheme.WAX_BLUE.equals(wax) || RogueScapeTheme.INFO.equals(wax))
		{
			return SPRITE_SEAL_BLUE;
		}
		return SPRITE_SEAL_GOLD;
	}

	private static void put(Client client, int id, BufferedImage image)
	{
		client.getSpriteOverrides().put(id, ImageUtil.getImageSpritePixels(image, client));
	}

	/**
	 * Paints the full book-spread background: aged paper with burnt edges, a masthead ink rule,
	 * the centre spine (page-curl shading, crease, binding stitches) and the stacked page edges.
	 * A direct port of the painted window's book chrome, baked into one image.
	 */
	public static BufferedImage bookBackground(int w, int h)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		RogueScapePaper.page(g, 0, 0, w, h);
		RogueScapePaper.inkRule(g, 14, PAGE_TOP - 4, w - 28);

		int top = PAGE_TOP;
		int bottom = h - 14;
		int cx = w / 2;

		// Page-curl: both pages dip toward the gutter — shadow deepens toward the crease.
		for (int i = 22; i >= 1; i--)
		{
			int a = (int) (44 * (1 - (i - 1) / 22.0));
			g.setColor(new Color(0x24, 0x18, 0x0C, Math.max(0, a)));
			g.drawLine(cx - i, top, cx - i, bottom);
			g.drawLine(cx + i, top, cx + i, bottom);
		}
		// The crease, with a thin paper highlight to either side.
		g.setColor(new Color(0x22, 0x16, 0x0A));
		g.drawLine(cx, top, cx, bottom);
		g.setColor(new Color(255, 255, 255, 70));
		g.drawLine(cx - 2, top, cx - 2, bottom);
		g.drawLine(cx + 2, top, cx + 2, bottom);

		// Binding stitches down the spine.
		g.setStroke(new java.awt.BasicStroke(1.5f));
		g.setColor(new Color(0x6E, 0x5A, 0x3E, 150));
		for (int sy = top + 16; sy < bottom - 10; sy += 24)
		{
			g.drawLine(cx - 3, sy, cx + 3, sy);
		}
		g.setStroke(new java.awt.BasicStroke(1f));

		// Outer page-stack edges — the book's thickness seen from the side.
		for (int i = 0; i < 4; i++)
		{
			g.setColor(new Color(0x24, 0x18, 0x0C, Math.max(8, 38 - i * 8)));
			g.drawLine(5 + i, top + 4, 5 + i, bottom - 4);
			g.drawLine(w - 6 - i, top + 4, w - 6 - i, bottom - 4);
		}

		g.dispose();
		return img;
	}

	/** The bookmark ribbon rendered to a transparent image. */
	public static BufferedImage ribbonImage(int w, int h)
	{
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RogueScapePaper.ribbon(g, w / 2, 0, w - 2, h - 1);
		g.dispose();
		return img;
	}
}
