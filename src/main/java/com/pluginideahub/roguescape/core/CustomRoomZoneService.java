package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import java.util.Set;

/**
 * Applies a creator-defined custom room zone to the live run: every non-boss stage gets its region
 * rule re-pointed at the selected region ids (keeping each stage's existing {@link RoomKind}), and a
 * run-loop note records the change. Pure-Java over the {@link RunContext} seam + a
 * {@link RogueScapeCustomRoomSelection}, so it is unit-testable without instantiating the plugin.
 *
 * <p>Guards on {@code run}/{@code session} only (not the loop) to match the original plugin behavior
 * exactly; {@code useCustomRoom} mirrors the plugin's {@code config.useCustomRoomForCurrentRun()}
 * gate (the plugin passes {@code true} when config is absent).
 */
public final class CustomRoomZoneService
{
	private CustomRoomZoneService() {}

	public static void applyToRun(RunContext ctx, RogueScapeCustomRoomSelection selection, boolean useCustomRoom)
	{
		if (ctx == null || ctx.run() == null || ctx.session() == null
			|| selection == null || selection.isEmpty())
		{
			return;
		}
		if (!useCustomRoom)
		{
			return;
		}
		RogueScapeRun run = ctx.run();
		RogueScapeRunSession session = ctx.session();
		Set<String> selectedRegions = selection.selectedRegionIdStrings();
		for (RunStage stage : session.route().stages())
		{
			if (stage == null || stage.type() == RunStageType.BOSS)
			{
				continue;
			}
			StageRegionRule existing = run.regionPolicy().ruleFor(stage.id());
			RoomKind kind = existing == null ? RoomKind.REGION : existing.roomKind();
			run.setRegionRule(stage.id(), new StageRegionRule(kind, selectedRegions, true));
		}
		session.recordRunLoopNote("Applied custom zone: " + selection.getName()
			+ " (" + selection.size() + " regions)");
	}
}
