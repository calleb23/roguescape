package com.pluginideahub.roguescape.core.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A pure-core description of one journal "two-page spread": a title/subtitle masthead plus a
 * left-page and a right-page list of blocks. The layout rule from the design doc —
 * <em>left page = "what I have / my choices", right page = "the world / the route / context"</em>
 * — is expressed here as data with no RuneLite or AWT dependency, so the per-phase split is
 * unit-testable. The window overlay layer maps these blocks onto its painters (see
 * {@code ui.JournalSpreadBlocks}).
 */
public final class JournalSpread
{
	/** Semantic colour role; the renderer maps each to a concrete theme colour. */
	public enum Tone { INK, POSITIVE, NEGATIVE, MUTED, GOLD }

	/** One selectable option (a contract card, a Begin stamp); actionId routes the click. */
	public static final class Choice
	{
		private final String title;
		private final String subtitle;
		private final String detail;
		private final Tone tone;
		private final boolean selected;
		private final String actionId;

		public Choice(String title, String subtitle, String detail, Tone tone, boolean selected, String actionId)
		{
			this.title = title == null ? "" : title;
			this.subtitle = subtitle == null ? "" : subtitle;
			this.detail = detail == null ? "" : detail;
			this.tone = tone == null ? Tone.INK : tone;
			this.selected = selected;
			this.actionId = actionId == null ? "" : actionId;
		}

		public String title() { return title; }
		public String subtitle() { return subtitle; }
		public String detail() { return detail; }
		public Tone tone() { return tone; }
		public boolean isSelected() { return selected; }
		public String actionId() { return actionId; }
	}

	/** One page element, kind-tagged, built via the static factories. */
	public static final class Block
	{
		public enum Kind { HEADING, TEXT, NOTE, GAP, CHAPTERS, HOURGLASS, CHOICES }

		private final Kind kind;
		private final String text;
		private final String value;
		private final Tone tone;
		private final List<SidePanelViewModel.Chapter> chapters;
		private final List<Choice> choices;

		private Block(Kind kind, String text, String value, Tone tone, List<SidePanelViewModel.Chapter> chapters)
		{
			this(kind, text, value, tone, chapters, null);
		}

		private Block(Kind kind, String text, String value, Tone tone, List<SidePanelViewModel.Chapter> chapters,
			List<Choice> choices)
		{
			this.kind = kind;
			this.text = text == null ? "" : text;
			this.value = value == null ? "" : value;
			this.tone = tone == null ? Tone.INK : tone;
			this.chapters = chapters == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(chapters));
			this.choices = choices == null ? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(choices));
		}

		public static Block heading(String text)
		{
			return new Block(Kind.HEADING, text, "", Tone.INK, null);
		}

		public static Block text(String text, Tone tone)
		{
			return new Block(Kind.TEXT, text, "", tone, null);
		}

		/** Margin-note text (rule lists, asides). */
		public static Block note(String text, Tone tone)
		{
			return new Block(Kind.NOTE, text, "", tone, null);
		}

		public static Block gap()
		{
			return new Block(Kind.GAP, "", "", Tone.INK, null);
		}

		public static Block chapters(List<SidePanelViewModel.Chapter> chapters)
		{
			return new Block(Kind.CHAPTERS, "", "", Tone.INK, chapters);
		}

		public static Block hourglass(String label, String time)
		{
			return new Block(Kind.HOURGLASS, label, time, Tone.INK, null);
		}

		/** Selectable options (contract cards / the Begin stamp) rendered as clickable tiles. */
		public static Block choices(List<Choice> choices)
		{
			return new Block(Kind.CHOICES, "", "", Tone.INK, null, choices);
		}

		public Kind kind() { return kind; }
		public String text() { return text; }
		public String value() { return value; }
		public Tone tone() { return tone; }
		public List<SidePanelViewModel.Chapter> chapters() { return chapters; }
		public List<Choice> choices() { return choices; }
	}

	private final String title;
	private final String subtitle;
	private final List<Block> left;
	private final List<Block> right;

	public JournalSpread(String title, String subtitle, List<Block> left, List<Block> right)
	{
		this.title = title == null ? "" : title;
		this.subtitle = subtitle == null ? "" : subtitle;
		this.left = left == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(left));
		this.right = right == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(right));
	}

	public String title() { return title; }
	public String subtitle() { return subtitle; }
	public List<Block> left() { return left; }
	public List<Block> right() { return right; }
}
