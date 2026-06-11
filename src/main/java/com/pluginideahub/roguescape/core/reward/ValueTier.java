package com.pluginideahub.roguescape.core.reward;

/**
 * Stage 4 — coarse value tier used by the fairness/economy seam. Boundaries are rough
 * because RuneLite GE prices fluctuate; the tier is for *bucketing*, not exact pricing.
 *
 * Approximate GP bands (subject to tuning):
 *  - TIER_1: under 100k
 *  - TIER_2: 100k - 1m
 *  - TIER_3: 1m - 10m
 *  - TIER_4: 10m - 100m
 *  - TIER_5: 100m - 1b
 *  - TIER_6: 1b+
 */
public enum ValueTier
{
	TIER_1, TIER_2, TIER_3, TIER_4, TIER_5, TIER_6;

	public static ValueTier ofPrice(long price)
	{
		if (price < 100_000L) return TIER_1;
		if (price < 1_000_000L) return TIER_2;
		if (price < 10_000_000L) return TIER_3;
		if (price < 100_000_000L) return TIER_4;
		if (price < 1_000_000_000L) return TIER_5;
		return TIER_6;
	}

	/** Returns true if {@code this} is the same tier or higher than {@code minimum}. */
	public boolean isAtLeast(ValueTier minimum)
	{
		return this.ordinal() >= minimum.ordinal();
	}

	/** Returns true if {@code this} is the same tier or lower than {@code maximum}. */
	public boolean isAtMost(ValueTier maximum)
	{
		return this.ordinal() <= maximum.ordinal();
	}
}
