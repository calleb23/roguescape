package com.pluginideahub.roguescape.core.relic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Stage 5 — a relic/modifier offered during a run. A relic groups one or more
 * {@link RelicEffect}s under a player-facing name and description. Relics are immutable;
 * activated state and one-shot consumption are tracked by {@link RelicEngine}.
 */
public final class Relic
{
	private final String relicId;
	private final String name;
	private final String description;
	private final List<RelicEffect> effects;

	public Relic(String relicId, String name, String description, List<RelicEffect> effects)
	{
		if (relicId == null || relicId.isEmpty()) throw new IllegalArgumentException("relicId required");
		if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
		this.relicId = relicId;
		this.name = name;
		this.description = description == null ? "" : description;
		this.effects = Collections.unmodifiableList(new ArrayList<>(effects == null ? Collections.emptyList() : effects));
	}

	public Relic(String relicId, String name, String description, RelicEffect... effects)
	{
		this(relicId, name, description, effects == null ? Collections.emptyList() : Arrays.asList(effects));
	}

	public String relicId() { return relicId; }
	public String name() { return name; }
	public String description() { return description; }
	public List<RelicEffect> effects() { return effects; }
}
