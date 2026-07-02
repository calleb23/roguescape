package com.pluginideahub.roguescape.core;

import java.util.Arrays;
import com.pluginideahub.prototype.core.PrototypeDeck;
import com.pluginideahub.prototype.core.PrototypeRuleCard;

public final class RogueScapePrototype
{
	private RogueScapePrototype()
	{
	}

	public static String displayName()
	{
		return "RogueScape";
	}

	public static PrototypeDeck starterDeck()
	{
		PrototypeDeck deck = new PrototypeDeck();
		deck.add(new PrototypeRuleCard(
			"one-inventory-boss-run",
			"One Inventory Boss Run",
			"Banking or death ends the run; inventory is the build.",
			Arrays.asList("Declare starter kit.", "Track acquired build pieces manually or by future inventory deltas.")
		));
		deck.add(new PrototypeRuleCard(
			"overworld-dungeon-crawl",
			"Overworld Dungeon Crawl",
			"Towns and regions act like rooms in a roguelike map.",
			Arrays.asList("Take one reward from each room.", "Leaving a room locks its local loot choices.")
		));
		deck.add(new PrototypeRuleCard(
			"fresh-ironman-scramble",
			"Fresh Ironman Scramble",
			"No Grand Exchange, no traded items; find everything from scratch.",
			Arrays.asList("Declare a starting region.", "Only FOUND_DURING_RUN or GATHERED_OR_CRAFTED items are legal.")
		));
		deck.add(new PrototypeRuleCard(
			"barrows-no-teleport-run",
			"Barrows No-Teleport Run",
			"Walk to Barrows and back using only items acquired along the route.",
			Arrays.asList("No teleportation scrolls or tablets allowed.", "Items found on the walk form the build.")
		));
		deck.add(new PrototypeRuleCard(
			"room-drop-only",
			"Room Drop Only",
			"Build is constrained to drops from a single chosen area.",
			Arrays.asList("Pick one area at run start.", "Picking up items outside that area is blocked.")
		));
		deck.add(new PrototypeRuleCard(
			"clue-scroll-build",
			"Clue Scroll Build",
			"Each clue scroll reward is a roguelike item draw seeding the build.",
			Arrays.asList("Start with one clue scroll.", "Each clue reward becomes a build piece.")
		));
		deck.add(new PrototypeRuleCard(
			"skilling-only-build",
			"Skilling-Only Build",
			"No combat loot; the build comes entirely from gathering and crafting.",
			Arrays.asList("Mining, fishing, woodcutting, and farming drops count.", "Combat drops are not part of the build.")
		));
		deck.add(new PrototypeRuleCard(
			"race-mode-any-percent",
			"Race Mode: Any%",
			"Timed challenge; first to complete the declared goal wins.",
			Arrays.asList("Timer starts at run start.", "Bank use ends the race and the run immediately.")
		));
		return deck;
	}
}
