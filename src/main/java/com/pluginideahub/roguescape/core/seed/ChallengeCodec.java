package com.pluginideahub.roguescape.core.seed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 9 — line-based codec for {@link ChallengeDefinition}.
 *
 * The format is intentionally minimal so plugin tests can verify it without bringing in a
 * JSON library. Each line is {@code key: value} or {@code key: a, b, c}. Round-trips are
 * canonical: encode(decode(s)) is stable for any valid encoded form.
 */
public final class ChallengeCodec
{
	private ChallengeCodec() {}

	public static String encode(ChallengeDefinition def)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("challengeId: ").append(def.challengeId()).append('\n');
		sb.append("goal: ").append(def.goal()).append('\n');
		sb.append("seed: ").append(def.seed()).append('\n');
		sb.append("mode: ").append(def.mode()).append('\n');
		sb.append("routeLength: ").append(def.routeLength()).append('\n');
		sb.append("bossCount: ").append(def.bossCount()).append('\n');
		sb.append("rooms: ").append(String.join(", ", def.roomNamePool())).append('\n');
		sb.append("bosses: ").append(String.join(", ", def.bossNamePool())).append('\n');
		sb.append("relics: ").append(String.join(", ", def.relicIdPool())).append('\n');
		sb.append("starterKit: ").append(String.join(", ", def.starterKit())).append('\n');
		return sb.toString();
	}

	public static ChallengeDefinition decode(String encoded)
	{
		if (encoded == null) throw new IllegalArgumentException("encoded required");
		ChallengeDefinition.Builder b = ChallengeDefinition.builder();
		for (String raw : encoded.split("\n"))
		{
			String line = raw.trim();
			if (line.isEmpty()) continue;
			int colon = line.indexOf(':');
			if (colon < 0) continue;
			String key = line.substring(0, colon).trim();
			String value = line.substring(colon + 1).trim();
			switch (key)
			{
				case "challengeId": b.challengeId(value); break;
				case "goal": b.goal(value); break;
				case "seed": b.seed(value); break;
				case "mode": b.mode(value); break;
				case "routeLength": b.routeLength(parseIntOr(value, 3)); break;
				case "bossCount": b.bossCount(parseIntOr(value, 1)); break;
				case "rooms": b.roomNamePool(splitList(value)); break;
				case "bosses": b.bossNamePool(splitList(value)); break;
				case "relics": b.relicIdPool(splitList(value)); break;
				case "starterKit": b.starterKit(splitList(value)); break;
				default: break;
			}
		}
		return b.build();
	}

	private static List<String> splitList(String value)
	{
		if (value == null || value.isEmpty()) return new ArrayList<>();
		String[] parts = value.split(",");
		List<String> out = new ArrayList<>(parts.length);
		for (String p : parts)
		{
			String trimmed = p.trim();
			if (!trimmed.isEmpty()) out.add(trimmed);
		}
		return out;
	}

	private static int parseIntOr(String s, int fallback)
	{
		try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
	}

	// Helper for callers who already have arrays.
	public static List<String> list(String... values)
	{
		return new ArrayList<>(Arrays.asList(values));
	}
}
