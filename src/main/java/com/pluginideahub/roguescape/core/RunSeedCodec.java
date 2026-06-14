package com.pluginideahub.roguescape.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure parsing for the custom-run seed string ({@code key=value; key=value} form) and its scalar
 * fields. Swing-free and unit-testable; the side panel delegates here. The serialize side will join
 * this once the run-builder state has its own model (see CLEANUP_PLAN W9 / CustomRunSpec).
 */
public final class RunSeedCodec
{
	private RunSeedCodec() {}

	/** Splits a {@code a=1; b=2} seed string into a lowercase-keyed map (empty for null/blank). */
	public static Map<String, String> parseFields(String seed)
	{
		Map<String, String> fields = new LinkedHashMap<>();
		if (seed == null || seed.trim().isEmpty())
		{
			return fields;
		}
		for (String part : seed.split(";"))
		{
			int eq = part.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			String key = part.substring(0, eq).trim().toLowerCase();
			String value = part.substring(eq + 1).trim();
			if (!key.isEmpty())
			{
				fields.put(key, value);
			}
		}
		return fields;
	}

	/** Parses a time-limit value (accepts a trailing {@code m}; {@code none}/{@code off}/blank -> 0). */
	public static int parseTimeMinutes(String value)
	{
		if (value == null)
		{
			return 0;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.isEmpty() || "none".equals(normalized) || "off".equals(normalized))
		{
			return 0;
		}
		if (normalized.endsWith("m"))
		{
			normalized = normalized.substring(0, normalized.length() - 1).trim();
		}
		try
		{
			return Math.max(0, Integer.parseInt(normalized));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	/** Parses a boss-cap value, clamped to 0..3 ({@code none}/{@code off}/blank/invalid -> 0). */
	public static int parseBossLimit(String value)
	{
		if (value == null)
		{
			return 0;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.isEmpty() || "none".equals(normalized) || "off".equals(normalized))
		{
			return 0;
		}
		try
		{
			return Math.max(0, Math.min(3, Integer.parseInt(normalized)));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}
}
