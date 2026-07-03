package com.pluginideahub.roguescape.core.relic;

/**
 * Stage 5 — coarse categories of relic effects.
 *
 * Plugin Hub safety constraint: a relic must never alter gameplay or assist the player; it
 * only modifies how the referee scores/permits items, what limits/warnings the recap surfaces,
 * and what one-shot mercies are available. Effects are interpreted by the run engine, not
 * acted upon in-game.
 */
public enum RelicEffectKind
{
	/** Allow the player one no-fail exemption from a specific rule (e.g. One Bank Mercy). */
	ONE_SHOT_MERCY,
	/** Set a hard upper bound on stacked items of a category (e.g. Four-Food Limit). */
	CATEGORY_LIMIT,
	/** Multiplies or adds to the score for matching items at recap. */
	SCORING_BIAS,
	/** Marks specific items/categories as forbidden — items become forbidden even if permitted-sourced. */
	RESTRICTION,
	/** Adds an allowance — converts an otherwise blocked source into a permitted one (rarely used). */
	PERMISSION,
	/** Lifts one restriction from the run's {@code RunRestrictions} — the canonical relic effect. */
	EASE_RESTRICTION,
	/**
	 * RETIRED (locked 2026-07-03): tier raises are upgrade-lane rewards, never relics. Kept only
	 * because quarantined legacy relics reference it; nothing applies it.
	 */
	RAISE_GEAR_TIER,
	/** Grants {@code magnitude} extra inventory slots under an inventory limit. */
	ADD_INVENTORY_SLOTS,
	/** Permits one more combat style under a COMBAT_STYLE lock (One Style's easer family). */
	PERMIT_COMBAT_STYLE
}
