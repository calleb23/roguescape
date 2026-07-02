package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.item.InventorySnapshot;
import com.pluginideahub.roguescape.core.item.ItemDelta;
import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.item.StarterKit;
import com.pluginideahub.roguescape.core.region.RouteRegionPolicy;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.reward.BankDraftPool;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import com.pluginideahub.roguescape.core.reward.BankItemClassifier;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import com.pluginideahub.roguescape.core.unlock.RunUnlockType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stages 2/3 — higher-level run orchestrator that composes the Stage-1 {@link RogueScapeRunSession}
 * with the route region policy and run constraints.
 *
 * Composition rather than inheritance is intentional: Stage 1 has a large stable test
 * surface, and the Stage 2/3 work happens around it without rewriting behaviour.
 *
 * The plugin no longer classifies item provenance as permitted/forbidden — disallowed actions are
 * blocked outright by {@code MenuEnforcementEvaluator}. Items observed in-run simply count toward
 * the current room's objective and the run score.
 */
public final class RogueScapeRun
{
	private final RogueScapeRunSession session;
	private final RouteRegionPolicy regionPolicy = new RouteRegionPolicy();
	private final List<ItemDelta> collectedItems = new ArrayList<>();
	private final List<RewardDraft> drafts = new ArrayList<>();
	private final Map<RunUnlockType, RunUnlock> unlocks = new EnumMap<>(RunUnlockType.class);
	private final BankDraftPool bankPool = new BankDraftPool();
	private final RelicEngine relicEngine = new RelicEngine();

	private StarterKit starterKit = new StarterKit();
	private String currentRegionId = "";
	private boolean bankAccessAllowed = false;
	private boolean regionRestrictionArmed = true;
	private InventorySnapshot startSnapshot = new InventorySnapshot();

	private RogueScapeRun(RogueScapeRunSession session) { this.session = session; }

	public static RogueScapeRun wrap(RogueScapeRunSession session)
	{
		return new RogueScapeRun(session);
	}

	public RogueScapeRunSession session() { return session; }
	public RouteRegionPolicy regionPolicy() { return regionPolicy; }
	public StarterKit starterKit() { return starterKit; }
	public String currentRegionId() { return currentRegionId; }
	public boolean bankAccessAllowed() { return bankAccessAllowed; }
	public boolean bankUnlocked() { return bankAccessAllowed || hasUnlock(RunUnlockType.BANK); }
	public boolean tradeUnlocked() { return hasUnlock(RunUnlockType.TRADE); }
	public boolean prayerUnlocked() { return hasUnlock(RunUnlockType.PRAYER); }
	public boolean potionUnlocked() { return hasUnlock(RunUnlockType.POTION); }
	public boolean regionRestrictionArmed() { return regionRestrictionArmed; }
	public InventorySnapshot startSnapshot() { return startSnapshot; }

	public String currentRoomName()
	{
		RunStage stage = currentEnteredStage();
		return stage != null ? stage.name() : null;
	}

	public List<ItemDelta> collectedItems() { return Collections.unmodifiableList(collectedItems); }
	public List<RewardDraft> drafts() { return Collections.unmodifiableList(drafts); }
	public List<RunUnlock> unlocks() { return Collections.unmodifiableList(new ArrayList<>(unlocks.values())); }
	public BankDraftPool bankPool() { return bankPool; }
	public RelicEngine relicEngine() { return relicEngine; }
	public List<Relic> heldRelics() { return relicEngine.relics(); }
	public Set<BankItemCategory> relicRestrictedCategories() { return relicEngine.restrictedCategories(); }
	public Map<BankItemCategory, Integer> relicCategoryLimits() { return relicEngine.categoryLimits(); }
	public Set<BankItemCategory> relicOverLimit() { return relicEngine.overLimit(); }

	/** Run score including any relic scoring bonuses. */
	public int effectiveScore() { return session.runScore() + relicEngine.scoreBonus(); }

