package com.pluginideahub.roguescape.core.enforcement;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.restriction.Restriction;
import com.pluginideahub.roguescape.core.restriction.RunRestrictions;

/**
 * Pure-Java enforcement rule model for an active RogueScape run.
 *
 * Captures which categories of player action the menu/event layer should block
 * (bank, trade, GE, ground pickup) and whether to surface a region-leave warning.
 *
 * Built either directly via the setters or derived from a run via {@link #forRun}.
 * Zero RuneLite dependencies — wiring into menus happens in the plugin layer.
 */
public final class RogueScapeEnforcementRules
{
	private boolean blockBank;
	private boolean blockTrade;
	private boolean blockGrandExchange;
	private boolean blockGroundPickup;
	private boolean blockWalkOutsideRoom;
	private boolean blockPrayer;
	private boolean blockPotions;
	private boolean warnLeaveRoom;

	public RogueScapeEnforcementRules() {}

	public boolean blockBank() { return blockBank; }
	public boolean blockTrade() { return blockTrade; }
	public boolean blockGrandExchange() { return blockGrandExchange; }
	public boolean blockGroundPickup() { return blockGroundPickup; }
	public boolean blockWalkOutsideRoom() { return blockWalkOutsideRoom; }
	public boolean blockPrayer() { return blockPrayer; }
	public boolean blockPotions() { return blockPotions; }
	public boolean warnLeaveRoom() { return warnLeaveRoom; }

	public RogueScapeEnforcementRules setBlockBank(boolean v) { this.blockBank = v; return this; }
	public RogueScapeEnforcementRules setBlockTrade(boolean v) { this.blockTrade = v; return this; }
	public RogueScapeEnforcementRules setBlockGrandExchange(boolean v) { this.blockGrandExchange = v; return this; }
	public RogueScapeEnforcementRules setBlockGroundPickup(boolean v) { this.blockGroundPickup = v; return this; }
	public RogueScapeEnforcementRules setBlockWalkOutsideRoom(boolean v) { this.blockWalkOutsideRoom = v; return this; }
	public RogueScapeEnforcementRules setBlockPrayer(boolean v) { this.blockPrayer = v; return this; }
	public RogueScapeEnforcementRules setBlockPotions(boolean v) { this.blockPotions = v; return this; }
	public RogueScapeEnforcementRules setWarnLeaveRoom(boolean v) { this.warnLeaveRoom = v; return this; }

	/**
	 * Derive enforcement rules from the run's {@link RunRestrictions} — the single verdict brain
	 * of the subtractive design. This class stays a thin menu-blocking view over that state; the
	 * only extra input is region-lock arming (a timing detail of stage entry, not a rule).
	 */
	public static RogueScapeEnforcementRules forRun(RogueScapeRun run)
	{
		RogueScapeEnforcementRules rules = new RogueScapeEnforcementRules();
		if (run == null) return rules;
		RunRestrictions r = run.currentRestrictions();
		boolean armedRegionLock = r.isRestricted(Restriction.LEAVE_REGION) && run.regionRestrictionArmed();
		return rules
			.setBlockBank(r.isRestricted(Restriction.BANK))
			.setBlockTrade(r.isRestricted(Restriction.TRADE))
			.setBlockGrandExchange(r.isRestricted(Restriction.GRAND_EXCHANGE))
			.setBlockGroundPickup(r.isRestricted(Restriction.GROUND_PICKUP_OUTSIDE_ROOM))
			.setBlockWalkOutsideRoom(armedRegionLock)
			.setBlockPrayer(r.isRestricted(Restriction.PRAYER))
			.setBlockPotions(r.isRestricted(Restriction.POTIONS))
			.setWarnLeaveRoom(armedRegionLock);
	}
}
