package com.pluginideahub.roguescape.core.legality;

/**
 * Stage 2/3 — pure function from (delta + context) -> ItemLegality.
 *
 * Order of evaluation is intentional:
 * 1. Manual marks always win — players can flag/approve explicitly.
 * 2. Obvious illegal channels (trade, GE, bank withdrawal when not allowed) fail outright.
 * 3. Region-restricted stages reject items gained outside the legal region.
 * 4. Provenance-derived legals: shop, gather/craft, observed loot.
 * 5. Anything we cannot place becomes SUSPICIOUS_UNKNOWN — never silently legal.
 *
 * Note: This classifier does NOT decide whether the run *fails* — the run engine combines
 * the classification with {@link StrictnessMode} to decide.
 */
public final class LegalityClassifier
{
	private LegalityClassifier() {}

	public static ItemLegality classify(ItemDelta delta, LegalityContext context)
	{
		if (delta == null) throw new IllegalArgumentException("delta required");
		if (context == null) throw new IllegalArgumentException("context required");

		ProvenanceHint hint = delta.provenanceHint();

		// 1. Manual marks first.
		if (hint == ProvenanceHint.MANUAL_DECLARED_LEGAL) return ItemLegality.LEGAL_MANUAL_APPROVAL;
		if (hint == ProvenanceHint.MANUAL_DECLARED_ILLEGAL) return ItemLegality.ILLEGAL_MANUAL_MARK;

		// 2. Pre-run supplies — items in inventory before the run that aren't in the kit.
		if (context.preRunSupplyExpected()
			&& !context.starterKit().contains(delta.itemId()))
		{
			return ItemLegality.ILLEGAL_PRE_RUN_SUPPLY;
		}

		// 3. Starter kit pieces are always legal under the kit cap.
		if (context.starterKit().contains(delta.itemId()) && hint == ProvenanceHint.UNKNOWN)
		{
			// Allowed to be UNKNOWN provenance because the kit was declared up front.
			return ItemLegality.LEGAL_STARTER_KIT;
		}

		// 4. Trade and GE are illegal regardless of region.
		if (hint == ProvenanceHint.OBSERVED_TRADE) return ItemLegality.ILLEGAL_TRADE;
		if (hint == ProvenanceHint.OBSERVED_GE_COLLECT) return ItemLegality.ILLEGAL_GE;

		// 5. Bank withdrawal: legal only if bank access is allowed AND the item is unlocked.
		if (hint == ProvenanceHint.OBSERVED_BANK_WITHDRAWAL)
		{
			if (!context.bankAccessAllowed()) return ItemLegality.ILLEGAL_BANK_WITHDRAWAL;
			return context.unlockedBankItem() ? ItemLegality.LEGAL_BANK_UNLOCK : ItemLegality.ILLEGAL_BANK_WITHDRAWAL;
		}

		// 6. Region-restricted stages: anything observed outside the allowed region is illegal.
		if (context.stageRule().restrictsRegion()
			&& !context.stageRule().isLegalRegion(context.currentRegionId()))
		{
			return ItemLegality.ILLEGAL_OUT_OF_REGION;
		}

		// 7. Known-legal provenance routes.
		switch (hint)
		{
			case OBSERVED_SHOP_PURCHASE: return ItemLegality.LEGAL_SHOP_PURCHASE;
			case OBSERVED_GATHERED:
			case OBSERVED_CRAFTED: return ItemLegality.LEGAL_GATHERED_OR_CRAFTED;
			case OBSERVED_LOOT:
			case OBSERVED_DIALOG_REWARD:
			case OBSERVED_QUEST_REWARD:
				if (context.stageRule().allowsRegionGain()) return ItemLegality.LEGAL_REGION_GAIN;
				return ItemLegality.LEGAL_ROOM_REWARD;
			case OBSERVED_GROUND_PICKUP:
				// Ground pickups are inherently ambiguous — could be another player's drop.
				return ItemLegality.SUSPICIOUS_UNKNOWN;
			case UNKNOWN:
			default:
				return ItemLegality.SUSPICIOUS_UNKNOWN;
		}
	}
}
