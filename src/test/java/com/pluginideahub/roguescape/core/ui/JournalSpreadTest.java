package com.pluginideahub.roguescape.core.ui;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunStageType;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Locks the pure-core Run-phase two-page spread: the left/right split now lives on the view model
 * (not the RuneLite adapter), so it can be asserted here without a client.
 */
public class JournalSpreadTest
{
	private static SidePanelViewModel runVm()
	{
		RogueScapeRunSession base = RogueScapeRunSession.start("Spread run");
		base.addStage("R1", RunStageType.ROOM, "Lumbridge", "", "Find one legal upgrade", 1);
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
		JournalSpread spread = SidePanelViewModel.contractSpread(
			com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE, "Scavenger Run", "rat-king-42",
			briefing, "");

		assertTrue(spread.title().contains("Contract"));
		// Left: the mode contracts (Scavenger selected) and the Begin stamp.
		assertTrue("mode choices on the left page", spread.left().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES
				&& b.choices().stream().anyMatch(c -> c.title().equals("Scavenger") && c.isSelected())));
		assertTrue("Begin stamp on the left page", spread.left().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.CHOICES
				&& b.choices().stream().anyMatch(c -> c.actionId().equals("start-run"))));
		// Right: the route briefing with win/lose.
		assertTrue("route rooms on the right page", spread.right().stream().anyMatch(b ->
			b.kind() == JournalSpread.Block.Kind.HEADING && b.text().startsWith("The Route")));
		assertTrue("win condition on the right page", spread.right().stream().anyMatch(b ->
			b.text().startsWith("WIN:")));
	}

	@Test
	public void contractSpreadWithoutBriefingExplainsWhy()
	{
		JournalSpread spread = SidePanelViewModel.contractSpread(
			com.pluginideahub.roguescape.core.RunMode.FRESH_SOURCE, "Run", "", null, "no route");
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
	public void leftPageHoldsEntryAndRulesOfThisPlace()
	{
		JournalSpread spread = runVm().runSpread();
		// The objective entry.
		assertTrue("objective appears on the left page",
			spread.left().stream().anyMatch(b -> b.text().contains("Find one legal upgrade")));
		// The "rules of this place" heading.
		assertTrue("rules heading appears on the left page",
			spread.left().stream().anyMatch(b ->
				b.kind() == JournalSpread.Block.Kind.HEADING && b.text().equals("The rules of this place")));
		// A forbidden rule (the No Food relic) shows as a negative-tone note.
		assertTrue("a forbidden rule note appears on the left page",
			spread.left().stream().anyMatch(b ->
				b.kind() == JournalSpread.Block.Kind.NOTE && b.tone() == JournalSpread.Tone.NEGATIVE));
	}

	@Test
	public void rightPageHoldsTheRecordHourglassAndScore()
	{
		JournalSpread spread = runVm().runSpread();
		assertTrue("The Record (chapters) appears on the right page",
			spread.right().stream().anyMatch(b -> b.kind() == JournalSpread.Block.Kind.CHAPTERS));
		assertTrue("The Hourglass appears on the right page",
			spread.right().stream().anyMatch(b -> b.kind() == JournalSpread.Block.Kind.HOURGLASS));
		assertTrue("the running score appears on the right page",
			spread.right().stream().anyMatch(b -> b.text().startsWith("Score:")));
	}

	@Test
	public void leftPageNamesWhatComesNext()
	{
		JournalSpread spread = runVm().runSpread();
		assertTrue("the next stage is named on the left page",
			spread.left().stream().anyMatch(b -> b.text().equals("Next: Varrock")));
	}
}
