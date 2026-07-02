package com.pluginideahub.roguescape.core.relic;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stage 5 — a single modifier carried by a relic. A relic can have one or more effects.
 *
 * Effects are interpreted by {@link RelicEngine}, which the run engine consults when
 * classifying items, summing the score, and checking soft limits. Effects are pure data —
 * they describe what to do, not how to do it.
 */
public final class RelicEffect
{
	private final RelicEffectKind kind;
	private final Set<BankItemCategory> categories;
	private final Set<String> itemIds;
	private final int magnitude;
	private final com.pluginideahub.roguescape.core.restriction.Restriction eases;

	private RelicEffect(RelicEffectKind kind, Set<BankItemCategory> categories, Set<String> itemIds, int magnitude)
	{
		this(kind, categories, itemIds, magnitude, null);
	}

	private RelicEffect(RelicEffectKind kind, Set<BankItemCategory> categories, Set<String> itemIds, int magnitude,
		com.pluginideahub.roguescape.core.restriction.Restriction eases)
	{
		if (kind == null) throw new IllegalArgumentException("kind required");
		this.kind = kind;
		this.categories = Collections.unmodifiableSet(new HashSet<>(categories));
		this.itemIds = Collections.unmodifiableSet(new HashSet<>(itemIds));
		this.magnitude = magnitude;
		this.eases = eases;
	}

	public RelicEffectKind kind() { return kind; }
	public Set<BankItemCategory> categories() { return categories; }
	public Set<String> itemIds() { return itemIds; }
	public int magnitude() { return magnitude; }
	/** The restriction this effect lifts (EASE_RESTRICTION only), else null. */
	public com.pluginideahub.roguescape.core.restriction.Restriction eases() { return eases; }

	public boolean matchesCategory(BankItemCategory c) { return categories.isEmpty() || categories.contains(c); }
	public boolean matchesItemId(String id) { return itemIds.isEmpty() || (id != null && itemIds.contains(id)); }

	public static RelicEffect oneShotMercy()
	{
		return new RelicEffect(RelicEffectKind.ONE_SHOT_MERCY, Collections.emptySet(), Collections.emptySet(), 1);
	}

	public static RelicEffect categoryLimit(BankItemCategory category, int max)
	{
		Set<BankItemCategory> cs = new HashSet<>();
		cs.add(category);
		return new RelicEffect(RelicEffectKind.CATEGORY_LIMIT, cs, Collections.emptySet(), Math.max(0, max));
	}

	public static RelicEffect scoringBias(BankItemCategory category, int bonusPerItem)
	{
		Set<BankItemCategory> cs = new HashSet<>();
		if (category != null) cs.add(category);
		return new RelicEffect(RelicEffectKind.SCORING_BIAS, cs, Collections.emptySet(), bonusPerItem);
	}

	public static RelicEffect restriction(BankItemCategory category)
	{
		Set<BankItemCategory> cs = new HashSet<>();
		cs.add(category);
		return new RelicEffect(RelicEffectKind.RESTRICTION, cs, Collections.emptySet(), 0);
	}

	public static RelicEffect restrictionById(String itemId)
	{
		Set<String> ids = new HashSet<>();
		if (itemId != null) ids.add(itemId.toLowerCase());
		return new RelicEffect(RelicEffectKind.RESTRICTION, Collections.emptySet(), ids, 0);
	}

	/** Lift one restriction — the canonical restriction-remover relic effect. */
	public static RelicEffect ease(com.pluginideahub.roguescape.core.restriction.Restriction restriction)
	{
		if (restriction == null) throw new IllegalArgumentException("restriction required");
		return new RelicEffect(RelicEffectKind.EASE_RESTRICTION, Collections.emptySet(), Collections.emptySet(), 0,
			restriction);
	}

	/** Raise the gear-tier cap by {@code by} equip levels. */
	public static RelicEffect raiseGearTier(int by)
	{
		return new RelicEffect(RelicEffectKind.RAISE_GEAR_TIER, Collections.emptySet(), Collections.emptySet(),
			Math.max(0, by));
	}

	/** Grant {@code slots} extra inventory slots under an inventory limit. */
	public static RelicEffect addInventorySlots(int slots)
	{
		return new RelicEffect(RelicEffectKind.ADD_INVENTORY_SLOTS, Collections.emptySet(), Collections.emptySet(),
			Math.max(0, slots));
	}
}
