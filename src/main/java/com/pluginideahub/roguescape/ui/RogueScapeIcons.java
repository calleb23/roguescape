package com.pluginideahub.roguescape.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Cached image provider for the RogueScape custom UI — the shared foundation both the in-game
 * window overlay and (later) the reward "ROLL SUPPLIES" cards draw their icons from.
 *
 * <p>Wraps RuneLite's {@link ItemManager} (inventory item sprites) and {@link SpriteManager}
 * (interface sprites). Both load asynchronously: item images are {@link AsyncBufferedImage}s
 * that are safe to draw immediately and fill themselves in once decoded; interface sprites are
 * fetched via {@code getSpriteAsync} and cached on arrival (returning {@code null} until ready).
 * Callers should treat every returned image as nullable and simply skip drawing when absent —
 * the next render frame will have it.
 *
 * <p>Construct with a {@code repaint} hook for Swing components that don't repaint continuously;
 * overlays repaint every game frame, so they can pass {@code null}. All access is expected on the
 * client thread (overlay render / SpriteManager callbacks); the caches are concurrent regardless.
 */
public class RogueScapeIcons
{
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	@Nullable
	private final Runnable repaint;

	private final ConcurrentHashMap<Long, BufferedImage> itemCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, BufferedImage> spriteCache = new ConcurrentHashMap<>();
	/** Sprite keys already requested, so we fire {@code getSpriteAsync} only once each. */
	private final ConcurrentHashMap<Long, Boolean> spriteRequested = new ConcurrentHashMap<>();

	public RogueScapeIcons(ItemManager itemManager, SpriteManager spriteManager, @Nullable Runnable repaint)
	{
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.repaint = repaint;
	}

	/** Item icon at quantity 1. Never throws; returns {@code null} if unavailable. */
	@Nullable
	public BufferedImage item(int itemId)
	{
		return item(itemId, 1, false);
	}

	/** Item icon with a quantity overlay (e.g. {@code Saradomin brew (4)} / coin stacks). */
	@Nullable
	public BufferedImage item(int itemId, int quantity, boolean stackable)
	{
		if (itemManager == null || itemId <= 0)
		{
			return null;
		}
		long key = (((long) itemId) << 33) | (((long) (stackable ? 1 : 0)) << 32) | (quantity & 0xFFFFFFFFL);
		BufferedImage cached = itemCache.get(key);
		if (cached != null)
		{
			return cached;
		}
		AsyncBufferedImage img = itemManager.getImage(itemId, quantity, stackable);
		if (img == null)
		{
			return null;
		}
		itemCache.put(key, img);
		if (repaint != null)
		{
			img.onLoaded(repaint);
		}
		return img;
	}

	/**
	 * Interface sprite (group {@code spriteId}, frame {@code file}). Returns {@code null} until the
	 * async fetch completes; the first call kicks off loading and subsequent frames return it.
	 */
	@Nullable
	public BufferedImage sprite(int spriteId, int file)
	{
		if (spriteManager == null)
		{
			return null;
		}
		long key = (((long) spriteId) << 32) | (file & 0xFFFFFFFFL);
		BufferedImage cached = spriteCache.get(key);
		if (cached != null)
		{
			return cached;
		}
		if (spriteRequested.putIfAbsent(key, Boolean.TRUE) == null)
		{
			spriteManager.getSpriteAsync(spriteId, file, img ->
			{
				if (img != null)
				{
					spriteCache.put(key, img);
				}
				if (repaint != null)
				{
					repaint.run();
				}
			});
		}
		return null;
	}

	/**
	 * Draws {@code img} scaled to fit a {@code size}x{@code size} box at {@code (x, y)}, preserving
	 * aspect ratio and centring within the box. No-op when {@code img} is null, so callers can pass
	 * the result of {@link #item}/{@link #sprite} straight through.
	 */
	public static void draw(Graphics2D g, @Nullable BufferedImage img, int x, int y, int size)
	{
		if (img == null || size <= 0)
		{
			return;
		}
		int iw = img.getWidth();
		int ih = img.getHeight();
		if (iw <= 0 || ih <= 0)
		{
			return;
		}
		double scale = Math.min(size / (double) iw, size / (double) ih);
		int dw = Math.max(1, (int) Math.round(iw * scale));
		int dh = Math.max(1, (int) Math.round(ih * scale));
		int dx = x + (size - dw) / 2;
		int dy = y + (size - dh) / 2;
		Object prev = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, dx, dy, dw, dh, null);
		if (prev != null)
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, prev);
		}
	}
}
