package com.pluginideahub.roguescape.core.campaign;

import com.pluginideahub.roguescape.core.RunPreset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CampaignLibrary
{
	private CampaignLibrary() {}

	public static CampaignDefinition find(RunPreset preset)
	{
		if (preset == null || preset == RunPreset.UNSPECIFIED)
		{
			return null;
		}
		for (CampaignDefinition campaign : all())
		{
			if (campaign.preset() == preset)
			{
				return campaign;
			}
		}
		return null;
	}

	public static List<CampaignDefinition> all()
	{
		return Collections.unmodifiableList(Arrays.asList(
			campaign(RunPreset.GOBLIN_RAT, "Goblin Rat", "A tiny crawl from swamp scraps into a first boss.",
				"Starter", rooms("lumbridge-swamp", "barbarian-village"), bosses("boss-giant-mole")),
			campaign(RunPreset.IRON_SCRAPPER, "Iron Scrapper", "Mine your footing, then turn rough gear into a kill.",
				"Easy", rooms("dwarven-mine", "varrock-east"), bosses("boss-sarachnis")),
			campaign(RunPreset.MAGE_SPARK, "Mage Spark", "Find runes, bargain for power, and wake the deep.",
				"Medium", rooms("varrock-east", "seers-village", "yanille"), bosses("boss-kraken")),
			campaign(RunPreset.ARCHERS_GAMBLE, "Archer's Gamble", "Stock up, gamble on range, then face kings below.",
				"Medium", rooms("catherby", "tree-gnome-stronghold", "edgeville-dungeon"), bosses("boss-dagannoth-kings")),
			campaign(RunPreset.MONK_MODE, "Monk Mode", "Low-gear discipline into a graveyard test.",
				"Hard", rooms("falador", "edgeville-dungeon"), bosses("boss-barrows-brothers")),
			campaign(RunPreset.WILDERNESS_RAT, "Wilderness Rat", "A sketchy edge route with wilderness pressure.",
				"Hard", rooms("edgeville", "wilderness-level-1-10", "edgeville-dungeon", "varrock-west"), bosses("boss-chaos-elemental")),
			campaign(RunPreset.CLUE_GREMLIN, "Clue Gremlin", "A strange trail of supplies, markets, and barrows.",
				"Odd", rooms("draynor-village", "seers-village", "ardougne-market"), bosses("boss-barrows-brothers")),
			campaign(RunPreset.MAX_MAIN_DRAFT, "Max Main Draft", "A full build route with gear, supplies, skilling, and two bosses.",
				"Long", rooms("varrock-east", "falador", "catherby", "dwarven-mine", "slayer-tower"),
				bosses("boss-general-graardor", "boss-vorkath"))));
	}

	private static CampaignDefinition campaign(RunPreset preset, String title, String description, String difficulty,
		List<String> roomIds, List<String> bossIds)
	{
		return new CampaignDefinition(preset, title, description, difficulty, roomIds, bossIds);
	}

	private static List<String> rooms(String... ids)
	{
		return list(ids);
	}

	private static List<String> bosses(String... ids)
	{
		return list(ids);
	}

	private static List<String> list(String... ids)
	{
		List<String> out = new ArrayList<>();
		Collections.addAll(out, ids);
		return out;
	}
}
