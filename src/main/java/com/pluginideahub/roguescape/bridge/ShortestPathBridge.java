package com.pluginideahub.roguescape.bridge;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;

/**
 * Bridge to the Shortest Path plugin via RuneLite's {@link PluginMessage} event API — the
 * mechanism Shortest Path documents for other plugins. Messages are fire-and-forget: if
 * Shortest Path isn't installed or enabled, nothing consumes them and the game is unaffected.
 *
 * <p>This replaces an earlier reflection-based bridge that located the plugin instance and
 * poked its private fields/methods, which is both fragile and a Plugin Hub rejection risk.
 */
public final class ShortestPathBridge
{
	private static final String NAMESPACE = "shortestpath";

	private final EventBus eventBus;
	private String status = "";

	public ShortestPathBridge(EventBus eventBus)
	{
		this.eventBus = eventBus;
	}

	/** Requests a path to {@code target} from the player's location; null clears the path. */
	public boolean setTarget(WorldPoint target)
	{
		if (eventBus == null)
		{
			return false;
		}
		if (target == null)
		{
			clear();
			return true;
		}
		Map<String, Object> data = new HashMap<>();
		data.put("target", target);
		eventBus.post(new PluginMessage(NAMESPACE, "path", data));
		status = "Shortest Path target sent";
		return true;
	}

	public void clear()
	{
		if (eventBus == null)
		{
			return;
		}
		eventBus.post(new PluginMessage(NAMESPACE, "clear"));
		status = "cleared";
	}

	/** Last bridge action, for the side panel's diagnostics row. */
	public String status()
	{
		return status;
	}
}
