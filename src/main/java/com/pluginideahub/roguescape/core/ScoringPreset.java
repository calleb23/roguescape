package com.pluginideahub.roguescape.core;

/**
 * Stage 5 — coarse scoring profile selected at run start. Each preset maps to a fixed
 * {@link ScoringRules} value set; the run engine consults the rules at recap time.
 */
public enum ScoringPreset
{
	BALANCED,
	SPEEDRUN,
	CREATOR_CHAOS;

	/**
	 * Maps a run's {@link RunMode} to its scoring profile. Seeded races are timed (SPEEDRUN),
	 * the custom creator leans chaotic (CREATOR_CHAOS), and everything else — including the
	 * default {@code UNSPECIFIED} — uses BALANCED.
	 */
	public static ScoringPreset forMode(RunMode mode)
	{
		if (mode == null) return BALANCED;
		switch (mode)
		{
			case SEEDED_RACE: return SPEEDRUN;
			case CUSTOM_CREATOR: return CREATOR_CHAOS;
			default: return BALANCED;
		}
	}
}
