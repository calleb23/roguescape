package com.pluginideahub.roguescape.core.enforcement;

import com.pluginideahub.roguescape.core.RogueScapeRun;

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
	 * Derive enforcement rules from a run's current state.
	 *
	 * Trade and GE are always blocked during an active run. Bank is blocked unless the run
	 * explicitly allows bank access. Ground pickup and region-leave warning only apply when
	 * the current stage actually restricts region.
	 */
	public static RogueScapeEnforcementRules forRun(RogueScapeRun run)
	{
		RogueScapeEnforcementRules rules = new RogueScapeEnforcementRules();
		if (run == null) return rules;
		boolean restrictsRegion = run.currentStageRule().restrictsRegion();
		boolean armedRegionLock = restrictsRegion && run.regionRestrictionArmed();
		return rules
			.setBlockBank(!run.bankUnlocked())
			.setBlockTrade(!run.tradeUnlocked())
			.setBlockGrandExchange(true)
			.setBlockGroundPickup(restrictsRegion)
			.setBlockWalkOutsideRoom(armedRegionLock)
			.setBlockPrayer(!run.prayerUnlocked())
			.setBlockPotions(!run.potionUnlocked())
			.setWarnLeaveRoom(armedRegionLock);
	}
}
