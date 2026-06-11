package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mutable state shared by the RogueScape custom-room world-map overlay,
 * toolbar panel, and plugin menu handlers.
 */
public class RogueScapeCustomRoomEditorState
{
	private final RogueScapeCustomRoomSelection selection;
	private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
	private volatile boolean editing;
	private volatile int hoveredRegionId = -1;
	private volatile int lastToggledRegionId = -1;
	private volatile String lastToggleSummary = "";

	public RogueScapeCustomRoomEditorState(RogueScapeCustomRoomSelection selection)
	{
		this.selection = selection;
	}

	public RogueScapeCustomRoomSelection selection()
	{
		return selection;
	}

	public boolean isEditing()
	{
		return editing;
	}

	public void setEditing(boolean editing)
	{
		if (this.editing != editing)
		{
			this.editing = editing;
			fireChange();
		}
	}

	public int getHoveredRegionId()
	{
		return hoveredRegionId;
	}

	public void setHoveredRegionId(int hoveredRegionId)
	{
		if (this.hoveredRegionId != hoveredRegionId)
		{
			this.hoveredRegionId = hoveredRegionId;
			fireChange();
		}
	}

	public int getLastToggledRegionId()
	{
		return lastToggledRegionId;
	}

	public String getLastToggleSummary()
	{
		return lastToggleSummary;
	}

	public int toggleHovered()
	{
		int id = hoveredRegionId;
		if (id < 0)
		{
			return -1;
		}
		boolean nowSelected = selection.toggleRegion(id);
		lastToggledRegionId = id;
		lastToggleSummary = "Region " + id + (nowSelected ? " added" : " removed");
		fireChange();
		return id;
	}

	public void onChange(Runnable listener)
	{
		if (listener != null)
		{
			changeListeners.add(listener);
		}
	}

	public void markChanged(String summary)
	{
		if (summary != null)
		{
			lastToggleSummary = summary;
		}
		fireChange();
	}

	public void fireChange()
	{
		for (Runnable listener : changeListeners)
		{
			try
			{
				listener.run();
			}
			catch (RuntimeException ignored)
			{
				// UI refresh is best-effort.
			}
		}
	}
}
