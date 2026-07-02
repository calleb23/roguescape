package com.pluginideahub.roguescape.core.region;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stage 5 — content-bank definition of a named room. A {@code RoomDefinition} is a
 * declarative description: it is content, not state. The route builder turns a definition
 * into a {@link com.pluginideahub.roguescape.core.RunStage} plus {@link StageRegionRule}
 * when an actual run instance is built.
 *
 * Region IDs are stored as strings matching the integer ID returned by
 * {@code WorldPoint.getRegionID()} so the pure-Java core stays free of RuneLite types.
 */
public final class RoomDefinition
{
	private final String id;
	private final String name;
	private final RoomKind kind;
	private final Set<String> regionIds;
	/** OSRS NPC id for boss stages (0 = none) — lets the widget UI render the 3D chathead. */
	private final int npcId;

	public RoomDefinition(String id, String name, RoomKind kind, Set<String> regionIds)
	{
		this(id, name, kind, regionIds, 0);
	}

	public RoomDefinition(String id, String name, RoomKind kind, Set<String> regionIds, int npcId)
	{
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("id required");
		if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
		if (kind == null) throw new IllegalArgumentException("kind required");
		this.id = id;
		this.name = name;
		this.kind = kind;
		Set<String> copy = new LinkedHashSet<>();
		if (regionIds != null)
		{
			for (String r : regionIds)
			{
				if (r != null && !r.isEmpty()) copy.add(r);
			}
		}
		this.regionIds = Collections.unmodifiableSet(copy);
		this.npcId = Math.max(0, npcId);
	}

	public String id() { return id; }
	public String name() { return name; }
	public RoomKind kind() { return kind; }
	public Set<String> regionIds() { return regionIds; }
	/** OSRS NPC id for the boss's chathead model, or 0 when none applies. */
	public int npcId() { return npcId; }
}
