package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 4 — the player's max-main bank-draft pool. Holds the catalog of usable bank items,
 * the fairness policy, and the set of unlock IDs already drafted.
 *
 * Drafts are produced deterministically from a seed-derived RNG so race mode can offer the
 * same option set to multiple players.
 */
public final class BankDraftPool
{
	private final Map<String, BankItem> catalog = new LinkedHashMap<>();
	private final Set<String> unlocked = new HashSet<>();
	private FairnessPolicy fairnessPolicy = FairnessPolicy.DEFAULT;

	public BankDraftPool add(BankItem item)
	{
		if (item != null) catalog.put(item.itemId(), item);
		return this;
	}

	public BankDraftPool addAll(Iterable<BankItem> items)
	{
		if (items != null) for (BankItem i : items) add(i);
		return this;
	}

	public BankDraftPool setFairnessPolicy(FairnessPolicy policy)
	{
		if (policy != null) this.fairnessPolicy = policy;
		return this;
	}

	public FairnessPolicy fairnessPolicy() { return fairnessPolicy; }

	public Map<String, BankItem> catalog() { return Collections.unmodifiableMap(catalog); }

	public Set<String> unlockedIds() { return Collections.unmodifiableSet(unlocked); }

	public boolean isUnlocked(String itemId) { return itemId != null && unlocked.contains(itemId); }

	public BankItem get(String itemId) { return catalog.get(itemId); }

	public int size() { return catalog.size(); }

	public List<BankItem> eligibleFor(ChestType chestType)
	{
		List<BankItem> out = new ArrayList<>();
		for (BankItem item : catalog.values())
		{
			if (!fairnessPolicy.accepts(item)) continue;
			if (!matchesChestType(chestType, item.category())) continue;
			out.add(item);
		}
		return out;
	}

	public Map<BankItemCategory, Integer> categoryCounts()
	{
		Map<BankItemCategory, Integer> counts = new EnumMap<>(BankItemCategory.class);
		for (BankItem item : catalog.values())
		{
			if (!fairnessPolicy.accepts(item)) continue;
			counts.merge(item.category(), 1, Integer::sum);
		}
		return counts;
	}

	public boolean canRoll(ChestType chestType)
	{
		List<BankItem> eligible = eligibleFor(chestType);
		BankItemCategory primary = primaryCategoryFor(chestType);
		int required = primary != null ? fairnessPolicy.minEligibleFor(primary) : 0;
		// Always require at least three eligible items per chest type for a 3-choice draft.
		int needed = Math.max(3, required);
		return eligible.size() >= needed;
	}

	public void markUnlocked(BankItem item)
	{
		if (item != null) unlocked.add(item.itemId());
	}

	private static boolean matchesChestType(ChestType chest, BankItemCategory category)
	{
		switch (chest)
		{
			case WEAPON:
				return category == BankItemCategory.MELEE_WEAPON
					|| category == BankItemCategory.RANGED_WEAPON
					|| category == BankItemCategory.MAGIC_WEAPON;
			case ARMOUR:
				return category == BankItemCategory.HELMET
					|| category == BankItemCategory.BODY
					|| category == BankItemCategory.LEGS
					|| category == BankItemCategory.BOOTS
					|| category == BankItemCategory.GLOVES
					|| category == BankItemCategory.CAPE
					|| category == BankItemCategory.SHIELD;
			case FOOD: return category == BankItemCategory.FOOD;
			case POTION: return category == BankItemCategory.POTION;
			case AMMO: return category == BankItemCategory.AMMO || category == BankItemCategory.RUNE;
			case SUPPLY:
				return category == BankItemCategory.FOOD
					|| category == BankItemCategory.POTION
					|| category == BankItemCategory.SKILLING_SUPPLY;
			case UTILITY:
				return category == BankItemCategory.TELEPORT
					|| category == BankItemCategory.RING
					|| category == BankItemCategory.NECK
					|| category == BankItemCategory.RUNE;
			case BANK_UNLOCK: return category != BankItemCategory.JUNK && category != BankItemCategory.UNKNOWN;
			default: return false;
		}
	}

	private static BankItemCategory primaryCategoryFor(ChestType chest)
	{
		switch (chest)
		{
			case WEAPON: return BankItemCategory.MELEE_WEAPON;
			case ARMOUR: return BankItemCategory.BODY;
			case FOOD: return BankItemCategory.FOOD;
			case POTION: return BankItemCategory.POTION;
			case AMMO: return BankItemCategory.AMMO;
			case UTILITY: return BankItemCategory.TELEPORT;
			case SUPPLY: return BankItemCategory.FOOD;
			case BANK_UNLOCK: return null;
			default: return null;
		}
	}
}
