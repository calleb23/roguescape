package com.pluginideahub.roguescape.core.legality;

/**
 * Stage 2 — strictness/policy mode applied when an item's provenance cannot be proven.
 *
 * STRICT: any suspicious or illegal event fails the run.
 * BALANCED: illegal events fail the run; suspicious events warn but require manual approval.
 * TRUST: only explicit illegal/manual-mark events fail the run; suspicious events log only.
 */
public enum StrictnessMode
{
	STRICT,
	BALANCED,
	TRUST
}
