package com.pluginideahub.roguescape.core.recap;

/**
 * Stage 8 — markdown + JSON renderers for {@link RunRecap}.
 *
 * The JSON renderer is hand-rolled (no Jackson/Gson runtime dependency at this stage) and
 * always escapes the small set of JSON-significant characters. Output remains stable across
 * runs with the same data so equality-style tests work.
 */
public final class RecapExport
{
	private RecapExport() {}

	public static String toMarkdown(RunRecap recap)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("# RogueScape Recap\n\n");
		sb.append("- Goal: ").append(recap.goal()).append('\n');
		sb.append("- Seed: ").append(recap.seed()).append('\n');
		sb.append("- State: ").append(recap.state()).append('\n');
		sb.append("- Result: ").append(recap.completionNote()).append('\n');
		sb.append("- Score: ").append(recap.score()).append('\n');
		sb.append("- Duration (ms): ").append(recap.durationMillis()).append('\n');
		sb.append("- Items legal/suspicious/illegal: ")
			.append(recap.legalCount()).append('/').append(recap.suspiciousCount()).append('/').append(recap.illegalCount())
			.append('\n');

		appendList(sb, "Stages", recap.stageRows());
		appendList(sb, "Items", recap.itemRows());
		appendList(sb, "Relics", recap.relicRows());
		appendList(sb, "Bank Unlocks", recap.unlockRows());
		return sb.toString();
	}

	private static void appendList(StringBuilder sb, String header, java.util.List<String> rows)
	{
		sb.append("\n## ").append(header).append('\n');
		if (rows.isEmpty()) sb.append("- (none)\n");
		else for (String row : rows) sb.append("- ").append(row).append('\n');
	}

	public static String toJson(RunRecap recap)
	{
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		field(sb, "goal", recap.goal(), true);
		field(sb, "seed", recap.seed(), false);
		field(sb, "state", recap.state().name(), false);
		field(sb, "completionNote", recap.completionNote(), false);
		fieldNum(sb, "score", recap.score());
		fieldNum(sb, "durationMillis", recap.durationMillis());
		fieldNum(sb, "legalCount", recap.legalCount());
		fieldNum(sb, "suspiciousCount", recap.suspiciousCount());
		fieldNum(sb, "illegalCount", recap.illegalCount());
		arrayField(sb, "stages", recap.stageRows());
		arrayField(sb, "items", recap.itemRows());
		arrayField(sb, "relics", recap.relicRows());
		arrayField(sb, "unlocks", recap.unlockRows());
		sb.append('}');
		return sb.toString();
	}

	private static void field(StringBuilder sb, String key, String value, boolean first)
	{
		if (!first) sb.append(',');
		sb.append('"').append(escape(key)).append('"').append(':').append('"').append(escape(value)).append('"');
	}

	private static void fieldNum(StringBuilder sb, String key, long value)
	{
		sb.append(',').append('"').append(escape(key)).append('"').append(':').append(value);
	}

	private static void arrayField(StringBuilder sb, String key, java.util.List<String> rows)
	{
		sb.append(',').append('"').append(escape(key)).append('"').append(":[");
		for (int i = 0; i < rows.size(); i++)
		{
			if (i > 0) sb.append(',');
			sb.append('"').append(escape(rows.get(i))).append('"');
		}
		sb.append(']');
	}

	static String escape(String s)
	{
		if (s == null) return "";
		StringBuilder sb = new StringBuilder(s.length() + 4);
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
				case '\\': sb.append("\\\\"); break;
				case '"': sb.append("\\\""); break;
				case '\b': sb.append("\\b"); break;
				case '\f': sb.append("\\f"); break;
				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				default:
					if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
					else sb.append(c);
			}
		}
		return sb.toString();
	}
}
