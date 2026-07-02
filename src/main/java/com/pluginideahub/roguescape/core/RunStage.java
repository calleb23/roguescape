package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import com.pluginideahub.roguescape.core.task.RoomTask;

public final class RunStage
{
	private final String id;
	private final String name;
	private final RunStageType type;
	private final String note;
	private final String objectiveLabel;
	private final RunObjectiveKind objectiveKind;
	private final int requiredItemGains;
	private RoomTask roomTask;
	private boolean entered;
	private boolean cleared;
	private int itemGains;
	private boolean bossDefeated;

	public RunStage(String id, String name, RunStageType type, String note)
	{
		this(id, name, type, note, defaultObjective(type), defaultRequiredItemGains(type));
	}

	public RunStage(String id, String name, RunStageType type, String note,
		String objectiveLabel, int requiredItemGains)
	{
		this(id, name, type, note, objectiveLabel, defaultObjectiveKind(type), requiredItemGains);
	}

	public RunStage(String id, String name, RunStageType type, String note,
		String objectiveLabel, RunObjectiveKind objectiveKind, int requiredItemGains)
	{
		this.id = id;
		this.name = name;
		this.type = type;
		this.note = note;
		this.objectiveLabel = objectiveLabel == null || objectiveLabel.isEmpty()
			? defaultObjective(type)
			: objectiveLabel;
		this.objectiveKind = objectiveKind != null ? objectiveKind : defaultObjectiveKind(type);
		this.requiredItemGains = Math.max(0, requiredItemGains);
	}

	public String id() { return id; }
	public String name() { return name; }
	public RunStageType type() { return type; }
	public String note() { return note; }
	public String objectiveLabel() { return objectiveLabel; }
	public RunObjectiveKind objectiveKind() { return objectiveKind; }
	public int requiredItemGains() { return requiredItemGains; }
	public RoomTask roomTask() { return roomTask; }
	public int itemGains() { return itemGains; }
	public boolean isEntered() { return entered; }
	public boolean isCleared() { return cleared; }
	public boolean bossDefeated() { return bossDefeated; }
	public boolean objectiveIsTrackable()
	{
		return roomTask != null || requiredItemGains > 0 || objectiveKind == RunObjectiveKind.BOSS_DEFEAT;
	}
	public boolean objectiveComplete()
	{
		if (roomTask != null)
		{
			return roomTask.complete();
		}
		if (objectiveKind == RunObjectiveKind.BOSS_DEFEAT)
		{
			return bossDefeated;
		}
		return requiredItemGains > 0 && itemGains >= requiredItemGains;
	}
	public String objectiveProgressLabel()
	{
		if (objectiveKind == RunObjectiveKind.BOSS_DEFEAT)
		{
			return objectiveLabel + (bossDefeated ? " (defeated)" : " (0 / 1)");
		}
		if (!objectiveIsTrackable())
		{
			return objectiveLabel;
		}
		if (roomTask != null)
		{
			return roomTask.progressLabel();
		}
		return objectiveLabel + " (" + Math.min(itemGains, requiredItemGains)
			+ " / " + requiredItemGains + ")";
	}

	public RunStage setRoomTask(RoomTask task)
	{
		this.roomTask = task;
		return this;
	}

	void markEntered() { entered = true; }
	void markCleared() { cleared = true; }
	void recordBossDefeat()
	{
		if (type == RunStageType.BOSS)
		{
			bossDefeated = true;
		}
	}
	void recordItemGain()
	{
		recordItemGain(BankItemCategory.UNKNOWN, ProvenanceHint.UNKNOWN);
	}

	void recordItemGain(BankItemCategory category, ProvenanceHint hint)
	{
		if (requiredItemGains > 0 && itemGains < requiredItemGains)
		{
			if (objectiveAccepts(category, hint))
			{
				itemGains++;
			}
		}
	}

	boolean recordStatChanged(String skillName)
	{
		return roomTask != null && roomTask.recordStatChanged(skillName);
	}

	private boolean objectiveAccepts(BankItemCategory category, ProvenanceHint hint)
	{
		BankItemCategory c = category != null ? category : BankItemCategory.UNKNOWN;
		ProvenanceHint h = hint != null ? hint : ProvenanceHint.UNKNOWN;
		switch (objectiveKind)
		{
			case COMBAT_DROP:
				return h == ProvenanceHint.OBSERVED_LOOT;
			case SHOP_PURCHASE:
				return h == ProvenanceHint.OBSERVED_SHOP_PURCHASE;
			case SKILLING_RESOURCE:
				return h == ProvenanceHint.OBSERVED_GATHERED
					|| h == ProvenanceHint.OBSERVED_CRAFTED
					|| c == BankItemCategory.SKILLING_SUPPLY;
			case WEAPON_UPGRADE:
				return c == BankItemCategory.MELEE_WEAPON
					|| c == BankItemCategory.RANGED_WEAPON
					|| c == BankItemCategory.MAGIC_WEAPON
					|| c == BankItemCategory.AMMO;
			case ARMOUR_UPGRADE:
				return c == BankItemCategory.SHIELD
					|| c == BankItemCategory.HELMET
					|| c == BankItemCategory.BODY
					|| c == BankItemCategory.LEGS
					|| c == BankItemCategory.BOOTS
					|| c == BankItemCategory.GLOVES
					|| c == BankItemCategory.CAPE
					|| c == BankItemCategory.NECK
					|| c == BankItemCategory.RING;
			case SUPPLY_ITEMS:
				return c == BankItemCategory.FOOD
					|| c == BankItemCategory.POTION
					|| c == BankItemCategory.RUNE
					|| c == BankItemCategory.AMMO
					|| c == BankItemCategory.TELEPORT
					|| c == BankItemCategory.SKILLING_SUPPLY;
			case ANY_ITEM:
			default:
				return true;
		}
	}

	private static String defaultObjective(RunStageType type)
	{
		return type == RunStageType.BOSS ? "Defeat the boss" : "Find an upgrade in this room";
	}

	private static int defaultRequiredItemGains(RunStageType type)
	{
		return type == RunStageType.BOSS ? 0 : 1;
	}

	private static RunObjectiveKind defaultObjectiveKind(RunStageType type)
	{
		return type == RunStageType.BOSS ? RunObjectiveKind.BOSS_DEFEAT : RunObjectiveKind.ANY_ITEM;
	}
}
