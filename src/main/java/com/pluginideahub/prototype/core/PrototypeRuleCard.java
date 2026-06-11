package com.pluginideahub.prototype.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrototypeRuleCard
{
	private final String id;
	private final String title;
	private final String hook;
	private final List<String> rules;

	public PrototypeRuleCard(String id, String title, String hook, List<String> rules)
	{
		this.id = id;
		this.title = title;
		this.hook = hook;
		this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
	}

	public String getId() { return id; }
	public String getTitle() { return title; }
	public String getHook() { return hook; }
	public List<String> getRules() { return rules; }
}
