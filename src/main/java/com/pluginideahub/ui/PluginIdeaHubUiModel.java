package com.pluginideahub.ui;

import com.pluginideahub.prototype.core.PrototypeDeck;
import com.pluginideahub.prototype.core.PrototypeRuleCard;
import com.pluginideahub.prototype.core.PrototypeSession;
import com.pluginideahub.prototype.functional.FunctionalPrototypeSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PluginIdeaHubUiModel
{
	private final String title;
	private final String goal;
	private final PrototypeSession session;
	private final FunctionalPrototypeSession functionalSession;
	private PrototypeRuleCard activeCard;
	private String lastNote;

	private PluginIdeaHubUiModel(String title, String goal, PrototypeSession session, FunctionalPrototypeSession functionalSession)
	{
		this.title = title;
		this.goal = goal;
		this.session = session;
		this.functionalSession = functionalSession;
	}

	public static PluginIdeaHubUiModel start(String title, String goal, PrototypeDeck deck)
	{
		PluginIdeaHubUiModel model = new PluginIdeaHubUiModel(title, goal, PrototypeSession.start(title, goal), FunctionalPrototypeSession.start(title, goal));
		PrototypeRuleCard firstCard = firstCard(deck);
		if (firstCard != null)
		{
			model.activeCard = firstCard;
			model.session.activate(firstCard);
		}
		model.recordManualAction("Plugin UI active");
		return model;
	}

	public static PluginIdeaHubUiModel start(String title, String goal, String cardTitle, String hook, List<String> rules)
	{
		PrototypeDeck deck = new PrototypeDeck();
		deck.add(new PrototypeRuleCard("starter", cardTitle, hook, rules));
		return start(title, goal, deck);
	}

	public String getTitle()
	{
		return title;
	}

	public String getGoal()
	{
		return goal;
	}

	public int activeCardCount()
	{
		return session.activeCardCount();
	}

	public int noteCount()
	{
		return session.noteCount();
	}

	public void recordManualAction(String note)
	{
		lastNote = note;
		session.recordNote(note);
		functionalSession.recordProgress(note, 0);
	}

	public void recordProgress(String action, int points)
	{
		lastNote = action;
		session.recordNote(action);
		functionalSession.recordProgress(action, points);
	}

	public void recordViolation(String violation)
	{
		lastNote = violation;
		session.recordNote("Violation: " + violation);
		functionalSession.recordViolation(violation);
	}

	public void completeObjective(String action)
	{
		lastNote = action;
		session.recordNote(action);
		functionalSession.completeObjective(action);
	}

	public List<String> overlayLines()
	{
		List<String> lines = new ArrayList<>();
		lines.add("Goal: " + goal);
		if (activeCard != null)
		{
			lines.add("Active: " + activeCard.getTitle());
			lines.add(activeCard.getHook());
		}
		lines.add("State: " + functionalSession.getState());
		lines.add("Score: " + functionalSession.getScore());
		lines.add("Violations: " + functionalSession.violationCount());
		if (functionalSession.getLastAction() != null)
		{
			lines.add("Last action: " + functionalSession.getLastAction());
		}
		lines.add("Notes: " + noteCount());
		if (lastNote != null)
		{
			lines.add("Last note: " + lastNote);
		}
		return lines;
	}

	public String recapMarkdown()
	{
		return session.recapMarkdown();
	}

	public String functionalRecapMarkdown()
	{
		return functionalSession.recapMarkdown();
	}

	private static PrototypeRuleCard firstCard(PrototypeDeck deck)
	{
		for (Map.Entry<String, PrototypeRuleCard> entry : deck.cards().entrySet())
		{
			return entry.getValue();
		}
		return null;
	}
}
