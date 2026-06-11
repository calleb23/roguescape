package com.pluginideahub.roguescape.core.adapter;

/**
 * Stage 6 — categories of observable game events the adapter can emit. Mirrors a subset of
 * RuneLite signals but is decoupled from the live SDK so the core can be tested without it.
 *
 * Plugin Hub safety: every event is observational — the adapter never acts on the game, and
 * downstream classifiers treat each observation as a signal, not proof.
 */
public enum ObservedEventKind
{
	INVENTORY_CHANGE,
	BANK_OPENED,
	BANK_WITHDRAWAL,
	BANK_DEPOSIT,
	TRADE_ACCEPTED,
	GE_COLLECTED,
	SHOP_PURCHASE,
	DEATH,
	REGION_CHANGED,
	GAME_TICK
}
