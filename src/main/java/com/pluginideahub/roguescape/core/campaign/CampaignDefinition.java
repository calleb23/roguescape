package com.pluginideahub.roguescape.core.campaign;

import com.pluginideahub.roguescape.core.RunPreset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CampaignDefinition
{
	private final RunPreset preset;
	private final String title;
	private final String description;
	private final String difficulty;
	private final List<String> roomIds;
	private final List<String> bossIds;

	public CampaignDefinition(RunPreset preset, String title, String description, String difficulty,
		List<String> roomIds, List<String> bossIds)
	{
		this.preset = preset == null ? RunPreset.UNSPECIFIED : preset;
		this.title = title == null ? "" : title;
		this.description = description == null ? "" : description;
		this.difficulty = difficulty == null ? "" : difficulty;
		this.roomIds = copy(roomIds);
		this.bossIds = copy(bossIds);
	}

	public RunPreset preset() { return preset; }
	public String title() { return title; }
	public String description() { return description; }
	public String difficulty() { return difficulty; }
	public List<String> roomIds() { return roomIds; }
	public List<String> bossIds() { return bossIds; }

	private static List<String> copy(List<String> in)
	{
		List<String> out = new ArrayList<>();
		if (in != null)
		{
			for (String s : in)
			{
				if (s != null && !s.isEmpty()) out.add(s);
			}
		}
		return Collections.unmodifiableList(out);
	}
}
