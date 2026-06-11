package com.pluginideahub.roguescape.core.unlock;

public enum RunUnlockType
{
	PRAYER("Prayer"),
	POTION("Potions"),
	INVENTORY("Inventory"),
	BANK("Bank"),
	TRADE("Trade");

	private final String label;

	RunUnlockType(String label)
	{
		this.label = label;
	}

	public String label()
	{
		return label;
	}
}
