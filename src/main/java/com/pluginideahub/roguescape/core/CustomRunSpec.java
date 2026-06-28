package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import java.util.ArrayList;
import java.util.List;

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
