package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEffect;
import com.pluginideahub.roguescape.core.relic.RelicEffectKind;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;

/**
 * Stateless presentation mapping for rewards and relics: titles, subtitles, descriptions, display
 * rarity, and the placeholder item-sprite icons. Shared by the reward overlay and the in-game
 * window so the mapping lives in one place rather than inside the plugin god-class.
 */
public final class RewardPresenter
{
	// Relic emblem item ids (real OSRS items) used as placeholder relic/artifact icons.
	private static final int ITEM_DRAGONSTONE = 1631; // Uncut dragonstone
	private static final int ITEM_COINS = 995;        // Coins
	private static final int ITEM_SHARK = 385;        // Shark
	private static final int ITEM_GLORY = 1712;       // Amulet of glory(6)
	private static final int ITEM_PRAYER_POTION = 2434;
	private static final int ITEM_BRONZE_ARROW = 882;

	private RewardPresenter() {}

	public static String humanCategory(BankItemCategory c)
	{
		return c == null ? "" : c.name().toLowerCase().replace('_', ' ');
	}

	public static String rewardTitle(RewardDraft draft)
	{
		if (draft == null || draft.chestType() == null)
		{
			return "ROLL REWARD";
		}
		switch (draft.chestType())
		{
			case RELIC: return "CLAIM YOUR RELIC";
			case SUPPLY: return "ROLL SUPPLIES";
			case UNLOCK: return "CHOOSE AN UNLOCK";
			case BANK_UNLOCK: return "UNLOCK BANK ITEM";
			default: return "ROLL REWARD";
		}
	}

	public static String rewardSubtitle(RewardDraft draft)
	{
		if (draft == null || draft.chestType() == null)
		{
			return "Choose one reward for the next stage";
		}
		switch (draft.chestType())
		{
			case RELIC: return "Choose one power for the next stage";
			case SUPPLY: return "Choose one random supply bundle";
			case UNLOCK: return "Choose one system to unlock for the run";
			case BANK_UNLOCK: return "Choose one bank item to make legal";
			default: return "Choose one reward for the next stage";
		}
	}

	public static String supplyRewardDescription(RewardOption option)
	{
		if (option == null || option.chestType() == null)
		{
			return "Adds supplies to this run.";
		}
		switch (option.chestType())
		{
			case FOOD: return "Adds emergency food.";
			case POTION: return "Adds basic potion supplies.";
			case AMMO: return "Adds ammo and rune supplies.";
			case UTILITY: return "Adds a utility escape option.";
			default: return "Adds supplies to this run.";
		}
	}

	public static int rewardIconFor(RewardOption option)
	{
		if (option == null || option.chestType() == null)
		{
			return ITEM_DRAGONSTONE;
		}
		switch (option.chestType())
		{
			case FOOD: return ITEM_SHARK;
			case POTION: return ITEM_PRAYER_POTION;
			case AMMO: return ITEM_BRONZE_ARROW;
			case UTILITY: return ITEM_GLORY;
			case UNLOCK: return ITEM_DRAGONSTONE;
			case BANK_UNLOCK: return ITEM_COINS;
			default: return ITEM_DRAGONSTONE;
		}
	}

	/** Display rarity heuristic (placeholder until rewards carry a real value tier): more effects = rarer. */
	public static RogueScapeRewardOverlay.Rarity relicRarity(Relic relic)
	{
		switch (relic.effects().size())
		{
			case 0: return RogueScapeRewardOverlay.Rarity.COMMON;
			case 1: return RogueScapeRewardOverlay.Rarity.RARE;
			case 2: return RogueScapeRewardOverlay.Rarity.EPIC;
			default: return RogueScapeRewardOverlay.Rarity.LEGENDARY;
		}
	}

	/** Picks a representative item sprite for a relic from its primary effect kind (placeholder art). */
	public static int relicIcon(Relic relic)
	{
		RelicEffectKind kind = relic.effects().isEmpty() ? null : relic.effects().get(0).kind();
		if (kind == null)
		{
			return ITEM_DRAGONSTONE;
		}
		switch (kind)
		{
			case SCORING_BIAS: return ITEM_COINS;
			case CATEGORY_LIMIT: return ITEM_SHARK;
			case ONE_SHOT_MERCY: return ITEM_GLORY;
			default: return ITEM_DRAGONSTONE;
		}
	}

	/** One short, human-readable summary line for a relic effect. */
	public static String relicEffectSummary(RelicEffect effect)
	{
		switch (effect.kind())
		{
			case ONE_SHOT_MERCY: return "One-shot mercy";
			case CATEGORY_LIMIT: return "Max " + effect.magnitude() + " " + effectCategories(effect);
			case SCORING_BIAS: return (effect.magnitude() >= 0 ? "+" : "") + effect.magnitude()
				+ " score: " + effectCategories(effect);
			case RESTRICTION: return "Forbids " + (effect.itemIds().isEmpty() ? effectCategories(effect) : "set items");
			case PERMISSION: return "Allows " + effectCategories(effect);
			default: return effect.kind().name();
		}
	}

	public static String effectCategories(RelicEffect effect)
	{
		if (effect.categories().isEmpty())
		{
			return "any";
		}
		StringBuilder sb = new StringBuilder();
		for (BankItemCategory c : effect.categories())
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(c.name().toLowerCase().replace('_', ' '));
		}
		return sb.toString();
	}
}
