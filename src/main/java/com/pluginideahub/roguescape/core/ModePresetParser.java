package com.pluginideahub.roguescape.core;

/**
 * Stage 5 — case-insensitive parsing of {@link RunMode} / {@link RunPreset} config strings.
 * Returns {@code UNSPECIFIED} on null, empty, or unrecognized input so the route builder
 * can always make a sensible default route.
 */
public final class ModePresetParser
{
	private ModePresetParser() {}

	public static RunMode parseMode(String raw)
	{
		if (raw == null) return RunMode.UNSPECIFIED;
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) return RunMode.UNSPECIFIED;
		try
		{
			return RunMode.valueOf(trimmed.toUpperCase());
		}
		catch (IllegalArgumentException ex)
		{
			return RunMode.UNSPECIFIED;
		}
	}

	public static RunPreset parsePreset(String raw)
	{
		// Named presets were removed; every value (including legacy saved strings) maps to
		// UNSPECIFIED so old configs keep working.
		return RunPreset.UNSPECIFIED;
	}
}
