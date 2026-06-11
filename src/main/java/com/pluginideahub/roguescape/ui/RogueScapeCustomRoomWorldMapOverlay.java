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
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
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
 * Highlights selected custom room regions on the RuneLite world map and reports
 * which region the mouse is currently over so the plugin can offer a
 * toggle action. Renders only when the editor is in editing mode.
 */
public class RogueScapeCustomRoomWorldMapOverlay extends Overlay
{
	private static final Color SELECTED_FILL = new Color(0, 220, 80, 160);
	private static final Color SELECTED_OUTLINE = new Color(0, 255, 100, 255);
	private static final Color HOVER_FILL = new Color(255, 235, 80, 120);
	private static final Color HOVER_OUTLINE = new Color(255, 220, 50, 240);
	private static final Color LABEL_BG = new Color(0, 0, 0, 160);
	private static final Color LABEL_FG = new Color(255, 255, 255, 230);
	private static final Color GRID = new Color(0, 0, 0, 35);
	private static final int REGION_TILE_SIZE = 64;

	private final Client client;
	private final BooleanSupplier editingEnabled;
	private final Supplier<Set<Integer>> selectedIds;
	private final Supplier<String> nameSupplier;
	private final IntConsumer hoveredRegionListener;

	public RogueScapeCustomRoomWorldMapOverlay(
		Client client,
		BooleanSupplier editingEnabled,
		Supplier<Set<Integer>> selectedIds,
		Supplier<String> nameSupplier,
		IntConsumer hoveredRegionListener)
	{
		this.client = client;
		this.editingEnabled = editingEnabled;
		this.selectedIds = selectedIds;
		this.nameSupplier = nameSupplier;
		this.hoveredRegionListener = hoveredRegionListener;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.MANUAL);
		// Draw after the world map interface (group id 595) renders.
		drawAfterInterface(InterfaceID.WORLDMAP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!editingEnabled.getAsBoolean())
		{
			hoveredRegionListener.accept(-1);
			return null;
		}

		Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		WorldMap worldMap = client.getWorldMap();
		if (mapWidget == null || worldMap == null || mapWidget.isHidden())
		{
			hoveredRegionListener.accept(-1);
			return null;
		}

		float zoom = worldMap.getWorldMapZoom();
		if (zoom <= 0f)
		{
			hoveredRegionListener.accept(-1);
			return null;
		}

		Rectangle bounds = mapWidget.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			hoveredRegionListener.accept(-1);
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

		Set<Integer> selected = selectedIds.get();
		int mouseX = client.getMouseCanvasPosition() != null ? client.getMouseCanvasPosition().getX() : -1;
		int mouseY = client.getMouseCanvasPosition() != null ? client.getMouseCanvasPosition().getY() : -1;
		boolean mouseInsideMap = bounds.contains(mouseX, mouseY);

		int hoveredRegionId = -1;
		Rectangle hoveredRect = null;

		// Faint grid + selected fills.
		for (int x = xRegionStart; x < xRegionEnd; x += REGION_TILE_SIZE)
		{
			for (int y = yRegionStart; y < yRegionEnd; y += REGION_TILE_SIZE)
			{
				int yOffsetTiles = y - yTileMin;
				int xOffsetTiles = x - (worldMapPos.getX() - widthInTiles / 2);
				int screenX = (int) (xOffsetTiles * zoom) + (int) bounds.getX();
				int screenY = bounds.height - (int) (yOffsetTiles * zoom) + (int) bounds.getY() - regionPixelSize;

				int regionId = ((x >> 6) << 8) | (y >> 6);

				Rectangle regionRect = new Rectangle(screenX, screenY, regionPixelSize, regionPixelSize);

				if (mouseInsideMap && hoveredRegionId == -1 && regionRect.contains(mouseX, mouseY))
				{
					hoveredRegionId = regionId;
					hoveredRect = regionRect;
				}

				graphics.setColor(GRID);
				graphics.drawRect(regionRect.x, regionRect.y, regionRect.width, regionRect.height);

				if (selected.contains(regionId))
				{
					graphics.setColor(SELECTED_FILL);
					graphics.fillRect(regionRect.x, regionRect.y, regionRect.width, regionRect.height);
					graphics.setColor(SELECTED_OUTLINE);
					graphics.drawRect(regionRect.x, regionRect.y, regionRect.width - 1, regionRect.height - 1);
					if (regionPixelSize >= 24)
					{
						drawRegionLabel(graphics, regionRect, Integer.toString(regionId), LABEL_FG, LABEL_BG);
					}
				}
			}
		}

