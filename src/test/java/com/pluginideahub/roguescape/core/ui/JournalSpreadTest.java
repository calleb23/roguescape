package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunStageType;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Locks the pure-core Run-phase two-page spread: the left/right split now lives on the view model
 * (not the RuneLite adapter), so it can be asserted here without a client.
 */
public class JournalSpreadTest
{
	private static SidePanelViewModel runVm()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Spread run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "", "Find one permitted upgrade", 1);
		base.addStage("R2", RunStageType.ROOM, "Varrock", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		run.chooseRelic(com.pluginideahub.roguescape.core.relic.ModifierLibrary.noFood());
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 1_000L);
		loop.markNow(62_000L);
		return SidePanelViewModel.active(loop, PanelTab.RUN);
	}

	@Test
	public void contractSpreadPutsChoicesLeftAndBriefingRight()
	{
		com.pluginideahub.roguescape.core.briefing.RunBriefing briefing =
			com.pluginideahub.roguescape.core.briefing.RunBriefingBuilder.preview(
				com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE,
				com.pluginideahub.roguescape.core.RunPreset.UNSPECIFIED,
				"rat-king-42", "Naked", false, 0);
		java.util.List<String> catalog = java.util.Arrays.asList(
			"The Rat King's Road", "The Molten Vigil", "The Sunken March", "The Iron Toll",
			"The Crooked Climb", "The Beggar's Oath");
		JournalSpread spread = SidePanelViewModel.contractSpread(
			com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE, "Dungeon Crawl Run", "rat-king-42",
			briefing, "", catalog, 1, 0);

		assertTrue(spread.title().contains("Contract"));
		// Left: the mode contracts (Dungeon Crawl selected) and the Begin stamp.
		assertTrue("mode choices on the left page", spread.left().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES
				&& b.choices().stream().anyMatch(c -> c.title().equals("Dungeon Crawl") && c.isSelected())));
		assertTrue("Begin stamp on the left page", spread.left().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES
				&& b.choices().stream().anyMatch(c -> c.actionId().equals("start-run"))));
		// Right: the route catalogue — every entry pickable, the selected one marked.
		assertTrue("catalogue heading with page count", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().startsWith("The Routes")));
		assertTrue("the picked route is selected", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES && b.choices().stream().anyMatch(c ->
				c.actionId().equals("route:1") && c.isSelected())));
		assertTrue("page arrows for a multi-page catalogue", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES && b.choices().stream().anyMatch(c ->
				c.actionId().equals("routes-page:next"))));
		assertTrue("win condition on the right page", spread.right().stream().anyMatch(b ->
			b.text().startsWith("WIN:")));
	}

	@Test
	public void contractSpreadWithoutBriefingExplainsWhy()
	{
		JournalSpread spread = SidePanelViewModel.contractSpread(
			com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE, "Run", "", null, "no route",
			java.util.Collections.emptyList(), 0, 0);
		assertTrue(spread.right().stream().anyMatch(b ->
			b.tone() == JournalSpread.Tone.NEGATIVE && b.text().contains("no route")));
	}

	@Test
	public void rewardSpreadPutsLedgerRight()
	{
		JournalSpread spread = runVm().rewardSpread("The chest opens", "Reward — 01:01");
		assertTrue(spread.title().contains("chest"));
		assertTrue("ledger heading on the right page", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("The Ledger")));
		assertTrue("hourglass on the right page", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HOURGLASS));
		assertTrue("pocket relics on the right page", spread.right().stream().anyMatch(b ->
			b.text().contains("No Food")));
		// Left page stays empty in core — the adapter injects the cards there.
		assertTrue(spread.left().isEmpty());
	}

	@Test
	public void terminalRunSpreadReadsAsTheFinalPage()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Final page");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "");
		base.enterStage("R1");
		RogueScapeRun run = RogueScapeRun.wrap(base);
		base.completeRun("Done", com.pluginideahub.roguescape.core.RunCompletionReason.MANUAL_SUCCESS);
		RogueScapeRunLoop loop = new RogueScapeRunLoop(run, 0L);
		JournalSpread spread = SidePanelViewModel.active(loop, PanelTab.RUN).runSpread();

		assertTrue(spread.title().equals("The Final Page"));
		assertTrue("recap stats land on the left page", spread.left().stream().anyMatch(b ->
			b.text().startsWith("Time afoot:")));
	}

	@Test
	public void mastheadNamesTheCurrentChapter()
	{
		JournalSpread spread = runVm().runSpread();
		assertTrue(spread.title().contains("Lumbridge"));
		assertTrue(spread.subtitle().contains("afoot"));
	}

	@Test
	public void leftPageHoldsTheCollections()
	{
		JournalSpread spread = runVm().runSpread();
		// Upgrades | Relics live in a two-column block; Curses are pinned after a FILL.
		JournalSpread.Block cols = spread.left().stream()
			.filter(b -> b.kind() == JournalSpread.Block.Kind.COLUMNS)
			.findFirst().orElse(null);
		assertNotNull("Upgrades|Relics columns present", cols);
		assertTrue(cols.colLeft().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("Upgrades")));
		assertTrue(cols.colRight().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("Relics")));
		assertTrue("relic pockets in the right sub-column",
			cols.colRight().stream().anyMatch(b -> b.kind() == JournalSpread.Block.Kind.POCKETS
				&& b.tone() == JournalSpread.Tone.GOLD));
		// Curses strip pinned to the page bottom after a FILL.
		assertTrue(spread.left().stream().anyMatch(b -> b.kind() == JournalSpread.Block.Kind.FILL));
		assertTrue(spread.left().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("Curses")));
		assertTrue("No Food shows on the curse strip",
			spread.left().stream().anyMatch(b -> b.kind() == JournalSpread.Block.Kind.POCKETS
				&& b.tone() == JournalSpread.Tone.NEGATIVE && b.names().contains("No Food")));
	}

	@Test
	public void rightPageHoldsTheJourney()
	{
		JournalSpread spread = runVm().runSpread();
		assertTrue("the route heading appears",
			spread.right().stream().anyMatch(b ->
				b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("The Route")));
		assertTrue("the current room is named",
			spread.right().stream().anyMatch(b -> b.text().startsWith("Room — Lumbridge")));
		assertTrue("the objective sits in the info block",
			spread.right().stream().anyMatch(b -> b.text().contains("Find one permitted upgrade")));
		assertTrue("what comes next is noted",
			spread.right().stream().anyMatch(b -> b.text().equals("Next: Varrock")));
		assertTrue("a forbidden rule note appears in the info block",
			spread.right().stream().anyMatch(b ->
				b.kind() == JournalSpread.Block.Kind.NOTE && b.tone() == JournalSpread.Tone.NEGATIVE));
	}

	@Test
	public void bossBandListsOnlyBosses()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Band run");
		base.addStage("B1", RunStageType.BOSS, "Giant Mole", "");
		base.addStage("B2", RunStageType.BOSS, "Scorpia", "");
		base.addStage("B3", RunStageType.BOSS, "Vorkath", "");
		base.enterStage("B1");
		RogueScapeRunLoop loop = new RogueScapeRunLoop(RogueScapeRun.wrap(base), 0L);
		JournalSpread spread = SidePanelViewModel.active(loop, PanelTab.RUN).runSpread();

		JournalSpread.Block band = spread.right().stream()
			.filter(b -> b.kind() == JournalSpread.Block.Kind.BOSS_BAND)
			.findFirst().orElse(null);
		assertNotNull("boss band present", band);
		assertEquals(3, band.chapters().size());
		assertTrue(band.chapters().get(0).isCurrent());
		assertTrue(band.chapters().stream().allMatch(SidePanelViewModel.Chapter::isBoss));
	}
}
