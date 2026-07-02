package com.pluginideahub.roguescape.core.region;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Stage 5 — content bank of named OSRS boss stages. Each definition is tagged
 * {@link RoomKind#BOSS} and carries the integer region IDs (as strings) of the boss's lair so
 * a RuneLite adapter can map world points to region permissions without exposing RuneLite types to the
 * pure-Java core.
 */
public final class BossLibrary
{
	private BossLibrary() {}

	private static Set<String> regions(String... ids)
	{
		Set<String> s = new LinkedHashSet<>();
		Collections.addAll(s, ids);
		return s;
	}

	public static RoomDefinition giantMole()
	{
		return new RoomDefinition("boss-giant-mole", "Giant Mole", RoomKind.BOSS, regions("6993"), 5779);
	}

	public static RoomDefinition kingBlackDragon()
	{
		return new RoomDefinition("boss-king-black-dragon", "King Black Dragon", RoomKind.BOSS, regions("9033"), 239);
	}

	public static RoomDefinition chaosElemental()
	{
		return new RoomDefinition("boss-chaos-elemental", "Chaos Elemental", RoomKind.BOSS, regions("9619"), 2054);
	}

	public static RoomDefinition scorpia()
	{
		return new RoomDefinition("boss-scorpia", "Scorpia", RoomKind.BOSS, regions("10278"), 6615);
	}

	public static RoomDefinition venenatis()
	{
		return new RoomDefinition("boss-venenatis", "Venenatis", RoomKind.BOSS, regions("10296"));
	}

	public static RoomDefinition vetion()
	{
		return new RoomDefinition("boss-vetion", "Vet'ion", RoomKind.BOSS, regions("10038"));
	}

	public static RoomDefinition callisto()
	{
		return new RoomDefinition("boss-callisto", "Callisto", RoomKind.BOSS, regions("9772"));
	}

	public static RoomDefinition barrowsBrothers()
	{
		return new RoomDefinition("boss-barrows-brothers", "Barrows Brothers", RoomKind.BOSS, regions("14131"));
	}

	public static RoomDefinition dagannothKings()
	{
		return new RoomDefinition("boss-dagannoth-kings", "Dagannoth Kings", RoomKind.BOSS, regions("11589"), 2265);
	}

	public static RoomDefinition sarachnis()
	{
		return new RoomDefinition("boss-sarachnis", "Sarachnis", RoomKind.BOSS, regions("7322"));
	}

	public static RoomDefinition cerberus()
	{
		return new RoomDefinition("boss-cerberus", "Cerberus", RoomKind.BOSS, regions("4883"), 5862);
	}

	public static RoomDefinition thermonuclearSmokeDevil()
	{
		return new RoomDefinition("boss-thermonuclear-smoke-devil", "Thermonuclear Smoke Devil", RoomKind.BOSS, regions("9363"), 499);
	}

	public static RoomDefinition kraken()
	{
		return new RoomDefinition("boss-kraken", "Kraken", RoomKind.BOSS, regions("9116"), 494);
	}

	public static RoomDefinition zulrah()
	{
		return new RoomDefinition("boss-zulrah", "Zulrah", RoomKind.BOSS, regions("9007"), 2042);
	}

	public static RoomDefinition vorkath()
	{
		return new RoomDefinition("boss-vorkath", "Vorkath", RoomKind.BOSS, regions("9023"), 8061);
	}

	public static RoomDefinition alchemicalHydra()
	{
		return new RoomDefinition("boss-alchemical-hydra", "Alchemical Hydra", RoomKind.BOSS, regions("5536"));
	}

	public static RoomDefinition corporealBeast()
	{
		return new RoomDefinition("boss-corporeal-beast", "Corporeal Beast", RoomKind.BOSS, regions("11842"), 319);
	}

	public static RoomDefinition abyssalSire()
	{
		return new RoomDefinition("boss-abyssal-sire", "Abyssal Sire", RoomKind.BOSS, regions("11851"), 5886);
	}

	public static RoomDefinition commanderZilyana()
	{
		return new RoomDefinition("boss-commander-zilyana", "Commander Zilyana", RoomKind.BOSS, regions("11602"), 2205);
	}

	public static RoomDefinition generalGraardor()
	{
		return new RoomDefinition("boss-general-graardor", "General Graardor", RoomKind.BOSS, regions("11347"), 2215);
	}

	/**
	 * The boss's OSRS NPC id by display name (0 when unknown) — used by the widget UI to render
	 * the 3D chathead. Ids are best-effort data; verify against the live client when adding more.
	 */
	public static int npcIdFor(String bossName)
	{
		if (bossName == null || bossName.isEmpty())
		{
			return 0;
		}
		for (RoomDefinition def : all())
		{
			if (def.name().equalsIgnoreCase(bossName))
			{
				return def.npcId();
			}
		}
		return 0;
	}

	/** Canonical list of all 20 bosses in this content bank. */
	public static List<RoomDefinition> all()
	{
		return Collections.unmodifiableList(Arrays.asList(
			giantMole(),
			kingBlackDragon(),
			chaosElemental(),
			scorpia(),
			venenatis(),
			vetion(),
			callisto(),
			barrowsBrothers(),
			dagannothKings(),
			sarachnis(),
			cerberus(),
			thermonuclearSmokeDevil(),
			kraken(),
			zulrah(),
			vorkath(),
			alchemicalHydra(),
			corporealBeast(),
			abyssalSire(),
			commanderZilyana(),
			generalGraardor()));
	}
}
