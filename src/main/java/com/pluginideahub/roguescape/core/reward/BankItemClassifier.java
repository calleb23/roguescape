package com.pluginideahub.roguescape.core.reward;

import java.util.Locale;

/**
 * Stage 4 — first-pass heuristic classifier for bank items. Knows about a small set of
 * canonical OSRS terms so it can guess category + a coarse value tier. Designed to be
 * replaced by a richer classifier (e.g. RuneLite ItemManager + GE price) without changing
 * the public API.
 */
public final class BankItemClassifier
{
	private BankItemClassifier() {}

	public static BankItem classify(String itemName, long approxPrice)
	{
		BankItemCategory category = guessCategory(itemName);
		ValueTier tier = ValueTier.ofPrice(approxPrice);
		return new BankItem(itemName.toLowerCase(Locale.ROOT), itemName, category, tier, approxPrice);
	}

	public static BankItemCategory guessCategory(String itemName)
	{
		if (itemName == null) return BankItemCategory.UNKNOWN;
		String lower = itemName.toLowerCase(Locale.ROOT);
		// Junk markers
		if (lower.startsWith("rune pouch placeholder") || lower.contains("placeholder")) return BankItemCategory.JUNK;
		if (lower.contains("clue scroll")) return BankItemCategory.SKILLING_SUPPLY;
		// Food
		if (lower.endsWith("shark") || lower.endsWith("lobster") || lower.endsWith("monkfish")
			|| lower.endsWith("trout") || lower.endsWith("salmon") || lower.endsWith("manta ray")
			|| lower.endsWith("karambwan") || lower.endsWith("anglerfish") || lower.endsWith("dark crab")
			|| lower.contains("cake")) return BankItemCategory.FOOD;
		if (lower.contains("potion") || lower.endsWith("(4)") || lower.endsWith("(3)") || lower.endsWith("(2)") || lower.endsWith("(1)")
			|| lower.startsWith("brew") || lower.contains("restore") || lower.contains("super combat")) return BankItemCategory.POTION;
		// Runes/ammo
		if (lower.endsWith("rune") || lower.endsWith("runes")) return BankItemCategory.RUNE;
		if (lower.contains("arrow") || lower.contains("bolt") || lower.contains("dart") || lower.contains("javelin"))
			return BankItemCategory.AMMO;
		// Weapons
		if (lower.contains("whip") || lower.contains("scimitar") || lower.contains("sword")
			|| lower.contains("dagger") || lower.contains("mace") || lower.contains("warhammer")
			|| lower.contains("axe ") || lower.endsWith("axe") || lower.contains("spear")) return BankItemCategory.MELEE_WEAPON;
		if (lower.contains("bow") || lower.contains("crossbow") || lower.contains("blowpipe")) return BankItemCategory.RANGED_WEAPON;
		if (lower.contains("staff") || lower.contains("wand") || lower.contains("trident")) return BankItemCategory.MAGIC_WEAPON;
		// Shields and armour slots
		if (lower.contains("shield") || lower.contains("defender")) return BankItemCategory.SHIELD;
		if (lower.contains("helm") || lower.contains("coif") || lower.contains("hood")) return BankItemCategory.HELMET;
		if (lower.contains("platebody") || lower.contains("chainbody") || lower.contains("top") || lower.contains("torso") || lower.contains("body"))
			return BankItemCategory.BODY;
		if (lower.contains("platelegs") || lower.contains("chaps") || lower.contains("legs") || lower.contains("skirt"))
			return BankItemCategory.LEGS;
		if (lower.contains("boots")) return BankItemCategory.BOOTS;
		if (lower.contains("gloves") || lower.contains("vambraces") || lower.contains("bracelet"))
			return BankItemCategory.GLOVES;
		if (lower.contains("cape") || lower.contains("cloak")) return BankItemCategory.CAPE;
		if (lower.contains("amulet") || lower.contains("necklace")) return BankItemCategory.NECK;
		if (lower.contains("ring")) return BankItemCategory.RING;
		// Utility / teleport
		if (lower.contains("teleport") || lower.contains("teletab") || lower.contains("tablet"))
			return BankItemCategory.TELEPORT;
		if (lower.contains("logs") || lower.contains("ore") || lower.contains("bar")
			|| lower.contains("fish") || lower.contains("seed") || lower.contains("herb"))
			return BankItemCategory.SKILLING_SUPPLY;
		return BankItemCategory.UNKNOWN;
	}
}
