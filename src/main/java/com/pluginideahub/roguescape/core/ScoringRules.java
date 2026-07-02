package com.pluginideahub.roguescape.core;

/**
 * Stage 5 — pure-Java scoring rules for a {@link ScoringPreset}. The rules describe how collected
 * items, cleared rooms/bosses, run time, and relic biases combine into a single run score. No
 * RuneLite dependencies.
 */
public final class ScoringRules
{
	private final int basePointsPerItem;
	private final int bonusPerClearedRoom;
	private final int bonusPerClearedBoss;
	private final boolean timeBonusEnabled;
	private final int timeBonusSecondsThreshold;
	private final int timeBonusPoints;
	private final double relicBonusMultiplier;

	public ScoringRules(int basePointsPerItem,
		int bonusPerClearedRoom,
		int bonusPerClearedBoss,
		boolean timeBonusEnabled,
		int timeBonusSecondsThreshold,
		int timeBonusPoints,
		double relicBonusMultiplier)
	{
		this.basePointsPerItem = basePointsPerItem;
		this.bonusPerClearedRoom = bonusPerClearedRoom;
		this.bonusPerClearedBoss = bonusPerClearedBoss;
		this.timeBonusEnabled = timeBonusEnabled;
		this.timeBonusSecondsThreshold = timeBonusSecondsThreshold;
		this.timeBonusPoints = timeBonusPoints;
		this.relicBonusMultiplier = relicBonusMultiplier;
	}

	public int basePointsPerItem() { return basePointsPerItem; }
	public int bonusPerClearedRoom() { return bonusPerClearedRoom; }
	public int bonusPerClearedBoss() { return bonusPerClearedBoss; }
	public boolean timeBonusEnabled() { return timeBonusEnabled; }
	public int timeBonusSecondsThreshold() { return timeBonusSecondsThreshold; }
	public int timeBonusPoints() { return timeBonusPoints; }
	public double relicBonusMultiplier() { return relicBonusMultiplier; }

	public static ScoringRules forPreset(ScoringPreset preset)
	{
		if (preset == null) preset = ScoringPreset.BALANCED;
		switch (preset)
		{
			case SPEEDRUN:
				return new ScoringRules(1, 3, 10, true, 1800, 50, 0.5);
			case CREATOR_CHAOS:
				return new ScoringRules(2, 8, 25, false, 0, 0, 2.0);
			case BALANCED:
			default:
				return new ScoringRules(1, 5, 15, false, 0, 0, 1.0);
		}
	}

	/**
	 * Returns the total recap score for the supplied run counts. {@code relicScoringBonus}
	 * is the raw sum returned by {@code RelicEngine#scoreBonus}; this method applies the
	 * preset's {@link #relicBonusMultiplier} to it before adding.
	 */
	public int calculateScore(int items,
		int clearedRooms,
		int clearedBosses,
		long runSeconds,
		int relicScoringBonus)
	{
		int total = 0;
		total += items * basePointsPerItem;
		total += clearedRooms * bonusPerClearedRoom;
		total += clearedBosses * bonusPerClearedBoss;
		if (timeBonusEnabled && runSeconds < timeBonusSecondsThreshold)
		{
			total += timeBonusPoints;
		}
		total += (int) Math.round(relicScoringBonus * relicBonusMultiplier);
		return total;
	}
}
