package com.pluginideahub.roguescape.core.adapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 6 — a single observation emitted by the passive RuneLite adapter (or by a test).
 * Carries enough context for the adapter helpers to convert to legality-classifier events
 * without exposing RuneLite types.
 */
public final class ObservedEvent
{
	private final ObservedEventKind kind;
	private final String regionId;
	private final long tick;
	private final Map<String, String> attributes;

	public ObservedEvent(ObservedEventKind kind, String regionId, long tick, Map<String, String> attributes)
	{
		if (kind == null) throw new IllegalArgumentException("kind required");
		this.kind = kind;
		this.regionId = regionId == null ? "" : regionId;
		this.tick = tick;
		Map<String, String> copy = new LinkedHashMap<>();
		if (attributes != null) copy.putAll(attributes);
		this.attributes = Collections.unmodifiableMap(copy);
	}

	public ObservedEventKind kind() { return kind; }
	public String regionId() { return regionId; }
	public long tick() { return tick; }
	public Map<String, String> attributes() { return attributes; }

	public String attr(String key) { return attributes.get(key); }
	public String attr(String key, String fallback)
	{
		String v = attributes.get(key);
		return v == null ? fallback : v;
	}
}
