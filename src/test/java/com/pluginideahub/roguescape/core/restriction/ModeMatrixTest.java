package com.pluginideahub.roguescape.core.restriction;

import com.pluginideahub.roguescape.core.ladder.BossLadderRun;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The mode × mechanic matrix (locked 2026-07-03, session 2), held as executable invariants:
 * the DC shackle set, the curse-offering rule, and curse-beats-prep in the Boss Ladder.
 */
public class ModeMatrixTest
{
	@Test
	public void dungeonCrawlShackleSetIsNineRestrictionsFoodFree()
	{
		RunRestrictions r = ModeShackles.dungeonCrawl();
		assertTrue(r.isRestricted(Restriction.BANK));
		assertTrue(r.isRestricted(Restriction.GRAND_EXCHANGE));
		assertTrue(r.isRestricted(Restriction.TRADE));
		assertTrue(r.isRestricted(Restriction.PRAYER));
		assertTrue(r.isRestricted(Restriction.PIETY));
		assertTrue(r.isRestricted(Restriction.RIGOUR));
		assertTrue(r.isRestricted(Restriction.AUGURY));
		assertTrue(r.isRestricted(Restriction.POTIONS));
		assertTrue(r.isRestricted(Restriction.SPELLBOOK));
		assertEquals(Spellbook.STANDARD, r.allowedSpellbook());
		assertEquals(9, r.count());
		// Food stays free — you must eat to survive the early rooms.
		assertFalse(r.isRestricted(Restriction.FOOD));
		// No lanes in DC.
		assertFalse(r.isRestricted(Restriction.GEAR_TIER_CAP));
	}

	@Test
	public void curseOfferingRuleHidesTheRedundantFiveInDungeonCrawl()
	{
		// Potions are in the shackle set, so Dry Throat is redundant in DC too — the generic
		// rule caught what the prose enumeration missed.
		List<Curse> offered = Curse.offerable(ModeShackles.dungeonCrawl());
		assertEquals(7, offered.size());
		assertFalse(offered.contains(Curse.SEALED_BANK));
		assertFalse(offered.contains(Curse.FAITHLESS));
		assertFalse(offered.contains(Curse.GODLESS));
		assertFalse(offered.contains(Curse.BOUND_BOOK));
		assertFalse(offered.contains(Curse.DRY_THROAT));
		assertTrue(offered.containsAll(Arrays.asList(
			Curse.FAMINE, Curse.ANCHORED, Curse.BARE_FISTED,
			Curse.QUIVERLESS, Curse.RUNELESS, Curse.TIGHT_POCKETS, Curse.ONE_STYLE)));
	}

	@Test
	public void bossLadderOffersAllTwelve()
	{
		// BL has no standard shackles — StartTier caps lanes, which no curse touches.
		RunRestrictions baseline = RunRestrictions.starting(StartTier.LOW, EnumSet.noneOf(Curse.class));
		assertEquals(12, Curse.offerable(baseline).size());
	}

	@Test
	public void curseBeatsPrepInTheBossLadder()
	{
		EnumSet<Curse> curses = EnumSet.of(Curse.SEALED_BANK);
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW, curses);
		BossLadderRun run = new BossLadderRun(Arrays.asList("Giant Mole"), r, curses);

		// PREP: the cursed economy locks stay sealed — the allowance never opens them.
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.BANK));
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.GRAND_EXCHANGE));
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.TRADE));
		assertFalse(run.prepAllowanceActive(Restriction.BANK));
	}

	@Test
	public void uncursedPrepStillOpensTheEconomy()
	{
		RunRestrictions r = RunRestrictions.starting(StartTier.LOW, EnumSet.of(Curse.FAMINE));
		// The mode itself seals the economy outside prep; FAMINE is unrelated to it.
		r.restrict(Restriction.BANK).restrict(Restriction.GRAND_EXCHANGE).restrict(Restriction.TRADE);
		BossLadderRun run = new BossLadderRun(Arrays.asList("Giant Mole"), r, EnumSet.of(Curse.FAMINE));

		assertEquals(RestrictionOutcome.ALLOW, run.decide(Restriction.BANK));
		assertTrue(run.prepAllowanceActive(Restriction.BANK));
		// The curse still bites in prep.
		assertEquals(RestrictionOutcome.BLOCK, run.decide(Restriction.FOOD));
	}
}
