package com.pluginideahub.roguescape.core.unlock;

public final class RunUnlock
{
	private final RunUnlockType type;
	private final String label;
	private final String sourceStageId;
	private final String sourceStageName;

	public RunUnlock(RunUnlockType type, String label, String sourceStageId, String sourceStageName)
	{
		if (type == null) throw new IllegalArgumentException("type required");
		this.type = type;
		this.label = label == null || label.trim().isEmpty() ? type.label() : label.trim();
		this.sourceStageId = sourceStageId == null ? "" : sourceStageId;
		this.sourceStageName = sourceStageName == null ? "" : sourceStageName;
	}

	public RunUnlockType type() { return type; }
	public String label() { return label; }
	public String sourceStageId() { return sourceStageId; }
	public String sourceStageName() { return sourceStageName; }

	public String displayRow()
	{
		return label + (sourceStageName.isEmpty() ? "" : " from " + sourceStageName);
	}
}
