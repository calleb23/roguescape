package com.pluginideahub.roguescape.core.region;

import java.util.Objects;

/**
 * Stage 3 — named legal region. Region IDs are strings rather than RuneLite world point ints
 * so the pure-Java core can name regions freely (e.g. "lumbridge", "karamja") and so a
 * RuneLite adapter can later map world points to these strings.
 */
public final class LegalRegion
{
	private final String id;
	private final String label;

	public LegalRegion(String id, String label)
	{
		if (id == null || id.isEmpty()) throw new IllegalArgumentException("region id required");
		this.id = id;
		this.label = label == null ? id : label;
	}

	public String id() { return id; }
	public String label() { return label; }

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof LegalRegion)) return false;
		LegalRegion that = (LegalRegion) o;
		return Objects.equals(id, that.id) && Objects.equals(label, that.label);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, label);
	}

	@Override
	public String toString()
	{
		return "LegalRegion{" + id + "}";
	}
}
