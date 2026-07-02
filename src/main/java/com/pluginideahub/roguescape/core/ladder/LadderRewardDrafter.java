package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEffect;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.reward.DeterministicRng;
import java.util.ArrayList;
import java.util.List;

/**
 * The Boss Ladder reward draft (MVP chunk 4): draft 1-of-N restriction-easers. Every card offered
 * would <em>actually do something</em> against the run's current restrictions — a chest never
 * offers "Food is permitted" when food was never forbidden. Deterministic under a seed so shared
 * seeds share drafts.
 */
public final class LadderRewardDrafter
{
	/** Default draft size — playtest data. */
	public static final int DRAFT_SIZE = 3;

	private LadderRewardDrafter()
	{
	}

	/** Draft up to {@code count} easers useful against the current restrictions. */
	public static List<Relic> draft(RunRestrictions current, long seed, int count)
	{
		List<Relic> useful = usefulEasers(current);
		new DeterministicRng(seed).shuffle(useful);
		return useful.size() <= count ? useful : new ArrayList<>(useful.subList(0, Math.max(0, count)));
	}

	/** Every easer in the pool that would change something about {@code current}. */
	public static List<Relic> usefulEasers(RunRestrictions current)
	{
		List<Relic> out = new ArrayList<>();
		if (current == null)
		{
			return out;
		}
		for (Relic relic : RelicLibrary.easers())
		{
			if (wouldEase(relic, current))
			{
				out.add(relic);
			}
		}
		return out;
	}

	/** True when applying this relic would loosen at least one live restriction. */
	public static boolean wouldEase(Relic relic, RunRestrictions current)
	{
		if (relic == null || current == null)
		{
			return false;
		}
		for (RelicEffect effect : relic.effects())
		{
			switch (effect.kind())
			{
				case EASE_RESTRICTION:
					if (current.isRestricted(effect.eases()))
					{
						return true;
					}
					break;
				case RAISE_GEAR_TIER:
					if (current.isRestricted(Restriction.GEAR_TIER_CAP)
						&& current.gearTierCap() != RunRestrictions.UNCAPPED)
					{
						return true;
					}
					break;
				case ADD_INVENTORY_SLOTS:
					if (current.isRestricted(Restriction.INVENTORY_LIMIT)
						&& current.inventoryLimit() != RunRestrictions.UNCAPPED)
					{
						return true;
					}
					break;
				default:
					break;
			}
		}
		return false;
	}
}
