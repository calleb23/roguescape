package com.pluginideahub.roguescape.core.relic;

import java.util.ArrayList;
import java.util.List;

/**
 * Convenience facade over {@link RelicLibrary} and {@link ModifierLibrary}: combined listing
 * and id lookup, so callers can resolve a chosen relic/modifier from its id.
 */
public final class RelicCatalog
{
	private RelicCatalog() {}

	/** Scoring-bonus relics. */
	public static List<Relic> relics()
	{
		return RelicLibrary.all();
	}

	/** Restriction-only curses/modifiers. */
	public static List<Relic> modifiers()
	{
		return ModifierLibrary.all();
	}

	/** Every relic and modifier, relics first. */
	public static List<Relic> all()
	{
		List<Relic> out = new ArrayList<>();
		out.addAll(RelicLibrary.all());
		out.addAll(ModifierLibrary.all());
		return out;
	}

	/** Resolves a relic or modifier by its {@code relicId}, or null if unknown. */
	public static Relic byId(String relicId)
	{
		if (relicId == null) return null;
		for (Relic r : all())
		{
			if (r.relicId().equals(relicId)) return r;
		}
		return null;
	}
}
