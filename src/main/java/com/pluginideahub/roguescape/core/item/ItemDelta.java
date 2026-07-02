package com.pluginideahub.roguescape.core.item;

import java.util.Objects;

/**
 * Stage 2 — observed positive inventory change. Negative deltas (drops/uses) are tracked
 * separately or as part of richer adapters; this type focuses on item *gains* because
 * legality is primarily about how new items entered the run.
 */
public final class ItemDelta
{
	private final String itemId;
	private final String itemName;
	private final int quantity;
	private final String locationNote;
	private final ProvenanceHint provenanceHint;

	public ItemDelta(String itemId, String itemName, int quantity, String locationNote, ProvenanceHint provenanceHint)
	{
		if (itemName == null) throw new IllegalArgumentException("itemName required");
		if (quantity <= 0) throw new IllegalArgumentException("ItemDelta quantity must be positive: " + quantity);
		this.itemId = itemId;
		this.itemName = itemName;
		this.quantity = quantity;
		this.locationNote = locationNote == null ? "" : locationNote;
		this.provenanceHint = provenanceHint == null ? ProvenanceHint.UNKNOWN : provenanceHint;
	}

	public String itemId() { return itemId; }
	public String itemName() { return itemName; }
	public int quantity() { return quantity; }
	public String locationNote() { return locationNote; }
	public ProvenanceHint provenanceHint() { return provenanceHint; }

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof ItemDelta)) return false;
		ItemDelta that = (ItemDelta) o;
		return quantity == that.quantity
			&& Objects.equals(itemId, that.itemId)
			&& Objects.equals(itemName, that.itemName)
			&& Objects.equals(locationNote, that.locationNote)
			&& provenanceHint == that.provenanceHint;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(itemId, itemName, quantity, locationNote, provenanceHint);
	}

	@Override
	public String toString()
	{
		return "ItemDelta{" + itemName + " x" + quantity + " hint=" + provenanceHint + "}";
	}
}
