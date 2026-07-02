package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.ui.JournalSpread;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps a pure-core {@link JournalSpread} onto RogueScape window blocks: a page-title masthead
 * followed by a single two-column {@code columns(left, right)} block. This is the RuneLite-side
 * translation of the tested core layout — it resolves tones to {@link RogueScapeTheme} colours
 * and nothing else. The left/right split itself lives (and is tested) in core.
 */
public final class JournalSpreadBlocks
{
	private JournalSpreadBlocks()
	{
	}

	public static List<RogueScapeWindowOverlay.Block> render(JournalSpread spread)
	{
		List<RogueScapeWindowOverlay.Block> out = new ArrayList<>();
		if (spread == null)
		{
			return out;
		}
		out.add(RogueScapeWindowOverlay.Block.pageTitle(spread.title(), spread.subtitle()));
		out.add(RogueScapeWindowOverlay.Block.columns(toBlocks(spread.left()), toBlocks(spread.right())));
		return out;
	}

	private static List<RogueScapeWindowOverlay.Block> toBlocks(List<JournalSpread.Block> in)
	{
		List<RogueScapeWindowOverlay.Block> out = new ArrayList<>();
		for (JournalSpread.Block b : in)
		{
			switch (b.kind())
			{
				case HEADING:
					out.add(RogueScapeWindowOverlay.Block.heading(b.text()));
					break;
				case TEXT:
					out.add(RogueScapeWindowOverlay.Block.text(b.text(), color(b.tone())));
					break;
				case NOTE:
					out.add(RogueScapeWindowOverlay.Block.note(b.text(), color(b.tone())));
					break;
				case GAP:
					out.add(RogueScapeWindowOverlay.Block.gap());
					break;
				case CHAPTERS:
					out.add(RogueScapeWindowOverlay.Block.chapters(b.chapters()));
					break;
				case HOURGLASS:
					out.add(RogueScapeWindowOverlay.Block.hourglass(b.text(), b.value()));
					break;
				default:
					break;
			}
		}
		return out;
	}

	private static Color color(JournalSpread.Tone tone)
	{
		switch (tone)
		{
			case POSITIVE:
				return RogueScapeTheme.POSITIVE;
			case NEGATIVE:
				return RogueScapeTheme.NEGATIVE;
			case MUTED:
				return RogueScapeTheme.TEXT_MUTED;
			case GOLD:
				return RogueScapeTheme.GOLD;
			case INK:
			default:
				return RogueScapeTheme.INK;
		}
	}
}
