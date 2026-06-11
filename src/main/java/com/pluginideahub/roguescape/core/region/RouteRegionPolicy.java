package com.pluginideahub.roguescape.core.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stage 3 — keeps a per-stage region rule mapping for a route. The route itself is owned
 * by {@code RunRoute}; this stays a sidecar so we don't touch Stage-1 classes that have
 * battle-tested tests.
 */
public final class RouteRegionPolicy
{
	private final Map<String, StageRegionRule> stageRules = new LinkedHashMap<>();

	public void setRule(String stageId, StageRegionRule rule)
	{
		if (stageId == null) return;
		if (rule == null) { stageRules.remove(stageId); return; }
		stageRules.put(stageId, rule);
	}

	public StageRegionRule ruleFor(String stageId)
	{
		StageRegionRule rule = stageRules.get(stageId);
		return rule != null ? rule : StageRegionRule.UNRESTRICTED;
	}

	public boolean hasRule(String stageId)
	{
		return stageId != null && stageRules.containsKey(stageId);
	}

	public Map<String, StageRegionRule> asMap()
	{
		return Collections.unmodifiableMap(stageRules);
	}
}
