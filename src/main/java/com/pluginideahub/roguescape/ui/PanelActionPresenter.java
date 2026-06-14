package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.ui.PanelAction;

/**
 * Presentation rules for a {@link PanelAction}: its button label and semantic button role. Kept
 * separate from the side panel so the mapping is testable and reusable.
 */
public final class PanelActionPresenter
{
	private PanelActionPresenter() {}

	public static String label(PanelAction action)
	{
		switch (action)
		{
			case START_RUN:      return "▶ START RUN";
			case RESET_RUN:      return "↻ Reset Run";
			case COMPLETE_STAGE: return "✓ Complete Stage";
			case CHOOSE_REWARD:
			case CHOOSE_REWARD_1: return "✦ Reward 1";
			case CHOOSE_REWARD_2: return "✦ Reward 2";
			case CHOOSE_REWARD_3: return "✦ Reward 3";
			case SKIP_REWARD:    return "⟲ Skip Reward";
			case NEXT_STAGE:     return "▶ Continue";
			case FAIL_RUN:       return "✗ End Run";
			default:             return action.name();
		}
	}

	/** Maps an action to its semantic button role (matches the asset sheet's button states). */
	public static RogueScapeTheme.ButtonRole roleFor(PanelAction action, boolean primary)
	{
		switch (action)
		{
			case START_RUN:
				return RogueScapeTheme.ButtonRole.GO;
			case FAIL_RUN:
				return RogueScapeTheme.ButtonRole.DANGER;
			case DEV_COMPLETE_STAGE:
				return RogueScapeTheme.ButtonRole.NEUTRAL;
			case COMPLETE_STAGE:
			case NEXT_STAGE:
			case CHOOSE_REWARD:
			case CHOOSE_REWARD_1:
			case CHOOSE_REWARD_2:
			case CHOOSE_REWARD_3:
				return RogueScapeTheme.ButtonRole.PRIMARY;
			default:
				return primary ? RogueScapeTheme.ButtonRole.PRIMARY : RogueScapeTheme.ButtonRole.NEUTRAL;
		}
	}
}
