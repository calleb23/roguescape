package com.pluginideahub.roguescape.core.reward;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stage 4 — fairness/economy policy applied to the bank-draft pool before a chest rolls.
 *
 * The first-pass policy is deliberately minimal but exposes seams the end-state design
 * calls out:
 *
 * <ul>
 *   <li>{@code minTier}/{@code maxTier} — value band a chest is allowed to roll within.</li>
 *   <li>{@code minEligiblePerCategory} — minimum number of eligible items required in a
 *       category before that category will roll (e.g. avoid rolling Weapon Chest when the
 *       bank only has 2 weapons).</li>
 *   <li>{@code excludedItemIds} — creator/manual exclusions (e.g. ban specific items).</li>
 *   <li>{@code excludedCategories} — exclude entire categories (e.g. ban JUNK).</li>
 * </ul>
 */
public final class FairnessPolicy
{
	public static final FairnessPolicy DEFAULT = new Builder().build();

	private final ValueTier minTier;
	private final ValueTier maxTier;
	private final Map<BankItemCategory, Integer> minEligiblePerCategory;
	private final Set<String> excludedItemIds;
	private final Set<BankItemCategory> excludedCategories;

	private FairnessPolicy(Builder b)
	{
		this.minTier = b.minTier;
		this.maxTier = b.maxTier;
		this.minEligiblePerCategory = Collections.unmodifiableMap(new EnumMap<>(b.minEligiblePerCategory));
		this.excludedItemIds = Collections.unmodifiableSet(new HashSet<>(b.excludedItemIds));
		this.excludedCategories = Collections.unmodifiableSet(new HashSet<>(b.excludedCategories));
	}

	public ValueTier minTier() { return minTier; }
	public ValueTier maxTier() { return maxTier; }
	public Map<BankItemCategory, Integer> minEligiblePerCategory() { return minEligiblePerCategory; }
	public Set<String> excludedItemIds() { return excludedItemIds; }
	public Set<BankItemCategory> excludedCategories() { return excludedCategories; }

	public int minEligibleFor(BankItemCategory category)
	{
		Integer v = minEligiblePerCategory.get(category);
		return v == null ? 0 : v;
	}

	public boolean accepts(BankItem item)
	{
		if (item == null) return false;
		if (excludedItemIds.contains(item.itemId())) return false;
		if (excludedCategories.contains(item.category())) return false;
		if (!item.valueTier().isAtLeast(minTier)) return false;
		if (!item.valueTier().isAtMost(maxTier)) return false;
		return true;
	}

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private ValueTier minTier = ValueTier.TIER_1;
		private ValueTier maxTier = ValueTier.TIER_6;
		private final EnumMap<BankItemCategory, Integer> minEligiblePerCategory = new EnumMap<>(BankItemCategory.class);
		private final Set<String> excludedItemIds = new HashSet<>();
		private final Set<BankItemCategory> excludedCategories = new HashSet<>();

		public Builder valueBand(ValueTier min, ValueTier max)
		{
			if (min != null) this.minTier = min;
			if (max != null) this.maxTier = max;
			return this;
		}

		public Builder minEligible(BankItemCategory cat, int min)
		{
			if (cat != null && min > 0) minEligiblePerCategory.put(cat, min);
			return this;
		}

		public Builder exclude(String itemId)
		{
			if (itemId != null) excludedItemIds.add(itemId);
			return this;
		}

		public Builder excludeCategory(BankItemCategory cat)
		{
			if (cat != null) excludedCategories.add(cat);
			return this;
		}

		public FairnessPolicy build() { return new FairnessPolicy(this); }
	}
}
