package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.ui.PanelTab;
import com.pluginideahub.roguescape.core.ui.SidePanelViewModel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/**
 * Render harness for the tabbed RogueScape window — paints the window with each tab selected to
 * PNGs under {@code build/ui-preview/} so the Collection-Log-style look can be eyeballed without
 * launching the game.
 */
public class RogueScapeWindowOverlayRenderTest
{
	@Test
	public void rendersTabsToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		writePng(lobbyTabs(), 0, new File(dir, "window-run-builder.png"));
		List<RogueScapeWindowOverlay.Tab> customTabs = customBuilderTabs();
		String[] customNames = {"mode", "loadout", "rooms", "route", "seed"};
		for (int i = 0; i < customTabs.size(); i++)
		{
			writePng(customTabs, i, new File(dir, "window-custom-" + customNames[i] + ".png"));
		}
		writePng(customTabs, 0, new File(dir, "window-custom-builder.png"));

		List<RogueScapeWindowOverlay.Tab> tabs = sampleTabs();
		String[] names = {"runctl", "live", "build", "artifacts", "modifiers", "progression"};
		for (int i = 0; i < tabs.size(); i++)
		{
			writePng(tabs, i, new File(dir, "window-" + names[i] + ".png"));
		}
		assertTrue(new File(dir, "window-live.png").exists());
		assertTrue(new File(dir, "window-run-builder.png").exists());
		assertTrue(new File(dir, "window-custom-builder.png").exists());
	}

	/**
	 * Renders the real {@link SidePanelViewModel#runSpread()} for a live run as a single two-page
	 * spread (no tab strip), so the tracer output can be eyeballed at build/ui-preview/window-run-spread.png.
	 */
	@Test
	public void rendersRunSpreadToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		RogueScapeRunSession base = RogueScapeRunSession.start("Preview run");
		base.addStage("B0", RunStageType.BOSS, "Obor", "");
		base.addStage("R1", RunStageType.ROOM, "Canifis", "", "Find a weapon upgrade", 1);
		base.addStage("B1", RunStageType.BOSS, "Giant Mole", "");
		base.addStage("B2", RunStageType.BOSS, "Scorpia", "");
		base.addStage("B3", RunStageType.BOSS, "Vorkath", "");
		base.enterStage("B0");
		base.clearStage("B0");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.chooseRelic(ModifierLibrary.noFood());
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.RelicLibrary.whisperedFaith());
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.RelicLibrary.keyToTheVault());
		run.grantUnlock(new com.pluginideahub.roguescape.core.unlock.RunUnlock(
			com.pluginideahub.roguescape.core.unlock.RunUnlockType.PRAYER, "Prayer unlocked", "B0", "Boss"));
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L);
		loop.markNow(379_000L);
		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		List<RogueScapeWindowOverlay.Tab> tabs = Collections.singletonList(
			new RogueScapeWindowOverlay.Tab("THE RUN", JournalSpreadBlocks.render(vm.runSpread())));
		writePng(tabs, 0, new File(dir, "window-run-spread.png"));
		assertTrue(new File(dir, "window-run-spread.png").exists());

		// The same spread painted as an open book (centre spine, no tabs).
		writePng(tabs, 0, new File(dir, "window-run-book.png"), true);
		assertTrue(new File(dir, "window-run-book.png").exists());
	}

	/** The Contract (lobby) spread as an open book, built from a real route briefing. */
	@Test
	public void rendersContractSpreadToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		com.pluginideahub.roguescape.core.briefing.RunBriefing briefing =
			com.pluginideahub.roguescape.core.briefing.RunBriefingBuilder.preview(
				com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE,
				com.pluginideahub.roguescape.core.RunPreset.UNSPECIFIED,
				"crawl-route-2", "Naked", false, 0);
		List<RogueScapeWindowOverlay.Tab> tabs = Collections.singletonList(
			new RogueScapeWindowOverlay.Tab("THE CONTRACT", JournalSpreadBlocks.render(
				SidePanelViewModel.contractSpread(com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE,
					"Dungeon Crawl Run", "", briefing, "",
					com.pluginideahub.roguescape.core.seed.RouteNames.smartName("crawl-route-2"), 1, 12,
					java.util.EnumSet.of(com.pluginideahub.roguescape.core.restriction.Curse.FAMINE,
						com.pluginideahub.roguescape.core.restriction.Curse.ANCHORED)))));
		writePng(tabs, 0, new File(dir, "window-contract-book.png"), true);
		assertTrue(new File(dir, "window-contract-book.png").exists());
	}

	/** The Reward spread as an open book: sample cards on the left, The Ledger on the right. */
	@Test
	public void rendersRewardSpreadToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		RogueScapeRunSession base = RogueScapeRunSession.start("Reward preview");
		base.addStage("R1", RunStageType.ROOM, "Canifis", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.chooseRelic(ModifierLibrary.noFood());
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L);
		loop.markNow(379_000L);
		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		List<RogueScapeWindowOverlay.Block> cardsPage = new ArrayList<>();
		cardsPage.add(RogueScapeWindowOverlay.Block.cards(Arrays.asList(
			new RogueScapeRewardOverlay.Card("One Bank Mercy", "RELIC",
				RogueScapeRewardOverlay.Rarity.RARE, 0, Arrays.asList("Unlocks one bank visit.")),
			new RogueScapeRewardOverlay.Card("Four-Food Limit", "RELIC",
				RogueScapeRewardOverlay.Rarity.EPIC, 0, Arrays.asList("Permits up to 4 food.")),
			new RogueScapeRewardOverlay.Card("No Potions", "CURSE",
				RogueScapeRewardOverlay.Rarity.LEGENDARY, 0, Arrays.asList("Curse: potions are forbidden.")))));
		List<RogueScapeWindowOverlay.Tab> tabs = Collections.singletonList(
			new RogueScapeWindowOverlay.Tab("THE CHEST", JournalSpreadBlocks.render(
				vm.rewardSpread("The chest opens", "Reward — 06:19"), cardsPage)));
		writePng(tabs, 0, new File(dir, "window-reward-book.png"), true);
		assertTrue(new File(dir, "window-reward-book.png").exists());
	}

	/** The Recap spread (The Final Page) as an open book for a completed run. */
	@Test
	public void rendersRecapSpreadToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		RogueScapeRunSession base = RogueScapeRunSession.start("Recap preview");
		base.addStage("R1", RunStageType.ROOM, "Canifis", "");
		base.addStage("B1", RunStageType.BOSS, "Giant Mole", "");
		base.enterStage("R1");
		base.clearStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.completeRun("Done", com.pluginideahub.roguescape.core.RunCompletionReason.MANUAL_SUCCESS);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L);
		loop.markNow(1_202_000L);
		SidePanelViewModel vm = SidePanelViewModel.active(loop, PanelTab.RUN);

		List<RogueScapeWindowOverlay.Tab> tabs = Collections.singletonList(
			new RogueScapeWindowOverlay.Tab("THE FINAL PAGE", JournalSpreadBlocks.render(vm.runSpread())));
		writePng(tabs, 0, new File(dir, "window-recap-book.png"), true);
		assertTrue(new File(dir, "window-recap-book.png").exists());
	}

	private static void writePng(List<RogueScapeWindowOverlay.Tab> tabs, int tab, File out) throws Exception
	{
		writePng(tabs, tab, out, false);
	}

	private static void writePng(List<RogueScapeWindowOverlay.Tab> tabs, int tab, File out, boolean book) throws Exception
	{
		RogueScapeWindowOverlay overlay = new RogueScapeWindowOverlay(() -> tabs);
		overlay.setOpen(true);
		overlay.setSelectedTab(tab);
		overlay.setBookMode(book);

		BufferedImage scratch = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		Dimension d = overlay.render(sg);
		sg.dispose();

		BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x3A4A2E));
		g.fillRect(0, 0, d.width, d.height);
		overlay.render(g);
		g.dispose();

		ImageIO.write(img, "png", out);
	}

	private static List<RogueScapeWindowOverlay.Tab> sampleTabs()
	{
		List<RogueScapeWindowOverlay.Tab> tabs = new ArrayList<>();

		List<RogueScapeWindowOverlay.Block> runctl = new ArrayList<>();
		runctl.add(RogueScapeWindowOverlay.Block.heading("CHOOSE YOUR REWARD"));
		runctl.add(RogueScapeWindowOverlay.Block.cards(Arrays.asList(
			new RogueScapeRewardOverlay.Card("Relic of Blood", "RELIC",
				RogueScapeRewardOverlay.Rarity.RARE, 0, Arrays.asList("Heals 2 HP on kill.")),
			new RogueScapeRewardOverlay.Card("Twisted Souls", "RELIC",
				RogueScapeRewardOverlay.Rarity.EPIC, 0, Arrays.asList("Forbids armour.", "Max 4 food")),
			new RogueScapeRewardOverlay.Card("Dark Hunger", "RELIC",
				RogueScapeRewardOverlay.Rarity.LEGENDARY, 0, Arrays.asList("One-shot mercy.")))));
		tabs.add(new RogueScapeWindowOverlay.Tab("RUN CONTROL", runctl));

		List<RogueScapeWindowOverlay.Block> live = new ArrayList<>();
		live.add(RogueScapeWindowOverlay.Block.heading("LIVE RUN"));
		live.add(RogueScapeWindowOverlay.Block.statBar("Route", 2 / 5.0, "2 / 5", RogueScapeTheme.BAR_PROGRESS));
		live.add(RogueScapeWindowOverlay.Block.badge("Room Active", "Time 06:18", RogueScapeTheme.ACCENT, 0));
		live.add(RogueScapeWindowOverlay.Block.heading("CURRENT STAGE"));
		live.add(RogueScapeWindowOverlay.Block.badge("Barbarian Village", "ROOM", RogueScapeTheme.GOLD, 0));
		live.add(RogueScapeWindowOverlay.Block.text("Objective: Find a weapon upgrade (0 / 1)",
			RogueScapeTheme.TEXT_PRIMARY));
		live.add(RogueScapeWindowOverlay.Block.text("Next: Giant Mole", RogueScapeTheme.TEXT_MUTED));
		live.add(RogueScapeWindowOverlay.Block.heading("BUILD STATE"));
		live.add(RogueScapeWindowOverlay.Block.text("Score: 18", RogueScapeTheme.GOLD));
		live.add(RogueScapeWindowOverlay.Block.text("Relics: 2   Permitted/Forbidden: 6/0", RogueScapeTheme.TEXT_PRIMARY));
		tabs.add(new RogueScapeWindowOverlay.Tab("LIVE RUN", live));

		List<RogueScapeWindowOverlay.Block> build = new ArrayList<>();
		build.add(RogueScapeWindowOverlay.Block.heading("BUILD - RELIC LOADOUT"));
		build.add(RogueScapeWindowOverlay.Block.text("Relics held: 3", RogueScapeTheme.TEXT_PRIMARY));
		build.add(RogueScapeWindowOverlay.Block.text("Relic score bonus: +25", RogueScapeTheme.POSITIVE));
		build.add(RogueScapeWindowOverlay.Block.gap());
		build.add(RogueScapeWindowOverlay.Block.text("Forbidden categories:", RogueScapeTheme.TEXT_PRIMARY));
		build.add(RogueScapeWindowOverlay.Block.text("  - armour", RogueScapeTheme.NEGATIVE));
		build.add(RogueScapeWindowOverlay.Block.gap());
		build.add(RogueScapeWindowOverlay.Block.text("Category limits:", RogueScapeTheme.TEXT_PRIMARY));
		build.add(RogueScapeWindowOverlay.Block.text("  Max 4 food  (have 5)", RogueScapeTheme.NEGATIVE));
		tabs.add(new RogueScapeWindowOverlay.Tab("BUILD", build));

		List<RogueScapeWindowOverlay.Block> arts = new ArrayList<>();
		arts.add(RogueScapeWindowOverlay.Block.heading("ARTIFACTS  (3)"));
		arts.add(RogueScapeWindowOverlay.Block.itemGrid(
			new int[]{1631, 995, 1712, 0, 0, 0, 0, 0, 0, 0, 0, 0}, Integer.MAX_VALUE));
		arts.add(RogueScapeWindowOverlay.Block.gap());
		arts.add(RogueScapeWindowOverlay.Block.text("- Relic of Blood", RogueScapeTheme.TEXT_PRIMARY));
		arts.add(RogueScapeWindowOverlay.Block.text("- Twisted Souls", RogueScapeTheme.TEXT_PRIMARY));
		arts.add(RogueScapeWindowOverlay.Block.text("- Dark Hunger", RogueScapeTheme.TEXT_PRIMARY));
		tabs.add(new RogueScapeWindowOverlay.Tab("ARTIFACTS", arts));

		List<RogueScapeWindowOverlay.Block> mods = new ArrayList<>();
		mods.add(RogueScapeWindowOverlay.Block.heading("MODIFIERS"));
		mods.add(RogueScapeWindowOverlay.Block.badge("Twisted Souls", "+15% enemy accuracy",
			RogueScapeTheme.NEGATIVE, 0));
		mods.add(RogueScapeWindowOverlay.Block.badge("Dark Hunger", "-25% food healing",
			RogueScapeTheme.ACCENT, 0));
		tabs.add(new RogueScapeWindowOverlay.Tab("MODIFIERS", mods));

		List<RogueScapeWindowOverlay.Block> prog = new ArrayList<>();
		prog.add(RogueScapeWindowOverlay.Block.heading("PROGRESSION"));
		prog.add(RogueScapeWindowOverlay.Block.text("Bosses Defeated: 6 / 18", RogueScapeTheme.TEXT_PRIMARY));
		prog.add(RogueScapeWindowOverlay.Block.text("Artifacts Found: 23 / 45", RogueScapeTheme.TEXT_PRIMARY));
		prog.add(RogueScapeWindowOverlay.Block.statBar("Upgrades", 14 / 36.0, "14 / 36", RogueScapeTheme.BAR_PROGRESS));
		tabs.add(new RogueScapeWindowOverlay.Tab("PROGRESSION", prog));

		return Arrays.asList(tabs.toArray(new RogueScapeWindowOverlay.Tab[0]));
	}

	private static List<RogueScapeWindowOverlay.Tab> lobbyTabs()
	{
		List<RogueScapeWindowOverlay.Block> blocks = new ArrayList<>();
		blocks.add(RogueScapeWindowOverlay.Block.heading("RUN BUILDER"));
		blocks.add(RogueScapeWindowOverlay.Block.modeTiles(Arrays.asList(
			new RogueScapeWindowOverlay.ModeTile("Scavenger", "Scavenge rooms, draft power, fight bosses.",
				"Fresh source", RogueScapeTheme.POSITIVE, true),
			new RogueScapeWindowOverlay.ModeTile("Rewarded", "Bosses unlock random gear and supplies.",
				"Campaign", RogueScapeTheme.RARITY_LEGENDARY, false),
			new RogueScapeWindowOverlay.ModeTile("Goal Run", "Build a route around account grinds.",
				"Unranked", RogueScapeTheme.INFO, false),
			new RogueScapeWindowOverlay.ModeTile("Weekly", "Shared seed, same ruleset, personal RNG.",
				"Featured seed", RogueScapeTheme.ACCENT, false),
			new RogueScapeWindowOverlay.ModeTile("Custom", "Build route, zones, and mods.",
				"Open builder", RogueScapeTheme.RARITY_EPIC, false))));
		blocks.add(RogueScapeWindowOverlay.Block.gap());
		blocks.add(RogueScapeWindowOverlay.Block.badge("Selected: Scavenger",
			"Preset Auto | Seed (random)", RogueScapeTheme.ACCENT, 0));
		blocks.add(RogueScapeWindowOverlay.Block.text("Run: Scavenger Run", RogueScapeTheme.TEXT_PRIMARY));
		blocks.add(RogueScapeWindowOverlay.Block.heading("BALANCED ROUTE"));
		blocks.add(RogueScapeWindowOverlay.Block.text("Auto-builds region, supply, gear, and boss stages.",
			RogueScapeTheme.TEXT_PRIMARY));

		return Arrays.asList(new RogueScapeWindowOverlay.Tab("RUN BUILDER", blocks),
			new RogueScapeWindowOverlay.Tab("RULES", Arrays.asList(
				RogueScapeWindowOverlay.Block.heading("RUN RULES"),
			RogueScapeWindowOverlay.Block.text("- No bank access", RogueScapeTheme.TEXT_PRIMARY))));
	}

	private static List<RogueScapeWindowOverlay.Tab> customBuilderTabs()
	{
		List<RogueScapeWindowOverlay.Block> mode = new ArrayList<>();
		mode.add(RogueScapeWindowOverlay.Block.heading("CUSTOM MODE"));
		mode.add(RogueScapeWindowOverlay.Block.modeTiles(Arrays.asList(
			new RogueScapeWindowOverlay.ModeTile("Scavenger", "Rooms define what can be collected.", "Room-first", RogueScapeTheme.POSITIVE, true),
			new RogueScapeWindowOverlay.ModeTile("Rewarded", "Bosses and rewards carry the run.", "Boss-first", RogueScapeTheme.RARITY_LEGENDARY, false))));
		mode.add(RogueScapeWindowOverlay.Block.gap());
		mode.add(RogueScapeWindowOverlay.Block.badge("Selected: Scavenger",
			"Custom controls stay in this separate window", RogueScapeTheme.RARITY_EPIC, 0));

		List<RogueScapeWindowOverlay.Block> loadout = new ArrayList<>();
		loadout.add(RogueScapeWindowOverlay.Block.heading("STARTING LOADOUT"));
		loadout.add(RogueScapeWindowOverlay.Block.modeTiles(Arrays.asList(
			new RogueScapeWindowOverlay.ModeTile("Naked", "Start with nothing.", "Hard", RogueScapeTheme.NEGATIVE, true),
			new RogueScapeWindowOverlay.ModeTile("Low Gear", "Minimal starter setup.", "Normal", RogueScapeTheme.INFO, false),
			new RogueScapeWindowOverlay.ModeTile("Mid Gear", "More accessible starting point.", "Easy", RogueScapeTheme.POSITIVE, false),
			new RogueScapeWindowOverlay.ModeTile("Custom Kit", "Sword, shortbow, arrows, and food.", "Hybrid", RogueScapeTheme.RARITY_EPIC, false))));

		List<RogueScapeWindowOverlay.Block> rooms = new ArrayList<>();
		rooms.add(RogueScapeWindowOverlay.Block.heading("ROOM SELECTOR"));
		rooms.add(RogueScapeWindowOverlay.Block.badge("Room", "Lumbridge Swamp", RogueScapeTheme.INFO, 0));
		rooms.add(RogueScapeWindowOverlay.Block.badge("Allowed Collection", "Supply", RogueScapeTheme.RARITY_EPIC, 0));
		rooms.add(RogueScapeWindowOverlay.Block.modeTiles(Arrays.asList(
			new RogueScapeWindowOverlay.ModeTile("Prev Room", "Cycle selected room backward.", "Room", RogueScapeTheme.TEXT_MUTED, false),
			new RogueScapeWindowOverlay.ModeTile("Next Room", "Cycle selected room forward.", "Room", RogueScapeTheme.INFO, false),
			new RogueScapeWindowOverlay.ModeTile("Prev Type", "Cycle allowed collection backward.", "Type", RogueScapeTheme.TEXT_MUTED, false),
			new RogueScapeWindowOverlay.ModeTile("Next Type", "Cycle allowed collection forward.", "Type", RogueScapeTheme.RARITY_EPIC, false),
			new RogueScapeWindowOverlay.ModeTile("Add Room", "Confirm room into the route.", "Confirm", RogueScapeTheme.POSITIVE, false))));
		rooms.add(RogueScapeWindowOverlay.Block.gap());
		rooms.add(RogueScapeWindowOverlay.Block.text("Confirmed route is empty. Add rooms, then add a boss.", RogueScapeTheme.TEXT_MUTED));

		List<RogueScapeWindowOverlay.Block> route = new ArrayList<>();
		route.add(RogueScapeWindowOverlay.Block.heading("ROUTE ORDER"));
		route.add(RogueScapeWindowOverlay.Block.modeTiles(Arrays.asList(
			new RogueScapeWindowOverlay.ModeTile("Select Up", "Move route cursor up.", "Cursor", RogueScapeTheme.TEXT_MUTED, false),
			new RogueScapeWindowOverlay.ModeTile("Select Down", "Move route cursor down.", "Cursor", RogueScapeTheme.TEXT_MUTED, false),
			new RogueScapeWindowOverlay.ModeTile("Move Up", "Move selected row up.", "Reorder", RogueScapeTheme.INFO, false),
			new RogueScapeWindowOverlay.ModeTile("Move Down", "Move selected row down.", "Reorder", RogueScapeTheme.INFO, false),
			new RogueScapeWindowOverlay.ModeTile("Remove", "Remove selected row.", "Delete", RogueScapeTheme.NEGATIVE, false))));
		route.add(RogueScapeWindowOverlay.Block.gap());
		route.add(RogueScapeWindowOverlay.Block.text("No custom rooms selected yet.", RogueScapeTheme.TEXT_MUTED));
		route.add(RogueScapeWindowOverlay.Block.text("End boss: (auto)", RogueScapeTheme.GOLD));

		List<RogueScapeWindowOverlay.Block> seed = new ArrayList<>();
		seed.add(RogueScapeWindowOverlay.Block.heading("CUSTOM SEED"));
		seed.add(RogueScapeWindowOverlay.Block.badge("Seed Preview",
			"mode=Scavenger;loadout=Naked;rooms=lumbridge-swamp:Supply,boss-bryophyta:Boss;mods=", RogueScapeTheme.ACCENT, 0));

		return Arrays.asList(new RogueScapeWindowOverlay.Tab("MODE", mode),
			new RogueScapeWindowOverlay.Tab("LOADOUT", loadout),
			new RogueScapeWindowOverlay.Tab("ROOMS", rooms),
			new RogueScapeWindowOverlay.Tab("ROUTE", route),
			new RogueScapeWindowOverlay.Tab("SEED", seed));
	}
}
