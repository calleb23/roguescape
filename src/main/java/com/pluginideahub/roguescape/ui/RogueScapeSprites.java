package com.pluginideahub.roguescape.ui;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.annotation.Nullable;
import net.runelite.client.util.ImageUtil;

/**
 * Loads the bundled RogueScape UI art sheet ({@code ui-sheet.png}) and exposes its components as
 * source rectangles, plus a 9-slice border renderer so ornate frames scale to any size without
 * distorting their corners.
 *
 * <p>The sheet is a single atlas (Caleb's design-system export); each component lives at a fixed
 * rectangle below. Loading is lazy and failure-tolerant — if the resource is missing the getters
 * return {@code null} and callers fall back to the procedural {@link RogueScapeFrame} look, so the
 * UI never hard-depends on the art being present.
 */
public final class RogueScapeSprites
{
	private RogueScapeSprites()
	{
	}

	// ---- Atlas rectangles (measured from ui-sheet.png, 1536x1024) ----
	private static final Rectangle MODAL_FRAME = new Rectangle(250, 64, 210, 150);
	private static final Rectangle MODAL_CREST = new Rectangle(334, 65, 44, 30);

	private static final Rectangle[] CARD_FRAMES = {
		new Rectangle(30, 445, 92, 148),   // COMMON
		new Rectangle(142, 445, 92, 148),  // RARE
		new Rectangle(252, 445, 92, 148),  // EPIC
		new Rectangle(363, 445, 92, 148),  // LEGENDARY
	};

	private static volatile BufferedImage sheet;
	private static volatile boolean attempted;

	/** The full atlas, or {@code null} if it couldn't be loaded. Lazy + cached. */
	@Nullable
	public static BufferedImage sheet()
	{
		if (!attempted)
		{
			synchronized (RogueScapeSprites.class)
			{
				if (!attempted)
				{
					attempted = true;
					try
					{
						sheet = ImageUtil.loadImageResource(RogueScapeSprites.class, "ui-sheet.png");
					}
					catch (Throwable t)
					{
						sheet = null;
					}
				}
			}
		}
		return sheet;
	}

	public static boolean available()
	{
		return sheet() != null;
	}

	/**
	 * Draws the overlay modal frame from the art sheet into the destination box: a border-only
	 * 9-slice for the ornate gold edges (the top inset stays above the hanging crest, so the crest
	 * isn't smeared), then the crest stamped at native size centred on the top edge. Returns false
	 * if the sheet is unavailable so the caller can fall back to {@link RogueScapeFrame}.
	 */
	public static boolean drawModalFrame(Graphics2D g, int dx, int dy, int dw, int dh)
	{
		BufferedImage s = sheet();
		if (s == null)
		{
			return false;
		}
		nineSliceBorder(g, s, MODAL_FRAME, dx, dy, dw, dh, 10, 16, 12, 16);
		int cw = MODAL_CREST.width;
		int ch = MODAL_CREST.height;
		blit(g, s, MODAL_CREST.x, MODAL_CREST.y, cw, ch, dx + (dw - cw) / 2, dy + 1, cw, ch);
		return true;
	}

	/** The overlay modal-frame region as a standalone image (for registering as a widget sprite). */
	@Nullable
	public static BufferedImage modalFrameImage()
	{
		BufferedImage s = sheet();
		if (s == null)
		{
			return null;
		}
		BufferedImage src = s.getSubimage(MODAL_FRAME.x, MODAL_FRAME.y, MODAL_FRAME.width, MODAL_FRAME.height);
		// Copy to a standalone ARGB image so it doesn't share the sheet's raster.
		BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = out.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	/** Source rect of the reward-card frame for a rarity ordinal (0..3), clamped. */
	@Nullable
	public static Rectangle cardFrame(int rarityOrdinal)
	{
		if (sheet() == null)
		{
			return null;
		}
		int i = Math.max(0, Math.min(CARD_FRAMES.length - 1, rarityOrdinal));
		return CARD_FRAMES[i];
	}

	/**
	 * Draws the 8 border pieces (4 corners + 4 edges) of source rect {@code src} scaled into the
	 * destination box, leaving the centre untouched so the caller fills it. Insets are in source
	 * pixels and may be asymmetric (the design's frames have a tall ornamented top, thin bottom).
	 */
	public static void nineSliceBorder(Graphics2D g, BufferedImage sheet, Rectangle src,
		int dx, int dy, int dw, int dh, int top, int right, int bottom, int left)
	{
		if (sheet == null || src == null || dw <= 0 || dh <= 0)
		{
			return;
		}
		int sx = src.x;
		int sy = src.y;
		int sw = src.width;
		int sh = src.height;
		int midSw = sw - left - right;
		int midSh = sh - top - bottom;
		int midDw = dw - left - right;
		int midDh = dh - top - bottom;

		// Corners (1:1-ish scale, never stretched out of proportion).
		blit(g, sheet, sx, sy, left, top, dx, dy, left, top);
		blit(g, sheet, sx + sw - right, sy, right, top, dx + dw - right, dy, right, top);
		blit(g, sheet, sx, sy + sh - bottom, left, bottom, dx, dy + dh - bottom, left, bottom);
		blit(g, sheet, sx + sw - right, sy + sh - bottom, right, bottom, dx + dw - right, dy + dh - bottom, right, bottom);

		// Edges (stretched along their length only).
		blit(g, sheet, sx + left, sy, midSw, top, dx + left, dy, midDw, top);
		blit(g, sheet, sx + left, sy + sh - bottom, midSw, bottom, dx + left, dy + dh - bottom, midDw, bottom);
		blit(g, sheet, sx, sy + top, left, midSh, dx, dy + top, left, midDh);
		blit(g, sheet, sx + sw - right, sy + top, right, midSh, dx + dw - right, dy + top, right, midDh);
	}

	private static void blit(Graphics2D g, BufferedImage img,
		int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh)
	{
		if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0)
		{
			return;
		}
		g.drawImage(img, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
	}
}
