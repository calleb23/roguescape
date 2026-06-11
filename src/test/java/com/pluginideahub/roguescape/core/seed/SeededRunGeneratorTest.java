package com.pluginideahub.roguescape.core.seed;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class SeededRunGeneratorTest
{
	private static ChallengeDefinition def(String seed)
	{
		return ChallengeDefinition.builder()
			.challengeId("WK-001")
			.goal("Defeat Obor from rooms")
			.seed(seed)
			.mode("FRESH_SOURCE")
			.routeLength(3)
			.bossCount(1)
			.roomNamePool("Lumbridge", "Varrock", "Falador", "Edgeville", "Draynor")
			.bossNamePool("Obor", "Bryophyta", "Tempoross")
			.relicIdPool("one-bank-mercy", "four-food-limit", "cursed-blades")
			.starterKit("Bronze dagger")
			.build();
	}

	@Test
	public void sameSeedProducesSameRunPlan()
	{
		GeneratedRunPlan a = SeededRunGenerator.generate(def("seed-X"));
		GeneratedRunPlan b = SeededRunGenerator.generate(def("seed-X"));
		assertEquals(a.route().size(), b.route().size());
		for (int i = 0; i < a.route().size(); i++)
		{
			assertEquals(a.route().get(i).id, b.route().get(i).id);
			assertEquals(a.route().get(i).name, b.route().get(i).name);
			assertEquals(a.route().get(i).isBoss, b.route().get(i).isBoss);
		}
		assertEquals(a.relics(), b.relics());
		assertEquals(a.starterKit(), b.starterKit());
	}

	@Test
	public void differentSeedsProduceDifferentPlans()
	{
		GeneratedRunPlan a = SeededRunGenerator.generate(def("seed-A"));
		GeneratedRunPlan b = SeededRunGenerator.generate(def("seed-B"));
		boolean differs = false;
		for (int i = 0; i < a.route().size(); i++)
		{
			if (!a.route().get(i).name.equals(b.route().get(i).name)) { differs = true; break; }
		}
		if (!differs && !a.relics().equals(b.relics())) differs = true;
		assertTrue("expected at least one route/relic to differ", differs);
	}

	@Test
	public void routeIncludesRoomsThenBosses()
	{
		GeneratedRunPlan plan = SeededRunGenerator.generate(def("seed-route"));
		assertEquals(4, plan.route().size());
		assertEquals("R1", plan.route().get(0).id);
		assertFalse(plan.route().get(0).isBoss);
		assertEquals("B1", plan.route().get(3).id);
		assertTrue(plan.route().get(3).isBoss);
	}

	@Test
	public void challengeCodecRoundTripsAllFields()
	{
		ChallengeDefinition before = def("seed-codec");
		String encoded = ChallengeCodec.encode(before);
		ChallengeDefinition after = ChallengeCodec.decode(encoded);
		assertEquals(before.challengeId(), after.challengeId());
		assertEquals(before.seed(), after.seed());
		assertEquals(before.goal(), after.goal());
		assertEquals(before.routeLength(), after.routeLength());
		assertEquals(before.bossCount(), after.bossCount());
		assertEquals(before.roomNamePool(), after.roomNamePool());
		assertEquals(before.bossNamePool(), after.bossNamePool());
		assertEquals(before.relicIdPool(), after.relicIdPool());
		assertEquals(before.starterKit(), after.starterKit());

		// Encoded form re-encodes identically (canonical round-trip).
		assertEquals(encoded, ChallengeCodec.encode(after));
	}

	@Test
	public void distinctRoomPickingAvoidsImmediateRepeats()
	{
		ChallengeDefinition def = ChallengeDefinition.builder()
			.challengeId("dist")
			.seed("seed-dist")
			.routeLength(3)
			.bossCount(0)
			.roomNamePool(Arrays.asList("A", "B", "C"))
			.build();
		GeneratedRunPlan plan = SeededRunGenerator.generate(def);
		String prev = null;
		for (GeneratedRunPlan.Stage s : plan.route())
		{
			if (prev != null) assertNotEquals(prev, s.name);
			prev = s.name;
		}
	}
}
