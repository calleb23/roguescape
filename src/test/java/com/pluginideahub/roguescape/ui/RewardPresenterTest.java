package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RewardPresenterTest
{
	@Test
	public void humanCategoryLowercasesAndReplacesUnderscores()
	{
		BankItemCategory any = BankItemCategory.values()[0];
		assertEquals(any.name().toLowerCase().replace('_', ' '), RewardPresenter.humanCategory(any));
		assertEquals("", RewardPresenter.humanCategory(null));
	}

	@Test
	public void rewardTitleAndSubtitleFallBackForNullDraft()
	{
		assertEquals("ROLL REWARD", RewardPresenter.rewardTitle(null));
		assertEquals("Choose one reward for the next stage", RewardPresenter.rewardSubtitle(null));
	}

	@Test
	public void rewardIconForReturnsAValidPlaceholderForNull()
	{
		assertTrue(RewardPresenter.rewardIconFor(null) > 0);
	}
}
