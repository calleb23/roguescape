package com.pluginideahub.roguescape.core.briefing;

import com.pluginideahub.roguescape.core.RunTimer;
import com.pluginideahub.roguescape.core.region.RoomKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, player-facing summary of exactly what a route contains <em>before</em> the run is
 * committed. The whole point is "nothing left to interpretation": every value here is read back
 * from a real, built {@link com.pluginideahub.roguescape.core.RogueScapeRunSession}/
 * {@link com.pluginideahub.roguescape.core.RogueScapeRun} pair, so the briefing can never drift
 * from the run the player actually starts.
 *
 * Pure Java — no RuneLite or Swing. The UI layer renders {@link #lines()} or reads the structured
 * getters. Every tunable shown here (objective gains, per-room timer, region lock) is also exposed
 * raw on {@link RoomLine} so a future balancing/editing tool can read and round-trip it.
 */
public final class RunBriefing
{
	/** One ordered stop on the route — a room or a boss — with its clear condition spelled out. */
	public static final class RoomLine
	{
		private final int index;
		private final String name;
		private final RoomKind kind;
		private final String kindLabel;
		private final String collectLabel;
		private final int requiredItemGains;
		private final boolean bossStage;
		private final boolean regionLocked;
		private final long timerMillis;

		RoomLine(int index, String name, RoomKind kind, String kindLabel, String collectLabel,
			int requiredItemGains, boolean bossStage, boolean regionLocked, long timerMillis)
		{
			this.index = index;
			this.name = name;
			this.kind = kind;
			this.kindLabel = kindLabel;
			this.collectLabel = collectLabel;
			this.requiredItemGains = Math.max(0, requiredItemGains);
			this.bossStage = bossStage;
			this.regionLocked = regionLocked;
			this.timerMillis = Math.max(0L, timerMillis);
		}

		public int index() { return index; }
		public String name() { return name; }
		public RoomKind kind() { return kind; }
		public String kindLabel() { return kindLabel; }
		public String collectLabel() { return collectLabel; }
		public int requiredItemGains() { return requiredItemGains; }
		public boolean bossStage() { return bossStage; }
		public boolean regionLocked() { return regionLocked; }
		public long timerMillis() { return timerMillis; }
		public boolean hasTimer() { return timerMillis > 0L; }

		/** How this stop is cleared, e.g. "Defeat the boss | 05:00 timer" or "Collect 2 | stay in region". */
		public String gatingLabel()
		{
			List<String> parts = new ArrayList<>();
			if (bossStage)
			{
				parts.add("Defeat the boss");
			}
			else if (requiredItemGains > 0)
			{
				parts.add("Collect " + requiredItemGains);
			}
			else
			{
				parts.add("Reach the stage");
			}
			if (hasTimer())
			{
				parts.add(RunTimer.format(timerMillis) + " timer");
			}
			if (regionLocked)
			{
				parts.add("stay in region");
			}
			return String.join(" | ", parts);
		}
	}

	private final String runTitle;
	private final String modeLabel;
	private final String modeSummary;
	private final List<RoomLine> rooms;
	private final String finalBossName;
	private final String loadoutLabel;
	private final String bankAccessLabel;
	private final String timeModelLabel;
	private final boolean routeLocked;
	private final String seedLabel;
	private final String winCondition;
	private final List<String> loseConditions;

	RunBriefing(String runTitle, String modeLabel, String modeSummary, List<RoomLine> rooms,
		String finalBossName, String loadoutLabel, String bankAccessLabel,
		String timeModelLabel, boolean routeLocked, String seedLabel, String winCondition,
		List<String> loseConditions)
	{
		this.runTitle = runTitle;
		this.modeLabel = modeLabel;
		this.modeSummary = modeSummary;
		this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
		this.finalBossName = finalBossName;
		this.loadoutLabel = loadoutLabel;
		this.bankAccessLabel = bankAccessLabel;
		this.timeModelLabel = timeModelLabel;
		this.routeLocked = routeLocked;
		this.seedLabel = seedLabel;
		this.winCondition = winCondition;
		this.loseConditions = Collections.unmodifiableList(new ArrayList<>(loseConditions));
	}

	public String runTitle() { return runTitle; }
	public String modeLabel() { return modeLabel; }
	public String modeSummary() { return modeSummary; }
	public List<RoomLine> rooms() { return rooms; }
	public String finalBossName() { return finalBossName; }
	public boolean hasFinalBoss() { return finalBossName != null && !finalBossName.isEmpty(); }
	public String loadoutLabel() { return loadoutLabel; }
	public String bankAccessLabel() { return bankAccessLabel; }
	public String timeModelLabel() { return timeModelLabel; }
	public boolean routeLocked() { return routeLocked; }
	public String seedLabel() { return seedLabel; }
	public String winCondition() { return winCondition; }
	public List<String> loseConditions() { return loseConditions; }

	public int roomCount()
	{
		int n = 0;
		for (RoomLine r : rooms) if (!r.bossStage()) n++;
		return n;
	}

	public int bossCount()
	{
		int n = 0;
		for (RoomLine r : rooms) if (r.bossStage()) n++;
		return n;
	}

	/**
	 * Flat, render-agnostic transcript of the whole briefing. The Swing/overlay layer can drop
	 * these straight into text blocks; tests assert on them so the contract stays stable.
	 */
	public List<String> lines()
	{
		List<String> out = new ArrayList<>();
		out.add(modeLabel + " — " + runTitle);
		out.add(modeSummary);
		out.add("");
		out.add("THE ROUTE (" + roomCount() + " rooms"
			+ (bossCount() > 0 ? " + " + bossCount() + " boss" + (bossCount() == 1 ? "" : "es") : "") + ")");
		if (!routeLocked)
		{
			out.add("These rooms re-roll when you Begin. Set a seed to lock this exact route.");
		}
		for (RoomLine r : rooms)
		{
			out.add(r.index() + ". " + r.name() + "  [" + r.kindLabel() + "]");
			out.add("     " + r.collectLabel());
			out.add("     Clear by: " + r.gatingLabel());
		}
		out.add("");
		out.add("THE RULES");
		out.add("Loadout: " + loadoutLabel);
		out.add(bankAccessLabel);
		out.add(timeModelLabel);
		out.add("Seed: " + seedLabel);
		out.add("");
		out.add("WIN: " + winCondition);
		out.add("LOSE:");
		for (String l : loseConditions)
		{
			out.add(" - " + l);
		}
		return out;
	}
}