	/**
	 * The run's current rules as a {@link com.pluginideahub.roguescape.core.restriction.RunRestrictions}
	 * — the single verdict brain of the subtractive design. Derived from the run's unlock flags,
	 * the active stage's region rule, and any relic-imposed category restrictions, so enforcement,
	 * the loadout gate, and the red-X markers all read the same state.
	 */
	public com.pluginideahub.roguescape.core.restriction.RunRestrictions currentRestrictions()
	{
		com.pluginideahub.roguescape.core.restriction.RunRestrictions r =
			new com.pluginideahub.roguescape.core.restriction.RunRestrictions();
		if (!bankUnlocked())
		{
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.BANK);
		}
		if (!tradeUnlocked())
		{
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.TRADE);
		}
		// The Grand Exchange stays sealed for the whole run (no unlock exists for it).
		r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.GRAND_EXCHANGE);
		if (!prayerUnlocked())
		{
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.PRAYER);
		}
		if (!potionUnlocked())
		{
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.POTIONS);
		}
		if (currentStageRule().restrictsRegion())
		{
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.LEAVE_REGION);
			r.restrict(com.pluginideahub.roguescape.core.restriction.Restriction.GROUND_PICKUP_OUTSIDE_ROOM);
		}
		for (BankItemCategory cat : relicRestrictedCategories())
		{
			com.pluginideahub.roguescape.core.restriction.Restriction mapped = categoryRestriction(cat);
			if (mapped != null)
			{
				r.restrict(mapped);
			}
		}
		return r;
	}

	private static com.pluginideahub.roguescape.core.restriction.Restriction categoryRestriction(BankItemCategory cat)
	{
		if (cat == null)
		{
			return null;
		}
		switch (cat)
		{
			case FOOD: return com.pluginideahub.roguescape.core.restriction.Restriction.FOOD;
			case POTION: return com.pluginideahub.roguescape.core.restriction.Restriction.POTIONS;
			case AMMO: return com.pluginideahub.roguescape.core.restriction.Restriction.AMMO;
			case RUNE: return com.pluginideahub.roguescape.core.restriction.Restriction.RUNES;
			case SHIELD: return com.pluginideahub.roguescape.core.restriction.Restriction.SHIELD;
			default: return null;
		}
	}

	public boolean hasUnlock(RunUnlockType type)
	{
		return type != null && unlocks.containsKey(type);
	}

	public RogueScapeRun grantUnlock(RunUnlock unlock)
	{
		if (unlock != null && !unlocks.containsKey(unlock.type()))
		{
			unlocks.put(unlock.type(), unlock);
			session.recordRunLoopNote("Unlocked " + unlock.displayRow());
		}
		return this;
	}

	public RogueScapeRun addRewardDraft(RewardDraft draft)
	{
		if (draft != null)
		{
			drafts.add(draft);
		}
		return this;
	}

	/** Activates a relic for the run: it now influences category limits and scoring. */
	public RogueScapeRun chooseRelic(Relic relic)
	{
		if (relic != null)
		{
			relicEngine.addRelic(relic);
			String effect = relic.description() == null ? "" : relic.description();
			session.addRelic(relic.name(), effect);
		}
		return this;
	}

	// ---------- Setup ----------

	public RogueScapeRun declareStarterKit(StarterKit kit)
	{
		this.starterKit = kit != null ? kit : new StarterKit();
		// Mirror into the legacy session for recap continuity.
		for (String id : starterKit.asMap().keySet())
		{
			session.declareStarterKitItem(id);
		}
		return this;
	}

	public RogueScapeRun setStartSnapshot(InventorySnapshot snapshot)
	{
		this.startSnapshot = snapshot != null ? snapshot : new InventorySnapshot();
		return this;
	}

	public RogueScapeRun setBankAccessAllowed(boolean b) { this.bankAccessAllowed = b; return this; }
	public RogueScapeRun setRegionRestrictionArmed(boolean b) { this.regionRestrictionArmed = b; return this; }

	public RogueScapeRun setRegionRule(String stageId, StageRegionRule rule)
	{
		regionPolicy.setRule(stageId, rule);
		return this;
	}

	// ---------- Player location / region ----------

	public RogueScapeRun moveToRegion(String regionId)
	{
		this.currentRegionId = regionId == null ? "" : regionId;
		return this;
	}

	public StageRegionRule currentStageRule()
	{
		RunStage current = currentEnteredStage();
		if (current == null) return StageRegionRule.UNRESTRICTED;
		return regionPolicy.ruleFor(current.id());
	}

	public RunStage currentEnteredStage()
	{
		RunStage last = null;
		for (RunStage s : session.route().stages())
		{
			if (s.isEntered() && !s.isCleared()) last = s;
		}
		if (last != null) return last;
		// Fallback: most recently entered stage, even if cleared.
		for (RunStage s : session.route().stages())
		{
			if (s.isEntered()) last = s;
		}
		return last;
	}

	public boolean currentRegionAllowed()
	{
		StageRegionRule rule = currentStageRule();
		if (!rule.restrictsRegion()) return true;
		return rule.isAllowedRegion(currentRegionId);
	}

	// ---------- Item events ----------

	/**
	 * Records an item observed during the run: it counts toward the current room's objective and
	 * the run score, feeds relic category limits, and (for boss loot) signals a boss defeat. No
	 * permission judgment is made — disallowed actions are blocked at the menu, not classified here.
	 */
	public void applyItemDelta(ItemDelta delta)
	{
		if (delta == null) throw new IllegalArgumentException("delta required");

		collectedItems.add(delta);
		BankItemCategory category = BankItemClassifier.guessCategory(delta.itemName());
		relicEngine.recordItem(category);

		RunStage stage = currentEnteredStage();
		session.recordCurrentStageItemGain(category, delta.provenanceHint());
		if (stage != null
			&& stage.type() == RunStageType.BOSS
			&& delta.provenanceHint() == ProvenanceHint.OBSERVED_LOOT)
		{
			session.recordCurrentStageBossDefeat("boss loot: " + delta.itemName());
		}

		String location = stage != null ? stage.name() : delta.locationNote();
		session.observeItemGain(delta.itemName(), delta.quantity(),
			RogueScapeRunSession.ItemSource.FOUND_DURING_RUN, location, "auto", 1);
	}

	public void applyItemDelta(String itemName, int quantity, ProvenanceHint hint)
	{
		applyItemDelta(new ItemDelta(itemName.toLowerCase(), itemName, quantity, "", hint));
	}

	public boolean recordBossDefeatSignal(String source)
	{
		return session.recordCurrentStageBossDefeat(source);
	}

	public boolean recordStatChanged(String skillName, int xp)
	{
		return session.recordCurrentStageStatChanged(skillName);
	}

	// ---------- Counts ----------

	public int itemsCollected() { return collectedItems.size(); }
}
