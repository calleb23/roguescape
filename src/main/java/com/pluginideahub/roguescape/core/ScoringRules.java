package com.pluginideahub.roguescape.core;

/**
 * Stage 5 — pure-Java scoring rules for a {@link ScoringPreset}. The rules describe how
 * legal items, illegal items, cleared rooms/bosses, run time, and relic biases combine into
 * a single run score. No RuneLite dependencies.
 */
public final class ScoringRules
{
	private final int basePointsPerLegalItem;
	private final int bonusPerClearedRoom;
	private final int bonusPerClearedBoss;
	private final boolean timeBonusEnabled;
	private final int timeBonusSecondsThreshold;
	private final int timeBonusPoints;
	private final int illegalPenaltyPerItem;
	private final double relicBonusMultiplier;

	public ScoringRules(int basePointsPerLegalItem,
		int bonusPerClearedRoom,
		int bonusPerClearedBoss,
		boolean timeBonusEnabled,
		int timeBonusSecondsThreshold,
		int timeBonusPoints,
		int illegalPenaltyPerItem,
		double relicBonusMultiplier)
	{
		this.basePointsPerLegalItem = basePointsPerLegalItem;
		this.bonusPerClearedRoom = bonusPerClearedRoom;
		this.bonusPerClearedBoss = bonusPerClearedBoss;
		this.timeBonusEnabled = timeBonusEnabled;
		this.timeBonusSecondsThreshold = timeBonusSecondsThreshold;
		this.timeBonusPoints = timeBonusPoints;
		this.illegalPenaltyPerItem = illegalPenaltyPerItem;
		this.relicBonusMultiplier = relicBonusMultiplier;
	}

	public int basePointsPerLegalItem() { return basePointsPerLegalItem; }
	public int bonusPerClearedRoom() { return bonusPerClearedRoom; }
	public int bonusPerClearedBoss() { return bonusPerClearedBoss; }
	public boolean timeBonusEnabled() { return timeBonusEnabled; }
	public int timeBonusSecondsThreshold() { return timeBonusSecondsThreshold; }
	public int timeBonusPoints() { return timeBonusPoints; }
	public int illegalPenaltyPerItem() { return illegalPenaltyPerItem; }
	public double relicBonusMultiplier() { return relicBonusMultiplier; }

	public static ScoringRules forPreset(ScoringPreset preset)
	{
		if (preset == null) preset = ScoringPreset.BALANCED;
		switch (preset)
		{
			case SPEEDRUN:
				return new ScoringRules(1, 3, 10, true, 1800, 50, -5, 0.5);
			case CREATOR_CHAOS:
				return new ScoringRules(2, 8, 25, false, 0, 0, -1, 2.0);
			case BALANCED:
			default:
				return new ScoringRules(1, 5, 15, false, 0, 0, -3, 1.0);
		}
	}

	/**
	 * Returns the total recap score for the supplied run counts. {@code relicScoringBonus}
	 * is the raw sum returned by {@code RelicEngine#scoreBonus}; this method applies the
	 * preset's {@link #relicBonusMultiplier} to it before adding.
	 */
	public int calculateScore(int legalItems,
		int illegalItems,
		int clearedRooms,
		int clearedBosses,
		long runSeconds,
		int relicScoringBonus)
	{
		int total = 0;
		total += legalItems * basePointsPerLegalItem;
		total += illegalItems * illegalPenaltyPerItem;
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
