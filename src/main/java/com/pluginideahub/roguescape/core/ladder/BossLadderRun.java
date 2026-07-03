package com.pluginideahub.roguescape.core.ladder;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RestrictionOutcome;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Boss Ladder run state machine (MVP chunk 3): an ordered list of bosses climbed under a
 * {@link RunRestrictions}, cycling <b>PREP → FIGHT → REWARD</b> per boss.
 *
 * <ul>
 *   <li><b>PREP</b> — bank / Grand Exchange / trade are temporarily allowed so the player can gear
 *       up; {@link #decide} grants that allowance without mutating the underlying restrictions.</li>
 *   <li><b>FIGHT</b> — entered via {@link #beginFight()} (the adapter validates the worn+carried
 *       loadout at this gate — chunk 6); the full restrictions are enforced.</li>
 *   <li><b>REWARD</b> — the boss fell ({@link #recordBossKill()}); the player picks a
 *       restriction-easing relic via {@link #chooseReward} (or {@link #skipReward()}), then the
 *       next boss's PREP begins — or the ladder completes after the final boss.</li>
 * </ul>
 *
 * Breaking a FAIL-enforced restriction (or abandoning) ends the run: {@link #fail}. Pure Java —
 * no RuneLite; the adapter drives the transitions and asks {@link #decide} for verdicts.
 */
public final class BossLadderRun
{
	private final List<String> bosses;
	private final RunRestrictions restrictions;
	private final List<Relic> earnedRelics = new ArrayList<>();
	private int bossIndex;
	private LadderPhase phase = LadderPhase.PREP;
	private String failReason = "";

	public BossLadderRun(List<String> bosses, RunRestrictions restrictions)
	{
		if (bosses == null || bosses.isEmpty())
		{
			throw new IllegalArgumentException("at least one boss required");
		}
		this.bosses = Collections.unmodifiableList(new ArrayList<>(bosses));
		this.restrictions = restrictions == null ? RunRestrictions.unrestricted() : restrictions;
	}

	// ---------- Transitions ----------

	/**
	 * The fight gate: PREP → FIGHT. The adapter validates the full worn+carried loadout against
	 * the restrictions before calling this (chunk 6); the prep allowance ends here.
	 */
	public void beginFight()
	{
		requirePhase(LadderPhase.PREP, "beginFight");
		phase = LadderPhase.FIGHT;
	}

	/** The current boss fell: FIGHT → REWARD. */
	public void recordBossKill()
	{
		requirePhase(LadderPhase.FIGHT, "recordBossKill");
		phase = LadderPhase.REWARD;
	}

	/**
	 * Claim the reward: applies the relic's easing to the run's restrictions, then advances to
	 * the next boss's PREP — or completes the ladder after the final boss.
	 */
	public void chooseReward(Relic relic)
	{
		requirePhase(LadderPhase.REWARD, "chooseReward");
		if (relic != null)
		{
			RelicEngine.applyEasing(relic, restrictions);
			earnedRelics.add(relic);
		}
		advance();
	}

	/** Claim a drafted card — a lane raise (upgrade) or a relic (permit) — and climb on. */
	public void chooseReward(LadderRewardCard card)
	{
		if (card != null && card.isRaise())
		{
			requirePhase(LadderPhase.REWARD, "chooseReward");
			restrictions.raiseLane(card.lane());
			advance();
			return;
		}
		chooseReward(card == null ? null : card.relic());
	}

	/** Decline the reward and climb on. */
	public void skipReward()
	{
		chooseReward((Relic) null);
	}

	/** A restriction was broken, the player died, or the run was abandoned — the run ends. */
	public void fail(String reason)
	{
		if (phase == LadderPhase.COMPLETE || phase == LadderPhase.FAILED)
		{
			return;
		}
		failReason = reason == null ? "" : reason;
		phase = LadderPhase.FAILED;
	}

	private void advance()
	{
		if (bossIndex + 1 >= bosses.size())
		{
			phase = LadderPhase.COMPLETE;
		}
		else
		{
			bossIndex++;
			phase = LadderPhase.PREP;
		}
	}

	private void requirePhase(LadderPhase expected, String action)
	{
		if (phase != expected)
		{
			throw new IllegalStateException(action + " requires " + expected + " but the run is " + phase);
		}
	}

	// ---------- Verdicts ----------

	/**
	 * The rule verdict for an attempted action right now. In PREP the economy locks (bank, GE,
	 * trade) are temporarily allowed — gearing up is the point of prep; everything else, and
	 * every phase after the gate, defers to the run's restrictions.
	 */
	public RestrictionOutcome decide(Restriction attempted)
	{
		if (phase == LadderPhase.PREP && isPrepAllowance(attempted))
		{
			return RestrictionOutcome.ALLOW;
		}
		return restrictions.decide(attempted);
	}

	/** True while PREP temporarily allows this action despite an active restriction. */
	public boolean prepAllowanceActive(Restriction attempted)
	{
		return phase == LadderPhase.PREP && isPrepAllowance(attempted)
			&& restrictions.isRestricted(attempted);
	}

	private static boolean isPrepAllowance(Restriction attempted)
	{
		return attempted == Restriction.BANK
			|| attempted == Restriction.GRAND_EXCHANGE
			|| attempted == Restriction.TRADE;
	}

	// ---------- State ----------

	public LadderPhase phase() { return phase; }
	public RunRestrictions restrictions() { return restrictions; }
	public List<String> bosses() { return bosses; }
	public int bossIndex() { return bossIndex; }
	public int bossesTotal() { return bosses.size(); }
	public String currentBoss() { return bosses.get(bossIndex); }
	public boolean isOver() { return phase == LadderPhase.COMPLETE || phase == LadderPhase.FAILED; }
	public String failReason() { return failReason; }
	public List<Relic> earnedRelics() { return Collections.unmodifiableList(earnedRelics); }

	/** Bosses already felled (the reward for the current one may still be pending). */
	public int bossesDefeated()
	{
		if (phase == LadderPhase.COMPLETE)
		{
			return bosses.size();
		}
		return bossIndex + (phase == LadderPhase.REWARD ? 1 : 0);
	}
}
