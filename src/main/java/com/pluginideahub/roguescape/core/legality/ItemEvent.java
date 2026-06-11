package com.pluginideahub.roguescape.core.legality;

import java.util.Objects;

/**
 * Stage 2 — classifier output. Pairs an {@link ItemDelta} with the {@link ItemLegality}
 * the classifier returned. The run engine consumes these to update counts/violations and
 * apply {@link StrictnessMode} policy.
 */
public final class ItemEvent
{
	private final ItemDelta delta;
	private final ItemLegality legality;
	private final String stageId;

	public ItemEvent(ItemDelta delta, ItemLegality legality, String stageId)
	{
		this.delta = Objects.requireNonNull(delta);
		this.legality = Objects.requireNonNull(legality);
		this.stageId = stageId;
	}

	public ItemDelta delta() { return delta; }
	public ItemLegality legality() { return legality; }
	public String stageId() { return stageId; }

	public boolean isLegal() { return legality.isLegal(); }
	public boolean isSuspicious() { return legality.isSuspicious(); }
	public boolean isIllegal() { return legality.isIllegal(); }
}
