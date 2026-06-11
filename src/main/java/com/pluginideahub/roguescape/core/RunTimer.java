package com.pluginideahub.roguescape.core;

/** Pure timer helpers for deterministic run-loop tests and display. */
public final class RunTimer
{
	private RunTimer() {}

	public static long elapsed(long startMillis, long nowMillis)
	{
		return Math.max(0L, nowMillis - startMillis);
	}

	public static String format(long elapsedMillis)
	{
		long totalSeconds = Math.max(0L, elapsedMillis) / 1000L;
		long seconds = totalSeconds % 60L;
		long minutes = (totalSeconds / 60L) % 60L;
		long hours = totalSeconds / 3600L;
		if (hours > 0L)
		{
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%02d:%02d", minutes, seconds);
	}
}
