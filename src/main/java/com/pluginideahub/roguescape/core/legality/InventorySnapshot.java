package com.pluginideahub.roguescape.core.legality;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 2 — immutable inventory snapshot keyed by item id. Used to compute positive deltas
 * between observations. Pre-run snapshots are used to detect items that already exist when
 * a run begins (pre-run supplies).
 */
public final class InventorySnapshot
{
	private final Map<String, Integer> quantities;

	public InventorySnapshot()
	{
		this.quantities = Collections.emptyMap();
	}

	public InventorySnapshot(Map<String, Integer> quantities)
	{
		Map<String, Integer> copy = new LinkedHashMap<>();
		if (quantities != null)
		{
			for (Map.Entry<String, Integer> e : quantities.entrySet())
			{
				if (e.getKey() == null) continue;
				int q = e.getValue() == null ? 0 : e.getValue();
				if (q > 0) copy.put(e.getKey(), q);
			}
		}
		this.quantities = Collections.unmodifiableMap(copy);
	}

	public int quantityOf(String itemId)
	{
		Integer q = quantities.get(itemId);
		return q == null ? 0 : q;
	}

	public Map<String, Integer> asMap()
	{
		return quantities;
	}

	public boolean contains(String itemId)
	{
		return quantityOf(itemId) > 0;
	}

	public int totalItems()
	{
		int sum = 0;
		for (Integer v : quantities.values()) sum += v;
		return sum;
	}

	public boolean isEmpty()
	{
		return quantities.isEmpty();
	}
}
