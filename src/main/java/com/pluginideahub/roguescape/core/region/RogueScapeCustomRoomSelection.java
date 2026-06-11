package com.pluginideahub.roguescape.core.region;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure Java model for a RogueScape creator-defined room. Stores a display
 * name plus an ordered, unique set of RuneLite region IDs selected from the
 * world map. Kept RuneLite-free so it can be tested and reused by UI layers.
 */
public final class RogueScapeCustomRoomSelection
{
	public static final String DEFAULT_NAME = "Custom Room";

	private String name;
	private final LinkedHashSet<Integer> selected = new LinkedHashSet<>();
	private final Deque<Change> history = new ArrayDeque<>();

	public RogueScapeCustomRoomSelection()
	{
		this(DEFAULT_NAME);
	}

	public RogueScapeCustomRoomSelection(String name)
	{
		this.name = sanitiseName(name);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = sanitiseName(name);
	}

	public Set<Integer> selectedRegionIds()
	{
		return Collections.unmodifiableSet(selected);
	}

	public Set<String> selectedRegionIdStrings()
	{
		return selected.stream().map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public int size()
	{
		return selected.size();
	}

	public boolean isEmpty()
	{
		return selected.isEmpty();
	}

	public boolean contains(int regionId)
	{
		return selected.contains(regionId);
	}

	public boolean toggleRegion(int regionId)
	{
		if (!isValidRegionId(regionId))
		{
			return false;
		}
		if (selected.remove(regionId))
		{
			history.push(Change.removed(regionId));
			return false;
		}
		selected.add(regionId);
		history.push(Change.added(regionId));
		return true;
	}

	public boolean addRegion(int regionId)
	{
		if (!isValidRegionId(regionId))
		{
			return false;
		}
		if (selected.add(regionId))
		{
			history.push(Change.added(regionId));
			return true;
		}
		return false;
	}

	public boolean removeRegion(int regionId)
	{
		if (selected.remove(regionId))
		{
			history.push(Change.removed(regionId));
			return true;
		}
		return false;
	}

	public int addRegions(Collection<Integer> regionIds)
	{
		if (regionIds == null || regionIds.isEmpty())
		{
			return 0;
		}
		List<Integer> added = new ArrayList<>();
		for (Integer id : regionIds)
		{
			if (id == null || !isValidRegionId(id))
			{
				continue;
			}
			if (selected.add(id))
			{
				added.add(id);
			}
		}
		if (!added.isEmpty())
		{
			history.push(Change.bulkAdded(added));
		}
		return added.size();
	}

	public void clear()
	{
		if (selected.isEmpty())
		{
			return;
		}
		history.push(Change.bulkRemoved(new ArrayList<>(selected)));
		selected.clear();
	}

	public boolean undoLastToggle()
	{
		Change change = history.pollFirst();
		if (change == null)
		{
			return false;
		}
		switch (change.kind)
		{
			case ADD:
				selected.remove(change.regionIds.get(0));
				break;
			case REMOVE:
				selected.add(change.regionIds.get(0));
				break;
			case BULK_ADD:
				selected.removeAll(change.regionIds);
				break;
			case BULK_REMOVE:
				selected.addAll(change.regionIds);
				break;
			default:
				break;
		}
		return true;
	}

	public void replaceFromCsv(String csv)
	{
		List<Integer> parsed = parseRegionIdsCsv(csv);
		if (!selected.isEmpty())
		{
			history.push(Change.bulkRemoved(new ArrayList<>(selected)));
		}
		selected.clear();
		if (!parsed.isEmpty())
		{
			selected.addAll(parsed);
			history.push(Change.bulkAdded(parsed));
		}
	}

	public static RogueScapeCustomRoomSelection fromCsv(String name, String csv)
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection(name);
		List<Integer> parsed = parseRegionIdsCsv(csv);
		if (!parsed.isEmpty())
		{
			selection.selected.addAll(parsed);
			selection.history.push(Change.bulkAdded(parsed));
		}
		return selection;
	}

	public String toCsv()
	{
		if (selected.isEmpty())
		{
			return "";
		}
		return selected.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	static List<Integer> parseRegionIdsCsv(String csv)
	{
		List<Integer> parsed = new ArrayList<>();
		LinkedHashSet<Integer> unique = new LinkedHashSet<>();
		if (csv == null || csv.trim().isEmpty())
		{
			return parsed;
		}
		for (String part : csv.split(","))
		{
			String trimmed = part.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			try
			{
				int id = Integer.parseInt(trimmed);
				if (isValidRegionId(id))
				{
					unique.add(id);
				}
			}
			catch (NumberFormatException ignored)
			{
				// Malformed entries are ignored so creators can paste rough CSV safely.
			}
		}
		parsed.addAll(unique);
		return parsed;
	}

	static boolean isValidRegionId(int regionId)
	{
		return regionId >= 0 && regionId <= 0xFFFF;
	}

	private static String sanitiseName(String name)
	{
		if (name == null)
		{
			return DEFAULT_NAME;
		}
		String trimmed = name.trim();
		return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
	}

	private enum Kind
	{
		ADD,
		REMOVE,
		BULK_ADD,
		BULK_REMOVE
	}

	private static final class Change
	{
		final Kind kind;
		final List<Integer> regionIds;

		private Change(Kind kind, List<Integer> regionIds)
		{
			this.kind = kind;
			this.regionIds = regionIds;
		}

		static Change added(int regionId)
		{
			return new Change(Kind.ADD, Collections.singletonList(regionId));
		}

		static Change removed(int regionId)
		{
			return new Change(Kind.REMOVE, Collections.singletonList(regionId));
		}

		static Change bulkAdded(List<Integer> regionIds)
		{
			return new Change(Kind.BULK_ADD, new ArrayList<>(regionIds));
		}

		static Change bulkRemoved(List<Integer> regionIds)
		{
			return new Change(Kind.BULK_REMOVE, new ArrayList<>(regionIds));
		}
	}
}
