package com.pluginideahub.roguescape.core.restriction;

/**
 * The fixed grade ladder every upgrade lane climbs (locked 2026-07-03):
 * <b>1 → 5 → 10 → 20 → 30 → 40 → 50 → 60 → 70</b> — every step is a felt, nameable jump
 * ("Armour rises to rune-grade"). Raising past the top band lifts the lane entirely
 * ({@link RunRestrictions#UNCAPPED}). Flat +N raises are dead.
 */
public final class GradeBands
{
	/** The ladder, ascending. Values are requirement levels (equip or creation). */
	public static final int[] BANDS = {1, 5, 10, 20, 30, 40, 50, 60, 70};

	private static final String[] NAMES = {
		"bronze-grade", "steel-grade", "black-grade", "mithril-grade", "adamant-grade",
		"rune-grade", "granite-grade", "dragon-grade", "beyond dragon-grade"
	};

	private GradeBands()
	{
	}

	/**
	 * The next band above {@code cap}, or {@link RunRestrictions#UNCAPPED} when the cap is at
	 * (or past) the top band — the raise past the top frees the lane entirely.
	 */
	public static int next(int cap)
	{
		if (cap == RunRestrictions.UNCAPPED)
		{
			return RunRestrictions.UNCAPPED;
		}
		for (int band : BANDS)
		{
			if (band > cap)
			{
				return band;
			}
		}
		return RunRestrictions.UNCAPPED;
	}

	/** The display name of the band at (or nearest below) {@code cap}; "unrestricted" when free. */
	public static String name(int cap)
	{
		if (cap == RunRestrictions.UNCAPPED)
		{
			return "unrestricted";
		}
		String name = NAMES[0];
		for (int i = 0; i < BANDS.length; i++)
		{
			if (BANDS[i] <= cap)
			{
				name = NAMES[i];
			}
		}
		return name;
	}
}
