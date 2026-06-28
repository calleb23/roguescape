package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterizes {@link CustomRoomZoneService} — the custom-zone application lifted out of
 * {@code RogueScapePlugin.applyCustomRoomZoneToRun()} behind the {@link RunContext} seam. Pins that
 * the selected regions are stamped onto every non-boss stage (preserving each stage's existing
 * {@link RoomKind}), boss stages are skipped, and the gates (useCustomRoom, empty/null selection,
 * lobby) are no-ops.
 */
public class CustomRoomZoneServiceTest
{
	private static RogueScapeRun runWithTwoRoomsAndBoss()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Zone test", "seed", RunMode.FRESH_SOURCE, RunPreset.GOBLIN_RAT);
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "starter room");
		base.addStage("R2", RunStageType.ROOM, "Varrock", "second room");
		base.addStage("B1", RunStageType.BOSS, "Obor", "boss room");
		base.enterStage("R1");
		return RogueScapeRun.wrap(base);
	}

	private static RunContext ctx(RogueScapeRun run)
	{
		return RunContext.active(run.session(), run, new RogueScapeRunLoop(run, 0L), "");
	}

	private static RogueScapeCustomRoomSelection selection(String name, int... regionIds)
	{
		RogueScapeCustomRoomSelection s = new RogueScapeCustomRoomSelection(name);
		for (int id : regionIds)
		{
			s.addRegion(id);
		}
		return s;
	}

	private static Set<String> set(String... ids)
	{
		return new LinkedHashSet<>(Arrays.asList(ids));
	}

	private static boolean hasRegion(StageRegionRule rule, String id)
	{
		return rule != null && rule.allowedRegionIds().contains(id);
	}

	@Test
	public void stampsSelectedRegionsOntoRoomStagesAndSkipsBoss()
	{
		RogueScapeRun run = runWithTwoRoomsAndBoss();
		// pre-existing boss rule to prove it is left untouched
		run.setRegionRule("B1", new StageRegionRule(RoomKind.BOSS, set("3000"), true));

		CustomRoomZoneService.applyToRun(ctx(run), selection("Zone", 12850, 12851), true);

		StageRegionRule r1 = run.regionPolicy().ruleFor("R1");
		StageRegionRule r2 = run.regionPolicy().ruleFor("R2");
		assertNotNull(r1);
		assertNotNull(r2);
		assertEquals(set("12850", "12851"), r1.allowedRegionIds());
		assertEquals(set("12850", "12851"), r2.allowedRegionIds());

		StageRegionRule b1 = run.regionPolicy().ruleFor("B1");
		assertEquals(set("3000"), b1.allowedRegionIds());
		assertEquals(RoomKind.BOSS, b1.roomKind());
	}

	@Test
	public void preservesExistingRoomKindOnRewrite()
	{
		RogueScapeRun run = runWithTwoRoomsAndBoss();
		run.setRegionRule("R1", new StageRegionRule(RoomKind.SUPPLY, set("999"), true));

		CustomRoomZoneService.applyToRun(ctx(run), selection("Zone", 12850), true);

		StageRegionRule r1 = run.regionPolicy().ruleFor("R1");
		assertEquals(RoomKind.SUPPLY, r1.roomKind());
		assertEquals(set("12850"), r1.allowedRegionIds());
	}

	@Test
	public void noOpWhenUseCustomRoomDisabled()
	{
		RogueScapeRun run = runWithTwoRoomsAndBoss();
		CustomRoomZoneService.applyToRun(ctx(run), selection("Zone", 12850), false);
		assertTrue(!hasRegion(run.regionPolicy().ruleFor("R1"), "12850"));
		assertTrue(!hasRegion(run.regionPolicy().ruleFor("R2"), "12850"));
	}

	@Test
	public void noOpWhenSelectionEmptyOrNull()
	{
		RogueScapeRun run = runWithTwoRoomsAndBoss();
		CustomRoomZoneService.applyToRun(ctx(run), selection("Empty"), true);
		CustomRoomZoneService.applyToRun(ctx(run), null, true);
		assertTrue(!hasRegion(run.regionPolicy().ruleFor("R1"), "12850"));
	}

	@Test
	public void noOpInLobby()
	{
		// lobby ctx has no run/session; the call must simply do nothing (no exception)
		CustomRoomZoneService.applyToRun(RunContext.lobby(), selection("Zone", 12850), true);
		CustomRoomZoneService.applyToRun(null, selection("Zone", 12850), true);
	}
}
