package com.pluginideahub.roguescape.core;

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
}
