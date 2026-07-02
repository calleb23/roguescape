package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.item.InventorySnapshot;
import com.pluginideahub.roguescape.core.item.ItemDelta;
import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage 6 — pure diff helper. Compares two snapshots and returns positive deltas as
 * {@link ItemDelta} instances. Used to convert RuneLite {@code ItemContainerChanged} signals
 * into the core's legality event stream without exposing RuneLite types.
 */
public final class InventoryDiff
{
	private InventoryDiff() {}

	/**
	 * Returns the positive deltas of {@code after} minus {@code before}, each annotated with
	 * the supplied provenance hint. Negative changes (drops/uses) are ignored — provenance
	 * matters for gains.
	 */
	public static List<ItemDelta> positiveDeltas(InventorySnapshot before, InventorySnapshot after, ProvenanceHint hint)
	{
		if (before == null) before = new InventorySnapshot();
		if (after == null) after = new InventorySnapshot();
		List<ItemDelta> out = new ArrayList<>();
		for (Map.Entry<String, Integer> e : after.asMap().entrySet())
		{
			String id = e.getKey();
			int afterQ = e.getValue();
			int beforeQ = before.quantityOf(id);
			int diff = afterQ - beforeQ;
			if (diff > 0)
			{
				out.add(new ItemDelta(id, id, diff, "", hint));
			}
		}
		return Collections.unmodifiableList(out);
	}
}
