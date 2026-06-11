package com.pluginideahub.roguescape.core.enforcement;

/**
 * Decision for a single menu entry under the current enforcement rules.
 *
 * <ul>
 *   <li>{@link #ALLOW} — the menu entry should remain available.</li>
 *   <li>{@link #BLOCK} — the menu entry should be hidden/consumed.</li>
 *   <li>{@link #WARN} — the player should see a visible warning but the entry is not blocked
 *       (currently used only for the region-leave sentinel check).</li>
 * </ul>
 */
public enum MenuEnforcementDecision
{
	ALLOW,
	BLOCK,
	WARN
}
