package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Centralized colors and fonts for the RogueScape side panel.
 *
 * <p>Palette approximates the design asset sheet's purple/gold "dungeon" aesthetic in code
 * (no image assets yet). All values live here so they retune in one place — fine-tune the hex
 * after seeing it live. Ornate frames and icons from the sheet are deferred to PNG assets.
 */
public final class RogueScapeTheme
{
	private RogueScapeTheme()
	{
	}

	// ---- Surfaces (charcoal dungeon stone) ----
	public static final Color PANEL_BG = new Color(0x101113);
	public static final Color SECTION_BG = new Color(0x181A1D);
	public static final Color SECTION_HEADER_BG = new Color(0x22252A);
	public static final Color BORDER = new Color(0x565A63);          // dark steel frame
	public static final Color BORDER_BRIGHT = new Color(0xA78A4A);   // aged gold edge

	// ---- Brand ----
	public static final Color GOLD = new Color(0xD0AA55);
	public static final Color GOLD_DIM = new Color(0x8F7848);
	public static final Color ACCENT = new Color(0xA972E0);          // purple
	public static final Color ACCENT_DIM = new Color(0x6E4E9E);

	// ---- Text ----
	public static final Color TEXT_PRIMARY = new Color(0xE3E0D8);    // soft white
	public static final Color TEXT_MUTED = new Color(0x9B9A94);      // stone grey
	public static final Color TEXT_GOLD = GOLD;

	// ---- Status colors ----
	public static final Color POSITIVE = new Color(0x5BBE6A);
	public static final Color NEGATIVE = new Color(0xC0504D);
	public static final Color INFO = new Color(0x4FA9C9);

	// ---- Rarity tiers (reward cards / artifacts) ----
	public static final Color RARITY_COMMON = new Color(0x9AA0A6);
	public static final Color RARITY_RARE = new Color(0x4A90D9);
	public static final Color RARITY_EPIC = new Color(0x9B59B6);
	public static final Color RARITY_LEGENDARY = new Color(0xD9A441);

	// ---- Bars ----
	public static final Color BAR_TRACK = new Color(0x25272B);
	public static final Color BAR_HP = new Color(0xB23A2E);
	public static final Color BAR_PRAYER = new Color(0x3FA9C9);
	public static final Color BAR_SYNERGY = new Color(0x9B59B6);
	public static final Color BAR_PROGRESS = GOLD;

	// ---- Button role colors (base / hover) ----
	public static final Color BTN_GREEN = new Color(0x2C4A30);
	public static final Color BTN_GREEN_HOVER = new Color(0x39603E);
	public static final Color BTN_RED = new Color(0x5A2626);
	public static final Color BTN_RED_HOVER = new Color(0x723030);
	public static final Color BTN_GOLD = new Color(0x2F2A20);
	public static final Color BTN_GOLD_HOVER = new Color(0x453A25);
	public static final Color BTN_NEUTRAL = new Color(0x23262A);
	public static final Color BTN_NEUTRAL_HOVER = new Color(0x30343A);
	public static final Color BTN_DISABLED = new Color(0x1C1E21);

	/** Semantic button roles matching the asset sheet's button states. */
	public enum ButtonRole
	{
		/** Start Run — green. */
		GO,
		/** Confirm / Continue / Reroll / primary actions — gold. */
		PRIMARY,
		/** End Run / Fail — red. */
		DANGER,
		/** Secondary actions — neutral dark. */
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

	public static Color buttonText(ButtonRole role)
	{
		switch (role)
		{
			case GO: return POSITIVE;
			case PRIMARY: return GOLD;
			case DANGER: return new Color(0xD98A8A);
			case NEUTRAL:
			default: return TEXT_PRIMARY;
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

	// ---- Fonts (sizing derived from the component's base font) ----
	// Bumped up a step for readability on the sidebar.
	public static Font header(Font base)
	{
		return base.deriveFont(Font.BOLD, 16f);
	}

	public static Font sectionTitle(Font base)
	{
		return base.deriveFont(Font.BOLD, 13f);
	}

	public static Font label(Font base)
	{
		return base.deriveFont(Font.PLAIN, 13f);
	}

	public static Font value(Font base)
	{
		return base.deriveFont(Font.BOLD, 13f);
	}

	public static Font button(Font base)
	{
		return base.deriveFont(Font.BOLD, 13f);
	}

	public static Font small(Font base)
	{
		return base.deriveFont(Font.PLAIN, 12f);
	}
}
