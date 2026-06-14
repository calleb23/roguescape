package com.pluginideahub.roguescape.core;

import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ItemEvent;
import com.pluginideahub.roguescape.core.legality.ItemLegality;
import com.pluginideahub.roguescape.core.legality.LegalityClassifier;
import com.pluginideahub.roguescape.core.legality.LegalityContext;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
import com.pluginideahub.roguescape.core.legality.StarterKit;
import com.pluginideahub.roguescape.core.legality.StrictnessMode;
import com.pluginideahub.roguescape.core.region.RouteRegionPolicy;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEngine;
import com.pluginideahub.roguescape.core.reward.BankDraftPool;
import com.pluginideahub.roguescape.core.reward.BankItem;
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
 * with the legality classifier and route region policy.
 *
 * Composition rather than inheritance is intentional: Stage 1 has a large stable test
 * surface, and the Stage 2/3 work happens around it without rewriting behaviour.
 *
 * The orchestrator is still pure-Java — RuneLite events are converted to {@link ItemDelta}
 * by a future Stage 6 adapter and fed in via {@link #applyItemDelta}.
 */
public final class RogueScapeRun
{
	private final RogueScapeRunSession session;
	private final RouteRegionPolicy regionPolicy = new RouteRegionPolicy();
	private final List<ItemEvent> itemEvents = new ArrayList<>();
	private final List<RewardDraft> drafts = new ArrayList<>();
	private final Map<RunUnlockType, RunUnlock> unlocks = new EnumMap<>(RunUnlockType.class);
	private final BankDraftPool bankPool = new BankDraftPool();
	private final RelicEngine relicEngine = new RelicEngine();

	private StarterKit starterKit = new StarterKit();
	private StrictnessMode strictness = StrictnessMode.BALANCED;
	private String currentRegionId = "";
	private boolean bankAccessAllowed = false;
	private boolean preRunSupplyExpected = false;
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
	public StrictnessMode strictness() { return strictness; }
	public String currentRegionId() { return currentRegionId; }
	public boolean bankAccessAllowed() { return bankAccessAllowed; }
	public boolean bankUnlocked() { return bankAccessAllowed || hasUnlock(RunUnlockType.BANK); }
	public boolean tradeUnlocked() { return hasUnlock(RunUnlockType.TRADE); }
	public boolean prayerUnlocked() { return hasUnlock(RunUnlockType.PRAYER); }
	public boolean potionUnlocked() { return hasUnlock(RunUnlockType.POTION); }
	public boolean preRunSupplyExpected() { return preRunSupplyExpected; }
	public boolean regionRestrictionArmed() { return regionRestrictionArmed; }
	public InventorySnapshot startSnapshot() { return startSnapshot; }

	public String currentRoomName()
	{
		RunStage stage = currentEnteredStage();
		return stage != null ? stage.name() : null;
	}

	public List<ItemEvent> itemEvents() { return Collections.unmodifiableList(itemEvents); }
	public List<RewardDraft> drafts() { return Collections.unmodifiableList(drafts); }
	public List<RunUnlock> unlocks() { return Collections.unmodifiableList(new ArrayList<>(unlocks.values())); }
	public BankDraftPool bankPool() { return bankPool; }
	public RelicEngine relicEngine() { return relicEngine; }
	public List<Relic> heldRelics() { return relicEngine.relics(); }
	public Set<BankItemCategory> relicRestrictedCategories() { return relicEngine.restrictedCategories(); }
	public Map<BankItemCategory, Integer> relicCategoryLimits() { return relicEngine.categoryLimits(); }
	public Set<BankItemCategory> relicOverLimit() { return relicEngine.overLimit(); }

	/**
	 * Run score including relic scoring bonuses, computed via the mode's {@link ScoringRules}.
	 * Run time is treated as unknown here (no SPEEDRUN time bonus); the timed overload is used
	 * once an elapsed clock is available (see the W6 RunContext seam).
	 */
	public int effectiveScore() { return effectiveScore(Long.MAX_VALUE); }

	/** Run score for a known elapsed time in seconds, enabling the SPEEDRUN time bonus. */
	public int effectiveScore(long runSeconds)
	{
		ScoringRules rules = ScoringRules.forPreset(ScoringPreset.forMode(session.mode()));
		// session.runScore() is the legacy legal-gain points basis (1 per legal gain); using it
		// keeps the BALANCED base term identical to the pre-unification score.
		return rules.calculateScore(session.runScore(), illegalCount(), clearedRooms(), clearedBosses(),
			runSeconds, relicEngine.scoreBonus());
	}

	public int clearedRooms()
	{
		int n = 0;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == RunStageType.ROOM && s.isCleared()) n++;
		}
		return n;
	}

	public int clearedBosses()
	{
		int n = 0;
		for (RunStage s : session.route().stages())
		{
			if (s.type() == RunStageType.BOSS && s.isCleared()) n++;
		}
		return n;
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

	/** Activates a relic for the run: it now influences legality, limits, and scoring. */
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

	public RogueScapeRun setStrictness(StrictnessMode mode)
	{
		if (mode != null) this.strictness = mode;
		return this;
	}

	public RogueScapeRun setStartSnapshot(InventorySnapshot snapshot)
	{
		this.startSnapshot = snapshot != null ? snapshot : new InventorySnapshot();
		return this;
	}

	public RogueScapeRun setBankAccessAllowed(boolean b) { this.bankAccessAllowed = b; return this; }
	public RogueScapeRun setPreRunSupplyExpected(boolean b) { this.preRunSupplyExpected = b; return this; }
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
		// If the current stage restricts region and we are now outside it, record a violation
		// in strict mode. Balanced/Trust just warn — surfaced via lastRegionStatus().
		StageRegionRule rule = currentStageRule();
		if (regionRestrictionArmed && rule.restrictsRegion() && !rule.isLegalRegion(currentRegionId))
		{
			if (strictness == StrictnessMode.STRICT)
			{
				session.recordViolation("Left legal region: " + currentRegionId,
					RogueScapeRunSession.RunEnding.UNKNOWN_ITEM_SOURCE);
			}
		}
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

	public boolean currentRegionLegal()
	{
		StageRegionRule rule = currentStageRule();
		if (!rule.restrictsRegion()) return true;
		return rule.isLegalRegion(currentRegionId);
	}

	// ---------- Item events ----------

	public ItemEvent applyItemDelta(ItemDelta delta)
	{
		if (delta == null) throw new IllegalArgumentException("delta required");

		boolean unlockedBank = bankPool.isUnlocked(delta.itemId());
		LegalityContext ctx = LegalityContext.builder()
			.starterKit(starterKit)
			.strictness(strictness)
			.stageRule(currentStageRule())
			.currentRegionId(currentRegionId)
			.bankAccessAllowed(bankUnlocked())
			.preRunSupplyExpected(preRunSupplyExpected)
			.unlockedBankItem(unlockedBank)
			.build();

		ItemLegality baseLegality = LegalityClassifier.classify(delta, ctx);
		RunStage stage = currentEnteredStage();
		String stageId = stage != null ? stage.id() : null;

		// Apply active relics: a forbidden category can turn a legal gain illegal, and a mercy
		// can soften an illegal bank withdrawal. Then record the item for limits/scoring.
		BankItemCategory category = BankItemClassifier.guessCategory(delta.itemName());
		ItemEvent provisional = new ItemEvent(delta, baseLegality, stageId);
		ItemLegality legality = relicEngine.adjustLegality(provisional, category);
		ItemEvent event = legality == baseLegality ? provisional : new ItemEvent(delta, legality, stageId);
		itemEvents.add(event);
		relicEngine.recordItem(category);
		if (legality.isLegal())
		{
			session.recordCurrentStageLegalItemGain(category, delta.provenanceHint());
			if (stage != null
				&& stage.type() == RunStageType.BOSS
				&& delta.provenanceHint() == ProvenanceHint.OBSERVED_LOOT)
			{
				session.recordCurrentStageBossDefeat("boss loot: " + delta.itemName());
			}
		}

		// Mirror legality into legacy reward counts. Suspicious in non-strict modes maps to
		// MANUALLY_APPROVED so the legacy auto-fail (UNKNOWN_OR_ILLEGAL) doesn't trigger.
		RogueScapeRunSession.ItemSource legacy = mapToLegacySource(event);
		String location = stage != null ? stage.name() : delta.locationNote();
		int points = pointsFor(legality);
		session.observeItemGain(delta.itemName(), delta.quantity(), legacy, location, "auto", points);
		return event;
	}

	public ItemEvent applyItemDelta(String itemName, int quantity, ProvenanceHint hint)
	{
		return applyItemDelta(new ItemDelta(itemName.toLowerCase(), itemName, quantity, "", hint));
	}

	public boolean recordBossDefeatSignal(String source)
	{
		return session.recordCurrentStageBossDefeat(source);
	}

	public boolean recordStatChanged(String skillName, int xp)
	{
		return session.recordCurrentStageStatChanged(skillName);
	}

	private RogueScapeRunSession.ItemSource mapToLegacySource(ItemEvent event)
	{
		ItemLegality legality = event.legality();
		if (legality.isLegal())
		{
			switch (legality)
			{
				case LEGAL_STARTER_KIT: return RogueScapeRunSession.ItemSource.STARTER_KIT;
				case LEGAL_SHOP_PURCHASE: return RogueScapeRunSession.ItemSource.BOUGHT_DURING_RUN;
				case LEGAL_GATHERED_OR_CRAFTED: return RogueScapeRunSession.ItemSource.GATHERED_OR_CRAFTED;
				case LEGAL_MANUAL_APPROVAL: return RogueScapeRunSession.ItemSource.MANUALLY_APPROVED;
				case LEGAL_REGION_GAIN:
				case LEGAL_ROOM_REWARD:
				case LEGAL_BANK_UNLOCK:
				default: return RogueScapeRunSession.ItemSource.FOUND_DURING_RUN;
			}
		}
		if (legality.isSuspicious())
		{
			// STRICT mode fails on suspicious -> route through UNKNOWN_OR_ILLEGAL.
			// BALANCED/TRUST surfaces as pending approval so legacy view stays ACTIVE.
			return strictness == StrictnessMode.STRICT
				? RogueScapeRunSession.ItemSource.UNKNOWN_OR_ILLEGAL
				: RogueScapeRunSession.ItemSource.MANUALLY_APPROVED;
		}
		return RogueScapeRunSession.ItemSource.UNKNOWN_OR_ILLEGAL;
	}

	private static int pointsFor(ItemLegality legality)
	{
		return legality.isLegal() ? 1 : 0;
	}

	// ---------- Counts ----------

	public int legalCount()
	{
		int n = 0;
		for (ItemEvent e : itemEvents) if (e.isLegal()) n++;
		return n;
	}

	public int suspiciousCount()
	{
		int n = 0;
		for (ItemEvent e : itemEvents) if (e.isSuspicious()) n++;
		return n;
	}

	public int illegalCount()
	{
		int n = 0;
		for (ItemEvent e : itemEvents) if (e.isIllegal()) n++;
		return n;
	}
}
