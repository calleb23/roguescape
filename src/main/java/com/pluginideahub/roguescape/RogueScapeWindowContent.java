package com.pluginideahub.roguescape;

import com.google.inject.Provides;
import com.pluginideahub.roguescape.core.BossKillChatMatcher;
import com.pluginideahub.roguescape.core.ModePresetParser;
import com.pluginideahub.roguescape.core.RogueScapePrototype;
import com.pluginideahub.roguescape.core.RogueScapeCustomRunFactory;
import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunCompletionReason;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPhase;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.briefing.RunBriefing;
import com.pluginideahub.roguescape.core.briefing.RunBriefingBuilder;
import com.pluginideahub.roguescape.core.adapter.InventoryDiff;
import com.pluginideahub.roguescape.bridge.ShortestPathBridge;
import com.pluginideahub.roguescape.core.adapter.ProvenanceSignalTracker;
import com.pluginideahub.roguescape.core.recap.RecapExport;
import com.pluginideahub.roguescape.core.recap.RunHistory;
import com.pluginideahub.roguescape.core.recap.RunRecap;
import com.pluginideahub.roguescape.core.reward.BankItem;
import com.pluginideahub.roguescape.core.reward.BankItemClassifier;
import com.pluginideahub.roguescape.core.reward.ValueTier;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementDecision;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementEvaluator;
import com.pluginideahub.roguescape.core.enforcement.RogueScapeEnforcementRules;
import com.pluginideahub.roguescape.core.item.InventorySnapshot;
import com.pluginideahub.roguescape.core.item.ItemDelta;
import com.pluginideahub.roguescape.core.item.ProvenanceHint;
import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.StageRegionRule;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicEffect;
import com.pluginideahub.roguescape.core.relic.RelicEffectKind;
import com.pluginideahub.roguescape.core.reward.BankItemCategory;
import com.pluginideahub.roguescape.core.reward.RewardDraft;
import com.pluginideahub.roguescape.core.reward.RewardOption;
import com.pluginideahub.roguescape.core.ui.OverlayViewModel;
import com.pluginideahub.roguescape.core.ui.PanelAction;
import com.pluginideahub.roguescape.core.ui.PanelTab;
import com.pluginideahub.roguescape.core.ui.SidePanelViewModel;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomEditorState;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomWorldMapOverlay;
import com.pluginideahub.roguescape.ui.RogueScapeCustomBuilderWidgetWindow;
import com.pluginideahub.roguescape.ui.RogueScapeJournalTabAdapter;
import com.pluginideahub.roguescape.ui.RogueScapeJournalWidgetProbe;
import com.pluginideahub.roguescape.ui.RogueScapeActiveRoomWorldMapOverlay;
import com.pluginideahub.roguescape.ui.RogueScapeObjectiveOverlay;
import com.pluginideahub.roguescape.ui.RogueScapeRoomMaskOverlay;
import com.pluginideahub.roguescape.ui.RogueScapeIcons;
import com.pluginideahub.roguescape.ui.RogueScapeRewardOverlay;
import com.pluginideahub.roguescape.ui.RogueScapeTheme;
import com.pluginideahub.roguescape.ui.RogueScapeWidgetWindow;
import com.pluginideahub.roguescape.ui.JournalSpreadBlocks;
import com.pluginideahub.roguescape.ui.RogueScapeWindowOverlay;
import com.pluginideahub.ui.PluginIdeaHubOverlay;
import com.pluginideahub.ui.PluginIdeaHubUiModel;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content provider for RogueScape's in-game window surfaces: the tabbed window overlay
 * (run builder, live run, build, artifacts, modifiers, progression), the custom-builder
 * widget window, and the reward overlay's cards/rail. Extracted from {@link RogueScapePlugin}
 * so the plugin class stays event wiring + run lifecycle; this class is pure view-building
 * and action routing back into the plugin.
 */
final class RogueScapeWindowContent
{
	private final RogueScapePlugin plugin;

	RogueScapeWindowContent(RogueScapePlugin plugin)
	{
		this.plugin = plugin;
	}

	List<RogueScapeWindowOverlay.Tab> windowTabs()
	{
		List<RogueScapeWindowOverlay.Tab> tabs = new ArrayList<>();
		if (plugin.rogueRun == null || plugin.runSession == null || plugin.runLoop == null)
		{
			List<RogueScapeWindowOverlay.Block> lobby = new ArrayList<>();
			RunMode mode = plugin.panel != null ? plugin.panel.selectedMode() : ModePresetParser.parseMode(plugin.config.modePreset());
			RunPreset preset = plugin.panel != null ? plugin.panel.selectedPreset() : RunPreset.UNSPECIFIED;
			String runTitle = plugin.panel != null && plugin.panel.selectedGoal() != null && !plugin.panel.selectedGoal().trim().isEmpty()
				? plugin.panel.selectedGoal().trim()
				: RogueScapePlugin.runModeName(mode);
			String seed = plugin.panel != null && plugin.panel.selectedSeed() != null && !plugin.panel.selectedSeed().trim().isEmpty()
				? plugin.panel.selectedSeed().trim()
				: plugin.config.seedText();
			if (mode == RunMode.CUSTOM_CREATOR)
			{
				// Custom mode: the window becomes the route builder (mirrors the widget window).
				return customBuilderTabs();
			}
			lobby.add(RogueScapeWindowOverlay.Block.heading("Pick Your Contract"));
			lobby.add(RogueScapeWindowOverlay.Block.modeTiles(lobbyModeTiles(mode, seed)));
			lobby.add(RogueScapeWindowOverlay.Block.gap());
			lobby.add(RogueScapeWindowOverlay.Block.badge("Selected: " + RogueScapePlugin.runModeName(mode), startLine(mode, preset, seed),
				RogueScapeTheme.BANNER, 0));
			lobby.add(RogueScapeWindowOverlay.Block.text("Run: " + runTitle, RogueScapeTheme.TEXT_PRIMARY));
			lobby.add(RogueScapeWindowOverlay.Block.modeTiles(startRunTile(mode)));
			lobby.add(RogueScapeWindowOverlay.Block.gap());
			addBriefingBlocks(lobby, mode, preset, seed);
			tabs.add(new RogueScapeWindowOverlay.Tab("CONTRACT", lobby));

			List<RogueScapeWindowOverlay.Block> rules = new ArrayList<>();
			rules.add(RogueScapeWindowOverlay.Block.heading("The Rules"));
			for (String line : plugin.formatRules().split("\\n"))
			{
				rules.add(RogueScapeWindowOverlay.Block.text("- " + line, RogueScapeTheme.TEXT_PRIMARY));
			}
			tabs.add(new RogueScapeWindowOverlay.Tab("THE RULES", rules));
			return tabs;
		}

		SidePanelViewModel vm = SidePanelViewModel.active(plugin.runLoop, PanelTab.RUN);

		// The in-game window is the open-book run spread (RogueScapeWindowOverlay#setBookMode turns
		// on the book chrome for it). Actions, curses, and the tally live on the side panel, keeping
		// the book bare-bones per the journal design; liveRunBlocks also handles the reward page.
		tabs.add(new RogueScapeWindowOverlay.Tab("THE ENTRY", liveRunBlocks(vm)));
		return tabs;
	}

