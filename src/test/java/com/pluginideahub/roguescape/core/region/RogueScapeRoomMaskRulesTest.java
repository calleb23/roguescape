package com.pluginideahub.roguescape.core.region;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RogueScapeRoomMaskRulesTest
{
	private static Set<String> regions(String... ids)
	{
		Set<String> s = new HashSet<>();
		for (String id : ids) s.add(id);
		return s;
	}

	@Test
	public void disabledNeverRenders()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(false, true, 100, regions("12342"));
		assertFalse(rules.shouldRender());
		assertFalse(rules.shouldMaskRegion(99999));
	}

	@Test
	public void notInRunDoesNotRender()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, false, 100, regions("12342"));
		assertFalse(rules.shouldRender());
		assertFalse(rules.shouldMaskRegion(99999));
	}

	@Test
	public void emptyAllowedSetDoesNotRender()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 100, Collections.emptySet());
		assertFalse(rules.shouldRender());
		assertFalse(rules.shouldMaskRegion(12342));
	}

	@Test
	public void nullAllowedSetIsTreatedAsEmpty()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 100, null);
		assertFalse(rules.shouldRender());
		assertFalse(rules.shouldMaskRegion(12342));
	}

	@Test
	public void allowedRegionIsNotMasked()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 100, regions("12342", "12343"));
		assertTrue(rules.shouldRender());
		assertFalse(rules.shouldMaskRegion(12342));
		assertFalse(rules.shouldMaskRegion(12343));
	}

	@Test
	public void outsideRegionIsMasked()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 100, regions("12342"));
		assertTrue(rules.shouldRender());
		assertTrue(rules.shouldMaskRegion(99999));
	}

	@Test
	public void opacityClampsLow()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 0, regions("12342"));
		assertEquals(20, rules.getClampedOpacity());
	}

	@Test
	public void opacityClampsHigh()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 999, regions("12342"));
		assertEquals(220, rules.getClampedOpacity());
	}

	@Test
	public void opacityPassesThroughInRange()
	{
		RogueScapeRoomMaskRules rules = new RogueScapeRoomMaskRules(true, true, 125, regions("12342"));
		assertEquals(125, rules.getClampedOpacity());
	}
}
