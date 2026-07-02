package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.reward.RelicDraftGenerator;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RelicGameplayTest
{
	private static RogueScapeRun roomRun(String label)
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(label);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("R2", RunStageType.ROOM, "Varrock", "");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.enterStage("R1");
		return run;
	}

	private static List<String> relicIds(RewardDraft draft)
	{
		List<String> ids = new ArrayList<>();
		for (RewardOption o : draft.options())
		{
			ids.add(o.relic() == null ? o.optionId() : o.relic().relicId());
		}
		return ids;
	}

	@Test
	public void relicDraftIsDeterministicAndRelicTyped()
	{
		RewardDraft a = RelicDraftGenerator.relicDraft("D", "R1", 42L, 3);
		RewardDraft b = RelicDraftGenerator.relicDraft("D", "R1", 42L, 3);
		assertEquals(3, a.options().size());
		assertTrue(a.options().get(0).isRelic());
		assertEquals(relicIds(a), relicIds(b));
	}

	@Test
	public void collectingAnItemCountsTowardTheRun()
	{
		RogueScapeRun run = roomRun("Control");
		run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_LOOT);
		assertEquals(1, run.itemsCollected());
	}

	@Test
	public void gluttonyAddsFoodScoringBonus()
	{
		RogueScapeRun run = roomRun("Score");
		run.chooseRelic(RelicLibrary.gluttony()); // food scores +3 each
		run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_LOOT);

		int sessionScore = run.session().runScore();
		assertEquals(sessionScore + 3, run.effectiveScore());
	}

	@Test
	public void exceedingACategoryCapIsFlagged()
	{
		RogueScapeRun run = roomRun("Cap");
		run.chooseRelic(ModifierLibrary.twoFoodMax()); // food capped at 2
		run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_LOOT);
		run.applyItemDelta("Lobster", 1, ProvenanceHint.OBSERVED_LOOT);
		assertTrue(run.relicOverLimit().isEmpty());

		run.applyItemDelta("Trout", 1, ProvenanceHint.OBSERVED_LOOT); // third food -> over cap
		assertFalse(run.relicOverLimit().isEmpty());
	}

	@Test
	public void choosingRelicRewardActivatesIt()
	{
		RogueScapeRun run = roomRun("Loop");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);
		run.session().recordCurrentStageItemGain();
		loop.completeCurrentStage(1_000L);

		RewardDraft draft = loop.pendingRewardDraft();
		assertTrue(draft.options().get(0).isRelic());

		loop.chooseReward(draft.options().get(0).optionId(), 2_000L);
		assertEquals(1, run.heldRelics().size());
		assertEquals(1, run.session().relicCount());
		assertFalse(run.heldRelics().get(0).name().isEmpty());
	}
}
