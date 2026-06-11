package com.pluginideahub.prototype.functional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FunctionalPrototypeSession
{
	public enum State
	{
		ACTIVE,
		COMPLETE
	}

	private final String title;
	private final String goal;
	private final List<String> actions = new ArrayList<>();
	private final List<String> violations = new ArrayList<>();
	private int score;
	private State state = State.ACTIVE;
	private String lastAction;

	private FunctionalPrototypeSession(String title, String goal)
	{
		this.title = title;
		this.goal = goal;
		recordProgress("Functional session started", 0);
	}

	public static FunctionalPrototypeSession start(String title, String goal)
	{
		return new FunctionalPrototypeSession(title, goal);
	}

	public void recordProgress(String action, int points)
	{
		lastAction = action;
		actions.add(action);
		score += points;
	}

	public void recordViolation(String violation)
	{
		lastAction = violation;
		violations.add(violation);
	}

	public void completeObjective(String action)
	{
		state = State.COMPLETE;
		recordProgress(action, 0);
	}

	public String getTitle()
	{
		return title;
	}

	public String getGoal()
	{
		return goal;
	}

	public State getState()
	{
		return state;
	}

	public int getScore()
	{
		return score;
	}

	public int violationCount()
	{
		return violations.size();
	}

	public String getLastAction()
	{
		return lastAction;
	}

	public List<String> actions()
	{
		return Collections.unmodifiableList(actions);
	}

	public List<String> violations()
	{
		return Collections.unmodifiableList(violations);
	}

	public String recapMarkdown()
	{
		StringBuilder recap = new StringBuilder();
		recap.append("# ").append(title).append(" Functional Recap\n\n");
		recap.append("Goal: ").append(goal).append("\n");
		recap.append("State: ").append(state).append("\n");
		recap.append("Score: ").append(score).append("\n");
		recap.append("Violations: ").append(violations.size()).append("\n\n");
		recap.append("## Actions\n");
		for (String action : actions)
		{
			recap.append("- ").append(action).append("\n");
		}
		recap.append("\n## Violations\n");
		for (String violation : violations)
		{
			recap.append("- ").append(violation).append("\n");
		}
		return recap.toString();
	}
}
