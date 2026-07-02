package com.pluginideahub.roguescape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SPIKE — sculpting a real room out of the live scene: clear everything in an area (objects,
 * walls, decorations, floor render), then build back with cache models placed as
 * {@link RuneLiteObject}s (the client-side fake-object mechanism — walls, doors and furniture
 * are all "just models", and the GPU plugin renders these dynamically, so no renderer hook is
 * needed and there is no one-plugin-at-a-time conflict).
 *
 * <p>The scene is rebuilt from map data on every reload (region change, LOADING), wiping all
 * edits, so the sculptor keeps an <b>edit plan</b> in world coordinates and re-applies it via
 * {@link #reapply()} after each reload. All mutation must run on the client thread.
 *
 * <p>Known unknown (test in-client): under the GPU plugin, static scene geometry is uploaded at
 * scene load, so tile removals made post-load may not disappear until the next reload. If so,
 * the fix is to trigger one reload after editing ({@link #requestSceneReload()}).
 */
final class RogueScapeRoomSculptor
{
	private static final Logger log = LoggerFactory.getLogger(RogueScapeRoomSculptor.class);

	/** One planned model placement, in world coordinates so it survives scene reloads. */
	private static final class Placement
	{
		final WorldPoint point;
		final int modelId;
		final int orientation;
		RuneLiteObject spawned;

		Placement(WorldPoint point, int modelId, int orientation)
		{
			this.point = point;
			this.modelId = modelId;
			this.orientation = orientation;
		}
	}

	private final Client client;
	private final ClientThread clientThread;

	// The edit plan (world coordinates, re-applied after every scene reload).
	private final Set<WorldPoint> clearedTiles = new LinkedHashSet<>();
	private final Set<WorldPoint> blankedFloors = new LinkedHashSet<>();
	private final List<Placement> placements = new ArrayList<>();

	RogueScapeRoomSculptor(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	boolean hasEdits()
	{
		return !clearedTiles.isEmpty() || !blankedFloors.isEmpty() || !placements.isEmpty();
	}

	/** Records + clears everything (objects, walls, decor, floor render) within the radius. */
	void clearAreaAroundPlayer(int radius)
	{
		clientThread.invoke(() ->
		{
			WorldPoint centre = playerPoint();
			if (centre == null)
			{
				return;
			}
			int added = 0;
			for (int dx = -radius; dx <= radius; dx++)
			{
				for (int dy = -radius; dy <= radius; dy++)
				{
					if (clearedTiles.add(centre.dx(dx).dy(dy)))
					{
						added++;
					}
				}
			}
			applyClears();
			log.info("[RogueScape sculptor] cleared r={} around {} ({} new tiles, plan={})",
				radius, centre, added, clearedTiles.size());
		});
	}

	/** Records + blanks only the floor render (keeps walls/objects) within the radius. */
	void blankFloorsAroundPlayer(int radius)
	{
		clientThread.invoke(() ->
		{
			WorldPoint centre = playerPoint();
			if (centre == null)
			{
				return;
			}
			for (int dx = -radius; dx <= radius; dx++)
			{
				for (int dy = -radius; dy <= radius; dy++)
				{
					blankedFloors.add(centre.dx(dx).dy(dy));
				}
			}
			applyFloorBlanks();
			log.info("[RogueScape sculptor] blanked floors r={} around {}", radius, centre);
		});
	}

	/** Records + spawns a cache model at the player's tile (the "build" primitive). */
	void placeModelAtPlayer(int modelId, int orientation)
	{
		if (modelId <= 0)
		{
			log.info("[RogueScape sculptor] no model id set — set one in the RogueScape config");
			return;
		}
		clientThread.invoke(() ->
		{
			WorldPoint centre = playerPoint();
			if (centre == null)
			{
				return;
			}
			Placement p = new Placement(centre, modelId, orientation);
			placements.add(p);
			spawn(p);
			log.info("[RogueScape sculptor] placed model {} o={} at {}", modelId, orientation, centre);
		});
	}

	/** Drops the whole edit plan, despawns placements and reloads the scene to restore the world. */
	void restoreAll()
	{
		clientThread.invoke(() ->
		{
			clearedTiles.clear();
			blankedFloors.clear();
			for (Placement p : placements)
			{
				despawn(p);
			}
			placements.clear();
			requestSceneReload();
			log.info("[RogueScape sculptor] restored — plan dropped, scene reload requested");
		});
	}

	/** Re-applies the whole edit plan (call on the first tick after a scene load). */
	void reapply()
	{
		if (!hasEdits())
		{
			return;
		}
		clientThread.invoke(() ->
		{
			applyClears();
			applyFloorBlanks();
			for (Placement p : placements)
			{
				despawn(p);
				spawn(p);
			}
			log.info("[RogueScape sculptor] re-applied plan: {} cleared, {} blanked, {} placed",
				clearedTiles.size(), blankedFloors.size(), placements.size());
		});
	}

	void shutDown()
	{
		clientThread.invoke(() ->
		{
			for (Placement p : placements)
			{
				despawn(p);
			}
		});
	}

	// ------------------------------------------------------------ application

	private void applyClears()
	{
		Scene scene = client.getScene();
		for (WorldPoint wp : clearedTiles)
		{
			Tile tile = tileAt(scene, wp);
			if (tile == null)
			{
				continue;
			}
			GameObject[] objects = tile.getGameObjects();
			if (objects != null)
			{
				for (GameObject go : objects)
				{
					if (go != null)
					{
						scene.removeGameObject(go);
					}
				}
			}
			// removeTile drops everything left on the tile: wall, decor, ground object, floor.
			scene.removeTile(tile);
		}
	}

	private void applyFloorBlanks()
	{
		Scene scene = client.getScene();
		for (WorldPoint wp : blankedFloors)
		{
			Tile tile = tileAt(scene, wp);
			if (tile != null)
			{
				tile.setSceneTilePaint(null);
				tile.setSceneTileModel(null);
			}
		}
	}

	private void spawn(Placement p)
	{
		LocalPoint lp = LocalPoint.fromWorld(client, p.point);
		if (lp == null)
		{
			return; // out of the loaded scene; reapply() will retry after the next load
		}
		Model model = client.loadModel(p.modelId);
		if (model == null)
		{
			log.info("[RogueScape sculptor] model {} not in cache", p.modelId);
			return;
		}
		RuneLiteObject rlo = client.createRuneLiteObject();
		rlo.setModel(model);
		rlo.setOrientation(p.orientation);
		rlo.setLocation(lp, p.point.getPlane());
		rlo.setActive(true);
		p.spawned = rlo;
	}

	private void despawn(Placement p)
	{
		if (p.spawned != null)
		{
			p.spawned.setActive(false);
			p.spawned = null;
		}
	}

	// ------------------------------------------------------------ helpers

	private WorldPoint playerPoint()
	{
		if (client.getLocalPlayer() == null)
		{
			return null;
		}
		return client.getLocalPlayer().getWorldLocation();
	}

	private Tile tileAt(Scene scene, WorldPoint wp)
	{
		if (scene == null || wp == null || wp.getPlane() < 0 || wp.getPlane() > 3)
		{
			return null;
		}
		int sx = wp.getX() - scene.getBaseX();
		int sy = wp.getY() - scene.getBaseY();
		Tile[][][] tiles = scene.getTiles();
		if (sx < 0 || sy < 0 || sx >= tiles[0].length || sy >= tiles[0][0].length)
		{
			return null;
		}
		return tiles[wp.getPlane()][sx][sy];
	}

	/** Forces a client scene rebuild (the same trick region-hiding plugins use). */
	void requestSceneReload()
	{
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				client.setGameState(GameState.LOADING);
			}
		});
	}
}
