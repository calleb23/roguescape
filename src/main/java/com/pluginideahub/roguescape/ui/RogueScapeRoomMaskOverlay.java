package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.region.RogueScapeRoomMaskRules;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

/**
 * Passive in-game scene overlay that dims visible tiles outside the current
 * RogueScape room's allowed regions. It never blocks movement or menu actions;
 * it only makes the room boundary obvious.
 */
public class RogueScapeRoomMaskOverlay extends Overlay
{
	private static final Color MASK_RGB = new Color(115, 115, 115);

	private final Client client;
	private final BooleanSupplier enabledSupplier;
	private final BooleanSupplier inRunSupplier;
	private final IntSupplier opacitySupplier;
	private final Supplier<Set<String>> allowedRegionIdsSupplier;

	public RogueScapeRoomMaskOverlay(
		Client client,
		BooleanSupplier enabledSupplier,
		BooleanSupplier inRunSupplier,
		IntSupplier opacitySupplier,
		Supplier<Set<String>> allowedRegionIdsSupplier)
	{
		this.client = client;
		this.enabledSupplier = enabledSupplier;
		this.inRunSupplier = inRunSupplier;
		this.opacitySupplier = opacitySupplier;
		this.allowedRegionIdsSupplier = allowedRegionIdsSupplier;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(
			enabledSupplier.getAsBoolean(),
			inRunSupplier.getAsBoolean(),
			opacitySupplier.getAsInt(),
			allowedRegionIdsSupplier.get()
		);
		if (!rules.shouldRender() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Tile[][][] tiles = client.getScene().getTiles();
		int plane = client.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return null;
		}

		graphics.setColor(new Color(MASK_RGB.getRed(), MASK_RGB.getGreen(), MASK_RGB.getBlue(), rules.getClampedOpacity()));
		Tile[][] planeTiles = tiles[plane];
		for (Tile[] column : planeTiles)
		{
			if (column == null)
			{
				continue;
			}
			for (Tile tile : column)
			{
				if (tile == null || tile.getWorldLocation() == null || tile.getLocalLocation() == null)
				{
					continue;
				}
				Polygon polygon = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
				if (polygon != null && rules.shouldMaskRegion(tile.getWorldLocation().getRegionID()))
				{
					graphics.fillPolygon(polygon);
				}
			}
		}
		return null;
	}
}
