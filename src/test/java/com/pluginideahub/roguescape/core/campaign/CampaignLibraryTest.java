package com.pluginideahub.roguescape.core.campaign;

import com.pluginideahub.roguescape.core.RunPreset;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CampaignLibraryTest
{
	@Test
	public void everyConcretePresetHasCampaignDefinition()
	{
		for (RunPreset preset : RunPreset.values())
		{
			if (preset == RunPreset.UNSPECIFIED)
			{
				continue;
			}
			CampaignDefinition campaign = CampaignLibrary.find(preset);
			assertNotNull("missing campaign for " + preset, campaign);
			assertFalse(campaign.title().isEmpty());
			assertFalse(campaign.description().isEmpty());
			assertFalse(campaign.difficulty().isEmpty());
			assertFalse(campaign.roomIds().isEmpty());
			assertFalse(campaign.bossIds().isEmpty());
		}
	}

	@Test
	public void unspecifiedHasNoCampaign()
	{
		assertNull(CampaignLibrary.find(RunPreset.UNSPECIFIED));
	}

	@Test
	public void goblinRatCampaignKeepsAuthoredRoute()
	{
		CampaignDefinition campaign = CampaignLibrary.find(RunPreset.GOBLIN_RAT);

		assertEquals("Goblin Rat", campaign.title());
		assertEquals("lumbridge-swamp", campaign.roomIds().get(0));
		assertEquals("barbarian-village", campaign.roomIds().get(1));
		assertEquals("boss-giant-mole", campaign.bossIds().get(0));
	}
}
