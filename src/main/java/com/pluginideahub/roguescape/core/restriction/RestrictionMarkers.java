package com.pluginideahub.roguescape.core.restriction;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import java.util.ArrayList;
import java.util.List;

/**
 * The single brain behind "what should be crossed out right now." Given the player's inventory,
 * classified by category, and the run's active restrictions, it returns the slots the overlay
 * should mark with a red X. Pure core — the adapter classifies the real inventory (via ItemManager)
 * and draws the crosses; this decides which.
 *
 * <p>The same {@link RunRestrictions#forbiddenBy}/{@link RunRestrictions#decideItemUse} logic that
 * produces these markers also drives the menu block, so the visual and the enforcement can never
 * disagree.
 */
public final class RestrictionMarkers
{
	private RestrictionMarkers() {}

	/**
	 * @param slotCategories one entry per inventory slot (index = slot), the item's category or
	 *        {@code null}/{@link BankItemCategory#UNKNOWN} for an empty or unclassified slot.
	 */
	public static List<RestrictionMarker> forInventory(List<BankItemCategory> slotCategories, RunRestrictions restrictions)
	{
		List<RestrictionMarker> markers = new ArrayList<>();
		if (slotCategories == null || restrictions == null)
		{
			return markers;
		}
		for (int slot = 0; slot < slotCategories.size(); slot++)
		{
			Restriction forbidden = restrictions.forbiddenBy(slotCategories.get(slot));
			if (forbidden != null)
			{
				markers.add(new RestrictionMarker(slot, forbidden));
			}
		}
		return markers;
	}
}
