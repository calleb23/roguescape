package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.restriction.GradeBands;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.UpgradeLane;

/**
 * One card in a Boss Ladder reward draft (locked 2026-07-03): either a LANE RAISE (an upgrade —
 * the lane's cap moves to its next band) or a RELIC (a permit easing a live restriction). The
 * two pools mix into one deterministic shuffle; see {@link LadderRewardDrafter}.
 */
public final class LadderRewardCard
{
	private final UpgradeLane lane;
	private final int toBand;
	private final Relic relic;

	private LadderRewardCard(UpgradeLane lane, int toBand, Relic relic)
	{
		this.lane = lane;
		this.toBand = toBand;
		this.relic = relic;
	}

	public static LadderRewardCard raise(UpgradeLane lane, int toBand)
	{
		if (lane == null)
		{
			throw new IllegalArgumentException("lane required");
		}
		return new LadderRewardCard(lane, toBand, null);
	}

	public static LadderRewardCard relic(Relic relic)
	{
		if (relic == null)
		{
			throw new IllegalArgumentException("relic required");
		}
		return new LadderRewardCard(null, 0, relic);
	}

	public boolean isRaise()
	{
		return lane != null;
	}

	/** The raised lane (raise cards only), else null. */
	public UpgradeLane lane()
	{
		return lane;
	}

	/** The band the lane rises to; {@link RunRestrictions#UNCAPPED} frees the lane entirely. */
	public int toBand()
	{
		return toBand;
	}

	/** The permit (relic cards only), else null. */
	public Relic relic()
	{
		return relic;
	}

	/** Card face title, e.g. "Weapon rises to rune-grade" or the relic's name. */
	public String title()
	{
		if (isRaise())
		{
			return toBand == RunRestrictions.UNCAPPED
				? lane.displayName() + " is unrestricted"
				: lane.displayName() + " rises to " + GradeBands.name(toBand);
		}
		return relic.name();
	}

	/** Card face description. */
	public String description()
	{
		if (isRaise())
		{
			return toBand == RunRestrictions.UNCAPPED
				? "The " + lane.displayName() + " lane is freed entirely."
				: "The " + lane.displayName() + " cap rises to level " + toBand + ".";
		}
		return relic.description();
	}
}
