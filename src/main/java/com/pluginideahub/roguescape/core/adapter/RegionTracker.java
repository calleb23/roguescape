package com.pluginideahub.roguescape.core.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 6 — region-change observer. Translates raw RuneLite region IDs / WorldPoint regions
 * into the plugin's named region IDs (e.g. {@code "lumbridge"}, {@code "karamja"}).
 *
 * Pure-Java: the adapter constructs the tracker with a map provided at startup; tests cover
 * the conversion logic without RuneLite.
 */
public final class RegionTracker
{
	private final Map<Integer, String> regionIdsByCode;
	private String currentRegionId = "";

	public RegionTracker()
	{
		this(new LinkedHashMap<>());
	}

	public RegionTracker(Map<Integer, String> regionIdsByCode)
	{
		this.regionIdsByCode = new LinkedHashMap<>();
		if (regionIdsByCode != null) this.regionIdsByCode.putAll(regionIdsByCode);
	}

	public RegionTracker map(int code, String regionId)
	{
		if (regionId != null) regionIdsByCode.put(code, regionId);
		return this;
	}

	public String currentRegionId() { return currentRegionId; }

	/** Returns true if the region changed as a result of this update. */
	public boolean observe(int regionCode)
	{
		String mapped = regionIdsByCode.get(regionCode);
		if (mapped == null) mapped = ""; // unknown region — empty string
		if (mapped.equals(currentRegionId)) return false;
		currentRegionId = mapped;
		return true;
	}

	public Map<Integer, String> mappings() { return regionIdsByCode; }
}
