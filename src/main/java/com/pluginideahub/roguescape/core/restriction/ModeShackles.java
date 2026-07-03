package com.pluginideahub.roguescape.core.restriction;

/**
 * The standard shackle sets each mode starts under (locked 2026-07-03, session 2).
 *
 * <p><b>Dungeon Crawl</b>: Bank + GE + Trade sealed, Prayer locked, the three high prayers
 * locked, spellbook bound to Standard, potions locked — <b>food stays free</b> (you must eat to
 * survive the early rooms). Nine live restrictions mean every chest has real choices all run;
 * the rise-of-power arc is the mode. Boss Ladder has no standard shackles — StartTier is its
 * one knob; Custom starts from its chassis's set and edits freely.
 */
public final class ModeShackles
{
	private ModeShackles()
	{
	}

	/** A fresh restriction state under the Dungeon Crawl standard shackle set. */
	public static RunRestrictions dungeonCrawl()
	{
		return applyDungeonCrawl(new RunRestrictions());
	}

	/** Applies the Dungeon Crawl shackle set to an existing state (chassis use). */
	public static RunRestrictions applyDungeonCrawl(RunRestrictions r)
	{
		if (r == null)
		{
			return null;
		}
		r.restrict(Restriction.BANK);
		r.restrict(Restriction.GRAND_EXCHANGE);
		r.restrict(Restriction.TRADE);
		r.restrict(Restriction.PRAYER);
		r.restrict(Restriction.PIETY);
		r.restrict(Restriction.RIGOUR);
		r.restrict(Restriction.AUGURY);
		r.restrict(Restriction.POTIONS);
		r.restrictSpellbook(Spellbook.STANDARD);
		return r;
	}
}
