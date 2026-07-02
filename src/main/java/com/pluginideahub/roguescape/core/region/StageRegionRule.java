package com.pluginideahub.roguescape.core.region;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stage 3 — region rule attached to a route stage. Defines:
 *
 * <ul>
 *   <li>{@code roomKind} — finer category for the stage (weapon/armour/supply/crafting/boss).</li>
 *   <li>{@code allowedRegionIds} — the allowed region IDs the player must remain within
 *       while this stage is current. Empty means "no region restriction for this stage".</li>
 *   <li>{@code allowsRegionGain} — whether items observed inside the allowed region count
 *       as allowed region gains. Boss/choice-chest stages may leave this false.</li>
 * </ul>
 *
 * Instances are immutable.
 */
public final class StageRegionRule
{
	public static final StageRegionRule UNRESTRICTED = new StageRegionRule(RoomKind.SUPPLY, Collections.emptySet(), true);

	private final RoomKind roomKind;
	private final Set<String> allowedRegionIds;
	private final boolean allowsRegionGain;

	public StageRegionRule(RoomKind roomKind, Set<String> allowedRegionIds, boolean allowsRegionGain)
	{
		this.roomKind = roomKind == null ? RoomKind.SUPPLY : roomKind;
		Set<String> copy = new LinkedHashSet<>();
		if (allowedRegionIds != null)
		{
			for (String id : allowedRegionIds)
			{
				if (id != null && !id.isEmpty()) copy.add(id);
			}
		}
		this.allowedRegionIds = Collections.unmodifiableSet(copy);
		this.allowsRegionGain = allowsRegionGain;
	}

	public RoomKind roomKind() { return roomKind; }
	public Set<String> allowedRegionIds() { return allowedRegionIds; }
	public boolean allowsRegionGain() { return allowsRegionGain; }
	public boolean restrictsRegion() { return !allowedRegionIds.isEmpty(); }

	public boolean isAllowedRegion(String regionId)
	{
		if (allowedRegionIds.isEmpty()) return true;
		return regionId != null && allowedRegionIds.contains(regionId);
	}
}
