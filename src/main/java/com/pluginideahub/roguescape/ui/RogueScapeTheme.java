package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Centralized colors and fonts for RogueScape's "adventurer's journal" look: aged paper
 * surfaces, ink text, wax-seal accents, rubber-stamp reds. All values live here so the
 * palette retunes in one place; {@link RogueScapePaper} holds the texture/stamp painters.
 */
public final class RogueScapeTheme
{
	private RogueScapeTheme()
	{
	}

	// ---- Paper surfaces ----
	public static final Color PAPER = new Color(0xC9B584);            // aged page
	public static final Color PAPER_DARK = new Color(0xB8A271);      // pocket / track shading
	public static final Color PAPER_CARD = new Color(0xD4C193);      // contracts, loot cards
	public static final Color EDGE = new Color(0x5C4326);            // burnt page edge / wood

	// Legacy surface names kept so existing painters keep compiling; all paper now.
	public static final Color PANEL_BG = PAPER;
	public static final Color SECTION_BG = PAPER;
	public static final Color SECTION_HEADER_BG = PAPER_DARK;
	public static final Color BORDER = EDGE;
	public static final Color BORDER_BRIGHT = new Color(0x8A744E);
	public static final Color SURFACE = PAPER_CARD;

	// ---- Ink ----
	public static final Color INK = new Color(0x332414);              // primary handwriting
	public static final Color INK_FADED = new Color(0x6E5A3E);        // margin notes
	public static final Color STAMP = new Color(0xB03224);            // rubber-stamp red
	public static final Color RIBBON = new Color(0x9E2B1F);           // bookmark ribbon

	// ---- Wax seals ----
	public static final Color WAX_RED = new Color(0xA42C1E);
	public static final Color WAX_GREEN = new Color(0x4F7A2B);
	public static final Color WAX_BLUE = new Color(0x2F5E8C);
	public static final Color WAX_GOLD = new Color(0xB98A2C);

	// ---- Brand / accents (ink-on-paper world) ----
	public static final Color GOLD = new Color(0x8A6210);             // gilded ink for values
	public static final Color GOLD_DIM = new Color(0x6E4A14);
	public static final Color ACCENT = INK;                           // titles are ink
	public static final Color BANNER = STAMP;                         // badge/banner fills
	public static final Color ACCENT_DIM = INK_FADED;

	// ---- Text ----
	public static final Color TEXT_PRIMARY = INK;
	public static final Color TEXT_MUTED = INK_FADED;
	public static final Color TEXT_GOLD = GOLD;

	// ---- Status colors (deep enough to read on paper) ----
	public static final Color POSITIVE = new Color(0x4F7A2B);
	public static final Color NEGATIVE = new Color(0x9E2B1F);
	public static final Color INFO = new Color(0x2F5E8C);

	// ---- Rarity tiers as wax colors ----
	public static final Color RARITY_COMMON = new Color(0x6E5A3E);
	public static final Color RARITY_RARE = WAX_BLUE;
	public static final Color RARITY_EPIC = new Color(0x6B3E8C);
	public static final Color RARITY_LEGENDARY = WAX_GOLD;

	// ---- Bars (inked gauges) ----
	public static final Color BAR_TRACK = PAPER_DARK;
	public static final Color BAR_HP = new Color(0x9E2B1F);
	public static final Color BAR_PRAYER = WAX_BLUE;
	public static final Color BAR_SYNERGY = new Color(0x6B3E8C);
	public static final Color BAR_PROGRESS = new Color(0xB03224);

	// ---- Button faces (paper buttons; stamps carry the color) ----
	public static final Color BTN_GREEN = new Color(0xC3BC8A);
	public static final Color BTN_GREEN_HOVER = new Color(0xCEC795);
	public static final Color BTN_RED = new Color(0xCDAE8C);
	public static final Color BTN_RED_HOVER = new Color(0xD8B997);
	public static final Color BTN_GOLD = new Color(0xCDBA86);
	public static final Color BTN_GOLD_HOVER = new Color(0xD8C591);
	public static final Color BTN_NEUTRAL = PAPER_CARD;
	public static final Color BTN_NEUTRAL_HOVER = new Color(0xDECC9E);
	public static final Color BTN_DISABLED = PAPER_DARK;

	/** Semantic button roles; in the journal they pick the stamp-ink color. */
	public enum ButtonRole
	{
		/** Start Run — green wax. */
		GO,
		/** Confirm / Continue / primary actions — gilded ink. */
		PRIMARY,
		/** End Run / Fail — stamp red. */
		DANGER,
		/** Secondary actions — plain ink. */
		NEUTRAL
	}

	public static Color buttonBg(ButtonRole role)
	{
		switch (role)
		{
			case GO: return BTN_GREEN;
			case PRIMARY: return BTN_GOLD;
			case DANGER: return BTN_RED;
			case NEUTRAL:
			default: return BTN_NEUTRAL;
		}
	}

	public static Color buttonHoverBg(ButtonRole role)
	{
		switch (role)
		{
			case GO: return BTN_GREEN_HOVER;
			case PRIMARY: return BTN_GOLD_HOVER;
			case DANGER: return BTN_RED_HOVER;
			case NEUTRAL:
			default: return BTN_NEUTRAL_HOVER;
		}
	}

	/** The stamp-ink color a button's border and label are drawn in. */
	public static Color buttonText(ButtonRole role)
	{
		switch (role)
		{
			case GO: return new Color(0x3E6122);
			case PRIMARY: return GOLD;
			case DANGER: return STAMP;
			case NEUTRAL:
			default: return INK;
		}
	}

	/** Lightens a color toward white by {@code amount} per channel (clamped). */
	public static Color lighten(Color c, int amount)
	{
		return new Color(
			Math.min(255, c.getRed() + amount),
			Math.min(255, c.getGreen() + amount),
			Math.min(255, c.getBlue() + amount));
	}

	// ---- Fonts: serif for the handwriting feel ----
	private static Font serif(int style, float size)
	{
		return new Font(Font.SERIF, style, Math.round(size));
	}

	public static Font header(Font base)
	{
		return serif(Font.BOLD, 18f);
	}

	public static Font sectionTitle(Font base)
	{
		return serif(Font.BOLD, 13f);
	}

	public static Font label(Font base)
	{
		return serif(Font.PLAIN, 13f);
	}

	public static Font value(Font base)
	{
		return serif(Font.BOLD, 13f);
	}

	public static Font button(Font base)
	{
		return serif(Font.BOLD, 13f);
	}

	public static Font small(Font base)
	{
		return serif(Font.ITALIC, 12f);
	}
}
