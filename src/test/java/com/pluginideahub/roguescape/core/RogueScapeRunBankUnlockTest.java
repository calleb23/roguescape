package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.legality.ItemLegality;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.legality.StrictnessMode;
import com.pluginideahub.roguescape.core.reward.BankDraftPool;
import com.pluginideahub.roguescape.core.reward.BankItem;
import com.pluginideahub.roguescape.core.reward.BankItemClassifier;
import com.pluginideahub.roguescape.core.reward.ChestType;
import com.pluginideahub.roguescape.core.reward.DeterministicRng;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardDrafter;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import org.junit.Test;

import static org.junit.Assert.*;

public class RogueScapeRunBankUnlockTest
{
	private static BankDraftPool buildFoodPool()
	{
		BankDraftPool pool = new BankDraftPool();
		for (String name : new String[]{"Shark", "Lobster", "Monkfish", "Anglerfish"})
		{
			pool.add(BankItemClassifier.classify(name, 1_000));
		}
		return pool;
	}

	@Test
	public void lockedBankWithdrawalIsIllegalEvenWhenBankAccessAllowed()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Max-main locked bank", "seed-locked", RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setStrictness(StrictnessMode.BALANCED)
			.setBankAccessAllowed(true);
		run.bankPool().addAll(buildFoodPool().catalog().values());

		base.enterStage("R1");
		ItemEvent withdraw = run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL, withdraw.legality());
		assertEquals(1, run.illegalCount());
	}

	@Test
	public void selectingARewardOptionUnlocksBankItemAndWithdrawalBecomesLegal()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Max-main unlock", "seed-unlock", RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.addStage("R2", RunStageType.ROOM, "Edgeville", "");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setStrictness(StrictnessMode.BALANCED)
			.setBankAccessAllowed(true);
		BankDraftPool pool = run.bankPool();
		pool.addAll(buildFoodPool().catalog().values());

		base.enterStage("R1");
		RewardDraft draft = RewardDrafter.draftBankUnlock(
			"draft-1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-unlock"));
		assertEquals(3, draft.options().size());
		RewardOption picked = draft.options().get(0);
		draft.select(picked.optionId());
		pool.markUnlocked(picked.bankItem());

		base.clearStage("R1");
		base.enterStage("R2");
		ItemEvent unlockedDraw = run.applyItemDelta(
			picked.bankItem().itemName(), 1, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.LEGAL_BANK_UNLOCK, unlockedDraw.legality());
		assertEquals(1, run.legalCount());
		assertEquals(0, run.illegalCount());
		assertEquals(RunState.ACTIVE, base.runState());

		// Withdrawing a still-locked item from the pool remains illegal.
		BankItem stillLocked = null;
		for (BankItem item : pool.catalog().values())
		{
			if (!pool.isUnlocked(item.itemId())) { stillLocked = item; break; }
		}
		assertNotNull("expected another locked item in the pool", stillLocked);
		ItemEvent stillIllegal = run.applyItemDelta(
			stillLocked.itemName(), 1, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL, stillIllegal.legality());
	}

	@Test
	public void bankWithdrawalWhenBankAccessDeniedIsAlwaysIllegal()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Fresh source ignores unlocks", "seed-fresh", RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");

		RogueScapeRun run = RogueScapeRun.wrap(base).setBankAccessAllowed(false);
		// Even with the pool unlocked, FRESH_SOURCE runs disallow bank entirely.
		run.bankPool().add(BankItemClassifier.classify("Shark", 1_000));
		run.bankPool().markUnlocked(run.bankPool().get("shark"));

		base.enterStage("R1");
		ItemEvent illegal = run.applyItemDelta("Shark", 1, ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.ILLEGAL_BANK_WITHDRAWAL, illegal.legality());
	}

	@Test
	public void simulatedMaxMainBankDraftRunUnlocksAndCompletes()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start(
			"Max-main simulated run", "seed-final", RunMode.BANK_DRAFT, RunPreset.UNSPECIFIED);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter");
		base.addStage("R2", RunStageType.ROOM, "Varrock", "bank-aware");
		base.addStage("B1", RunStageType.BOSS, "Obor", "boss");

		RogueScapeRun run = RogueScapeRun.wrap(base)
			.setStrictness(StrictnessMode.BALANCED)
			.setBankAccessAllowed(true);
		BankDraftPool pool = run.bankPool();
		pool.addAll(buildFoodPool().catalog().values());

		base.enterStage("R1");
		assertTrue(pool.canRoll(ChestType.FOOD));
		RewardDraft draft = RewardDrafter.draftBankUnlock(
			"draft-1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-final"));
		RewardOption picked = draft.options().get(0);
		draft.select(picked.optionId());
		pool.markUnlocked(picked.bankItem());
		base.clearStage("R1");

		base.enterStage("R2");
		ItemEvent foodDraw = run.applyItemDelta(picked.bankItem().itemName(), 4,
			ProvenanceHint.OBSERVED_BANK_WITHDRAWAL);
		assertEquals(ItemLegality.LEGAL_BANK_UNLOCK, foodDraw.legality());
		base.clearStage("R2");

		base.enterStage("B1");
		ItemEvent bossLoot = run.applyItemDelta("Obor key", 1, ProvenanceHint.OBSERVED_LOOT);
		assertTrue(bossLoot.isLegal());
		base.clearStage("B1");
		base.completeRun("Obor down", RunCompletionReason.GOAL_COMPLETE);

		assertEquals(RunState.COMPLETE, base.runState());
		assertEquals(2, run.legalCount());
		assertEquals(1, pool.unlockedIds().size());
	}
}
