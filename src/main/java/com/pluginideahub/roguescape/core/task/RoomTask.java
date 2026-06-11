package com.pluginideahub.roguescape.core.task;

public final class RoomTask
{
	private final String label;
	private final String skillName;
	private final int xpEventsRequired;
	private int xpEvents;

	public RoomTask(String label, String skillName, int xpEventsRequired)
	{
		this.label = label == null || label.trim().isEmpty() ? "Gain XP in this room" : label.trim();
		this.skillName = skillName == null ? "" : skillName.trim().toLowerCase();
		this.xpEventsRequired = Math.max(1, xpEventsRequired);
	}

	public String label() { return label; }
	public int xpEvents() { return xpEvents; }
	public int xpEventsRequired() { return xpEventsRequired; }
	public boolean complete() { return xpEvents >= xpEventsRequired; }

	public boolean recordStatChanged(String changedSkill)
	{
		String normalized = changedSkill == null ? "" : changedSkill.trim().toLowerCase();
		if (!skillName.isEmpty() && !skillName.equals(normalized))
		{
			return false;
		}
		if (!complete())
		{
			xpEvents++;
		}
		return true;
	}

	public String progressLabel()
	{
		return label + " (" + Math.min(xpEvents, xpEventsRequired) + " / " + xpEventsRequired + ")";
	}
}
