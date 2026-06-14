package com.pluginideahub.roguescape.bridge;

import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges RogueScape to the external <a href="https://github.com/Skretzo/shortest-path">Shortest
 * Path</a> plugin via reflection: it locates the plugin among the loaded plugins and sets/clears its
 * {@code WorldPoint} target for the current travel stage. Reflection is used so RogueScape has no
 * hard compile dependency on a plugin the user may not have installed.
 *
 * <p>Self-contained: holds its own dedup key + human-readable status; the host plugin feeds it the
 * current stage rule and travel state each tick.
 */
public final class ShortestPathBridge
{
	private static final Logger log = LoggerFactory.getLogger(ShortestPathBridge.class);

	private final Client client;
	private final PluginManager pluginManager;

	private String lastTargetKey = "";
	private String status = "";

	public ShortestPathBridge(Client client, PluginManager pluginManager)
	{
		this.client = client;
		this.pluginManager = pluginManager;
	}

	/** Human-readable status line for overlays (empty when nothing to report). */
	public String status()
	{
		return status;
	}

	/** Clears the local dedup key and status without touching the external plugin. */
	public void reset()
	{
		lastTargetKey = "";
		status = "";
	}

	/**
	 * Sets the Shortest Path target to the current stage's region while travelling, or clears it
	 * otherwise. No-ops when the target is unchanged since the last call.
	 */
	public void syncTarget(boolean travelling, StageRegionRule rule)
	{
		WorldPoint target = travelling ? targetPoint(rule) : null;
		String key = key(target);
		if (key.equals(lastTargetKey))
		{
			return;
		}
		if (setTarget(target))
		{
			lastTargetKey = key;
			status = target == null ? "cleared" : "Shortest Path target set";
		}
		else if (target != null)
		{
			if (status == null || status.isEmpty())
			{
				status = "Shortest Path not found";
			}
		}
	}

	/** Clears the external target (used on run reset). */
	public void clear()
	{
		if (lastTargetKey.isEmpty() && status.isEmpty())
		{
			return;
		}
		if (setTarget(null))
		{
			status = "cleared";
		}
		lastTargetKey = "";
	}

	/** Converts the first allowed region of a stage rule into a jumpable world point, or null. */
	public WorldPoint targetPoint(StageRegionRule rule)
	{
		if (rule == null || !rule.restrictsRegion() || rule.allowedRegionIds().isEmpty())
		{
			return null;
		}
		String first = rule.allowedRegionIds().iterator().next();
		try
		{
			int regionId = Integer.parseInt(first);
			int regionX = regionId >> 8;
			int regionY = regionId & 0xFF;
			int plane = 0;
			Player player = client == null ? null : client.getLocalPlayer();
			if (player != null && player.getWorldLocation() != null)
			{
				plane = player.getWorldLocation().getPlane();
			}
			return new WorldPoint(regionX * 64 + 32, regionY * 64 + 32, plane);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static String key(WorldPoint target)
	{
		return target == null ? "" : target.getX() + "," + target.getY() + "," + target.getPlane();
	}

	private boolean setTarget(WorldPoint target)
	{
		Object shortestPath = pluginInstance();
		if (shortestPath == null)
		{
			log.debug("RogueScape Shortest Path bridge: plugin not found");
			return false;
		}
		try
		{
			if (!pluginActive(shortestPath))
			{
				status = "Shortest Path is off";
				log.debug("RogueScape Shortest Path bridge: plugin found but inactive ({})",
					shortestPath.getClass().getName());
				return false;
			}
			Method setTarget = findMethod(shortestPath.getClass(), "setTarget", WorldPoint.class);
			if (setTarget == null)
			{
				return setTargetFields(shortestPath, target);
			}
			setTarget.setAccessible(true);
			setTarget.invoke(shortestPath, target);
			log.debug("RogueScape Shortest Path bridge: set target {}", target);
			return true;
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not call Shortest Path setTarget; trying field fallback", ex);
			return setTargetFields(shortestPath, target);
		}
	}

	private Object pluginInstance()
	{
		if (pluginManager == null)
		{
			return null;
		}
		try
		{
			Method getPlugins = pluginManager.getClass().getMethod("getPlugins");
			Object plugins = getPlugins.invoke(pluginManager);
			if (!(plugins instanceof Iterable))
			{
				return null;
			}
			for (Object plugin : (Iterable<?>) plugins)
			{
				if (plugin == null)
				{
					continue;
				}
				Class<?> type = plugin.getClass();
				String name = type.getName();
				PluginDescriptor descriptor = type.getAnnotation(PluginDescriptor.class);
				String descriptorName = descriptor == null ? "" : descriptor.name();
				if ("Shortest Path".equalsIgnoreCase(descriptorName)
					|| "shortestpath.ShortestPathPlugin".equals(name)
					|| name.endsWith(".ShortestPathPlugin")
					|| "ShortestPathPlugin".equals(type.getSimpleName()))
				{
					log.debug("RogueScape Shortest Path bridge: found {} ({})", descriptorName, name);
					return plugin;
				}
			}
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not inspect loaded plugins for Shortest Path", ex);
		}
		return null;
	}

	private boolean pluginActive(Object plugin)
	{
		if (pluginManager == null || plugin == null)
		{
			return false;
		}
		try
		{
			Method isPluginActive = pluginManager.getClass().getMethod("isPluginActive", Plugin.class);
			Object active = isPluginActive.invoke(pluginManager, plugin);
			return Boolean.TRUE.equals(active);
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not check Shortest Path active state; assuming active", ex);
			return true;
		}
	}

	private boolean setTargetFields(Object shortestPath, WorldPoint target)
	{
		try
		{
			Field targetField = findField(shortestPath.getClass(), "target");
			if (targetField == null)
			{
				status = "Shortest Path API changed";
				return false;
			}
			targetField.setAccessible(true);
			targetField.set(shortestPath, target);

			Field updateField = findField(shortestPath.getClass(), "pathUpdateScheduled");
			if (updateField != null)
			{
				updateField.setAccessible(true);
				updateField.setBoolean(shortestPath, true);
			}
			log.debug("RogueScape Shortest Path bridge: field target {}", target);
			return true;
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			status = "Shortest Path bridge failed";
			log.debug("Could not set Shortest Path target fields", ex);
			return false;
		}
	}

	private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
	{
		Class<?> current = type;
		while (current != null)
		{
			try
			{
				return current.getDeclaredMethod(name, parameterTypes);
			}
			catch (NoSuchMethodException ignored)
			{
				current = current.getSuperclass();
			}
		}
		return null;
	}

	private static Field findField(Class<?> type, String name)
	{
		Class<?> current = type;
		while (current != null)
		{
			try
			{
				return current.getDeclaredField(name);
			}
			catch (NoSuchFieldException ignored)
			{
				current = current.getSuperclass();
			}
		}
		return null;
	}
}
