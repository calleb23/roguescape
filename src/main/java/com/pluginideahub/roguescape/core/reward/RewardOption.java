package com.pluginideahub.roguescape.core.reward;

import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.unlock.RunUnlock;
import java.util.Objects;

/**
 * Stage 4 — a single option inside a {@link RewardDraft}. Carries one of: a {@link BankItem}
 * reference (bank-unlock chests), a {@link Relic} (relic chests), or just a free-form item
 * descriptor (fresh-source reward chests).
 */
public final class RewardOption
{
	private final String optionId;
	private final String label;
	private final ChestType chestType;
	private final BankItem bankItem;
	private final Relic relic;
	private final RunUnlock unlock;

	public RewardOption(String optionId, String label, ChestType chestType, BankItem bankItem)
	{
		this(optionId, label, chestType, bankItem, null);
	}

	public RewardOption(String optionId, String label, ChestType chestType, BankItem bankItem, Relic relic)
	{
		this(optionId, label, chestType, bankItem, relic, null);
	}

	public RewardOption(String optionId, String label, ChestType chestType, BankItem bankItem, Relic relic, RunUnlock unlock)
	{
		this.optionId = Objects.requireNonNull(optionId, "optionId");
		this.label = label == null ? optionId : label;
		this.chestType = chestType != null ? chestType : ChestType.SUPPLY;
		this.bankItem = bankItem;
		this.relic = relic;
		this.unlock = unlock;
	}

	/** Builds a relic reward option labelled with the relic's name. */
	public static RewardOption ofRelic(String optionId, Relic relic)
	{
		Objects.requireNonNull(relic, "relic");
		return new RewardOption(optionId, relic.name(), ChestType.RELIC, null, relic);
	}

	public static RewardOption ofUnlock(String optionId, RunUnlock unlock)
	{
		Objects.requireNonNull(unlock, "unlock");
		return new RewardOption(optionId, unlock.label(), ChestType.UNLOCK, null, null, unlock);
	}

	public String optionId() { return optionId; }
	public String label() { return label; }
	public ChestType chestType() { return chestType; }
	public BankItem bankItem() { return bankItem; }
	public Relic relic() { return relic; }
	public RunUnlock unlock() { return unlock; }
	public boolean isBankUnlock() { return chestType == ChestType.BANK_UNLOCK && bankItem != null; }
	public boolean isRelic() { return relic != null; }
	public boolean isUnlock() { return unlock != null; }

	@Override
	public String toString()
	{
		return "RewardOption{" + optionId + " [" + chestType + "]}";
	}
}
