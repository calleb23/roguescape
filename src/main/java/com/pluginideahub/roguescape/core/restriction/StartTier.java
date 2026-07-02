package com.pluginideahub.roguescape.core.restriction;

/**
 * How shackled a run begins. The tier sets the starting gear-tier cap (tier = max equip-level
 * requirement, per the locked design) — the run then earns the cap upward through rewards.
 * Cap values are data, deferred to playtest.
 */
public enum StartTier
{
	/** Nothing above level-1 gear — the full "rise of power" fantasy. */
	NONE("None", 1),
	/** Up to mithril-grade (equip level 20). */
	LOW("Low", 20),
	/** Up to rune-grade (equip level 40). */
	MEDIUM("Medium", 40),
	/** Up to dragon-grade (equip level 60) — jump straight to high-end content. */
	HIGH("High", 60);

	private final String label;
	private final int gearTierCap;

	StartTier(String label, int gearTierCap)
	{
		this.label = label;
		this.gearTierCap = gearTierCap;
	}

	public String label()
	{
		return label;
	}

	/** The starting gear-tier cap (max equip-level requirement) this tier imposes. */
	public int gearTierCap()
	{
		return gearTierCap;
	}

	/** Apply this tier's starting cap to the run's restrictions. */
	public void apply(RunRestrictions restrictions)
	{
		if (restrictions != null)
		{
			restrictions.restrictGearTier(gearTierCap);
		}
	}
}
