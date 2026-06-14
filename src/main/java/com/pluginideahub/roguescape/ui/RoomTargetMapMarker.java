package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Owns the single world-map marker that points at the active room's target tile, including its
 * procedurally drawn icon and a coord-keyed dedup so the marker is only rebuilt when the target
 * actually moves.
 */
public final class RoomTargetMapMarker
{
	private final WorldMapPointManager worldMapPointManager;

	private WorldMapPoint point;
	private String lastKey = "";

	public RoomTargetMapMarker(WorldMapPointManager worldMapPointManager)
	{
		this.worldMapPointManager = worldMapPointManager;
	}

	/**
	 * Shows a jumpable marker at {@code target} (labelled with {@code roomName}), or removes it when
	 * disabled or there is no target.
	 */
	public void sync(boolean enabled, WorldPoint target, String roomName)
	{
		if (!enabled || worldMapPointManager == null || target == null)
		{
			remove();
			return;
		}
		String key = key(target);
		if (key.equals(lastKey) && point != null)
		{
			return;
		}
		remove();
		point = new WorldMapPoint(target, markerImage());
		point.setName("RogueScape: " + (roomName == null ? "" : roomName));
		point.setTarget(target);
		point.setJumpOnClick(true);
		worldMapPointManager.add(point);
		lastKey = key;
	}

	public void remove()
	{
		if (worldMapPointManager != null && point != null)
		{
			worldMapPointManager.remove(point);
		}
		point = null;
		lastKey = "";
	}

	private static String key(WorldPoint target)
	{
		return target.getX() + "," + target.getY() + "," + target.getPlane();
	}

	private static BufferedImage markerImage()
	{
		BufferedImage image = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0, 0, 0, 165));
		g.fillOval(1, 1, 23, 23);
		g.setColor(new Color(0, 235, 110, 235));
		g.fillOval(4, 4, 17, 17);
		g.setColor(new Color(235, 255, 235, 255));
		g.drawOval(4, 4, 17, 17);
		g.drawLine(12, 6, 12, 18);
		g.drawLine(6, 12, 18, 12);
		g.dispose();
		return image;
	}
}
