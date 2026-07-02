package com.pluginideahub.roguescape.core.ladder;

/** Where a Boss Ladder run currently is. */
public enum LadderPhase
{
	/** Between bosses: bank/GE temporarily allowed to gear up; ends at the fight gate. */
	PREP("Prep"),
	/** Fighting the current boss under the full restrictions. */
	FIGHT("Fight"),
	/** The boss fell — a restriction-easing reward is on offer. */
	REWARD("Reward"),
	/** The ladder is climbed. */
	COMPLETE("Complete"),
	/** A restriction was broken (or the run was abandoned) — the run is over. */
	FAILED("Failed");

	private final String label;

	LadderPhase(String label)
	{
		this.label = label;
	}

	public String label()
	{
		return label;
	}
}
