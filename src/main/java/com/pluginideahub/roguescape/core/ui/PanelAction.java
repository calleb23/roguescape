package com.pluginideahub.roguescape.core.ui;

/**
 * Simplified panel actions for the clean two-state UX.
 */
public enum PanelAction
{
	START_RUN,
	RESET_RUN,
	COMPLETE_STAGE,
	CHOOSE_REWARD,
	CHOOSE_REWARD_1,
	CHOOSE_REWARD_2,
	CHOOSE_REWARD_3,
	SKIP_REWARD,
	NEXT_STAGE,
	FAIL_RUN,
	/** Developer-only: complete the active stage even if objective tracking is unfinished. */
	DEV_COMPLETE_STAGE,
	/** Developer-only: force the run to a completed state for UI testing. */
	DEV_COMPLETE_RUN,
	/** Developer-only: log the live in-game side journal widget tree. */
	DEV_LOG_JOURNAL
}
