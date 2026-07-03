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
	/** Copy the finished run's recap (markdown) to the system clipboard. */
	EXPORT_RECAP,
	/** Open/close the in-game journal (book) window. */
	TOGGLE_WINDOW,
	/** Developer-only: complete the active stage even if objective tracking is unfinished. */
	DEV_COMPLETE_STAGE,
	/** Developer-only: force the run to a completed state for UI testing. */
	DEV_COMPLETE_RUN,
	/** Developer-only: emulate walking into the current stage's allowed region (skips travel). */
	DEV_ENTER_ROOM,
	/** Developer-only: emulate the current boss falling (fires the kill signal). */
	DEV_BOSS_KILL,
	/** Developer-only: log the live in-game side journal widget tree. */
	DEV_LOG_JOURNAL,
	/** Developer-only sculptor: clear everything (objects, walls, floor) around the player. */
	DEV_LOBBY_CLEAR,
	/** Developer-only sculptor: blank only the floor render around the player. */
	DEV_LOBBY_BLANK_FLOORS,
	/** Developer-only sculptor: place the configured cache model at the player's tile. */
	DEV_LOBBY_PLACE_MODEL,
	/** Developer-only sculptor: drop the edit plan and reload the scene (undo everything). */
	DEV_LOBBY_RESTORE
}
