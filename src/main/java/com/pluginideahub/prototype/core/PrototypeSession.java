package com.pluginideahub.prototype.core;

import java.util.ArrayList;
import java.util.List;

public class PrototypeSession
{
	private final String prototypeName;
	private final String goal;
	private final List<PrototypeRuleCard> activeCards = new ArrayList<>();
	private final List<String> notes = new ArrayList<>();

	private PrototypeSession(String prototypeName, String goal)
	{
		this.prototypeName = prototypeName;
		this.goal = goal;
	}

	public static PrototypeSession start(String prototypeName, String goal)
	{
		return new PrototypeSession(prototypeName, goal);
	}

	public void activate(PrototypeRuleCard card)
	{
		activeCards.add(card);
	}

	public int activeCardCount()
	{
		return activeCards.size();
	}

	public void recordNote(String note)
	{
		notes.add(note);
	}

	public int noteCount()
	{
		return notes.size();
	}

	public String recapMarkdown()
	{
		StringBuilder recap = new StringBuilder();
		recap.append("# ").append(prototypeName).append(" Recap\n\n");
		recap.append("Goal: ").append(goal).append("\n");
		recap.append("Active cards: ").append(activeCards.size()).append("\n");
		for (PrototypeRuleCard card : activeCards)
		{
			recap.append("- ").append(card.getTitle()).append(": ").append(card.getHook()).append("\n");
			for (String rule : card.getRules())
			{
				recap.append("  - ").append(rule).append("\n");
			}
		}
		recap.append("Notes: ").append(notes.size()).append("\n");
		for (String note : notes)
		{
			recap.append("- ").append(note).append("\n");
		}
		return recap.toString();
	}
}
