package com.pluginideahub.roguescape.core.restriction;

/**
 * The verdict for an attempted action under the run's active restrictions.
 *
 * <ul>
 *   <li>{@link #ALLOW} — permitted; nothing happens.</li>
 *   <li>{@link #BLOCK} — prevent the action (e.g. consume the menu entry).</li>
 *   <li>{@link #FAIL} — the action got through and is forbidden; end the run.</li>
 * </ul>
 */
public enum RestrictionOutcome
{
	ALLOW,
	BLOCK,
	FAIL
}
