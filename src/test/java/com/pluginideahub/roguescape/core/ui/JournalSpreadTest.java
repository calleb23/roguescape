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
