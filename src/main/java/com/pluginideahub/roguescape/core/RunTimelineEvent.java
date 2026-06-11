package com.pluginideahub.roguescape.core;

public final class RunTimelineEvent
{
	public enum Type
	{
		STAGE_ADDED,
		STAGE_ENTERED,
		STAGE_CLEARED,
		ITEM_GAINED,
		RELIC_ADDED,
		VIOLATION,
		STARTER_KIT_DECLARED,
		RUN_LOOP_NOTE,
		RUN_COMPLETED,
		RUN_FAILED
	}

	private final int sequence;
	private final Type type;
	private final String description;

	public RunTimelineEvent(int sequence, Type type, String description)
	{
		this.sequence = sequence;
		this.type = type;
		this.description = description;
	}

	public int sequence() { return sequence; }
	public Type type() { return type; }
	public String description() { return description; }
}
