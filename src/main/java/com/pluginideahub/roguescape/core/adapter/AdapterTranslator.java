package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.item.ItemDelta;
import com.pluginideahub.roguescape.core.item.ProvenanceHint;

/**
 * Stage 6 — converts an {@link ObservedEvent} (adapter signal) into an {@link ItemDelta}
 * carrying the right {@link ProvenanceHint}. The adapter calls this for every observed
 * event to feed the run engine.
 *
 * The mapping is intentionally narrow and explicit — it never invents provenance. Events
 * without item content (like {@link ObservedEventKind#GAME_TICK}) return {@code null}.
 */
public final class AdapterTranslator
{
	private AdapterTranslator() {}

	public static ItemDelta toItemDelta(ObservedEvent event)
	{
		if (event == null) return null;
		String name = event.attr("itemName");
		if (name == null || name.isEmpty()) return null;
		String id = event.attr("itemId", name.toLowerCase());
		int quantity = parseInt(event.attr("quantity"), 1);
		if (quantity <= 0) return null;
		ProvenanceHint hint = hintFor(event.kind());
		return new ItemDelta(id, name, quantity, event.attr("locationNote", event.regionId()), hint);
	}

	public static ProvenanceHint hintFor(ObservedEventKind kind)
	{
		if (kind == null) return ProvenanceHint.UNKNOWN;
		switch (kind)
		{
			case BANK_WITHDRAWAL: return ProvenanceHint.OBSERVED_BANK_WITHDRAWAL;
			case TRADE_ACCEPTED: return ProvenanceHint.OBSERVED_TRADE;
			case GE_COLLECTED: return ProvenanceHint.OBSERVED_GE_COLLECT;
			case SHOP_PURCHASE: return ProvenanceHint.OBSERVED_SHOP_PURCHASE;
			case INVENTORY_CHANGE: return ProvenanceHint.UNKNOWN;
			default: return ProvenanceHint.UNKNOWN;
		}
	}

	private static int parseInt(String s, int fallback)
	{
		if (s == null || s.isEmpty()) return fallback;
		try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
	}
}
