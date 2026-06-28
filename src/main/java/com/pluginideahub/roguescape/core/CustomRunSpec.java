package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.region.BossLibrary;
import com.pluginideahub.roguescape.core.region.RoomDefinition;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.RoomLibrary;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RuneLite/Swing-free model of the custom run-builder's state — the canonical store the side panel
 * (and, via the panel's getters, the in-game builder window) read and mutate while assembling a
 * custom run. Lifted out of {@code RogueScapePanel} in increments to make the builder logic
 * unit-testable without Swing; this first slice owns the scalar constraint state (game mode, loadout,
 * strictness, bank unlocks, and the time + boss caps).
 */
public final class CustomRunSpec
{
	private String customBuilderGameMode = "Scavenger";
	private String customBuilderLoadout = "Naked";
	private String customStrictness = "Balanced";
	private boolean customBankUnlocks;
	private int customTimeLimitMinutes;
	private int customBossLimit;

	public String customBuilderGameMode()
	{
		return customBuilderGameMode;
	}

	public void setCustomBuilderGameMode(String customBuilderGameMode)
	{
		if (customBuilderGameMode != null && !customBuilderGameMode.trim().isEmpty())
		{
			this.customBuilderGameMode = customBuilderGameMode.trim();
		}
	}

	public String customBuilderLoadout()
	{
		return customBuilderLoadout;
	}

	public void setCustomBuilderLoadout(String customBuilderLoadout)
	{
		if (customBuilderLoadout != null && !customBuilderLoadout.trim().isEmpty())
		{
			this.customBuilderLoadout = customBuilderLoadout.trim();
		}
	}

	public String customStrictness()
	{
		return customStrictness;
	}

	public void setCustomStrictness(String customStrictness)
	{
		this.customStrictness = customStrictness;
	}

	public boolean customBankUnlocks()
	{
		return customBankUnlocks;
	}

	public void setCustomBankUnlocks(boolean customBankUnlocks)
	{
		this.customBankUnlocks = customBankUnlocks;
	}

	public int customTimeLimitMinutes()
	{
		return customTimeLimitMinutes;
	}

	public void setCustomTimeLimitMinutes(int customTimeLimitMinutes)
	{
		this.customTimeLimitMinutes = customTimeLimitMinutes;
	}

	public int customBossLimit()
	{
		return customBossLimit;
	}

	public void setCustomBossLimit(int customBossLimit)
	{
		this.customBossLimit = customBossLimit;
	}

	public void cycleCustomStrictness()
	{
		if ("Balanced".equals(customStrictness))
		{
			customStrictness = "Trust";
		}
		else if ("Trust".equals(customStrictness))
		{
			customStrictness = "Strict";
		}
		else
		{
			customStrictness = "Balanced";
		}
	}

	public void toggleCustomBankUnlocks()
	{
		customBankUnlocks = !customBankUnlocks;
	}

	public void cycleCustomTimeLimit()
	{
		if (customTimeLimitMinutes == 0)
		{
			customTimeLimitMinutes = 30;
		}
		else if (customTimeLimitMinutes == 30)
		{
			customTimeLimitMinutes = 60;
		}
		else if (customTimeLimitMinutes == 60)
		{
			customTimeLimitMinutes = 90;
		}
		else
		{
			customTimeLimitMinutes = 0;
		}
	}

	public void cycleCustomBossLimit()
	{
		if (customBossLimit == 0)
		{
			customBossLimit = 1;
		}
		else if (customBossLimit < 3)
		{
			customBossLimit++;
		}
		else
		{
			customBossLimit = 0;
		}
	}

	/** The shareable seed string encoding the whole builder selection. */
	public String customSeedPreview()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mode=").append(customBuilderGameMode);
		sb.append(";loadout=").append(customBuilderLoadout);
		sb.append(";rooms=");
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			if (i > 0) sb.append(",");
			sb.append(selectedRoomIds.get(i)).append(":");
			sb.append(i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All");
		}
		sb.append(";mods=").append(String.join(",", selectedModifierIds));
		sb.append(";strictness=").append(customStrictness);
		sb.append(";bank=").append(customBankUnlocks ? "on" : "off");
		sb.append(";time=").append(customTimeLimitMinutes == 0 ? "none" : customTimeLimitMinutes + "m");
		sb.append(";bosscap=").append(customBossLimit == 0 ? "none" : customBossLimit);
		return sb.toString();
	}

	/** Rebuilds the whole builder selection from a seed string; true iff the seed contained any fields. */
	public boolean applyCustomSeed(String seed)
	{
		Map<String, String> fields = RunSeedCodec.parseFields(seed);
		if (fields.isEmpty())
		{
			return false;
		}
		setCustomBuilderGameMode(fields.get("mode"));
		setCustomBuilderLoadout(fields.get("loadout"));
		applyRouteFromSeed(fields.get("rooms"), fields.get("boss"));
		applyModifierIdsFromCsv(fields.get("mods"));
		String strictness = fields.get("strictness");
		if ("Trust".equalsIgnoreCase(strictness)) setCustomStrictness("Trust");
		else if ("Strict".equalsIgnoreCase(strictness)) setCustomStrictness("Strict");
		else if ("Balanced".equalsIgnoreCase(strictness)) setCustomStrictness("Balanced");
		String bank = fields.get("bank");
		if (bank != null)
		{
			setCustomBankUnlocks("on".equalsIgnoreCase(bank) || "true".equalsIgnoreCase(bank));
		}
		String time = fields.get("time");
		if (time != null)
		{
			setCustomTimeLimitMinutes(RunSeedCodec.parseTimeMinutes(time));
		}
		String bossCap = fields.get("bosscap");
		if (bossCap != null)
		{
			setCustomBossLimit(RunSeedCodec.parseBossLimit(bossCap));
		}
		return true;
	}

	// --- route: handpicked rooms/bosses + allowance, plus the room/allowance/boss option cursors ---

	private final List<String> selectedRoomIds = new ArrayList<>();
	private final List<String> selectedRoomAllowances = new ArrayList<>();
	private int selectedRouteIndex = -1;
	private int customRoomCursor;
	private int customBossCursor;
	private int customAllowanceCursor;
	private static final String[] CUSTOM_ALLOWANCES = {"Supply", "Armour", "Weapons", "Skilling", "All", "Shopping"};

	/** Ordered route IDs the player handpicked. Entries may be rooms or bosses. */
	public List<String> selectedRoomIds()
	{
		return new ArrayList<>(selectedRoomIds);
	}

	public List<String> selectedRoomLabels()
	{
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			String allowance = i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All";
			labels.add(roomName(selectedRoomIds.get(i)) + " [" + allowance + "] -> " + rewardPreviewForAllowance(allowance));
		}
		return labels;
	}

	public List<String> customRouteLabels()
	{
		return selectedRoomLabels();
	}

	public List<String> selectedRoomAllowances()
	{
		List<String> allowances = new ArrayList<>();
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			allowances.add(i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All");
		}
		return allowances;
	}

	public int selectedRouteIndex()
	{
		return selectedRouteIndex;
	}

	public String selectedBossId()
	{
		for (String id : selectedRoomIds)
		{
			if (bossExists(id))
			{
				return id;
			}
		}
		return "";
	}

	// room option cursor

	public String customSelectedRoomLabel()
	{
		List<RoomDefinition> rooms = customRoomOptions();
		if (rooms.isEmpty())
		{
			return "(no rooms)";
		}
		customRoomCursor = clamp(customRoomCursor, rooms.size());
		return rooms.get(customRoomCursor).name();
	}

	public List<String> customRoomOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (RoomDefinition def : customRoomOptions())
		{
			labels.add(def.name());
		}
		return labels;
	}

	public int customSelectedRoomIndex()
	{
		return clamp(customRoomCursor, customRoomOptions().size());
	}

	public void selectCustomRoomIndex(int index)
	{
		customRoomCursor = clamp(index, customRoomOptions().size());
	}

	public void pageCustomRoomIndex(int delta)
	{
		customRoomCursor = clamp(customRoomCursor + delta, customRoomOptions().size());
	}

	public void customPreviousRoom()
	{
		customRoomCursor = previousIndex(customRoomCursor, customRoomOptions().size());
	}

	public void customNextRoom()
	{
		customRoomCursor = nextIndex(customRoomCursor, customRoomOptions().size());
	}

	// allowance option cursor

	public String customSelectedAllowanceLabel()
	{
		customAllowanceCursor = clamp(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
		return CUSTOM_ALLOWANCES[customAllowanceCursor];
	}

	public List<String> customAllowanceOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (String allowance : CUSTOM_ALLOWANCES)
		{
			labels.add(allowance);
		}
		return labels;
	}

	public int customSelectedAllowanceIndex()
	{
		return clamp(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	public void selectCustomAllowanceIndex(int index)
	{
		customAllowanceCursor = clamp(index, CUSTOM_ALLOWANCES.length);
	}

	public void customPreviousAllowance()
	{
		customAllowanceCursor = previousIndex(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	public void customNextAllowance()
	{
		customAllowanceCursor = nextIndex(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	// boss option cursor

	public String customSelectedBossLabel()
	{
		List<RoomDefinition> bosses = BossLibrary.all();
		if (bosses.isEmpty())
		{
			return "(no bosses)";
		}
		customBossCursor = clamp(customBossCursor, bosses.size());
		return bosses.get(customBossCursor).name();
	}

	public List<String> customBossOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (RoomDefinition def : BossLibrary.all())
		{
			labels.add(def.name());
		}
		return labels;
	}

	public int customSelectedBossIndex()
	{
		return clamp(customBossCursor, BossLibrary.all().size());
	}

	public void selectCustomBossIndex(int index)
	{
		customBossCursor = clamp(index, BossLibrary.all().size());
	}

	public void pageCustomBossIndex(int delta)
	{
		customBossCursor = clamp(customBossCursor + delta, BossLibrary.all().size());
	}

	public void customPreviousBoss()
	{
		customBossCursor = previousIndex(customBossCursor, BossLibrary.all().size());
	}

	public void customNextBoss()
	{
		customBossCursor = nextIndex(customBossCursor, BossLibrary.all().size());
	}

	// route mutation (callers add the Swing refresh when these report a change)

	/** Appends an entry if not already present, marking it the selected row; true iff it was added. */
	private boolean addRouteEntry(String id, String allowance)
	{
		if (selectedRoomIds.contains(id))
		{
			return false;
		}
		selectedRoomIds.add(id);
		selectedRoomAllowances.add(allowance);
		selectedRouteIndex = selectedRoomIds.size() - 1;
		return true;
	}

	public boolean addFirstRoomOfKind(RoomKind kind)
	{
		return addFirstRoomOfKind(kind, allowanceLabel(kind));
	}

	public boolean addFirstRoomOfKind(RoomKind kind, String allowance)
	{
		if (kind == null)
		{
			return false;
		}
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() == kind && def.kind() != RoomKind.BOSS && !selectedRoomIds.contains(def.id()))
			{
				return addRouteEntry(def.id(),
					allowance == null || allowance.trim().isEmpty() ? allowanceLabel(kind) : allowance.trim());
			}
		}
		return false;
	}

	public boolean addRoomForAllowance(String allowance)
	{
		String normalized = allowance == null ? "" : allowance.trim().toLowerCase();
		if ("supply".equals(normalized))
		{
			return addFirstRoomOfKind(RoomKind.SUPPLY, "Supply");
		}
		else if ("armour".equals(normalized) || "armor".equals(normalized))
		{
			return addFirstRoomOfKind(RoomKind.ARMOUR, "Armour");
		}
		else if ("weapons".equals(normalized) || "weapon".equals(normalized))
		{
			return addFirstRoomOfKind(RoomKind.WEAPON, "Weapons");
		}
		else if ("skilling".equals(normalized))
		{
			return addFirstRoomOfKind(RoomKind.SKILLING, "Skilling");
		}
		else if ("shopping".equals(normalized) || "shop".equals(normalized))
		{
			return addFirstRoomOfKind(RoomKind.SHOP, "Shopping");
		}
		return addFirstRoomOfKind(RoomKind.REGION, "All");
	}

	/** Adds a route entry by explicit id+allowance (used by the Swing room combo); true iff added. */
	public boolean addRoute(String id, String allowance)
	{
		return addRouteEntry(id, allowance);
	}

	public boolean addSelectedCustomRoom()
	{
		List<RoomDefinition> rooms = customRoomOptions();
		if (rooms.isEmpty())
		{
			return false;
		}
		customRoomCursor = clamp(customRoomCursor, rooms.size());
		return addRouteEntry(rooms.get(customRoomCursor).id(), customSelectedAllowanceLabel());
	}

	public boolean addSelectedCustomBoss()
	{
		List<RoomDefinition> bosses = BossLibrary.all();
		if (bosses.isEmpty())
		{
			return false;
		}
		customBossCursor = clamp(customBossCursor, bosses.size());
		RoomDefinition boss = bosses.get(customBossCursor);
		if (customBossLimit() > 0 && bossCountInCustomRoute() >= customBossLimit())
		{
			return false;
		}
		return addRouteEntry(boss.id(), "Boss");
	}

	public boolean selectFirstBoss()
	{
		selectCustomBossIndex(0);
		return addSelectedCustomBoss();
	}

	public boolean selectBossById(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		List<RoomDefinition> bosses = BossLibrary.all();
		for (int i = 0; i < bosses.size(); i++)
		{
			if (bosses.get(i).id().equals(id))
			{
				customBossCursor = i;
				return addSelectedCustomBoss();
			}
		}
		return false;
	}

	public boolean removeLastCustomRoom()
	{
		if (selectedRoomIds.isEmpty())
		{
			return false;
		}
		int idx = selectedRouteIndex >= 0 && selectedRouteIndex < selectedRoomIds.size()
			? selectedRouteIndex : selectedRoomIds.size() - 1;
		selectedRoomIds.remove(idx);
		if (idx < selectedRoomAllowances.size())
		{
			selectedRoomAllowances.remove(idx);
		}
		selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : Math.min(idx, selectedRoomIds.size() - 1);
		return true;
	}

	/** Removes the trailing entry (used by the Swing "remove" button); true iff something was removed. */
	public boolean removeLastRoom()
	{
		if (selectedRoomIds.isEmpty())
		{
			return false;
		}
		selectedRoomIds.remove(selectedRoomIds.size() - 1);
		if (!selectedRoomAllowances.isEmpty())
		{
			selectedRoomAllowances.remove(selectedRoomAllowances.size() - 1);
		}
		selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : Math.min(selectedRouteIndex, selectedRoomIds.size() - 1);
		return true;
	}

	public void clearRoute()
	{
		selectedRoomIds.clear();
		selectedRoomAllowances.clear();
		selectedRouteIndex = -1;
	}

	public void selectPreviousRouteRow()
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = selectedRouteIndex <= 0 ? 0 : selectedRouteIndex - 1;
		}
	}

	public void selectNextRouteRow()
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = selectedRouteIndex < 0 ? 0 : Math.min(selectedRoomIds.size() - 1, selectedRouteIndex + 1);
		}
	}

	public void selectRouteRow(int index)
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = clamp(index, selectedRoomIds.size());
		}
	}

	public boolean moveSelectedRouteUp()
	{
		if (selectedRouteIndex <= 0 || selectedRouteIndex >= selectedRoomIds.size())
		{
			return false;
		}
		swapRouteRows(selectedRouteIndex, selectedRouteIndex - 1);
		selectedRouteIndex--;
		return true;
	}

	public boolean moveSelectedRouteDown()
	{
		if (selectedRouteIndex < 0 || selectedRouteIndex >= selectedRoomIds.size() - 1)
		{
			return false;
		}
		swapRouteRows(selectedRouteIndex, selectedRouteIndex + 1);
		selectedRouteIndex++;
		return true;
	}

	/** Rebuilds the route lists from a seed's {@code rooms} and {@code boss} fields. */
	public void applyRouteFromSeed(String rooms, String boss)
	{
		selectedRoomIds.clear();
		selectedRoomAllowances.clear();
		if (rooms != null && !rooms.trim().isEmpty())
		{
			for (String part : rooms.split(","))
			{
				String[] pair = part.split(":", 2);
				String id = pair.length > 0 ? pair[0].trim() : "";
				if (routeEntryExists(id))
				{
					selectedRoomIds.add(id);
					selectedRoomAllowances.add(pair.length > 1 && !pair[1].trim().isEmpty() ? pair[1].trim() : "All");
				}
			}
		}
		if (boss != null && !"auto".equalsIgnoreCase(boss) && !"none".equalsIgnoreCase(boss) && routeEntryExists(boss.trim())
			&& !selectedRoomIds.contains(boss.trim()))
		{
			selectedRoomIds.add(boss.trim());
			selectedRoomAllowances.add("Boss");
		}
		selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : selectedRoomIds.size() - 1;
	}

	// route helpers

	private List<RoomDefinition> customRoomOptions()
	{
		List<RoomDefinition> rooms = new ArrayList<>();
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() != RoomKind.BOSS)
			{
				rooms.add(def);
			}
		}
		return rooms;
	}

	private boolean roomExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (RoomDefinition def : customRoomOptions())
		{
			if (def.id().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private boolean routeEntryExists(String id)
	{
		return roomExists(id) || bossExists(id);
	}

	private boolean bossExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (RoomDefinition def : BossLibrary.all())
		{
			if (def.id().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private int bossCountInCustomRoute()
	{
		int count = 0;
		for (String id : selectedRoomIds)
		{
			if (bossExists(id))
			{
				count++;
			}
		}
		return count;
	}

	private void swapRouteRows(int a, int b)
	{
		String room = selectedRoomIds.get(a);
		selectedRoomIds.set(a, selectedRoomIds.get(b));
		selectedRoomIds.set(b, room);
		if (a < selectedRoomAllowances.size() && b < selectedRoomAllowances.size())
		{
			String allowance = selectedRoomAllowances.get(a);
			selectedRoomAllowances.set(a, selectedRoomAllowances.get(b));
			selectedRoomAllowances.set(b, allowance);
		}
	}

	private String roomName(String id)
	{
		for (RoomDefinition def : customRoomOptions())
		{
			if (def.id().equals(id)) return def.name();
		}
		for (RoomDefinition def : BossLibrary.all())
		{
			if (def.id().equals(id)) return def.name();
		}
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.id().equals(id)) return def.name();
		}
		return id;
	}

	public static String allowanceLabel(RoomKind kind)
	{
		if (kind == RoomKind.SUPPLY) return "Supply";
		if (kind == RoomKind.ARMOUR) return "Armour";
		if (kind == RoomKind.WEAPON) return "Weapons";
		if (kind == RoomKind.SKILLING) return "Skilling";
		if (kind == RoomKind.SHOP) return "Shopping";
		return "All";
	}

	private static String rewardPreviewForAllowance(String allowance)
	{
		String value = allowance == null ? "" : allowance.trim().toLowerCase();
		if ("boss".equals(value)) return "random relic draft";
		if ("supply".equals(value)) return "random supply draft";
		if ("all".equals(value)) return "random relic draft";
		return "random unlock draft";
	}

	private static int previousIndex(int current, int size)
	{
		if (size <= 0) return 0;
		return current <= 0 ? size - 1 : current - 1;
	}

	private static int nextIndex(int current, int size)
	{
		if (size <= 0) return 0;
		return current >= size - 1 ? 0 : current + 1;
	}

	// --- starting curses / modifiers ---

	private final List<String> selectedModifierIds = new ArrayList<>();
	private int customModifierCursor;

	public List<String> selectedModifierIds()
	{
		return new ArrayList<>(selectedModifierIds);
	}

	public List<String> customModifierOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (Relic r : ModifierLibrary.all())
		{
			labels.add(r.name());
		}
		return labels;
	}

	public int customModifierPageStart()
	{
		return clamp(customModifierCursor, ModifierLibrary.all().size());
	}

	public void pageCustomModifierIndex(int delta)
	{
		customModifierCursor = clamp(customModifierCursor + delta, ModifierLibrary.all().size());
	}

	public List<String> selectedModifierLabels()
	{
		List<String> labels = new ArrayList<>();
		for (String id : selectedModifierIds)
		{
			labels.add(modifierName(id));
		}
		return labels;
	}

	public List<Integer> selectedModifierIndexes()
	{
		List<Integer> indexes = new ArrayList<>();
		List<Relic> modifiers = ModifierLibrary.all();
		for (int i = 0; i < modifiers.size(); i++)
		{
			if (selectedModifierIds.contains(modifiers.get(i).relicId()))
			{
				indexes.add(i);
			}
		}
		return indexes;
	}

	public void toggleModifierIndex(int index)
	{
		List<Relic> modifiers = ModifierLibrary.all();
		if (modifiers.isEmpty())
		{
			return;
		}
		int idx = clamp(index, modifiers.size());
		String id = modifiers.get(idx).relicId();
		if (selectedModifierIds.contains(id))
		{
			selectedModifierIds.remove(id);
		}
		else
		{
			selectedModifierIds.add(id);
		}
	}

	/** Adds the modifier id if not already present; returns true iff it was added. */
	public boolean addModifierIdIfAbsent(String id)
	{
		if (selectedModifierIds.contains(id))
		{
			return false;
		}
		selectedModifierIds.add(id);
		return true;
	}

	public void clearModifiers()
	{
		selectedModifierIds.clear();
	}

	/** Replaces the modifier selection from a comma-separated id list, keeping only known, unique ids. */
	public void applyModifierIdsFromCsv(String mods)
	{
		selectedModifierIds.clear();
		if (mods != null && !mods.trim().isEmpty())
		{
			for (String id : mods.split(","))
			{
				String modId = id.trim();
				if (modifierExists(modId) && !selectedModifierIds.contains(modId))
				{
					selectedModifierIds.add(modId);
				}
			}
		}
	}

	private boolean modifierExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (Relic relic : ModifierLibrary.all())
		{
			if (relic.relicId().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private String modifierName(String id)
	{
		for (Relic r : ModifierLibrary.all())
		{
			if (r.relicId().equals(id))
			{
				return r.name();
			}
		}
		return id;
	}

	private static int clamp(int current, int size)
	{
		if (size <= 0) return 0;
		return Math.max(0, Math.min(size - 1, current));
	}
}
