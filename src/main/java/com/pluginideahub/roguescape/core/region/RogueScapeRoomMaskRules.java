package com.pluginideahub.roguescape.core.region;

import java.util.Collections;
import java.util.Set;

/**
 * Pure rules for the passive room-region overlay mask. RuneLite overlay asks this
 * whether a visible tile should be dimmed; configuration edge cases stay out of
 * the overlay so they can be unit-tested.
 *
 * Allowed region IDs are stored as Strings to match {@link StageRegionRule}, which
 * is the source-of-truth for the current stage's legal regions.
 */
public final class RogueScapeRoomMaskRules
{
	private static final int MIN_OPACITY = 20;
	private static final int MAX_OPACITY = 220;

	private final boolean enabled;
	private final boolean inRun;
	private final int opacity;
	private final Set<String> allowedRegionIds;

	public RogueScapeRoomMaskRules(boolean enabled, boolean inRun, int opacity, Set<String> allowedRegionIds)
	{
		this.enabled = enabled;
		this.inRun = inRun;
		this.opacity = opacity;
		this.allowedRegionIds = allowedRegionIds == null ? Collections.emptySet() : allowedRegionIds;
	}

	public boolean shouldRender()
	{
		return enabled && inRun && !allowedRegionIds.isEmpty();
	}

	public boolean shouldMaskRegion(int regionId)
	{
		return shouldRender() && !allowedRegionIds.contains(String.valueOf(regionId));
	}

	public int getClampedOpacity()
	{
		return Math.max(MIN_OPACITY, Math.min(MAX_OPACITY, opacity));
	}
}
