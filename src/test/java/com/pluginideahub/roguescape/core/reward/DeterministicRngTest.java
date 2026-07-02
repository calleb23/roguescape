package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pins the deterministic RNG contract: seeds shared between players must reproduce
 * bit-identical sequences and shuffle orders on any JVM. The expected values below are
 * frozen on purpose — if they change, shared seeds and race routes silently diverge.
 */
public class DeterministicRngTest
{
	@Test
	public void hashIsStableAndPinned()
	{
		assertEquals(0L, DeterministicRng.hash(null));
		assertEquals(0xCBF29CE484222325L, DeterministicRng.hash(""));
		// FNV-1a 64-bit of "roguescape" — frozen reference value.
		assertEquals(-4177531466326829267L, DeterministicRng.hash("roguescape"));
	}

	@Test
	public void sameSeedYieldsIdenticalSequence()
	{
		DeterministicRng a = new DeterministicRng("race-seed-1");
		DeterministicRng b = new DeterministicRng("race-seed-1");
		for (int i = 0; i < 100; i++)
		{
			assertEquals(a.nextLong(), b.nextLong());
		}
	}

	@Test
	public void shuffleOrderIsPinnedForKnownSeed()
	{
		List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
		new DeterministicRng(42L).shuffle(items);
		assertEquals(Arrays.asList("b", "d", "e", "f", "a", "c"), items);
	}

	@Test
	public void shuffleMatchesRewardDrafterShuffle()
	{
		List<String> viaRng = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
		List<String> viaDrafter = new ArrayList<>(viaRng);
		new DeterministicRng(7L).shuffle(viaRng);
		RewardDrafter.shuffle(viaDrafter, new DeterministicRng(7L));
		assertEquals(viaRng, viaDrafter);
	}
}
