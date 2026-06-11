package com.pluginideahub.roguescape.core.reward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 4 — three-option reward draft offered to the player. The draft is *presented*
 * deterministically (e.g. seeded), but the choice itself is user-driven; this class only
 * records the available options and which was picked.
 */
public final class RewardDraft
{
	private final String draftId;
	private final String stageId;
	private final ChestType chestType;
	private final List<RewardOption> options;
	private RewardOption selected;
	private boolean rejected;

	public RewardDraft(String draftId, String stageId, ChestType chestType, List<RewardOption> options)
	{
		if (draftId == null) throw new IllegalArgumentException("draftId required");
		if (options == null || options.isEmpty()) throw new IllegalArgumentException("at least one option required");
		this.draftId = draftId;
		this.stageId = stageId;
		this.chestType = chestType != null ? chestType : ChestType.SUPPLY;
		this.options = Collections.unmodifiableList(new ArrayList<>(options));
	}

	public String draftId() { return draftId; }
	public String stageId() { return stageId; }
	public ChestType chestType() { return chestType; }
	public List<RewardOption> options() { return options; }
	public RewardOption selected() { return selected; }
	public boolean isSelected() { return selected != null; }
	public boolean isRejected() { return rejected; }

	public RewardOption select(String optionId)
	{
		if (this.selected != null) return this.selected;
		for (RewardOption o : options)
		{
			if (o.optionId().equals(optionId))
			{
				this.selected = o;
				return o;
			}
		}
		throw new IllegalArgumentException("unknown optionId: " + optionId);
	}

	public void reject()
	{
		if (this.selected != null) return;
		this.rejected = true;
	}
}
