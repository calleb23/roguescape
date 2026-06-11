package com.pluginideahub.roguescape.core;

import java.util.Locale;

/** Text matcher for boss-completion chat without depending on RuneLite event types. */
public final class BossKillChatMatcher
{
	private BossKillChatMatcher() {}

	public static boolean matches(String bossName, String message)
	{
		if (bossName == null || bossName.trim().isEmpty() || message == null)
		{
			return false;
		}
		String clean = clean(message);
		String text = clean.toLowerCase(Locale.ROOT);
		String boss = bossName.toLowerCase(Locale.ROOT);
		String compactBoss = compact(boss);
		String compactText = compact(text);
		boolean namesBoss = text.contains(boss) || compactText.contains(compactBoss);
		boolean looksLikeKill = text.contains("kill count")
			|| text.contains("kc")
			|| text.contains("defeated")
			|| text.contains("you have killed")
			|| text.contains("you killed")
			|| text.contains("duration:");
		return namesBoss && looksLikeKill;
	}

	public static String clean(String message)
	{
		if (message == null)
		{
			return "";
		}
		return message.replaceAll("<[^>]*>", " ")
			.replace('\u00A0', ' ')
			.trim()
			.replaceAll("\\s+", " ");
	}

	private static String compact(String value)
	{
		return value == null ? "" : value.replaceAll("[^a-z0-9]", "");
	}
}
