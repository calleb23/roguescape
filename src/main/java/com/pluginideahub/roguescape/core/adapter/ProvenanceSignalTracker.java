package com.pluginideahub.roguescape.core.adapter;

import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import java.util.Locale;

/**
 * Stage 6 — tiny passive provenance signal memory for live RuneLite wiring.
 *
 * RuneLite inventory deltas do not prove why an item appeared. This helper only preserves
 * short-lived nearby signals (menu clicks/chat text) so the adapter can mark the next gain
 * as bank/trade/GE/shop/ground-pickup when the signal is obvious. Everything else remains
 * UNKNOWN and is surfaced as suspicious by the core.
 */
public final class ProvenanceSignalTracker
{
	private static final int DEFAULT_WINDOW_TICKS = 4;

	private ProvenanceHint pendingHint = ProvenanceHint.UNKNOWN;
	private int ticksRemaining;
	private String latestSignal = "";

	public ProvenanceHint currentHint()
	{
		return pendingHint;
	}

	public String latestSignal()
	{
		return latestSignal;
	}

	public boolean hasPendingHint()
	{
		return pendingHint != ProvenanceHint.UNKNOWN && ticksRemaining > 0;
	}

	public void observeMenu(String option, String target)
	{
		observe(classifyMenu(option, target), compact(option, target));
	}

	public void observeChat(String message)
	{
		observe(classifyChat(message), clean(message));
	}

	public void observe(ProvenanceHint hint, String signalText)
	{
		latestSignal = signalText == null ? "" : signalText;
		if (hint == null || hint == ProvenanceHint.UNKNOWN)
		{
			return;
		}
		pendingHint = hint;
		ticksRemaining = DEFAULT_WINDOW_TICKS;
	}

	public ProvenanceHint consumePendingHint()
	{
		ProvenanceHint hint = hasPendingHint() ? pendingHint : ProvenanceHint.UNKNOWN;
		pendingHint = ProvenanceHint.UNKNOWN;
		ticksRemaining = 0;
		return hint;
	}

	public void onGameTick()
	{
		if (ticksRemaining > 0)
		{
			ticksRemaining--;
		}
		if (ticksRemaining <= 0)
		{
			pendingHint = ProvenanceHint.UNKNOWN;
		}
	}

	public void reset()
	{
		pendingHint = ProvenanceHint.UNKNOWN;
		ticksRemaining = 0;
		latestSignal = "";
	}

	public static ProvenanceHint classifyMenu(String option, String target)
	{
		String text = compact(option, target).toLowerCase(Locale.ROOT);
		if (text.isEmpty()) return ProvenanceHint.UNKNOWN;

		if (containsAny(text, "grand exchange", "collect bought", "collect offer", "exchange collect", " ge "))
		{
			return ProvenanceHint.OBSERVED_GE_COLLECT;
		}
		if (containsAny(text, "accept trade", "trade with", "trading with", "trade "))
		{
			return ProvenanceHint.OBSERVED_TRADE;
		}
		if (containsAny(text, "withdraw", "withdraw-all", "withdraw-x", "bank chest", "bank booth", "deposit box"))
		{
			return ProvenanceHint.OBSERVED_BANK_WITHDRAWAL;
		}
		if (containsAny(text, "buy ", "buy-", "shop", "store"))
		{
			return ProvenanceHint.OBSERVED_SHOP_PURCHASE;
		}
		if (containsAny(text, "take ", "pick-up", "pickup", "pick up"))
		{
			return ProvenanceHint.OBSERVED_GROUND_PICKUP;
		}
		return ProvenanceHint.UNKNOWN;
	}

	public static ProvenanceHint classifyChat(String message)
	{
		String text = clean(message).toLowerCase(Locale.ROOT);
		if (text.isEmpty()) return ProvenanceHint.UNKNOWN;

		if (containsAny(text, "grand exchange", "collected", "bought item", "completed offer"))
		{
			return ProvenanceHint.OBSERVED_GE_COLLECT;
		}
		if (containsAny(text, "accepted trade", "trade accepted", "you trade", "trading with"))
		{
			return ProvenanceHint.OBSERVED_TRADE;
		}
		if (containsAny(text, "you buy", "you bought", "you purchase", "you purchased", "shop"))
		{
			return ProvenanceHint.OBSERVED_SHOP_PURCHASE;
		}
		if (containsAny(text, "you withdraw", "withdrawn from your bank", "from your bank"))
		{
			return ProvenanceHint.OBSERVED_BANK_WITHDRAWAL;
		}
		return ProvenanceHint.UNKNOWN;
	}

	public static boolean isLikelyDeathMessage(String message)
	{
		String text = clean(message).toLowerCase(Locale.ROOT);
		return containsAny(text, "oh dear, you are dead", "you have died", "you are dead", "death's office");
	}

	private static boolean containsAny(String text, String... needles)
	{
		for (String needle : needles)
		{
			if (text.contains(needle)) return true;
		}
		return false;
	}

	private static String compact(String option, String target)
	{
		String left = clean(option);
		String right = clean(target);
		if (left.isEmpty()) return right;
		if (right.isEmpty()) return left;
		return left + " " + right;
	}

	private static String clean(String text)
	{
		if (text == null) return "";
		return text.replaceAll("<[^>]*>", " ").replace('\u00A0', ' ').trim().replaceAll("\\s+", " ");
	}
}