		if (hoveredRect != null)
		{
			graphics.setColor(HOVER_FILL);
			graphics.fillRect(hoveredRect.x, hoveredRect.y, hoveredRect.width, hoveredRect.height);
			graphics.setColor(HOVER_OUTLINE);
			graphics.drawRect(hoveredRect.x, hoveredRect.y, hoveredRect.width - 1, hoveredRect.height - 1);
			drawRegionLabel(graphics, hoveredRect, "Region " + hoveredRegionId, LABEL_FG, LABEL_BG);
		}

		drawHud(graphics, bounds, selected.size(), hoveredRegionId);

		hoveredRegionListener.accept(hoveredRegionId);

		graphics.setClip(originalClip);
		graphics.setComposite(originalComposite);
		if (originalHint != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalHint);
		}
		return null;
	}

	private void drawHud(Graphics2D graphics, Rectangle bounds, int selectedCount, int hoveredRegionId)
	{
		String name = nameSupplier.get();
		String topLine = "RogueScape room creator" + (name != null && !name.isEmpty() ? ": " + name : "");
		String countLine = "Selected regions: " + selectedCount;
		String hoverLine = hoveredRegionId >= 0
			? "Hover: " + hoveredRegionId + "  (right-click → Toggle RogueScape room region)"
			: "Hover the map to highlight a region";

		Font previousFont = graphics.getFont();
		graphics.setFont(previousFont.deriveFont(Font.BOLD, 12f));
		int padding = 6;
		int lineHeight = graphics.getFontMetrics().getHeight();
		int boxWidth = Math.max(
			graphics.getFontMetrics().stringWidth(topLine),
			Math.max(graphics.getFontMetrics().stringWidth(countLine),
				graphics.getFontMetrics().stringWidth(hoverLine))) + padding * 2;
		int boxHeight = lineHeight * 3 + padding * 2;
		int x = bounds.x + padding;
		int y = bounds.y + padding;

		Composite oldComposite = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
		graphics.setColor(LABEL_BG);
		graphics.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
		graphics.setComposite(oldComposite);

		graphics.setColor(LABEL_FG);
		graphics.drawString(topLine, x + padding, y + padding + graphics.getFontMetrics().getAscent());
		graphics.drawString(countLine, x + padding, y + padding + graphics.getFontMetrics().getAscent() + lineHeight);
		graphics.drawString(hoverLine, x + padding, y + padding + graphics.getFontMetrics().getAscent() + lineHeight * 2);
		graphics.setFont(previousFont);
	}

	private static void drawRegionLabel(Graphics2D graphics, Rectangle rect, String text, Color fg, Color bg)
	{
		Font previousFont = graphics.getFont();
		graphics.setFont(previousFont.deriveFont(Font.PLAIN, 10f));
		int textWidth = graphics.getFontMetrics().stringWidth(text);
		int textHeight = graphics.getFontMetrics().getAscent();
		int padding = 2;
		int boxWidth = textWidth + padding * 2;
		int boxHeight = textHeight + padding * 2;
		int x = rect.x + (rect.width - boxWidth) / 2;
		int y = rect.y + (rect.height - boxHeight) / 2;
		graphics.setColor(bg);
		graphics.fillRect(x, y, boxWidth, boxHeight);
		graphics.setColor(fg);
		graphics.drawString(text, x + padding, y + padding + textHeight - 1);
		graphics.setFont(previousFont);
	}
}
