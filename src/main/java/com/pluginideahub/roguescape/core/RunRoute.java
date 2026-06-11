package com.pluginideahub.roguescape.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RunRoute
{
	private final List<RunStage> stages = new ArrayList<>();

	public void addStage(RunStage stage)
	{
		stages.add(stage);
	}

	public int size()
	{
		return stages.size();
	}

	public boolean isEmpty()
	{
		return stages.isEmpty();
	}

	public List<RunStage> stages()
	{
		return Collections.unmodifiableList(stages);
	}

	public RunStage stageById(String id)
	{
		for (RunStage s : stages)
		{
			if (s.id().equals(id)) return s;
		}
		return null;
	}

	public boolean contains(String id)
	{
		return stageById(id) != null;
	}

	public int clearedCount()
	{
		int count = 0;
		for (RunStage s : stages) if (s.isCleared()) count++;
		return count;
	}

	public boolean isComplete()
	{
		if (stages.isEmpty()) return false;
		for (RunStage s : stages) if (!s.isCleared()) return false;
		return true;
	}

	public RunStage finalStage()
	{
		return stages.isEmpty() ? null : stages.get(stages.size() - 1);
	}
}
