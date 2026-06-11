package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.legality.StrictnessMode;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("pluginideahub-roguescape")
public interface RogueScapeConfig extends Config
{
	@ConfigItem(
		keyName = "goalText",
		name = "Goal",
		description = "Declared goal for this run",
		position = 0
	)
	default String goalText()
	{
		return "Start nearly empty and let found items become the build";
	}

	@ConfigItem(
		keyName = "seedText",
		name = "Seed",
		description = "Optional seed label for this run (leave blank for none)",
		position = 1
	)
	default String seedText()
	{
		return "";
	}

	@ConfigItem(
		keyName = "modePreset",
		name = "Mode Preset",
		description = "Optional mode name (FRESH_SOURCE, BANK_DRAFT, REGION_CRAWL, CUSTOM_CREATOR, SEEDED_RACE). Leave blank for none.",
		position = 2
	)
	default String modePreset()
	{
		return "";
	}

	@ConfigItem(
		keyName = "runPreset",
		name = "Run Preset",
		description = "Optional preset name (GOBLIN_RAT, IRON_SCRAPPER, MAGE_SPARK, ARCHERS_GAMBLE, MONK_MODE, WILDERNESS_RAT, CLUE_GREMLIN, MAX_MAIN_DRAFT). Leave blank for none.",
		position = 3
	)
	default String runPreset()
	{
		return "";
	}

	@ConfigItem(
		keyName = "strictnessMode",
		name = "Strictness",
		description = "How hard RogueScape should treat suspicious/illegal observations",
		position = 4
	)
	default StrictnessMode strictnessMode()
	{
		return StrictnessMode.BALANCED;
	}

	@ConfigItem(
		keyName = "bankAccessAllowed",
		name = "Allow Bank Unlocks",
		description = "Treat configured/unlocked bank withdrawals as potentially legal instead of always illegal",
		position = 5
	)
	default boolean bankAccessAllowed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "preRunSupplyExpected",
		name = "Flag Pre-run Supplies",
		description = "Flag non-starter-kit inventory items as illegal pre-run supplies when the run begins",
		position = 6
	)
	default boolean preRunSupplyExpected()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showRoomRegionMask",
		name = "Grey Out Outside Room",
		description = "Dim visible tiles outside the current room's allowed region IDs. Passive visual aid only; movement is not blocked.",
		position = 7
	)
	default boolean showRoomRegionMask()
	{
		return true;
	}

	@Range(
		min = 20,
		max = 220
	)
	@ConfigItem(
		keyName = "roomMaskOpacity",
		name = "Room Mask Opacity",
		description = "Opacity for the grey overlay outside the current room. Higher values make off-room space darker.",
		position = 8
	)
	default int roomMaskOpacity()
	{
		return 125;
	}

	@ConfigItem(
		keyName = "customRoomName",
		name = "Custom Room Name",
		description = "Name for the creator-defined RogueScape room built from world-map region IDs.",
		position = 9
	)
	default String customRoomName()
	{
		return "Custom Room";
	}

	@ConfigItem(
		keyName = "customRoomRegionIdsCsv",
		name = "Custom Room Region IDs",
		description = "Comma-separated RuneLite region IDs for the custom RogueScape room. Prefer using the Builder tab/world-map editor.",
		position = 10
	)
	default String customRoomRegionIdsCsv()
	{
		return "";
	}

	@ConfigItem(
		keyName = "useCustomRoomForCurrentRun",
		name = "Use Custom Room For Mask",
		description = "MVP custom-run hook: use the saved custom room as the current room's allowed regions for the grey-out mask.",
		position = 11
	)
	default boolean useCustomRoomForCurrentRun()
	{
		return false;
	}

	@ConfigItem(
		keyName = "experimentalJournalTab",
		name = "Experimental Quest-tab UI",
		description = "Probe the in-game side journal widgets for a future RogueScape quest-tab page. Does not modify widgets yet.",
		position = 12
	)
	default boolean experimentalJournalTab()
	{
		return false;
	}

	@ConfigItem(
		keyName = "developerMode",
		name = "Developer Mode",
		description = "Show DEV TOOLS in the side panel to simulate stepping a run (complete stage, pick reward, finish/fail) without playing. For building and testing the UI.",
		position = 13
	)
	default boolean developerMode()
	{
		return true;
	}
}
