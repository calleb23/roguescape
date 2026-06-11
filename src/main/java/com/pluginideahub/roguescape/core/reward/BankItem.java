package com.pluginideahub.roguescape.core.reward;

import java.util.Objects;

/**
 * Stage 4 — a candidate item in the player's bank-draft pool. Items are immutable and
 * carry classifier output (category + value tier) so the fairness policy can filter the
 * pool without re-running heuristics on every draft.
 */
public final class BankItem
{
	private final String itemId;
	private final String itemName;
	private final BankItemCategory category;
	private final ValueTier valueTier;
	private final long approxPrice;

	public BankItem(String itemId, String itemName, BankItemCategory category, ValueTier tier, long approxPrice)
	{
		if (itemName == null || itemName.isEmpty()) throw new IllegalArgumentException("itemName required");
		this.itemId = itemId != null ? itemId : itemName.toLowerCase();
		this.itemName = itemName;
		this.category = category != null ? category : BankItemCategory.UNKNOWN;
		this.valueTier = tier != null ? tier : ValueTier.ofPrice(approxPrice);
		this.approxPrice = Math.max(0, approxPrice);
	}

	public String itemId() { return itemId; }
	public String itemName() { return itemName; }
	public BankItemCategory category() { return category; }
	public ValueTier valueTier() { return valueTier; }
	public long approxPrice() { return approxPrice; }

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof BankItem)) return false;
		BankItem that = (BankItem) o;
		return Objects.equals(itemId, that.itemId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(itemId);
	}

	@Override
	public String toString()
	{
		return "BankItem{" + itemName + " [" + category + "/" + valueTier + "]}";
	}
}
