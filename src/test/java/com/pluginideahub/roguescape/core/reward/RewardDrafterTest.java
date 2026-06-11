package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.*;

public class RewardDrafterTest
{
	private static BankDraftPool poolOfFood(String... names)
	{
		BankDraftPool pool = new BankDraftPool();
		for (String n : names) pool.add(BankItemClassifier.classify(n, 100));
		return pool;
	}

	@Test
	public void draftFromOptionsReturnsThreeDistinctOptions()
	{
		List<RewardOption> options = new ArrayList<>();
		for (int i = 0; i < 6; i++)
		{
			options.add(new RewardOption("opt-" + i, "Option " + i, ChestType.SUPPLY, null));
		}
		RewardDraft draft = RewardDrafter.draftFromOptions("d1", "R1", ChestType.SUPPLY, options,
			new DeterministicRng("seed-a"));
		assertEquals(3, draft.options().size());
		Set<String> ids = new HashSet<>();
		for (RewardOption o : draft.options()) ids.add(o.optionId());
		assertEquals("draft must contain three distinct options", 3, ids.size());
	}

	@Test
	public void sameSeedYieldsSameDraft()
	{
		BankDraftPool pool = poolOfFood("Shark", "Lobster", "Monkfish", "Trout", "Salmon", "Anglerfish");
		RewardDraft a = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-A"));
		RewardDraft b = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-A"));
		assertEquals(a.options().size(), b.options().size());
		for (int i = 0; i < a.options().size(); i++)
		{
			assertEquals(a.options().get(i).optionId(), b.options().get(i).optionId());
		}
	}

	@Test
	public void differentSeedsYieldDifferentDraftsOnAdequatePool()
	{
		BankDraftPool pool = poolOfFood("Shark", "Lobster", "Monkfish", "Trout",
			"Salmon", "Anglerfish", "Manta ray", "Dark crab", "Karambwan");
		RewardDraft a = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-A"));
		RewardDraft b = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed-B"));
		// With 9 items and two different seeds, at least one of the three picks should differ.
		boolean anyDiff = false;
		for (int i = 0; i < a.options().size(); i++)
		{
			if (!a.options().get(i).optionId().equals(b.options().get(i).optionId()))
			{
				anyDiff = true;
				break;
			}
		}
		assertTrue("different seeds should produce different draft order", anyDiff);
	}

	@Test
	public void alreadyUnlockedItemsAreSkippedByBankDraft()
	{
		BankDraftPool pool = poolOfFood("Shark", "Lobster", "Monkfish", "Trout");
		// Unlock three of four. The remaining draft should only include the one unaccounted-for item.
		pool.markUnlocked(pool.get("shark"));
		pool.markUnlocked(pool.get("lobster"));
		pool.markUnlocked(pool.get("monkfish"));
		RewardDraft draft = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("any"));
		assertEquals(1, draft.options().size());
		assertEquals("trout", draft.options().get(0).optionId());
	}

	@Test
	public void selectingAnOptionMarksSelected()
	{
		BankDraftPool pool = poolOfFood("Shark", "Lobster", "Monkfish");
		RewardDraft draft = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, pool, new DeterministicRng("seed"));
		String first = draft.options().get(0).optionId();
		draft.select(first);
		assertTrue(draft.isSelected());
		assertEquals(first, draft.selected().optionId());
	}

	@Test
	public void emptyEligiblePoolProducesRejectedDraft()
	{
		BankDraftPool empty = new BankDraftPool();
		RewardDraft draft = RewardDrafter.draftBankUnlock("d1", "R1", ChestType.FOOD, empty, new DeterministicRng("x"));
		assertTrue(draft.isRejected());
	}
}
