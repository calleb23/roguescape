package com.pluginideahub.prototype.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PrototypeDeck
{
	private final Map<String, PrototypeRuleCard> cards = new LinkedHashMap<>();

	public PrototypeDeck add(PrototypeRuleCard card)
	{
		cards.put(card.getId(), card);
		return this;
	}

	public PrototypeRuleCard cardById(String id)
	{
		return cards.get(id);
	}

	public int size()
	{
		return cards.size();
	}

	public Map<String, PrototypeRuleCard> cards()
	{
		return Collections.unmodifiableMap(cards);
	}
}
