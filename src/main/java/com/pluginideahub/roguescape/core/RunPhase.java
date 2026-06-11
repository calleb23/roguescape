package com.pluginideahub.roguescape.core;

/**
 * Playable RogueScape run-loop phase. This is deliberately narrower than the
 * terminal run state: an ACTIVE run can be inside a room/boss or back at base
 * choosing rewards before manually starting the next stage.
 */
public enum RunPhase
{
	TRAVEL_TO_STAGE("Travel"),
	ROOM_ACTIVE("Room"),
	BOSS_ACTIVE("Boss"),
	BASE_REWARD("Reward"),
	RUN_COMPLETE("Complete"),
	RUN_FAILED("Failed");

	private final String displayName;

	RunPhase(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
