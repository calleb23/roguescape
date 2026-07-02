package com.pluginideahub.roguescape.core.seed;

import com.pluginideahub.roguescape.core.reward.DeterministicRng;

/**
 * Smart route names: a stable, evocative title derived from the route seed, so the route
 * catalogue reads like a table of contents ("The Rat King's Road") instead of raw seed strings.
 * Same seed → same name, always.
 */
public final class RouteNames
{
	private static final String[] FIRST = {
		"The Rat King's", "The Molten", "The Sunken", "The Crooked", "The Pilgrim's",
		"The Beggar's", "The Iron", "The Whispering", "The Drowned", "The Gilded",
		"The Wolf's", "The Broken", "The Last", "The Silent", "The Wandering", "The Cursed"
	};

	private static final String[] SECOND = {
		"Road", "Vigil", "Descent", "Crossing", "March",
		"Bargain", "Hunt", "Passage", "Reckoning", "Climb",
		"Detour", "Oath", "Gambit", "Toll", "Circuit", "Pilgrimage"
	};

	private RouteNames()
	{
	}

	/** The stable display name for a route seed. */
	public static String smartName(String seed)
	{
		long h = DeterministicRng.hash(seed == null ? "" : seed);
		int a = (int) Math.floorMod(h, FIRST.length);
		int b = (int) Math.floorMod(h >>> 8, SECOND.length);
		return FIRST[a] + " " + SECOND[b];
	}
}
