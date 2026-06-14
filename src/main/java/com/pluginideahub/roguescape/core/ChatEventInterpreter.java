package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.adapter.ProvenanceSignalTracker;

/**
 * Interprets chat lines for run semantics: death detection (records a violation), boss-kill
 * matching against the current boss stage (records the defeat signal), and forwarding the rest to
 * the provenance tracker. Pure-Java and composed of already-tested matchers; the plugin just feeds
 * it the message and applies the {@link Result}.
 */
public final class ChatEventInterpreter
{
	private ChatEventInterpreter() {}

	/** What the host should do after a chat line: maybe update the provenance signal / repaint. */
	public static final class Result
	{
		private final String signal;
		private final boolean refresh;

		Result(String signal, boolean refresh)
		{
			this.signal = signal;
			this.refresh = refresh;
		}

		/** New provenance-signal text, or {@code null} to leave it unchanged. */
		public String signal()
		{
			return signal;
		}

		/** Whether the overlays/panel should refresh now. */
		public boolean refresh()
		{
			return refresh;
		}
	}

	public static Result interpret(String message, RogueScapeRun run, RogueScapeRunSession session,
		ProvenanceSignalTracker signals)
	{
		if (ProvenanceSignalTracker.isLikelyDeathMessage(message))
		{
			if (session != null)
			{
				session.recordViolation("Observed death", RogueScapeRunSession.RunEnding.DEATH);
			}
			return new Result("death observed", true);
		}
		String bossSignal = observeBossKill(message, run);
		if (signals != null)
		{
			signals.observeChat(message);
		}
		String latest = signals == null ? "" : signals.latestSignal();
		if (latest != null && !latest.isEmpty())
		{
			return new Result(latest, bossSignal != null);
		}
		return new Result(bossSignal, bossSignal != null);
	}

	private static String observeBossKill(String message, RogueScapeRun run)
	{
		if (run == null || message == null)
		{
			return null;
		}
		RunStage stage = run.currentEnteredStage();
		if (stage == null || stage.type() != RunStageType.BOSS)
		{
			return null;
		}
		String clean = BossKillChatMatcher.clean(message);
		if (!BossKillChatMatcher.matches(stage.name(), clean))
		{
			return null;
		}
		return run.recordBossDefeatSignal("chat: " + clean) ? "boss defeated: " + stage.name() : null;
	}
}
