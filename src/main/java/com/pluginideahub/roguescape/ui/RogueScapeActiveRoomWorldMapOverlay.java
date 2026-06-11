package com.pluginideahub.roguescape.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Live-run world-map overlay. It mirrors the custom region editor's green
 * region styling, but renders the current active destination instead of the
 * editable selection.
 */
public class RogueScapeActiveRoomWorldMapOverlay extends Overlay
{
	private static final Color TARGET_FILL = new Color(0, 220, 90, 145);
	private static final Color TARGET_OUTLINE = new Color(70, 255, 140, 255);
	private static final Color LABEL_BG = new Color(0, 0, 0, 165);
	private static final Color LABEL_FG = new Color(235, 255, 235, 240);
	private static final Color GRID = new Color(0, 0, 0, 28);
	private static final int REGION_TILE_SIZE = 64;

	private final Client client;
	private final BooleanSupplier enabledSupplier;
	private final Supplier<Set<String>> allowedRegionIdsSupplier;
	private final Supplier<String> roomNameSupplier;

	public RogueScapeActiveRoomWorldMapOverlay(
		Client client,
		BooleanSupplier enabledSupplier,
		Supplier<Set<String>> allowedRegionIdsSupplier,
		Supplier<String> roomNameSupplier)
	{
		this.client = client;
		this.enabledSupplier = enabledSupplier;
		this.allowedRegionIdsSupplier = allowedRegionIdsSupplier;
		this.roomNameSupplier = roomNameSupplier;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.WORLDMAP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Set<Integer> selected = parseRegionIds(allowedRegionIdsSupplier.get());
		if (!enabledSupplier.getAsBoolean() || selected.isEmpty())
		{
			return null;
		}

		Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		WorldMap worldMap = client.getWorldMap();
		if (mapWidget == null || worldMap == null || mapWidget.isHidden())
		{
			return null;
		}

		float zoom = worldMap.getWorldMapZoom();
		if (zoom <= 0f)
		{
			return null;
		}

		Rectangle bounds = mapWidget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return null;
		}

		Shape originalClip = graphics.getClip();
		Composite originalComposite = graphics.getComposite();
		Object originalHint = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setClip(bounds);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int widthInTiles = (int) Math.ceil(bounds.getWidth() / zoom);
		int heightInTiles = (int) Math.ceil(bounds.getHeight() / zoom);
		Point worldMapPos = worldMap.getWorldMapPosition();
		int yTileMin = worldMapPos.getY() - heightInTiles / 2;
		int xRegionStart = (worldMapPos.getX() - widthInTiles / 2) & ~(REGION_TILE_SIZE - 1);
		int xRegionEnd = ((worldMapPos.getX() + widthInTiles / 2) & ~(REGION_TILE_SIZE - 1)) + REGION_TILE_SIZE;
		int yRegionStart = yTileMin & ~(REGION_TILE_SIZE - 1);
		int yRegionEnd = ((worldMapPos.getY() + heightInTiles / 2) & ~(REGION_TILE_SIZE - 1)) + REGION_TILE_SIZE;
		int regionPixelSize = (int) Math.ceil(REGION_TILE_SIZE * zoom);

		for (int x = xRegionStart; x < xRegionEnd; x += REGION_TILE_SIZE)
		{
			for (int y = yRegionStart; y < yRegionEnd; y += REGION_TILE_SIZE)
			{
				int yOffsetTiles = y - yTileMin;
				int xOffsetTiles = x - (worldMapPos.getX() - widthInTiles / 2);
				int screenX = (int) (xOffsetTiles * zoom) + bounds.x;
				int screenY = bounds.height - (int) (yOffsetTiles * zoom) + bounds.y - regionPixelSize;
				int regionId = ((x >> 6) << 8) | (y >> 6);
				Rectangle regionRect = new Rectangle(screenX, screenY, regionPixelSize, regionPixelSize);

				graphics.setColor(GRID);
				graphics.drawRect(regionRect.x, regionRect.y, regionRect.width, regionRect.height);
				if (selected.contains(regionId))
				{
					graphics.setColor(TARGET_FILL);
					graphics.fillRect(regionRect.x, regionRect.y, regionRect.width, regionRect.height);
					graphics.setColor(TARGET_OUTLINE);
					graphics.drawRect(regionRect.x, regionRect.y, regionRect.width - 1, regionRect.height - 1);
					if (regionPixelSize >= 24)
					{
						drawRegionLabel(graphics, regionRect, "RogueScape " + regionId);
					}
				}
			}
		}

		drawHud(graphics, bounds, selected.size());
		graphics.setClip(originalClip);
		graphics.setComposite(originalComposite);
		if (originalHint != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalHint);
		}
		return null;
	}

	private void drawHud(Graphics2D graphics, Rectangle bounds, int selectedCount)
	{
		String name = roomNameSupplier.get();
		String topLine = "RogueScape destination" + (name == null || name.isEmpty() ? "" : ": " + name);
		String bottomLine = "Green region is the active room. Click the RogueScape map marker to jump here.";
		Font previousFont = graphics.getFont();
		graphics.setFont(previousFont.deriveFont(Font.BOLD, 12f));
		int padding = 6;
		int lineHeight = graphics.getFontMetrics().getHeight();
		int boxWidth = Math.max(graphics.getFontMetrics().stringWidth(topLine),
			graphics.getFontMetrics().stringWidth(bottomLine)) + padding * 2;
		int boxHeight = lineHeight * 2 + padding * 2;
		int x = bounds.x + padding;
		int y = bounds.y + padding;

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.82f));
		graphics.setColor(LABEL_BG);
		graphics.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
		graphics.setComposite(oldComposite);
		graphics.setColor(LABEL_FG);
		graphics.drawString(topLine + " (" + selectedCount + " region" + (selectedCount == 1 ? "" : "s") + ")",
			x + padding, y + padding + graphics.getFontMetrics().getAscent());
		graphics.drawString(bottomLine, x + padding,
			y + padding + graphics.getFontMetrics().getAscent() + lineHeight);
		graphics.setFont(previousFont);
	}

	private static void drawRegionLabel(Graphics2D graphics, Rectangle rect, String text)
	{
		Font previousFont = graphics.getFont();
		graphics.setFont(previousFont.deriveFont(Font.BOLD, 10f));
		int textWidth = graphics.getFontMetrics().stringWidth(text);
		int textHeight = graphics.getFontMetrics().getAscent();
		int padding = 3;
		int boxWidth = textWidth + padding * 2;
		int boxHeight = textHeight + padding * 2;
		int x = rect.x + (rect.width - boxWidth) / 2;
		int y = rect.y + (rect.height - boxHeight) / 2;
		graphics.setColor(LABEL_BG);
		graphics.fillRoundRect(x, y, boxWidth, boxHeight, 5, 5);
		graphics.setColor(LABEL_FG);
		graphics.drawString(text, x + padding, y + padding + textHeight - 1);
		graphics.setFont(previousFont);
	}

	private static Set<Integer> parseRegionIds(Set<String> ids)
	{
		Set<Integer> out = new LinkedHashSet<>();
		if (ids == null)
		{
			return out;
		}
		for (String id : ids)
		{
			try
			{
				out.add(Integer.parseInt(id));
			}
			catch (NumberFormatException ignored)
			{
				// Custom region data is numeric in live use; ignore malformed rows.
			}
		}
		return out;
	}
}
