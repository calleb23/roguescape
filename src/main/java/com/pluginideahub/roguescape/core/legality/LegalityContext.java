package com.pluginideahub.roguescape.core.legality;

import com.pluginideahub.roguescape.core.region.StageRegionRule;

/**
 * Stage 2/3 — minimal context the classifier needs to judge a {@link ItemDelta}.
 * Pure data — no RuneLite types. Adapters or the session build this and call the
 * classifier with it.
 */
public final class LegalityContext
{
	private final StarterKit starterKit;
	private final StrictnessMode strictness;
	private final String currentRegionId;
	private final StageRegionRule stageRule;
	private final boolean bankAccessAllowed;
	private final boolean preRunSupplyExpected;
	private final boolean unlockedBankItem;

	private LegalityContext(Builder b)
	{
		this.starterKit = b.starterKit != null ? b.starterKit : new StarterKit();
		this.strictness = b.strictness != null ? b.strictness : StrictnessMode.BALANCED;
		this.currentRegionId = b.currentRegionId;
		this.stageRule = b.stageRule != null ? b.stageRule : StageRegionRule.UNRESTRICTED;
		this.bankAccessAllowed = b.bankAccessAllowed;
		this.preRunSupplyExpected = b.preRunSupplyExpected;
		this.unlockedBankItem = b.unlockedBankItem;
	}

	public StarterKit starterKit() { return starterKit; }
	public StrictnessMode strictness() { return strictness; }
	public String currentRegionId() { return currentRegionId; }
	public StageRegionRule stageRule() { return stageRule; }
	public boolean bankAccessAllowed() { return bankAccessAllowed; }
	public boolean preRunSupplyExpected() { return preRunSupplyExpected; }
	public boolean unlockedBankItem() { return unlockedBankItem; }

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private StarterKit starterKit;
		private StrictnessMode strictness;
		private String currentRegionId;
		private StageRegionRule stageRule;
		private boolean bankAccessAllowed;
		private boolean preRunSupplyExpected;
		private boolean unlockedBankItem;

		public Builder starterKit(StarterKit kit) { this.starterKit = kit; return this; }
		public Builder strictness(StrictnessMode m) { this.strictness = m; return this; }
		public Builder currentRegionId(String id) { this.currentRegionId = id; return this; }
		public Builder stageRule(StageRegionRule rule) { this.stageRule = rule; return this; }
		public Builder bankAccessAllowed(boolean b) { this.bankAccessAllowed = b; return this; }
		public Builder preRunSupplyExpected(boolean b) { this.preRunSupplyExpected = b; return this; }
		public Builder unlockedBankItem(boolean b) { this.unlockedBankItem = b; return this; }

		public LegalityContext build() { return new LegalityContext(this); }
	}
}
