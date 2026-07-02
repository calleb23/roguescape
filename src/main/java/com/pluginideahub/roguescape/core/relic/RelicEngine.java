package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 5 — interprets active relics during a run.
 *
 * The engine is pure-Java with no RuneLite dependencies. The run engine is expected to:
 *   1. {@link #addRelic} the relics chosen by the player at start/draft.
 *   2. Track running totals with {@link #recordItem} so category-limit warnings surface.
 *   3. Use {@link #scoreBonus} when summing run score at recap.
 */
public final class RelicEngine
{
	private final List<Relic> relics = new ArrayList<>();
	private final Map<String, Integer> mercyChargesLeft = new LinkedHashMap<>();
	private final EnumMap<BankItemCategory, Integer> categoryCounts = new EnumMap<>(BankItemCategory.class);
	private final List<String> consumedNotes = new ArrayList<>();

	public RelicEngine addRelic(Relic relic)
	{
		if (relic == null) return this;
		relics.add(relic);
		for (RelicEffect effect : relic.effects())
		{
			if (effect.kind() == RelicEffectKind.ONE_SHOT_MERCY)
			{
				mercyChargesLeft.merge(relic.relicId(), Math.max(1, effect.magnitude()), Integer::sum);
			}
		}
		return this;
	}

	public List<Relic> relics() { return Collections.unmodifiableList(relics); }
	public List<String> consumedNotes() { return Collections.unmodifiableList(consumedNotes); }

	public int mercyChargesLeft(String relicId)
	{
		Integer v = mercyChargesLeft.get(relicId);
		return v == null ? 0 : v;
	}

	public int categoryCount(BankItemCategory cat)
	{
		Integer v = categoryCounts.get(cat);
		return v == null ? 0 : v;
	}

	/** Increment counts after the engine has accepted an item event. */
	public void recordItem(BankItemCategory cat)
	{
		if (cat == null) return;
		categoryCounts.merge(cat, 1, Integer::sum);
	}

	/** Categories forbidden by active relic RESTRICTION effects, in enum order. */
	public Set<BankItemCategory> restrictedCategories()
	{
		Set<BankItemCategory> out = EnumSet.noneOf(BankItemCategory.class);
		for (Relic r : relics)
		{
			for (RelicEffect e : r.effects())
			{
				if (e.kind() == RelicEffectKind.RESTRICTION)
				{
					out.addAll(e.categories());
				}
			}
		}
		return out;
	}

	/** Category caps imposed by active relic CATEGORY_LIMIT effects (category -> max). */
	public Map<BankItemCategory, Integer> categoryLimits()
	{
		Map<BankItemCategory, Integer> out = new EnumMap<>(BankItemCategory.class);
		for (Relic r : relics)
		{
			for (RelicEffect e : r.effects())
			{
				if (e.kind() != RelicEffectKind.CATEGORY_LIMIT) continue;
				for (BankItemCategory c : e.categories())
				{
					out.merge(c, e.magnitude(), Math::min);
				}
			}
		}
		return out;
	}

	/** Returns the categories that have exceeded their relic-imposed limit. */
	public Set<BankItemCategory> overLimit()
	{
		Set<BankItemCategory> out = new HashSet<>();
		for (Relic r : relics)
		{
			for (RelicEffect e : r.effects())
			{
				if (e.kind() != RelicEffectKind.CATEGORY_LIMIT) continue;
				for (BankItemCategory c : e.categories())
				{
					if (categoryCount(c) > e.magnitude()) out.add(c);
				}
			}
		}
		return out;
	}

	/** Relic scoring bonus computed from the categories recorded via {@link #recordItem}. */
	public int scoreBonus()
	{
		return scoreBonus(categoryCounts);
	}

	/** Returns the relic-derived scoring bonus for the supplied recap counts. */
	public int scoreBonus(Map<BankItemCategory, Integer> counts)
	{
		if (counts == null || counts.isEmpty()) return 0;
		int total = 0;
		for (Relic r : relics)
		{
			for (RelicEffect e : r.effects())
			{
				if (e.kind() != RelicEffectKind.SCORING_BIAS) continue;
				for (BankItemCategory c : e.categories())
				{
					Integer count = counts.get(c);
					if (count != null) total += e.magnitude() * count;
				}
			}
		}
		return total;
	}
}
