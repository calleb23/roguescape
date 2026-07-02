package com.pluginideahub.roguescape.core.campaign;

import com.pluginideahub.roguescape.core.RunPreset;
import java.util.Collections;
import java.util.List;

public final class CampaignLibrary
{
	private CampaignLibrary() {}

	public static CampaignDefinition find(RunPreset preset)
	{
		if (preset == null || preset == RunPreset.UNSPECIFIED)
		{
			return null;
		}
		for (CampaignDefinition campaign : all())
		{
			if (campaign.preset() == preset)
			{
				return campaign;
			}
		}
		return null;
	}

	/**
	 * Curated preset runs are intentionally empty: the earlier auto-generated presets had no real
	 * game-design context and were not worth keeping. Real, hand-built runs will be authored later.
	 * The campaign mechanism stays so that authored runs can drop straight in; with no campaigns,
	 * every run is auto-generated from the room/boss libraries.
	 */
	public static List<CampaignDefinition> all()
	{
		return Collections.emptyList();
	}
}
