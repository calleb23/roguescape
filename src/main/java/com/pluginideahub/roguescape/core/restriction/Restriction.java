package com.pluginideahub.roguescape.core.restriction;

/**
 * The catalog of things a run can forbid.
 *
 * RogueScape is a <em>subtractive</em> roguelike: a run starts with some restrictions active, and
 * relics EASE them (permit the action). RogueScape cannot change underlying game mechanics — it can
 * only restrict and un-restrict — so every lever in the game is one of these.
 *
 * <p>Each restriction declares how it is enforced:
 * <ul>
 *   <li>{@link Enforcement#BLOCK} — the action can be prevented outright (e.g. hide the menu entry).</li>
 *   <li>{@link Enforcement#FAIL} — it cannot be pre-blocked reliably, so doing it is detected and
 *       ends the run. Same deterrent, later timing.</li>
 * </ul>
 *
 * <p>Parameterised restrictions (gear-tier cap, inventory limit, allowed spellbook/combat style)
 * carry their value on {@link RunRestrictions}; the enum value only marks that the cap is in force.
 *
 * Pure data, no behaviour — the levers are a table, not hardcoded logic.
 */
public enum Restriction
{
	BANK(Family.ECONOMY, Enforcement.BLOCK, "Use the bank"),
	TRADE(Family.ECONOMY, Enforcement.BLOCK, "Trade other players"),
	GRAND_EXCHANGE(Family.ECONOMY, Enforcement.BLOCK, "Use the Grand Exchange"),

	FOOD(Family.CONSUMABLE, Enforcement.BLOCK, "Eat food"),
	POTIONS(Family.CONSUMABLE, Enforcement.BLOCK, "Drink potions"),

	TELEPORTS(Family.MOVEMENT, Enforcement.BLOCK, "Teleport"),
	LEAVE_REGION(Family.MOVEMENT, Enforcement.FAIL, "Leave the room's region"),
	GROUND_PICKUP_OUTSIDE_ROOM(Family.MOVEMENT, Enforcement.BLOCK, "Pick up items outside the room"),

	PRAYER(Family.COMBAT, Enforcement.FAIL, "Use prayer"),
	PIETY(Family.COMBAT, Enforcement.FAIL, "Use Piety"),
	RIGOUR(Family.COMBAT, Enforcement.FAIL, "Use Rigour"),
	AUGURY(Family.COMBAT, Enforcement.FAIL, "Use Augury"),

	SHIELD(Family.GEAR, Enforcement.BLOCK, "Equip a shield"),
	GEAR_TIER_CAP(Family.GEAR, Enforcement.BLOCK, "Equip above the gear-tier cap"),

	AMMO(Family.RESOURCE, Enforcement.FAIL, "Use ammunition"),
	RUNES(Family.RESOURCE, Enforcement.FAIL, "Cast with runes"),
	INVENTORY_LIMIT(Family.RESOURCE, Enforcement.BLOCK, "Carry over the inventory limit"),

	SPELLBOOK(Family.STYLE, Enforcement.FAIL, "Use a forbidden spellbook"),
	COMBAT_STYLE(Family.STYLE, Enforcement.FAIL, "Use a forbidden combat style");

	/** Grouping for legible display in the briefing / custom editor. */
	public enum Family { ECONOMY, CONSUMABLE, MOVEMENT, COMBAT, GEAR, RESOURCE, STYLE }

	/** How the plugin holds the line on a restriction. */
	public enum Enforcement { BLOCK, FAIL }

	private final Family family;
	private final Enforcement enforcement;
	private final String forbids;

	Restriction(Family family, Enforcement enforcement, String forbids)
	{
		this.family = family;
		this.enforcement = enforcement;
		this.forbids = forbids;
	}

	public Family family() { return family; }
	public Enforcement enforcement() { return enforcement; }

	/** Player-facing phrase for the action this restriction forbids, e.g. "Eat food". */
	public String forbids() { return forbids; }

	/** True when this restriction's value lives on {@link RunRestrictions} rather than being a plain on/off. */
	public boolean isParameterised()
	{
		return this == GEAR_TIER_CAP || this == INVENTORY_LIMIT || this == SPELLBOOK || this == COMBAT_STYLE;
	}
}