	/** True when the window is showing a live run — rendered as the open-book two-page spread. */
	boolean isRunSpread()
	{
		return plugin.rogueRun != null && plugin.runSession != null && plugin.runLoop != null;
	}

	private static void addActionTile(List<RogueScapeWindowOverlay.ModeTile> tiles, SidePanelViewModel vm,
		PanelAction action, String title, String subtitle, java.awt.Color color)
	{
		if (vm.isActionEnabled(action))
		{
			tiles.add(new RogueScapeWindowOverlay.ModeTile(title, subtitle, "", color, false, "action-" + action.name()));
		}
	}

	private static List<RogueScapeWindowOverlay.ModeTile> lobbyModeTiles(RunMode selectedMode, String seed)
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Scavenger", "Earn power, room by room.",
			"Fresh source", RogueScapeTheme.POSITIVE, selectedMode == RunMode.FRESH_SOURCE || selectedMode == RunMode.UNSPECIFIED,
			"scavenger"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Rewarded", "Short prep, boss loot.",
			"Campaign", RogueScapeTheme.RARITY_LEGENDARY, selectedMode == RunMode.BANK_DRAFT,
			"rewarded"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Custom", "Draw your own route.",
			"Open builder", RogueScapeTheme.RARITY_EPIC, selectedMode == RunMode.CUSTOM_CREATOR,
			"custom"));
		return tiles;
	}

	void selectRunBuilderMode(String actionId)
	{
		if (actionId != null && actionId.startsWith("action-"))
		{
			try
			{
				plugin.dispatchAction(PanelAction.valueOf(actionId.substring("action-".length())));
			}
			catch (IllegalArgumentException ignored)
			{
			}
			return;
		}
		if (plugin.panel == null || plugin.runSession != null || plugin.rogueRun != null)
		{
			return;
		}
		if ("start-run".equals(actionId))
		{
			plugin.dispatchAction(PanelAction.START_RUN);
			return;
		}
		plugin.panel.selectRunBuilderMode(actionId);
		if ("custom".equals(actionId))
		{
			if (plugin.customBuilderWidgetWindow != null)
			{
				plugin.customBuilderWidgetWindow.setOpen(true);
			}
		}
		plugin.refreshSidePanel();
	}

	private List<RogueScapeWindowOverlay.ModeTile> startRunTile(RunMode mode)
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("BEGIN THE RUN",
			mode == RunMode.CUSTOM_CREATOR ? "Sign and stamp the custom route" : "Sign and stamp the contract",
			"The route is drawn", RogueScapeTheme.POSITIVE, false, "start-run"));
		return tiles;
	}



	private static String startLine(RunMode mode, RunPreset preset, String seed)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Preset ");
		sb.append(preset == null || preset == RunPreset.UNSPECIFIED ? "Auto" : preset.name());
		if (mode == RunMode.SEEDED_RACE)
		{
			sb.append(" | Seed ");
			sb.append(seed == null || seed.trim().isEmpty() ? "(random)" : seed.trim());
		}
		return sb.toString();
	}

	/**
	 * Renders the pre-run briefing: the exact route the chosen mode will build, room by room, with
	 * each room's clear condition spelled out, plus the run-wide rules and win/lose conditions. The
	 * briefing is generated from a real preview run (via {@link RunBriefingBuilder}) using the same
	 * loadout/bank settings {@code startRun} applies, so what the player reads here is what they
	 * actually get when they Begin.
	 */
	private void addBriefingBlocks(List<RogueScapeWindowOverlay.Block> out, RunMode mode, RunPreset preset, String seed)
	{
		String loadout = plugin.panel != null ? plugin.panel.customBuilderLoadout() : "Naked";
		RunBriefing briefing;
		try
		{
			briefing = RunBriefingBuilder.preview(mode, preset, seed, loadout,
				plugin.config.bankAccessAllowed(), 0);
		}
		catch (RuntimeException ex)
		{
			out.add(RogueScapeWindowOverlay.Block.text("Could not preview this route: " + ex.getMessage(),
				RogueScapeTheme.NEGATIVE));
			return;
		}

		out.add(RogueScapeWindowOverlay.Block.text(briefing.modeSummary(), RogueScapeTheme.TEXT_MUTED));
		out.add(RogueScapeWindowOverlay.Block.gap());

		String routeHeading = "THE ROUTE — " + briefing.roomCount() + " ROOMS"
			+ (briefing.bossCount() > 0 ? " + BOSS" : "");
		out.add(RogueScapeWindowOverlay.Block.heading(routeHeading));
		if (!briefing.routeLocked())
		{
			out.add(RogueScapeWindowOverlay.Block.text("Rooms re-roll on Begin. Set a seed to lock this exact route.",
				RogueScapeTheme.TEXT_MUTED));
		}
		for (RunBriefing.RoomLine room : briefing.rooms())
		{
			out.add(RogueScapeWindowOverlay.Block.text(room.index() + ". " + room.name() + "  [" + room.kindLabel() + "]",
				room.bossStage() ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.GOLD));
			out.add(RogueScapeWindowOverlay.Block.text("     " + room.collectLabel(), RogueScapeTheme.TEXT_PRIMARY));
			out.add(RogueScapeWindowOverlay.Block.text("     Clear by: " + room.gatingLabel(), RogueScapeTheme.TEXT_MUTED));
		}

		out.add(RogueScapeWindowOverlay.Block.gap());
		out.add(RogueScapeWindowOverlay.Block.heading("THE RULES"));
		out.add(RogueScapeWindowOverlay.Block.text("Loadout: " + briefing.loadoutLabel(), RogueScapeTheme.TEXT_PRIMARY));
		out.add(RogueScapeWindowOverlay.Block.text(briefing.bankAccessLabel(), RogueScapeTheme.TEXT_PRIMARY));
		out.add(RogueScapeWindowOverlay.Block.text(briefing.timeModelLabel(), RogueScapeTheme.TEXT_PRIMARY));
		out.add(RogueScapeWindowOverlay.Block.text("Seed: " + briefing.seedLabel(), RogueScapeTheme.TEXT_MUTED));

		out.add(RogueScapeWindowOverlay.Block.gap());
		out.add(RogueScapeWindowOverlay.Block.text("WIN: " + briefing.winCondition(), RogueScapeTheme.POSITIVE));
		for (String lose : briefing.loseConditions())
		{
			out.add(RogueScapeWindowOverlay.Block.text("LOSE: " + lose, RogueScapeTheme.NEGATIVE));
		}
	}

	private List<RogueScapeWindowOverlay.Tab> customBuilderTabs()
	{
		List<RogueScapeWindowOverlay.Tab> tabs = new ArrayList<>();

		List<RogueScapeWindowOverlay.Block> mode = new ArrayList<>();
		mode.add(RogueScapeWindowOverlay.Block.heading("CUSTOM MODE"));
		mode.add(RogueScapeWindowOverlay.Block.modeTiles(customModeActions()));
		mode.add(RogueScapeWindowOverlay.Block.gap());
		mode.add(RogueScapeWindowOverlay.Block.badge("Selected: " + customGameModeLabel(),
			"Custom controls stay in this separate window", RogueScapeTheme.RARITY_EPIC, 0));
		mode.add(RogueScapeWindowOverlay.Block.text("Reward rules follow the normal mode rules. Custom only changes route, room allowances, bosses, loadout, and constraints.",
			RogueScapeTheme.TEXT_PRIMARY));
		tabs.add(new RogueScapeWindowOverlay.Tab("MODE", mode));

		List<RogueScapeWindowOverlay.Block> loadout = new ArrayList<>();
		loadout.add(RogueScapeWindowOverlay.Block.heading("STARTING LOADOUT"));
		loadout.add(RogueScapeWindowOverlay.Block.modeTiles(customLoadoutActions()));
		loadout.add(RogueScapeWindowOverlay.Block.gap());
		loadout.add(RogueScapeWindowOverlay.Block.text("Selected: " + customLoadoutLabel(), RogueScapeTheme.GOLD));
		tabs.add(new RogueScapeWindowOverlay.Tab("LOADOUT", loadout));

		List<RogueScapeWindowOverlay.Block> roomsTab = new ArrayList<>();
		roomsTab.add(RogueScapeWindowOverlay.Block.heading("ROOM SELECTOR"));
		roomsTab.add(RogueScapeWindowOverlay.Block.badge("Room", plugin.panel == null ? "(none)" : plugin.panel.customSelectedRoomLabel(),
			RogueScapeTheme.INFO, 0));
		roomsTab.add(RogueScapeWindowOverlay.Block.badge("Allowed Collection", plugin.panel == null ? "All" : plugin.panel.customSelectedAllowanceLabel(),
			RogueScapeTheme.RARITY_EPIC, 0));
		roomsTab.add(RogueScapeWindowOverlay.Block.modeTiles(customRoomSelectorActions()));
		roomsTab.add(RogueScapeWindowOverlay.Block.gap());
		addConfirmedRouteBlocks(roomsTab);
		tabs.add(new RogueScapeWindowOverlay.Tab("ROOMS", roomsTab));

		List<RogueScapeWindowOverlay.Block> bosses = new ArrayList<>();
		bosses.add(RogueScapeWindowOverlay.Block.heading("BOSS SELECTOR"));
		bosses.add(RogueScapeWindowOverlay.Block.badge("Boss", plugin.panel == null ? "(auto)" : plugin.panel.customSelectedBossLabel(),
			RogueScapeTheme.NEGATIVE, 0));
		bosses.add(RogueScapeWindowOverlay.Block.modeTiles(customBossSelectorActions()));
		bosses.add(RogueScapeWindowOverlay.Block.gap());
		bosses.add(RogueScapeWindowOverlay.Block.text("End boss: " + customBossLabel(), RogueScapeTheme.GOLD));
		tabs.add(new RogueScapeWindowOverlay.Tab("BOSSES", bosses));

		List<RogueScapeWindowOverlay.Block> route = new ArrayList<>();
		route.add(RogueScapeWindowOverlay.Block.heading("ROUTE ORDER"));
		route.add(RogueScapeWindowOverlay.Block.modeTiles(customRouteOrderActions()));
		route.add(RogueScapeWindowOverlay.Block.gap());
		addConfirmedRouteBlocks(route);
		route.add(RogueScapeWindowOverlay.Block.gap());
		route.add(RogueScapeWindowOverlay.Block.text("End boss: " + customBossLabel(), RogueScapeTheme.GOLD));
		tabs.add(new RogueScapeWindowOverlay.Tab("ROUTE", route));

		List<RogueScapeWindowOverlay.Block> zones = new ArrayList<>();
		zones.add(RogueScapeWindowOverlay.Block.heading("CUSTOM ZONE"));
		zones.add(RogueScapeWindowOverlay.Block.modeTiles(customZoneActions()));
		zones.add(RogueScapeWindowOverlay.Block.gap());
		if (plugin.customRoomEditorState == null || plugin.customRoomEditorState.selection().isEmpty())
		{
			zones.add(RogueScapeWindowOverlay.Block.text("No custom zone saved yet.", RogueScapeTheme.TEXT_MUTED));
			zones.add(RogueScapeWindowOverlay.Block.text("Use the world-map zone builder to paint region IDs for this run.",
				RogueScapeTheme.TEXT_PRIMARY));
		}
		else
		{
			zones.add(RogueScapeWindowOverlay.Block.badge(plugin.customRoomEditorState.selection().getName(),
				plugin.customRoomEditorState.selection().size() + " region ids selected", RogueScapeTheme.INFO, 0));
			zones.add(RogueScapeWindowOverlay.Block.text(plugin.customRoomEditorState.selection().toCsv(), RogueScapeTheme.TEXT_MUTED));
		}
		tabs.add(new RogueScapeWindowOverlay.Tab("ZONES", zones));

		List<RogueScapeWindowOverlay.Block> modBlocks = new ArrayList<>();
		modBlocks.add(RogueScapeWindowOverlay.Block.heading("STARTING MODIFIERS"));
		List<String> selectedMods = plugin.panel == null ? Collections.emptyList() : plugin.panel.selectedModifierIds();
		if (selectedMods.isEmpty())
		{
			modBlocks.add(RogueScapeWindowOverlay.Block.text("No starting modifiers selected.", RogueScapeTheme.TEXT_MUTED));
		}
		else
		{
			for (String id : selectedMods)
			{
				modBlocks.add(RogueScapeWindowOverlay.Block.badge(id, "Applied when the custom run starts",
					RogueScapeTheme.NEGATIVE, 0));
			}
		}
		tabs.add(new RogueScapeWindowOverlay.Tab("MODIFIERS", modBlocks));

		List<RogueScapeWindowOverlay.Block> seed = new ArrayList<>();
		seed.add(RogueScapeWindowOverlay.Block.heading("CUSTOM SEED"));
		seed.add(RogueScapeWindowOverlay.Block.text("This is the shape of the share code: mode, loadout, rooms, allowances, boss, and modifiers.",
			RogueScapeTheme.TEXT_PRIMARY));
		seed.add(RogueScapeWindowOverlay.Block.gap());
		seed.add(RogueScapeWindowOverlay.Block.badge("Seed Preview", "share code below",
			RogueScapeTheme.BANNER, 0));
		String code = plugin.panel == null ? "mode=Scavenger;loadout=Naked" : plugin.panel.customSeedPreview();
		for (String segment : code.split(";"))
		{
			if (!segment.isEmpty())
			{
				seed.add(RogueScapeWindowOverlay.Block.text("  " + segment, RogueScapeTheme.TEXT_PRIMARY));
			}
		}
		tabs.add(new RogueScapeWindowOverlay.Tab("SEED", seed));

		return tabs;
	}

	RogueScapeCustomBuilderWidgetWindow.View customBuilderWidgetView()
	{
		if (plugin.panel == null)
		{
			return RogueScapeCustomBuilderWidgetWindow.View.empty();
		}
		return new RogueScapeCustomBuilderWidgetWindow.View(
			plugin.panel.customBuilderGameMode(),
			plugin.panel.customBuilderLoadout(),
			RogueScapeCustomRunFactory.loadoutKitLines(plugin.panel.customBuilderLoadout()),
			plugin.panel.customBankUnlocks(),
			plugin.panel.customTimeLimitMinutes(),
			plugin.panel.customBossLimit(),
			plugin.panel.customSelectedRoomLabel(),
			plugin.panel.customSelectedAllowanceLabel(),
			plugin.panel.customSelectedBossLabel(),
			plugin.panel.customRoomOptionLabels(),
			plugin.panel.customAllowanceOptionLabels(),
			plugin.panel.customBossOptionLabels(),
			plugin.panel.customModifierOptionLabels(),
			plugin.panel.selectedModifierLabels(),
			plugin.panel.selectedModifierIndexes(),
			plugin.panel.customModifierPageStart(),
			plugin.panel.customSelectedRoomIndex(),
			plugin.panel.customSelectedAllowanceIndex(),
			plugin.panel.customSelectedBossIndex(),
			plugin.panel.customRouteLabels(),
			plugin.panel.selectedRouteIndex(),
			plugin.panel.customSeedPreview()
		);
	}

	private void addConfirmedRouteBlocks(List<RogueScapeWindowOverlay.Block> out)
	{
		List<String> rooms = plugin.panel == null ? Collections.emptyList() : plugin.panel.selectedRoomLabels();
		if (rooms.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.text("Confirmed route is empty. Add rooms, then add a boss.",
				RogueScapeTheme.TEXT_MUTED));
		}
		else
		{
			int i = 0;
			int selectedIdx = plugin.panel == null ? -1 : plugin.panel.selectedRouteIndex();
			for (String row : rooms)
			{
				out.add(RogueScapeWindowOverlay.Block.text((i == selectedIdx ? "> " : "  ") + (i + 1) + ". " + row,
					i == selectedIdx ? RogueScapeTheme.GOLD : RogueScapeTheme.TEXT_PRIMARY));
				i++;
			}
		}
	}

	private String customBossLabel()
	{
		if (plugin.panel == null)
		{
			return "(auto)";
		}
		String boss = plugin.panel.selectedBossId();
		return boss == null || boss.isEmpty() ? "(auto)" : boss;
	}

	private String customGameModeLabel()
	{
		return plugin.panel == null ? "Scavenger" : plugin.panel.customBuilderGameMode();
	}

	private String customLoadoutLabel()
	{
		return plugin.panel == null ? "Naked" : plugin.panel.customBuilderLoadout();
	}

	private List<RogueScapeWindowOverlay.ModeTile> customModeActions()
	{
		String selected = customGameModeLabel();
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Scavenger", "Rooms define what can be collected.",
			"Room-first", RogueScapeTheme.POSITIVE, "Scavenger".equals(selected), "custom:mode-scavenger"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Rewarded", "Bosses and rewards carry the run.",
			"Boss-first", RogueScapeTheme.RARITY_LEGENDARY, "Rewarded".equals(selected), "custom:mode-rewarded"));
		return tiles;
	}

	private List<RogueScapeWindowOverlay.ModeTile> customLoadoutActions()
	{
		String selected = customLoadoutLabel();
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Naked", "Start with nothing.", "Hard", RogueScapeTheme.NEGATIVE,
			"Naked".equals(selected), "custom:loadout-naked"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Low Gear", "Minimal starter setup.", "Normal", RogueScapeTheme.INFO,
			"Low Gear".equals(selected), "custom:loadout-low"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Mid Gear", "More accessible starting point.", "Easy", RogueScapeTheme.POSITIVE,
			"Mid Gear".equals(selected), "custom:loadout-mid"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Custom Kit", "Sword, shortbow, arrows, and food.", "Hybrid", RogueScapeTheme.RARITY_EPIC,
			"Custom Kit".equals(selected), "custom:loadout-custom"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customRoomAllowanceActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Supply", "Food, potions, ammo, and utility.", "Add room", RogueScapeTheme.INFO, "custom:add-supply"));
		tiles.add(customActionTile("Armour", "Armour upgrades are legal here.", "Add room", RogueScapeTheme.RARITY_RARE, "custom:add-armour"));
		tiles.add(customActionTile("Weapons", "Weapon upgrades are legal here.", "Add room", RogueScapeTheme.POSITIVE, "custom:add-weapons"));
		tiles.add(customActionTile("Skilling", "Gathered resources are legal here.", "Add room", RogueScapeTheme.RARITY_EPIC, "custom:add-skilling"));
		tiles.add(customActionTile("All", "Any local legal gain counts.", "Add room", RogueScapeTheme.GOLD, "custom:add-all"));
		tiles.add(customActionTile("Shopping", "Shop purchases are legal here.", "Add room", RogueScapeTheme.RARITY_LEGENDARY, "custom:add-shopping"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customRoomSelectorActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Prev Room", "Cycle selected room backward.", "Room", RogueScapeTheme.TEXT_MUTED, "custom:room-prev"));
		tiles.add(customActionTile("Next Room", "Cycle selected room forward.", "Room", RogueScapeTheme.INFO, "custom:room-next"));
		tiles.add(customActionTile("Prev Type", "Cycle allowed collection backward.", "Type", RogueScapeTheme.TEXT_MUTED, "custom:type-prev"));
		tiles.add(customActionTile("Next Type", "Cycle allowed collection forward.", "Type", RogueScapeTheme.RARITY_EPIC, "custom:type-next"));
		tiles.add(customActionTile("Add Room", "Confirm room into the route.", "Confirm", RogueScapeTheme.POSITIVE, "custom:add-selected-room"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customBossActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Default Boss", "Pick the first boss from the pool.", "Set boss", RogueScapeTheme.NEGATIVE, "custom:first-boss"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customBossSelectorActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Prev Boss", "Cycle boss backward.", "Boss", RogueScapeTheme.TEXT_MUTED, "custom:boss-prev"));
		tiles.add(customActionTile("Next Boss", "Cycle boss forward.", "Boss", RogueScapeTheme.NEGATIVE, "custom:boss-next"));
		tiles.add(customActionTile("Add Boss", "Confirm boss for the route.", "Confirm", RogueScapeTheme.POSITIVE, "custom:add-selected-boss"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customRouteOrderActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Select Up", "Move route cursor up.", "Cursor", RogueScapeTheme.TEXT_MUTED, "custom:select-up"));
		tiles.add(customActionTile("Select Down", "Move route cursor down.", "Cursor", RogueScapeTheme.TEXT_MUTED, "custom:select-down"));
		tiles.add(customActionTile("Move Up", "Move selected route row up.", "Reorder", RogueScapeTheme.INFO, "custom:move-up"));
		tiles.add(customActionTile("Move Down", "Move selected route row down.", "Reorder", RogueScapeTheme.INFO, "custom:move-down"));
		tiles.add(customActionTile("Remove", "Remove selected route row.", "Delete", RogueScapeTheme.NEGATIVE, "custom:remove-room"));
		tiles.add(customActionTile("Clear", "Reset custom route choices.", "Route only", RogueScapeTheme.TEXT_MUTED, "custom:clear-route"));
		return tiles;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> customZoneActions()
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(customActionTile("Edit Map", "Open world-map region painting.", "Toggle", RogueScapeTheme.ACCENT, "custom:edit-zone"));
		tiles.add(customActionTile("Use Zone", "Apply saved zone to current run.", "Activate", RogueScapeTheme.POSITIVE, "custom:use-zone"));
		return tiles;
	}

	private static RogueScapeWindowOverlay.ModeTile customActionTile(String title, String subtitle,
		String detail, java.awt.Color color, String action)
	{
		return new RogueScapeWindowOverlay.ModeTile(title, subtitle, detail, color, false, action);
	}

	void handleCustomBuilderAction(String actionId)
	{
		if (plugin.panel == null || actionId == null || plugin.runSession != null || plugin.rogueRun != null)
		{
			return;
		}
		if ("custom:mode-scavenger".equals(actionId))
		{
			plugin.panel.setCustomBuilderGameMode("Scavenger");
		}
		else if ("custom:mode-rewarded".equals(actionId))
		{
			plugin.panel.setCustomBuilderGameMode("Rewarded");
		}
		else if ("custom:loadout-naked".equals(actionId))
		{
			plugin.panel.setCustomBuilderLoadout("Naked");
		}
		else if ("custom:loadout-low".equals(actionId))
		{
			plugin.panel.setCustomBuilderLoadout("Low Gear");
		}
		else if ("custom:loadout-mid".equals(actionId))
		{
			plugin.panel.setCustomBuilderLoadout("Mid Gear");
		}
		else if ("custom:loadout-custom".equals(actionId))
		{
			plugin.panel.setCustomBuilderLoadout("Custom Kit");
		}
		else if ("custom:add-all".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("All");
		}
		else if ("custom:add-supply".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("Supply");
		}
		else if ("custom:add-armour".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("Armour");
		}
		else if ("custom:add-weapons".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("Weapons");
		}
		else if ("custom:add-skilling".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("Skilling");
		}
		else if ("custom:add-shopping".equals(actionId))
		{
			plugin.panel.addRoomForAllowance("Shopping");
		}
		else if ("custom:room-prev".equals(actionId))
		{
			plugin.panel.customPreviousRoom();
		}
		else if ("custom:room-next".equals(actionId))
		{
			plugin.panel.customNextRoom();
		}
		else if (actionId.startsWith("custom:room-index:"))
		{
			plugin.panel.selectCustomRoomIndex(parseTrailingInt(actionId));
		}
		else if ("custom:room-page-up".equals(actionId))
		{
			plugin.panel.pageCustomRoomIndex(-10);
		}
		else if ("custom:room-page-down".equals(actionId))
		{
			plugin.panel.pageCustomRoomIndex(10);
		}
		else if ("custom:type-prev".equals(actionId))
		{
			plugin.panel.customPreviousAllowance();
		}
		else if ("custom:type-next".equals(actionId))
		{
			plugin.panel.customNextAllowance();
		}
		else if (actionId.startsWith("custom:type-index:"))
		{
			plugin.panel.selectCustomAllowanceIndex(parseTrailingInt(actionId));
		}
		else if ("custom:add-selected-room".equals(actionId))
		{
			plugin.panel.addSelectedCustomRoom();
		}
		else if ("custom:boss-prev".equals(actionId))
		{
			plugin.panel.customPreviousBoss();
		}
		else if ("custom:boss-next".equals(actionId))
		{
			plugin.panel.customNextBoss();
		}
		else if (actionId.startsWith("custom:boss-index:"))
		{
			plugin.panel.selectCustomBossIndex(parseTrailingInt(actionId));
		}
		else if ("custom:boss-page-up".equals(actionId))
		{
			plugin.panel.pageCustomBossIndex(-10);
		}
		else if ("custom:boss-page-down".equals(actionId))
		{
			plugin.panel.pageCustomBossIndex(10);
		}
		else if ("custom:add-selected-boss".equals(actionId))
		{
			plugin.panel.addSelectedCustomBoss();
		}
		else if ("custom:select-up".equals(actionId))
		{
			plugin.panel.selectPreviousRouteRow();
		}
		else if ("custom:select-down".equals(actionId))
		{
			plugin.panel.selectNextRouteRow();
		}
		else if (actionId.startsWith("custom:route-index:"))
		{
			plugin.panel.selectRouteRow(parseTrailingInt(actionId));
		}
		else if ("custom:move-up".equals(actionId))
		{
			plugin.panel.moveSelectedRouteUp();
		}
		else if ("custom:move-down".equals(actionId))
		{
			plugin.panel.moveSelectedRouteDown();
		}
		else if ("custom:remove-room".equals(actionId))
		{
			plugin.panel.removeLastCustomRoom();
		}
		else if ("custom:clear-route".equals(actionId))
		{
			plugin.panel.clearCustomRoute();
		}
		else if (actionId.startsWith("custom:modifier-index:"))
		{
			plugin.panel.toggleCustomModifierIndex(parseTrailingInt(actionId));
		}
		else if ("custom:modifier-page-up".equals(actionId))
		{
			plugin.panel.pageCustomModifierIndex(-10);
		}
		else if ("custom:modifier-page-down".equals(actionId))
		{
			plugin.panel.pageCustomModifierIndex(10);
		}
		else if ("custom:clear-modifiers".equals(actionId))
		{
			plugin.panel.clearCustomModifiers();
		}
		else if ("custom:toggle-bank".equals(actionId))
		{
			plugin.panel.toggleCustomBankUnlocks();
		}
		else if ("custom:cycle-time".equals(actionId))
		{
			plugin.panel.cycleCustomTimeLimit();
		}
		else if ("custom:cycle-boss-limit".equals(actionId))
		{
			plugin.panel.cycleCustomBossLimit();
		}
		else if ("custom:load-seed".equals(actionId))
		{
			plugin.panel.applyCustomSeed(plugin.panel.selectedSeed());
		}
		else if ("custom:start-run".equals(actionId))
		{
			plugin.panel.selectRunBuilderMode("custom");
			if (plugin.customBuilderWidgetWindow != null)
			{
				plugin.customBuilderWidgetWindow.setOpen(false);
			}
			plugin.startRunOnClientThread();
			return;
		}
		else if ("custom:first-boss".equals(actionId))
		{
			plugin.panel.selectFirstBoss();
		}
		else if ("custom:edit-zone".equals(actionId) && plugin.customRoomEditorState != null)
		{
			plugin.customRoomEditorState.setEditing(!plugin.customRoomEditorState.isEditing());
		}
		else if ("custom:use-zone".equals(actionId))
		{
			plugin.activateCustomRoomForRun();
		}
		plugin.refreshSidePanel();
	}

	private static int parseTrailingInt(String value)
	{
		if (value == null)
		{
			return 0;
		}
		int idx = value.lastIndexOf(':');
		if (idx < 0 || idx >= value.length() - 1)
		{
			return 0;
		}
		try
		{
			return Integer.parseInt(value.substring(idx + 1));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	/**
	 * LIVE RUN as the journal's diary-entry page (mockup concept-journal-live.png): the
	 * current chapter as a masthead with the ribbon bookmark, the entry + the room's rules
	 * down the left column, THE RECORD and the hourglass down the right. A pending reward
	 * still takes over the page (the cards need the height).
	 */
	private List<RogueScapeWindowOverlay.Block> liveRunBlocks(SidePanelViewModel vm)
	{
		List<RogueScapeWindowOverlay.Block> live = new ArrayList<>();

		List<RogueScapeRewardOverlay.Card> pendingCards = pendingRewardCards();
		if (pendingCards != null)
		{
			if (plugin.runLoop != null)
			{
				live.add(RogueScapeWindowOverlay.Block.pageTitle("The chest opens",
					plugin.runLoop.phase().getDisplayName() + " — " + plugin.runLoop.runElapsedLabel()));
			}
			live.add(RogueScapeWindowOverlay.Block.heading("The Chest Opens"));
			live.add(RogueScapeWindowOverlay.Block.cards(pendingCards));
			return live;
		}

		// The Run spread (masthead + two columns) is now a tested pure-core contract on the view
		// model; this layer only paints it. See core.ui.SidePanelViewModel#runSpread.
		live.addAll(JournalSpreadBlocks.render(vm.runSpread()));

		if (!plugin.latestProvenanceSignal.isEmpty())
		{
			live.add(RogueScapeWindowOverlay.Block.text("Noted: " + plugin.latestProvenanceSignal,
				RogueScapeTheme.TEXT_MUTED));
		}
		return live;
	}

	/** A simple lobby placeholder tab: a heading plus one muted explanatory line. */
	private static List<RogueScapeWindowOverlay.Block> placeholderTab(String heading, String message)
	{
		List<RogueScapeWindowOverlay.Block> blocks = new ArrayList<>();
		blocks.add(RogueScapeWindowOverlay.Block.heading(heading));
		blocks.add(RogueScapeWindowOverlay.Block.text(message, RogueScapeTheme.TEXT_MUTED));
		return blocks;
	}

	/** Counts route stages; {@code clearedOnly} counts only cleared ones, else the total. */


	/** Converts view-model text rows into window blocks, dropping decoration lines and coloring by content. */
	private static void addTextBlocks(List<RogueScapeWindowOverlay.Block> out, List<String> rows)
	{
		for (String row : rows)
		{
			if (row == null)
			{
				continue;
			}
			if (row.isEmpty())
			{
				out.add(RogueScapeWindowOverlay.Block.gap());
				continue;
			}
			if (row.startsWith("═"))
			{
				continue;
			}
			String lower = row.toLowerCase();
			java.awt.Color color;
			if (row.contains("✗") || lower.contains("illegal") || lower.contains("failed") || lower.contains("over "))
			{
				color = RogueScapeTheme.NEGATIVE;
			}
			else if (row.contains("✓") || lower.contains("complete") || row.trim().startsWith("+"))
			{
				color = RogueScapeTheme.POSITIVE;
			}
			else if (row.startsWith("  ") || row.startsWith("-"))
			{
				color = RogueScapeTheme.TEXT_MUTED;
			}
			else
			{
				color = RogueScapeTheme.TEXT_PRIMARY;
			}
			out.add(RogueScapeWindowOverlay.Block.text(row, color));
		}
	}

	/** ARTIFACTS tab — held relics as an icon grid (emblems) plus their names. */
	private List<RogueScapeWindowOverlay.Block> artifactsBlocks()
	{
		List<RogueScapeWindowOverlay.Block> out = new ArrayList<>();
		List<Relic> held = plugin.rogueRun.heldRelics();
		out.add(RogueScapeWindowOverlay.Block.heading("Relics in pocket  (" + held.size() + ")"));
		if (held.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.text("No artifacts yet.", RogueScapeTheme.TEXT_MUTED));
			out.add(RogueScapeWindowOverlay.Block.text(
				"Claim relics from reward chests to build your collection.", RogueScapeTheme.TEXT_MUTED));
			return out;
		}
		// One rarity-colored badge per relic (name + effect), matching the MODIFIERS tab.
		for (Relic relic : held)
		{
			boolean curse = relic.description() != null && relic.description().startsWith("Curse");
			out.add(RogueScapeWindowOverlay.Block.badge(
				relic.name(),
				relic.description() == null || relic.description().isEmpty()
					? (relic.effects().isEmpty() ? "" : relicEffectSummary(relic.effects().get(0)))
					: relic.description(),
				curse ? RogueScapeTheme.NEGATIVE : rarityColor(relicRarity(relic)),
				relicIcon(relic)));
		}
		return out;
	}

	/** BUILD tab — the aggregate effect of the held relic loadout (all real, relic-derived data). */
	/** Maps a reward-card index to its choose action (reuses the panel-action dispatch). */
	PanelAction rewardActionForIndex(int idx)
	{
		switch (idx)
		{
			case 0: return PanelAction.CHOOSE_REWARD_1;
			case 1: return PanelAction.CHOOSE_REWARD_2;
			default: return PanelAction.CHOOSE_REWARD_3;
		}
	}

	/** ROLL SUPPLIES content; non-null only while a reward choice is actually pending. */
	RogueScapeRewardOverlay.RewardView rewardView()
	{
		if (!plugin.config.experimentalJournalTab() || plugin.runLoop == null)
		{
			return null;
		}
		if (plugin.runLoop.phase() != RunPhase.BASE_REWARD || plugin.runLoop.baseRewardResolved())
		{
			return null;
		}
		RewardDraft draft = plugin.runLoop.pendingRewardDraft();
		if (draft == null || draft.isSelected() || draft.isRejected() || draft.options().isEmpty())
		{
			return null;
		}
		List<RogueScapeRewardOverlay.Card> cards = new ArrayList<>();
		for (RewardOption option : draft.options())
		{
			cards.add(rewardCard(option));
		}
		return new RogueScapeRewardOverlay.RewardView(
			draft.draftId(),
			rewardTitle(draft),
			rewardSubtitle(draft),
			cards,
			rewardRailRows(),
			artifactItemIds());
	}

	private static String rewardTitle(RewardDraft draft)
	{
		if (draft == null || draft.chestType() == null)
		{
			return "The chest opens";
		}
		switch (draft.chestType())
		{
			case RELIC: return "The chest holds a relic";
			case SUPPLY: return "The chest holds supplies";
			case UNLOCK: return "The chest holds a key";
			case BANK_UNLOCK: return "The chest holds bank spoils";
			default: return "The chest opens";
		}
	}

	private static String rewardSubtitle(RewardDraft draft)
	{
		if (draft == null || draft.chestType() == null)
		{
			return "choose one — the rest crumble to dust";
		}
		switch (draft.chestType())
		{
			case RELIC: return "choose one power — the rest crumble to dust";
			case SUPPLY: return "choose one bundle — the rest crumble to dust";
			case UNLOCK: return "choose one key — the rest crumble to dust";
			case BANK_UNLOCK: return "choose one item to make lawful";
			default: return "choose one — the rest crumble to dust";
		}
	}

	private List<String> rewardRailRows()
	{
		List<String> rows = new ArrayList<>();
		if (plugin.runSession != null)
		{
			rows.add("Chapter: " + Math.min(plugin.countStages(true) + 1, plugin.countStages(false)) + " of " + plugin.countStages(false));
			String completed = stageName(plugin.runLoop == null ? null : plugin.runLoop.completedStageId());
			if (!completed.isEmpty())
			{
				rows.add("Stamped: " + completed);
			}
			String next = plugin.nextUnclearedStageName();
			if (!next.isEmpty())
			{
				rows.add("Next: " + next);
			}
		}
		rows.add("Build:");
		rows.add(plugin.rogueRun == null || plugin.rogueRun.heldRelics().isEmpty()
			? "No relic build yet"
			: plugin.rogueRun.heldRelics().get(plugin.rogueRun.heldRelics().size() - 1).name());
		rows.add("");
		rows.add("Score: " + (plugin.rogueRun == null ? 0 : plugin.rogueRun.effectiveScore()));
		rows.add("Relics: " + (plugin.rogueRun == null ? 0 : plugin.rogueRun.heldRelics().size()));
		rows.add("Items: " + (plugin.rogueRun == null ? 0 : plugin.rogueRun.itemsCollected()));
		List<String> modifiers = rewardModifierRows();
		if (!modifiers.isEmpty())
		{
			rows.add("");
			rows.add("Curses:");
			for (String modifier : modifiers)
			{
				rows.add(modifier);
			}
		}
		if (!plugin.latestProvenanceSignal.isEmpty())
		{
			rows.add("");
			rows.add("Noted:");
			rows.add(plugin.latestProvenanceSignal);
		}
		return rows;
	}

	private String stageName(String stageId)
	{
		if (plugin.runSession == null || stageId == null || stageId.isEmpty())
		{
			return "";
		}
		com.pluginideahub.roguescape.core.RunStage stage = plugin.runSession.route().stageById(stageId);
		return stage == null ? "" : stage.name();
	}



	private List<String> rewardModifierRows()
	{
		List<String> rows = new ArrayList<>();
		if (plugin.rogueRun == null)
		{
			return rows;
		}
		if (plugin.rogueRun.bankAccessAllowed())
		{
			rows.add("Bank unlocks active");
		}
		int relicCount = 0;
		for (Relic relic : plugin.rogueRun.heldRelics())
		{
			if (relicCount++ >= 2)
			{
				break;
			}
			rows.add(relic.name());
		}
		return rows;
	}

	private List<Integer> artifactItemIds()
	{
		List<Integer> ids = new ArrayList<>();
		if (plugin.rogueRun == null)
		{
			return ids;
		}
		for (Relic relic : plugin.rogueRun.heldRelics())
		{
			ids.add(relicIcon(relic));
		}
		return ids;
	}

	/** Reward cards for an unresolved pending draft, or {@code null} if none is pending. */
	private List<RogueScapeRewardOverlay.Card> pendingRewardCards()
	{
		if (plugin.runLoop == null || plugin.runLoop.phase() != RunPhase.BASE_REWARD || plugin.runLoop.baseRewardResolved())
		{
			return null;
		}
		RewardDraft draft = plugin.runLoop.pendingRewardDraft();
		if (draft == null || draft.isSelected() || draft.isRejected() || draft.options().isEmpty())
		{
			return null;
		}
		List<RogueScapeRewardOverlay.Card> cards = new ArrayList<>();
		for (RewardOption option : draft.options())
		{
			cards.add(rewardCard(option));
		}
		return cards;
	}

	/** Builds a reward card from an option. */
	private RogueScapeRewardOverlay.Card rewardCard(RewardOption option)
	{
		if (option.isRelic())
		{
			Relic relic = option.relic();
			List<String> lines = new ArrayList<>();
			if (!relic.description().isEmpty())
			{
				lines.add(relic.description());
			}
			for (RelicEffect effect : relic.effects())
			{
				lines.add(relicEffectSummary(effect));
			}
			return new RogueScapeRewardOverlay.Card(relic.name(), "RELIC", relicRarity(relic), relicIcon(relic), lines);
		}
		List<String> lines = new ArrayList<>();
		if (option.isUnlock())
		{
			lines.add("Unlocks a run rule.");
			lines.add("Source: " + option.unlock().sourceStageName());
			lines.add("Takes effect immediately.");
			return new RogueScapeRewardOverlay.Card(option.label(), "UNLOCK",
				RogueScapeRewardOverlay.Rarity.RARE, rewardIconFor(option), lines);
		}
		if (option.isBankUnlock() && option.bankItem() != null)
		{
			lines.add("Makes this bank item legal.");
			lines.add("Bank withdrawal must match the chosen item.");
			return new RogueScapeRewardOverlay.Card(option.label(), "BANK",
				RogueScapeRewardOverlay.Rarity.RARE, rewardIconFor(option), lines);
		}
		lines.add(supplyRewardDescription(option));
		lines.add("Random bundle type, chosen from this draft.");
		String category = option.chestType() == null ? "" : option.chestType().name();
		return new RogueScapeRewardOverlay.Card(option.label(), category,
			RogueScapeRewardOverlay.Rarity.COMMON, rewardIconFor(option), lines);
	}

	private static String supplyRewardDescription(RewardOption option)
	{
		if (option == null || option.chestType() == null)
		{
			return "Adds supplies to this run.";
		}
		switch (option.chestType())
		{
			case FOOD: return "Adds emergency food.";
			case POTION: return "Adds basic potion supplies.";
			case AMMO: return "Adds ammo and rune supplies.";
			case UTILITY: return "Adds a utility escape option.";
			default: return "Adds supplies to this run.";
		}
	}

	private static int rewardIconFor(RewardOption option)
	{
		if (option == null || option.chestType() == null)
		{
			return RogueScapePlugin.ITEM_DRAGONSTONE;
		}
		switch (option.chestType())
		{
			case FOOD: return RogueScapePlugin.ITEM_SHARK;
			case POTION: return RogueScapePlugin.ITEM_PRAYER_POTION;
			case AMMO: return RogueScapePlugin.ITEM_BRONZE_ARROW;
			case UTILITY: return RogueScapePlugin.ITEM_GLORY;
			case UNLOCK: return RogueScapePlugin.ITEM_DRAGONSTONE;
			case BANK_UNLOCK: return RogueScapePlugin.ITEM_COINS;
			default: return RogueScapePlugin.ITEM_DRAGONSTONE;
		}
	}

	/** Display rarity heuristic (placeholder until rewards carry a real value tier): more effects = rarer. */
	private static java.awt.Color rarityColor(RogueScapeRewardOverlay.Rarity rarity)
	{
		switch (rarity)
		{
			case LEGENDARY: return RogueScapeTheme.RARITY_LEGENDARY;
			case EPIC: return RogueScapeTheme.RARITY_EPIC;
			case RARE: return RogueScapeTheme.RARITY_RARE;
			default: return RogueScapeTheme.RARITY_COMMON;
		}
	}

	private static RogueScapeRewardOverlay.Rarity relicRarity(Relic relic)
	{
		switch (relic.effects().size())
		{
			case 0: return RogueScapeRewardOverlay.Rarity.COMMON;
			case 1: return RogueScapeRewardOverlay.Rarity.RARE;
			case 2: return RogueScapeRewardOverlay.Rarity.EPIC;
			default: return RogueScapeRewardOverlay.Rarity.LEGENDARY;
		}
	}

	/** Picks a representative item sprite for a relic from its primary effect kind (placeholder art). */
	private static int relicIcon(Relic relic)
	{
		RelicEffectKind kind = relic.effects().isEmpty() ? null : relic.effects().get(0).kind();
		if (kind == null)
		{
			return RogueScapePlugin.ITEM_DRAGONSTONE;
		}
		switch (kind)
		{
			case SCORING_BIAS: return RogueScapePlugin.ITEM_COINS;
			case CATEGORY_LIMIT: return RogueScapePlugin.ITEM_SHARK;
			case ONE_SHOT_MERCY: return RogueScapePlugin.ITEM_GLORY;
			default: return RogueScapePlugin.ITEM_DRAGONSTONE;
		}
	}

	/** One short, human-readable summary line for a relic effect. */
	private static String relicEffectSummary(RelicEffect effect)
	{
		switch (effect.kind())
		{
			case ONE_SHOT_MERCY: return "One-shot mercy";
			case CATEGORY_LIMIT: return "Max " + effect.magnitude() + " " + effectCategories(effect);
			case SCORING_BIAS: return (effect.magnitude() >= 0 ? "+" : "") + effect.magnitude()
				+ " score: " + effectCategories(effect);
			case RESTRICTION: return "Forbids " + (effect.itemIds().isEmpty() ? effectCategories(effect) : "set items");
			case PERMISSION: return "Allows " + effectCategories(effect);
			default: return effect.kind().name();
		}
	}

	private static String effectCategories(RelicEffect effect)
	{
		if (effect.categories().isEmpty())
		{
			return "any";
		}
		StringBuilder sb = new StringBuilder();
		for (BankItemCategory c : effect.categories())
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(c.name().toLowerCase().replace('_', ' '));
		}
		return sb.toString();
	}

	/** Logs the live side journal widget tree (for tuning the journal injection). */
}
