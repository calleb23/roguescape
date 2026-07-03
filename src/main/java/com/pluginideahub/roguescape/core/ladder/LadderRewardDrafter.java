package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEffect;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.restriction.GradeBands;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import com.pluginideahub.roguescape.core.restriction.UpgradeLane;
import java.util.ArrayList;
import java.util.List;

/**
 * The Boss Ladder reward draft (locked 2026-07-03): <b>pure mixed pool</b>. One deterministic
 * seeded shuffle over (remaining lane raises) + (relic easers useful against the live
 * restrictions); deal {@link #DRAFT_SIZE}, no duplicates within a draft. Every card offered
 * would <em>actually do something</em> — a chest never offers "Food is permitted" when food was
 * never forbidden, never offers a raise on a freed lane. When fewer useful cards exist the
 * draft shrinks honestly (2 / 1 / empty = auto-skip); no filler.
 */
public final class LadderRewardDrafter
{
	/** Default draft size — playtest data. */
	public static final int DRAFT_SIZE = 3;

	private LadderRewardDrafter()
	{
	}

	/** Deal up to {@code count} cards from the mixed pool, deterministic under the seed. */
	public static List<LadderRewardCard> draft(RunRestrictions current, long seed, int count)
	{
		List<LadderRewardCard> pool = pool(current);
		new com.pluginideahub.roguescape.core.reward.DeterministicRng(seed).shuffle(pool);
		return pool.size() <= count ? pool : new ArrayList<>(pool.subList(0, Math.max(0, count)));
	}

	/** The whole mixed pool: every remaining lane raise + every useful easer. */
	public static List<LadderRewardCard> pool(RunRestrictions current)
	{
		List<LadderRewardCard> out = new ArrayList<>();
		if (current == null)
		{
			return out;
		}
		for (UpgradeLane lane : UpgradeLane.values())
		{
			int cap = current.laneCap(lane);
			if (cap != RunRestrictions.UNCAPPED)
			{
				out.add(LadderRewardCard.raise(lane, GradeBands.next(cap)));
			}
		}
		for (Relic relic : usefulEasers(current))
		{
			out.add(LadderRewardCard.relic(relic));
		}
		return out;
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
				case PERMIT_COMBAT_STYLE:
					if (current.isRestricted(Restriction.COMBAT_STYLE)
						&& !current.combatStyleAllowed(effect.permitsStyle()))
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
