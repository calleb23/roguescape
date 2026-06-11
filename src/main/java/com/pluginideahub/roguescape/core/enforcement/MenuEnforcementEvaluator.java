package com.pluginideahub.roguescape.core.enforcement;

/**
 * Pure-Java decision helper that maps a RuneLite menu option/target pair to an
 * {@link MenuEnforcementDecision} under a given {@link RogueScapeEnforcementRules}.
 *
 * Matching is case-insensitive. The {@code option} and {@code target} strings should be
 * supplied as-is from {@code MenuEntry.getOption()} / {@code MenuEntry.getTarget()}.
 *
 * The sentinel option {@link #REGION_CHECK_OPTION} is used by the plugin to ask the
 * evaluator whether the player has left the legal region, without any menu text.
 */
public final class MenuEnforcementEvaluator
{
	public static final String REGION_CHECK_OPTION = "__REGION_CHECK__";

	private MenuEnforcementEvaluator() {}

	public static MenuEnforcementDecision evaluate(
		String option, String target,
		RogueScapeEnforcementRules rules,
		boolean playerInsideLegalRegion)
	{
		if (rules == null) return MenuEnforcementDecision.ALLOW;

		String opt = option == null ? "" : option.trim().toLowerCase();
		String tgt = target == null ? "" : target.trim().toLowerCase();

		if (REGION_CHECK_OPTION.equalsIgnoreCase(option))
		{
			return rules.warnLeaveRoom() && !playerInsideLegalRegion
				? MenuEnforcementDecision.WARN
				: MenuEnforcementDecision.ALLOW;
		}

		if (rules.blockBank() && isBankAction(opt, tgt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockTrade() && isTradeAction(opt, tgt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockPrayer() && isPrayerAction(opt, tgt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockPotions() && isPotionAction(opt, tgt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockGrandExchange() && isGrandExchangeAction(opt, tgt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockGroundPickup() && !playerInsideLegalRegion && isGroundPickup(opt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		if (rules.blockWalkOutsideRoom() && !playerInsideLegalRegion && isWalkAction(opt))
		{
			return MenuEnforcementDecision.BLOCK;
		}

		return MenuEnforcementDecision.ALLOW;
	}

	private static boolean isBankAction(String opt, String tgt)
	{
		switch (opt)
		{
			case "bank":
			case "open bank":
			case "bank banker":
			case "collect":
			case "bank-deposit":
			case "bank deposit":
				return true;
			default:
				break;
		}
		return tgt.contains("banker") || tgt.contains("bank chest") || tgt.contains("bank booth");
	}

	private static boolean isTradeAction(String opt, String tgt)
	{
		if ("trade".equals(opt) || "trade with".equals(opt))
		{
			return true;
		}
		if ("offer".equals(opt))
		{
			return !tgt.startsWith("grand exchange");
		}
		return false;
	}

	private static boolean isGrandExchangeAction(String opt, String tgt)
	{
		switch (opt)
		{
			case "exchange":
			case "grand exchange":
			case "collect":
				return true;
			default:
				break;
		}
		return tgt.contains("grand exchange") || tgt.contains("ge clerk");
	}

	private static boolean isPrayerAction(String opt, String tgt)
	{
		if ("activate".equals(opt) || "deactivate".equals(opt) || "select".equals(opt))
		{
			return tgt.contains("prayer")
				|| tgt.contains("protect from")
				|| tgt.contains("piety")
				|| tgt.contains("rigour")
				|| tgt.contains("augury");
		}
		return false;
	}

	private static boolean isPotionAction(String opt, String tgt)
	{
		if (!"drink".equals(opt) && !"sip".equals(opt))
		{
			return false;
		}
		return tgt.contains("potion")
			|| tgt.contains("brew")
			|| tgt.contains("restore")
			|| tgt.contains("antipoison")
			|| tgt.contains("serum");
	}

	private static boolean isGroundPickup(String opt)
	{
		return "take".equals(opt) || "pick-up".equals(opt) || "pick up".equals(opt);
	}

	private static boolean isWalkAction(String opt)
	{
		return "walk here".equals(opt) || "walk".equals(opt);
	}
}
