package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.RunContext;
import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import java.util.List;
import java.util.function.IntFunction;

/**
 * Owns the inventory-diff pipeline the plugin's {@code onItemContainerChanged} delegates to: diff the
 * previous vs next inventory snapshot, consume the pending provenance hint exactly once, annotate each
 * gained item with a resolved name + the current region, apply it to the run, and report the refreshed
 * status lines. The RuneLite-specific parts — snapshotting the live container and resolving an item id
 * to a name — stay in the plugin and are passed in (an {@link InventorySnapshot} plus an item-name
 * resolver), so the whole pipeline is pure and unit-testable without a live {@code Client}.
 */
public final class InventoryProvenanceTracker
{
	private InventoryProvenanceTracker() {}

	/** Outcome of an inventory change: whether anything was applied + the refreshed status strings. */
	public static final class Result
	{
		private final boolean changed;
		private final String latestObservedItem;
		private final String latestProvenanceSignal;

		private Result(boolean changed, String latestObservedItem, String latestProvenanceSignal)
		{
			this.changed = changed;
			this.latestObservedItem = latestObservedItem;
			this.latestProvenanceSignal = latestProvenanceSignal;
		}

		/** No positive gains (or no active run): the host leaves its signal/item fields untouched. */
		public static Result unchanged()
		{
			return new Result(false, "", "");
		}

		public boolean changed()
		{
			return changed;
		}

		/** Last applied "name xN" string; only meaningful when {@link #changed()}. */
		public String latestObservedItem()
		{
			return latestObservedItem;
		}

		/** Provenance line for the consumed hint; only meaningful when {@link #changed()}. */
		public String latestProvenanceSignal()
		{
			return latestProvenanceSignal;
		}
	}

	/**
	 * Applies the positive gains of {@code next} over {@code previous} to {@code ctx.run()}. The pending
	 * hint is read once for the diff and consumed once for the annotation, matching the plugin's original
	 * ordering. Returns {@link Result#unchanged()} when there is no active run or no positive delta; the
	 * caller still advances its own baseline to {@code next} regardless.
	 */
	public static Result apply(RunContext ctx, ProvenanceSignalTracker signals,
		InventorySnapshot previous, InventorySnapshot next, IntFunction<String> itemNameById)
	{
		if (ctx == null || ctx.run() == null)
		{
			return Result.unchanged();
		}
		ProvenanceHint pendingHint = signals == null ? ProvenanceHint.UNKNOWN : signals.currentHint();
		List<ItemDelta> deltas = InventoryDiff.positiveDeltas(previous, next, pendingHint);
		if (deltas.isEmpty())
		{
			return Result.unchanged();
		}
		ProvenanceHint consumedHint = signals == null ? ProvenanceHint.UNKNOWN : signals.consumePendingHint();
		String regionNote = regionNote(ctx.currentRegionId());
		String latestObservedItem = "";
		for (ItemDelta delta : deltas)
		{
			ItemDelta named = withNameAndRegion(delta, consumedHint, regionNote, itemNameById);
			ctx.run().applyItemDelta(named);
			latestObservedItem = named.itemName() + " x" + named.quantity();
		}
		return new Result(true, latestObservedItem, provenanceLine(consumedHint));
	}

	private static ItemDelta withNameAndRegion(ItemDelta delta, ProvenanceHint consumedHint,
		String regionNote, IntFunction<String> itemNameById)
	{
		String itemId = delta.itemId();
		String itemName = delta.itemName();
		try
		{
			int numericId = Integer.parseInt(itemId);
			String resolved = itemNameById == null ? null : itemNameById.apply(numericId);
			itemName = resolved == null || resolved.isEmpty() ? itemId : resolved;
		}
		catch (NumberFormatException ignored)
		{
			// InventorySnapshot keys are numeric in live RuneLite wiring, but pure tests may use names.
		}
		return new ItemDelta(itemId, itemName, delta.quantity(), regionNote, consumedHint);
	}

	private static String regionNote(String currentRegionId)
	{
		return currentRegionId == null || currentRegionId.isEmpty() ? "unknown region" : "region " + currentRegionId;
	}

	private static String provenanceLine(ProvenanceHint hint)
	{
		return hint == null || hint == ProvenanceHint.UNKNOWN ? "unknown source" : hint.name();
	}
}
