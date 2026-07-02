package com.pluginideahub.roguescape.core.restriction;

/**
 * One thing the overlay should cross out with a red X, and why.
 *
 * <p>For inventory markers, {@link #slot} is the inventory slot index (0–27) and {@link #reason}
 * is the active restriction that forbids the item there — e.g. {@code slot 3, FOOD}. The adapter
 * turns this into a red X drawn over that widget; the {@code reason} can drive a tooltip
 * ("No Food"). The decision is made in pure core; only the drawing is client-side.
 */
public final class RestrictionMarker
{
	private final int slot;
	private final Restriction reason;

	public RestrictionMarker(int slot, Restriction reason)
	{
		this.slot = slot;
		this.reason = reason;
	}

	public int slot() { return slot; }
	public Restriction reason() { return reason; }

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof RestrictionMarker)) return false;
		RestrictionMarker m = (RestrictionMarker) o;
		return slot == m.slot && reason == m.reason;
	}

	@Override
	public int hashCode()
	{
		return slot * 31 + (reason == null ? 0 : reason.hashCode());
	}

	@Override
	public String toString()
	{
		return "RestrictionMarker{slot=" + slot + ", reason=" + reason + "}";
	}
}
