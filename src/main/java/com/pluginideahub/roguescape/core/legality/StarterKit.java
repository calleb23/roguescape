package com.pluginideahub.roguescape.core.legality;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 2 — declared starter kit. Items present in the kit (up to the declared quantity)
 * are classified as LEGAL_STARTER_KIT. Anything in inventory beyond the declared kit at
 * run start is treated as pre-run supply / suspicious by the classifier.
 */
public final class StarterKit
{
	private final Map<String, Integer> declared;

	public StarterKit()
	{
		this.declared = Collections.emptyMap();
	}

	public StarterKit(Map<String, Integer> declared)
	{
		Map<String, Integer> copy = new LinkedHashMap<>();
		if (declared != null)
		{
			for (Map.Entry<String, Integer> e : declared.entrySet())
			{
				if (e.getKey() == null) continue;
				int q = e.getValue() == null ? 0 : e.getValue();
				if (q > 0) copy.put(e.getKey(), q);
			}
		}
		this.declared = Collections.unmodifiableMap(copy);
	}

	public Map<String, Integer> asMap()
	{
		return declared;
	}

	public int declaredQuantity(String itemId)
	{
		Integer q = declared.get(itemId);
		return q == null ? 0 : q;
	}

	public boolean contains(String itemId)
	{
		return declaredQuantity(itemId) > 0;
	}

	public boolean isEmpty()
	{
		return declared.isEmpty();
	}

	public int totalItems()
	{
		int sum = 0;
		for (Integer v : declared.values()) sum += v;
		return sum;
	}
}
