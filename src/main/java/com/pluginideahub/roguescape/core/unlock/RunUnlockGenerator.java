package com.pluginideahub.roguescape.core.unlock;

import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;

public final class RunUnlockGenerator
{
	private RunUnlockGenerator() {}

	public static RunUnlock forClearedStage(RunStage stage, StageRegionRule rule)
	{
		if (stage == null || stage.type() != RunStageType.ROOM)
		{
			return null;
		}
		RoomKind kind = rule == null ? RoomKind.REGION : rule.roomKind();
		RunUnlockType type = typeFor(kind);
		return new RunUnlock(type, labelFor(type, stage.name()), stage.id(), stage.name());
	}

	private static RunUnlockType typeFor(RoomKind kind)
	{
		if (kind == RoomKind.SUPPLY) return RunUnlockType.POTION;
		if (kind == RoomKind.SKILLING) return RunUnlockType.BANK;
		if (kind == RoomKind.SHOP) return RunUnlockType.TRADE;
		if (kind == RoomKind.WEAPON || kind == RoomKind.COMBAT) return RunUnlockType.PRAYER;
		if (kind == RoomKind.ARMOUR) return RunUnlockType.INVENTORY;
		return RunUnlockType.INVENTORY;
	}

	private static String labelFor(RunUnlockType type, String roomName)
	{
		switch (type)
		{
			case PRAYER: return "Prayer unlocked";
			case POTION: return "Potion drinking unlocked";
			case BANK: return "Bank withdrawals unlocked";
			case TRADE: return "Trading unlocked";
			case INVENTORY:
			default:
				return "Inventory expansion unlocked";
		}
	}
}
