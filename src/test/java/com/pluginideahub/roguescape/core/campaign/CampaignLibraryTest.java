package com.pluginideahub.roguescape.core.campaign;

import com.pluginideahub.roguescape.core.RunPreset;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CampaignLibraryTest
{
	@Test
	public void noCuratedCampaignsAreDefinedYet()
	{
		assertTrue("curated presets were removed pending real authored content", CampaignLibrary.all().isEmpty());
	}

	@Test
	public void findReturnsNullForEveryPreset()
	{
		for (RunPreset preset : RunPreset.values())
		{
			assertNull("no campaign should resolve while the library is empty", CampaignLibrary.find(preset));
		}
	}
}
