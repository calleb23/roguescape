package com.pluginideahub.roguescape.core;

/**
 * Stage 5 — coarse scoring profile selected at run start. Each preset maps to a fixed
 * {@link ScoringRules} value set; the run engine consults the rules at recap time.
 */
public enum ScoringPreset
{
	BALANCED,
	SPEEDRUN,
	CREATOR_CHAOS
}
