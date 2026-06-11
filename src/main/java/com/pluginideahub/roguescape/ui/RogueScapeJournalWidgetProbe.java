package com.pluginideahub.roguescape.ui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Read-only inspector for the built-in side journal (group {@code InterfaceID.SIDE_JOURNAL}).
 *
 * <p>This does not create, hide, or reposition widgets — it only reports what is present so
 * we can plan injection against real runtime data. {@link #dumpLines()} produces a detailed
 * tree dump intended for the log; {@link #probe()} produces a one-line availability summary.
 *
 * <p>Note: the constants in {@code InterfaceID.SideJournal} are <em>packed component IDs</em>
 * (group {@code << 16 | child}), so they must be passed to the single-argument
 * {@link Client#getWidget(int)} — NOT to {@code getWidget(group, child)}.
 */
public final class RogueScapeJournalWidgetProbe
{
	private static final int MAX_CHILDREN_PER_NODE = 16;

	private final Client client;

	public RogueScapeJournalWidgetProbe(Client client)
	{
		this.client = client;
	}

	public ProbeResult probe()
	{
		if (client == null)
		{
			return ProbeResult.unavailable();
		}

		Widget tabs = client.getWidget(InterfaceID.SideJournal.TABS);
		Widget questList = client.getWidget(InterfaceID.SideJournal.QUEST_LIST);
		Widget taskList = client.getWidget(InterfaceID.SideJournal.TASK_LIST);
		Widget adventureList = client.getWidget(InterfaceID.SideJournal.ADVENTUREPATH_LIST);
		Widget tabContainer = client.getWidget(InterfaceID.SideJournal.TAB_CONTAINER);

		boolean anyAvailable = tabs != null || questList != null || taskList != null
			|| adventureList != null || tabContainer != null;
		if (!anyAvailable)
		{
			return ProbeResult.unavailable();
		}

		String summary = "Journal: "
			+ status("tabs", tabs)
			+ " " + status("quest", questList)
			+ " " + status("tasks", taskList)
			+ " " + status("adv", adventureList)
			+ " " + status("container", tabContainer);
		return new ProbeResult(true, summary);
	}

	/**
	 * Detailed, human-readable dump of the side journal's key widgets and their immediate
	 * children. Safe to call any time; returns a short note if the journal isn't loaded.
	 */
	public List<String> dumpLines()
	{
		List<String> out = new ArrayList<>();
		if (client == null)
		{
			out.add("Journal dump: client unavailable");
			return out;
		}

		int[] roots = {
			InterfaceID.SideJournal.UNIVERSE,
			InterfaceID.SideJournal.TABS,
			InterfaceID.SideJournal.SUMMARY_LIST,
			InterfaceID.SideJournal.QUEST_LIST,
			InterfaceID.SideJournal.TASK_LIST,
			InterfaceID.SideJournal.ADVENTUREPATH_LIST,
			InterfaceID.SideJournal.LEAGUE_LIST,
			InterfaceID.SideJournal.TAB_CONTAINER
		};
		String[] names = {
			"UNIVERSE", "TABS", "SUMMARY_LIST", "QUEST_LIST",
			"TASK_LIST", "ADVENTUREPATH_LIST", "LEAGUE_LIST", "TAB_CONTAINER"
		};

		out.add("=== RogueScape side journal dump (group "
			+ (InterfaceID.SideJournal.UNIVERSE >>> 16) + ") ===");
		for (int i = 0; i < roots.length; i++)
		{
			Widget w = client.getWidget(roots[i]);
			out.add(names[i] + " (" + describeId(roots[i]) + ") -> " + describe(w));
			appendChildren(out, w);
		}
		return out;
	}

	private void appendChildren(List<String> out, Widget parent)
	{
		if (parent == null)
		{
			return;
		}
		Widget[] kids = parent.getChildren();
		if (kids == null || kids.length == 0)
		{
			return;
		}
		int limit = Math.min(kids.length, MAX_CHILDREN_PER_NODE);
		for (int i = 0; i < limit; i++)
		{
			out.add("    [" + i + "] " + describe(kids[i]));
		}
		if (kids.length > limit)
		{
			out.add("    ... +" + (kids.length - limit) + " more children");
		}
	}

	private static String describe(Widget w)
	{
		if (w == null)
		{
			return "null";
		}
		Rectangle b = w.getBounds();
		String bounds = b == null ? "?" : (b.x + "," + b.y + " " + b.width + "x" + b.height);
		Widget[] kids = w.getChildren();
		int kc = kids == null ? 0 : kids.length;
		String text = w.getText();
		String textPart = (text == null || text.isEmpty()) ? "" : " text='" + trim(text) + "'";
		int spriteId = w.getSpriteId();
		String spritePart = spriteId >= 0 ? " sprite=" + spriteId : "";
		int col = w.getTextColor();
		String colPart = col != 0 ? " col=" + Integer.toHexString(col) : "";
		return describeId(w.getId())
			+ " type=" + w.getType()
			+ (w.isHidden() ? " HIDDEN" : "")
			+ " [" + bounds + "]"
			+ spritePart
			+ colPart
			+ " kids=" + kc
			+ textPart;
	}

	/**
	 * Detailed recursive dump of the real Collection Log frame (group 621) — captures each
	 * widget's type, sprite id, bounds and text so the window can be cloned from real data.
	 * Returns a short note if the Collection Log isn't open.
	 */
	public List<String> dumpCollectionLog()
	{
		List<String> out = new ArrayList<>();
		if (client == null)
		{
			out.add("CLog dump: client unavailable");
			return out;
		}
		int group = InterfaceID.Collection.FRAME >>> 16;
		out.add("=== Collection Log dump (group " + group + ") ===");
		int found = 0;
		for (int child = 0; child < 256; child++)
		{
			Widget w = client.getWidget(group, child);
			if (w == null)
			{
				continue;
			}
			found++;
			out.add("-- component " + group + ":" + child + " --");
			walk(out, w, 0);
		}
		if (found == 0)
		{
			out.add("No components present — open the Collection Log first, then dump.");
		}
		return out;
	}

	private void walk(List<String> out, Widget w, int depth)
	{
		if (w == null || depth > 2)
		{
			return;
		}
		out.add(indent(depth) + describe(w));
		walkArray(out, w.getStaticChildren(), depth + 1);
		walkArray(out, w.getNestedChildren(), depth + 1);
		walkArray(out, w.getDynamicChildren(), depth + 1);
	}

	private void walkArray(List<String> out, Widget[] kids, int depth)
	{
		if (kids == null)
		{
			return;
		}
		int limit = Math.min(kids.length, 40);
		for (int i = 0; i < limit; i++)
		{
			walk(out, kids[i], depth);
		}
		if (kids.length > limit)
		{
			out.add(indent(depth) + "... +" + (kids.length - limit) + " more");
		}
	}

	private static String indent(int depth)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < depth; i++)
		{
			b.append("  ");
		}
		return b.toString();
	}

	/** Decodes a packed component ID to a readable {@code group:child} form. */
	static String describeId(int packedId)
	{
		return (packedId >>> 16) + ":" + (packedId & 0xFFFF);
	}

	private static String trim(String s)
	{
		String oneLine = s.replace('\n', ' ');
		return oneLine.length() <= 40 ? oneLine : oneLine.substring(0, 40) + "…";
	}

	private static String status(String label, Widget widget)
	{
		return label + "=" + (widget != null && !widget.isHidden() ? "ok" : "-");
	}

	public static final class ProbeResult
	{
		private final boolean available;
		private final String summaryLine;

		private ProbeResult(boolean available, String summaryLine)
		{
			this.available = available;
			this.summaryLine = summaryLine;
		}

		public static ProbeResult unavailable()
		{
			return new ProbeResult(false, "Journal: unavailable");
		}

		public boolean available()
		{
			return available;
		}

		public String summaryLine()
		{
			return summaryLine;
		}
	}
}
