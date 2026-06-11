package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.StarterKit;
import com.pluginideahub.roguescape.core.legality.StrictnessMode;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java assembler for Custom Builder runs. The RuneLite plugin and tests both use this so
 * the custom UI choices are proven to become a real session, route, rule set, starter kit,
 * modifiers, and loop.
 */
public final class RogueScapeCustomRunFactory
{
	private RogueScapeCustomRunFactory() {}

	public static StartedRun start(Config config)
	{
		if (config == null)
		{
			config = Config.builder().build();
		}
		String goal = config.goal == null || config.goal.trim().isEmpty() ? "Custom RogueScape Run" : config.goal.trim();
		RogueScapeRunSession session = RogueScapeRunSession.start(goal, emptyToNull(config.seed),
			RunMode.CUSTOM_CREATOR, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session)
			.declareStarterKit(starterKitForLoadout(config.loadout))
			.setStrictness(strictnessForLabel(config.strictness))
			.setBankAccessAllowed(config.bankUnlocks)
			.setPreRunSupplyExpected(config.preRunSupplyExpected)
			.setStartSnapshot(config.startSnapshot);

		session.recordRunLoopNote("Custom mode: " + (config.customMode == null || config.customMode.isEmpty()
			? "Scavenger" : config.customMode));
		session.recordRunLoopNote("Loadout: " + (config.loadout == null || config.loadout.isEmpty()
			? "Naked" : config.loadout));

		for (String relicId : safe(config.modifierIds))
		{
			Relic relic = RelicCatalog.byId(relicId);
			if (relic != null)
			{
				run.chooseRelic(relic);
			}
		}

		List<String> roomIds = safe(config.roomIds);
		String bossId = config.bossId == null ? "" : config.bossId;
		if (roomIds.isEmpty() && bossId.isEmpty())
		{
			RunRouteBuilder.buildRoute(RunMode.CUSTOM_CREATOR, RunPreset.UNSPECIFIED, config.seed, session, run);
		}
		else
		{
			RunRouteBuilder.buildExplicitRoute(roomIds, safe(config.roomAllowances), bossId, session, run);
		}

		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, config.startedAtMillis)
			.setBaseRewardsEnabled(true)
			.setTravelGatedStages(true);
		if (config.timeLimitMinutes > 0)
		{
			loop.setTimeLimitMillis(config.timeLimitMinutes * 60_000L);
			session.recordRunLoopNote("Time limit: " + config.timeLimitMinutes + " minutes");
		}
		return new StartedRun(session, run, loop);
	}

	public static StarterKit starterKitForLoadout(String loadout)
	{
		return new StarterKit(loadoutKitItems(loadout));
	}

	public static List<String> loadoutKitLines(String loadout)
	{
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : loadoutKitItems(loadout).entrySet())
		{
			lines.add(entry.getValue() + "x " + loadoutItemName(entry.getKey()));
		}
		if (lines.isEmpty())
		{
			lines.add("No starter items");
		}
		return lines;
	}

	public static StrictnessMode strictnessForLabel(String label)
	{
		String value = label == null ? "" : label.trim().toLowerCase();
		if ("trust".equals(value)) return StrictnessMode.TRUST;
		if ("strict".equals(value)) return StrictnessMode.STRICT;
		return StrictnessMode.BALANCED;
	}

	private static Map<String, Integer> loadoutKitItems(String loadout)
	{
		Map<String, Integer> items = new LinkedHashMap<>();
		String value = loadout == null ? "" : loadout.trim().toLowerCase();
		if ("low gear".equals(value))
		{
			items.put("1277", 1);
			items.put("1171", 1);
			items.put("315", 3);
		}
		else if ("mid gear".equals(value))
		{
			items.put("1277", 1);
			items.put("1171", 1);
			items.put("1139", 1);
			items.put("1075", 1);
			items.put("315", 5);
			items.put("2309", 2);
		}
		else if ("custom kit".equals(value))
		{
			items.put("1277", 1);
			items.put("841", 1);
			items.put("882", 30);
			items.put("315", 5);
		}
		return items;
	}

	private static String loadoutItemName(String itemId)
	{
		if ("1277".equals(itemId)) return "Bronze sword";
		if ("1171".equals(itemId)) return "Wooden shield";
		if ("315".equals(itemId)) return "Shrimps";
		if ("1139".equals(itemId)) return "Bronze med helm";
		if ("1075".equals(itemId)) return "Bronze platelegs";
		if ("2309".equals(itemId)) return "Bread";
		if ("841".equals(itemId)) return "Shortbow";
		if ("882".equals(itemId)) return "Bronze arrows";
		return itemId;
	}

	private static String emptyToNull(String value)
	{
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	private static List<String> safe(List<String> values)
	{
		return values == null ? Collections.emptyList() : values;
	}

	public static final class StartedRun
	{
		private final RogueScapeRunSession session;
		private final RogueScapeRun run;
		private final RogueScapeRunLoop loop;

		private StartedRun(RogueScapeRunSession session, RogueScapeRun run, RogueScapeRunLoop loop)
		{
			this.session = session;
			this.run = run;
			this.loop = loop;
		}

		public RogueScapeRunSession session() { return session; }
		public RogueScapeRun run() { return run; }
		public RogueScapeRunLoop loop() { return loop; }
	}

	public static final class Config
	{
		private String goal;
		private String seed;
		private String customMode = "Scavenger";
		private String loadout = "Naked";
		private List<String> roomIds = Collections.emptyList();
		private List<String> roomAllowances = Collections.emptyList();
		private String bossId = "";
		private List<String> modifierIds = Collections.emptyList();
		private String strictness = "Balanced";
		private boolean bankUnlocks;
		private boolean preRunSupplyExpected;
		private int timeLimitMinutes;
		private long startedAtMillis;
		private InventorySnapshot startSnapshot = new InventorySnapshot();

		private Config() {}

		public static Config builder() { return new Config(); }

		public Config goal(String value) { this.goal = value; return this; }
		public Config seed(String value) { this.seed = value; return this; }
		public Config customMode(String value) { this.customMode = value; return this; }
		public Config loadout(String value) { this.loadout = value; return this; }
		public Config roomIds(List<String> values) { this.roomIds = values; return this; }
		public Config roomAllowances(List<String> values) { this.roomAllowances = values; return this; }
		public Config bossId(String value) { this.bossId = value; return this; }
		public Config modifierIds(List<String> values) { this.modifierIds = values; return this; }
		public Config strictness(String value) { this.strictness = value; return this; }
		public Config bankUnlocks(boolean value) { this.bankUnlocks = value; return this; }
		public Config preRunSupplyExpected(boolean value) { this.preRunSupplyExpected = value; return this; }
		public Config timeLimitMinutes(int value) { this.timeLimitMinutes = Math.max(0, value); return this; }
		public Config startedAtMillis(long value) { this.startedAtMillis = value; return this; }
		public Config startSnapshot(InventorySnapshot value)
		{
			this.startSnapshot = value == null ? new InventorySnapshot() : value;
			return this;
		}

		private Config build() { return this; }
	}
}
