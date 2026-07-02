package com.pluginideahub.roguescape.core.region;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stage 5 — content bank of named OSRS rooms. Each static factory returns a
 * {@link RoomDefinition}; {@link #all()} returns the full canonical list. The route builder
 * consumes these definitions when constructing a {@link com.pluginideahub.roguescape.core.RunRoute}.
 *
 * Region IDs match the integer ID returned by {@code WorldPoint.getRegionID()} so a thin
 * RuneLite adapter can map world points to room permissions without exposing RuneLite types
 * to the pure-Java core.
 */
public final class RoomLibrary
{
	private RoomLibrary() {}

	private static Set<String> regions(String... ids)
	{
		Set<String> s = new LinkedHashSet<>();
		Collections.addAll(s, ids);
		return s;
	}

	public static RoomDefinition lumbridgeSwamp()
	{
		return new RoomDefinition("lumbridge-swamp", "Lumbridge Swamp", RoomKind.SUPPLY, regions("12851", "12850", "12849"));
	}

	public static RoomDefinition draynorVillage()
	{
		return new RoomDefinition("draynor-village", "Draynor Village", RoomKind.SUPPLY, regions("12338"));
	}

	public static RoomDefinition barbarianVillage()
	{
		return new RoomDefinition("barbarian-village", "Barbarian Village", RoomKind.WEAPON, regions("12341"));
	}

	public static RoomDefinition edgeville()
	{
		return new RoomDefinition("edgeville", "Edgeville", RoomKind.CRAFTING, regions("12342"));
	}

	public static RoomDefinition varrockEast()
	{
		return new RoomDefinition("varrock-east", "Varrock East", RoomKind.WEAPON, regions("12854"));
	}

	public static RoomDefinition varrockWest()
	{
		return new RoomDefinition("varrock-west", "Varrock West", RoomKind.CRAFTING, regions("12853"));
	}

	public static RoomDefinition grandExchange()
	{
		return new RoomDefinition("grand-exchange", "Grand Exchange", RoomKind.SUPPLY, regions("12598"));
	}

	public static RoomDefinition falador()
	{
		return new RoomDefinition("falador", "Falador", RoomKind.ARMOUR, regions("11828", "12084"));
	}

	public static RoomDefinition portSarim()
	{
		return new RoomDefinition("port-sarim", "Port Sarim", RoomKind.SUPPLY, regions("11566"));
	}

	public static RoomDefinition rimmington()
	{
		return new RoomDefinition("rimmington", "Rimmington", RoomKind.CRAFTING, regions("11564"));
	}

	public static RoomDefinition karamjaJungle()
	{
		return new RoomDefinition("karamja-jungle", "Karamja Jungle", RoomKind.SUPPLY, regions("11310"));
	}

	public static RoomDefinition alKharid()
	{
		return new RoomDefinition("al-kharid", "Al-Kharid", RoomKind.ARMOUR, regions("13105"));
	}

	public static RoomDefinition shantayPass()
	{
		return new RoomDefinition("shantay-pass", "Shantay Pass", RoomKind.SUPPLY, regions("13104"));
	}

	public static RoomDefinition ardougneMarket()
	{
		return new RoomDefinition("ardougne-market", "Ardougne Market", RoomKind.SUPPLY, regions("10291"));
	}

	public static RoomDefinition yanille()
	{
		return new RoomDefinition("yanille", "Yanille", RoomKind.SUPPLY, regions("10288"));
	}

	public static RoomDefinition catherby()
	{
		return new RoomDefinition("catherby", "Catherby", RoomKind.SUPPLY, regions("11062"));
	}

	public static RoomDefinition seersVillage()
	{
		return new RoomDefinition("seers-village", "Seers Village", RoomKind.SUPPLY, regions("10807"));
	}

	public static RoomDefinition camelotCastle()
	{
		return new RoomDefinition("camelot-castle", "Camelot Castle", RoomKind.ARMOUR, regions("11062"));
	}

	public static RoomDefinition relekka()
	{
		return new RoomDefinition("relekka", "Relekka", RoomKind.ARMOUR, regions("10297"));
	}

	public static RoomDefinition piscatoris()
	{
		return new RoomDefinition("piscatoris", "Piscatoris", RoomKind.SUPPLY, regions("9273"));
	}

	public static RoomDefinition slayerTower()
	{
		return new RoomDefinition("slayer-tower", "Slayer Tower", RoomKind.WEAPON, regions("13623"));
	}

	public static RoomDefinition canifis()
	{
		return new RoomDefinition("canifis", "Canifis", RoomKind.ARMOUR, regions("13878"));
	}

	public static RoomDefinition mortMyreSwamp()
	{
		return new RoomDefinition("mort-myre-swamp", "Mort Myre Swamp", RoomKind.SUPPLY, regions("13878", "13877"));
	}

	public static RoomDefinition barrows()
	{
		return new RoomDefinition("barrows", "Barrows", RoomKind.BOSS, regions("14131"));
	}

	public static RoomDefinition edgevilleDungeon()
	{
		return new RoomDefinition("edgeville-dungeon", "Edgeville Dungeon", RoomKind.WEAPON, regions("12442"));
	}

	public static RoomDefinition wildernessLevel1To10()
	{
		return new RoomDefinition("wilderness-level-1-10", "Wilderness Level 1-10", RoomKind.WEAPON, regions("12604", "12603"));
	}

	public static RoomDefinition dwarvenMine()
	{
		return new RoomDefinition("dwarven-mine", "Dwarven Mine", RoomKind.SUPPLY, regions("12441"));
	}

	public static RoomDefinition taverley()
	{
		return new RoomDefinition("taverley", "Taverley", RoomKind.SUPPLY, regions("11572"));
	}

	public static RoomDefinition treeGnomeStronghold()
	{
		return new RoomDefinition("tree-gnome-stronghold", "Tree Gnome Stronghold", RoomKind.SUPPLY, regions("9781"));
	}

	public static RoomDefinition pestControlIsland()
	{
		return new RoomDefinition("pest-control-island", "Pest Control Island", RoomKind.BOSS, regions("10537"));
	}

	/** Canonical list of all 30 rooms in this content bank. */
	public static List<RoomDefinition> all()
	{
		return Collections.unmodifiableList(Arrays.asList(
			lumbridgeSwamp(),
			draynorVillage(),
			barbarianVillage(),
			edgeville(),
			varrockEast(),
			varrockWest(),
			grandExchange(),
			falador(),
			portSarim(),
			rimmington(),
			karamjaJungle(),
			alKharid(),
			shantayPass(),
			ardougneMarket(),
			yanille(),
			catherby(),
			seersVillage(),
			camelotCastle(),
			relekka(),
			piscatoris(),
			slayerTower(),
			canifis(),
			mortMyreSwamp(),
			barrows(),
			edgevilleDungeon(),
			wildernessLevel1To10(),
			dwarvenMine(),
			taverley(),
			treeGnomeStronghold(),
			pestControlIsland()));
	}
}
