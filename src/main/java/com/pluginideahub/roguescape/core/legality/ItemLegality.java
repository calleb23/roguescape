package com.pluginideahub.roguescape.core.legality;

/**
 * Stage 2 — legality states for items observed during a run.
 *
 * The model is intentionally explicit and verbose. RuneLite cannot always prove provenance,
 * so the classifier must be honest about uncertainty rather than collapsing everything into
 * legal/illegal.
 */
public enum ItemLegality
{
	LEGAL_STARTER_KIT,
	LEGAL_REGION_GAIN,
	LEGAL_ROOM_REWARD,
	LEGAL_BANK_UNLOCK,
	LEGAL_SHOP_PURCHASE,
	LEGAL_GATHERED_OR_CRAFTED,
	LEGAL_MANUAL_APPROVAL,
	SUSPICIOUS_UNKNOWN,
	ILLEGAL_BANK_WITHDRAWAL,
	ILLEGAL_TRADE,
	ILLEGAL_GE,
	ILLEGAL_OUT_OF_REGION,
	ILLEGAL_PRE_RUN_SUPPLY,
	ILLEGAL_MANUAL_MARK;

	public boolean isLegal()
	{
		return name().startsWith("LEGAL_");
	}

	public boolean isSuspicious()
	{
		return this == SUSPICIOUS_UNKNOWN;
	}

	public boolean isIllegal()
	{
		return name().startsWith("ILLEGAL_");
	}
}
