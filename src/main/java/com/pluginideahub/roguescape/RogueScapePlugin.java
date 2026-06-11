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
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import com.pluginideahub.roguescape.core.RunStage;
import com.pluginideahub.roguescape.core.RunStageType;
import com.pluginideahub.roguescape.core.RunState;
import com.pluginideahub.roguescape.core.adapter.InventoryDiff;
import com.pluginideahub.roguescape.core.adapter.ProvenanceSignalTracker;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementDecision;
import com.pluginideahub.roguescape.core.enforcement.MenuEnforcementEvaluator;
import com.pluginideahub.roguescape.core.enforcement.RogueScapeEnforcementRules;
import com.pluginideahub.roguescape.core.legality.InventorySnapshot;
import com.pluginideahub.roguescape.core.legality.ItemDelta;
import com.pluginideahub.roguescape.core.legality.ProvenanceHint;
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
import com.pluginideahub.roguescape.ui.RogueScapeWindowOverlay;
import com.pluginideahub.ui.PluginIdeaHubOverlay;
import com.pluginideahub.ui.PluginIdeaHubUiModel;
import java.awt.Dimension;
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
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "RogueScape",
	description = "RogueScape: Plugin Hub-safe OSRS roguelike runs — rooms, boss stages, relics, rewards, and recaps.",
	tags = {"challenge", "creator", "roguelike", "roguescape"}
)
public class RogueScapePlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(RogueScapePlugin.class);

	private static final String CONFIG_GROUP = "pluginideahub-roguescape";
	private static final String CONFIG_KEY_CUSTOM_ROOM_NAME = "customRoomName";
	private static final String CONFIG_KEY_CUSTOM_ROOM_IDS = "customRoomRegionIdsCsv";
	private static final String CONFIG_KEY_USE_CUSTOM_ROOM = "useCustomRoomForCurrentRun";
	private static final String TOGGLE_ROOM_MENU_OPTION = "Toggle RogueScape room region";
	private static final String TOGGLE_ROOM_MENU_TARGET_PREFIX = "RogueScape room: ";

	// Relic emblem item ids (real OSRS items) used as placeholder relic/artifact icons.
	private static final int ITEM_DRAGONSTONE = 1631; // Uncut dragonstone
	private static final int ITEM_COINS = 995;        // Coins
	private static final int ITEM_SHARK = 385;        // Shark
	private static final int ITEM_GLORY = 1712;       // Amulet of glory(6)
	private static final int ITEM_PRAYER_POTION = 2434;
	private static final int ITEM_BRONZE_ARROW = 882;

	@Inject
	private RogueScapeConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	private RogueScapeIcons icons;
	private PluginIdeaHubUiModel uiModel;
	private PluginIdeaHubOverlay overlay;
	private RogueScapeObjectiveOverlay objectiveOverlay;
	private RogueScapeRoomMaskOverlay roomMaskOverlay;
	private RogueScapeActiveRoomWorldMapOverlay activeRoomWorldMapOverlay;
	private RogueScapeCustomRoomWorldMapOverlay customRoomWorldMapOverlay;
	private RogueScapeCustomRoomEditorState customRoomEditorState;
	private RogueScapeJournalWidgetProbe journalProbe;
	private RogueScapeJournalTabAdapter journalTabAdapter;
	private RogueScapeWindowOverlay window;
	private RogueScapeCustomBuilderWidgetWindow customBuilderWidgetWindow;
	private RogueScapeWidgetWindow widgetWindow;
	private int clogDumpCountdown = -1;
	private RogueScapeRewardOverlay rewardOverlay;
	private RogueScapePanel panel;
	private NavigationButton navigationButton;
	private RogueScapeRunSession runSession;
	private RogueScapeRun rogueRun;
	private RogueScapeRunLoop runLoop;
	private InventorySnapshot previousInventorySnapshot = new InventorySnapshot();
	private final ProvenanceSignalTracker provenanceSignals = new ProvenanceSignalTracker();
	private String latestObservedItem = "";
	private String latestProvenanceSignal = "";
	private String currentRegionId = "";
	private String lastShortestPathTargetKey = "";
	private String shortestPathStatus = "";
	private WorldMapPoint roomTargetMapPoint;
	private String lastRoomTargetMapPointKey = "";

	@Override
	protected void startUp()
	{
		customRoomEditorState = new RogueScapeCustomRoomEditorState(
			RogueScapeCustomRoomSelection.fromCsv(config.customRoomName(), config.customRoomRegionIdsCsv())
		);
		// Boot into the lobby (no active run) so the player can select and START a run.
		uiModel = PluginIdeaHubUiModel.start(RogueScapePrototype.displayName(), config.goalText(), RogueScapePrototype.starterDeck());
		refreshOverlaySummary();
		overlay = new PluginIdeaHubOverlay(uiModel::getTitle, this::overlayLines);
		overlayManager.add(overlay);
		objectiveOverlay = new RogueScapeObjectiveOverlay(this::objectiveView);
		overlayManager.add(objectiveOverlay);
		journalProbe = new RogueScapeJournalWidgetProbe(client);
		// Overlays repaint every game frame, so no explicit repaint hook is needed.
		icons = new RogueScapeIcons(itemManager, spriteManager, null);
		window = new RogueScapeWindowOverlay(this::windowTabs, icons);
		window.setModeTileHandler(this::selectRunBuilderMode);
		window.setCanvasSize(() -> new Dimension(client.getCanvasWidth(), client.getCanvasHeight()));
		overlayManager.add(window);
		mouseManager.registerMouseListener(window);
		customBuilderWidgetWindow = new RogueScapeCustomBuilderWidgetWindow(client, clientThread, () -> true,
			this::customBuilderWidgetView, this::handleCustomBuilderAction);
		mouseManager.registerMouseListener(customBuilderWidgetWindow);
		// SPIKE: real widget-based window (proves the Collection-Log-style approach).
		widgetWindow = new RogueScapeWidgetWindow(client, clientThread, config::experimentalJournalTab, this::windowTabs);
		mouseManager.registerMouseListener(widgetWindow);
		rewardOverlay = new RogueScapeRewardOverlay(
			this::rewardView,
			icons,
			idx -> dispatchAction(rewardActionForIndex(idx)),
			() -> dispatchAction(PanelAction.SKIP_REWARD),
			() -> new Dimension(client.getCanvasWidth(), client.getCanvasHeight())
		);
		overlayManager.add(rewardOverlay);
		mouseManager.registerMouseListener(rewardOverlay);
		journalTabAdapter = new RogueScapeJournalTabAdapter(client, config::experimentalJournalTab, this::toggleWindow);
		roomMaskOverlay = new RogueScapeRoomMaskOverlay(
			client,
			config::showRoomRegionMask,
			() -> rogueRun != null && runSession != null,
			config::roomMaskOpacity,
			this::currentAllowedRoomRegions
		);
		overlayManager.add(roomMaskOverlay);
		activeRoomWorldMapOverlay = new RogueScapeActiveRoomWorldMapOverlay(
			client,
			this::activeRoomWorldMapEnabled,
			this::currentAllowedRoomRegions,
			this::currentRoomName
		);
		overlayManager.add(activeRoomWorldMapOverlay);
		customRoomWorldMapOverlay = new RogueScapeCustomRoomWorldMapOverlay(
			client,
			customRoomEditorState::isEditing,
			() -> customRoomEditorState.selection().selectedRegionIds(),
			() -> customRoomEditorState.selection().getName(),
			customRoomEditorState::setHoveredRegionId
		);
		overlayManager.add(customRoomWorldMapOverlay);
		panel = new RogueScapePanel(customRoomEditorState, this::persistCustomRoomSelection, this::activateCustomRoomForRun, this::dispatchAction, config::developerMode);
		navigationButton = NavigationButton.builder()
			.tooltip("RogueScape")
			.icon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB))
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		refreshSidePanel();
	}

	@Override
	protected void shutDown()
	{
		if (overlay != null)
		{
			overlayManager.remove(overlay);
			overlay = null;
		}
		if (roomMaskOverlay != null)
		{
			overlayManager.remove(roomMaskOverlay);
			roomMaskOverlay = null;
		}
		removeRoomTargetMapPoint();
		if (activeRoomWorldMapOverlay != null)
		{
			overlayManager.remove(activeRoomWorldMapOverlay);
			activeRoomWorldMapOverlay = null;
		}
		if (objectiveOverlay != null)
		{
			overlayManager.remove(objectiveOverlay);
			objectiveOverlay = null;
		}
		if (customRoomWorldMapOverlay != null)
		{
			overlayManager.remove(customRoomWorldMapOverlay);
			customRoomWorldMapOverlay = null;
		}
		if (window != null)
		{
			mouseManager.unregisterMouseListener(window);
			overlayManager.remove(window);
			window = null;
		}
		if (customBuilderWidgetWindow != null)
		{
			mouseManager.unregisterMouseListener(customBuilderWidgetWindow);
			customBuilderWidgetWindow.shutDown();
			customBuilderWidgetWindow = null;
		}
		if (rewardOverlay != null)
		{
			mouseManager.unregisterMouseListener(rewardOverlay);
			overlayManager.remove(rewardOverlay);
			rewardOverlay = null;
		}
		if (widgetWindow != null)
		{
			mouseManager.unregisterMouseListener(widgetWindow);
			widgetWindow.shutDown();
			widgetWindow = null;
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		panel = null;
		customRoomEditorState = null;
		journalProbe = null;
		journalTabAdapter = null;
		uiModel = null;
		runSession = null;
		rogueRun = null;
		runLoop = null;
		previousInventorySnapshot = new InventorySnapshot();
		provenanceSignals.reset();
		latestObservedItem = "";
		latestProvenanceSignal = "";
		currentRegionId = "";
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (rogueRun == null || event == null || event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		InventorySnapshot nextSnapshot = currentInventorySnapshot();
		ProvenanceHint hint = provenanceSignals.currentHint();
		List<ItemDelta> deltas = InventoryDiff.positiveDeltas(previousInventorySnapshot, nextSnapshot, hint);
		if (!deltas.isEmpty())
		{
			ProvenanceHint consumedHint = provenanceSignals.consumePendingHint();
			latestProvenanceSignal = provenanceLine(consumedHint);
			for (ItemDelta delta : deltas)
			{
				ItemDelta namedDelta = withItemNameAndRegion(delta, consumedHint);
				rogueRun.applyItemDelta(namedDelta);
				latestObservedItem = namedDelta.itemName() + " x" + namedDelta.quantity();
			}
			refreshOverlaySummary();
		}
		previousInventorySnapshot = nextSnapshot;
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		provenanceSignals.onGameTick();
		if (journalTabAdapter != null)
		{
			journalTabAdapter.ensureInjected();
		}
		if (widgetWindow != null)
		{
			widgetWindow.onTick();
		}
		if (customBuilderWidgetWindow != null)
		{
			customBuilderWidgetWindow.onTick();
		}
		if (clogDumpCountdown > 0 && --clogDumpCountdown == 0 && journalProbe != null)
		{
			for (String line : journalProbe.dumpCollectionLog())
			{
				log.info("[RogueScape clog] {}", line);
			}
		}
		if (runLoop != null)
		{
			runLoop.markNow(System.currentTimeMillis());
			syncShortestPathTarget();
			syncRoomTargetMapPoint();
		}
		if (rogueRun == null || client == null)
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		WorldPoint worldPoint = player.getWorldLocation();
		if (worldPoint == null)
		{
			return;
		}

		String regionId = String.valueOf(worldPoint.getRegionID());
		if (!regionId.equals(currentRegionId))
		{
			currentRegionId = regionId;
			rogueRun.moveToRegion(regionId);
			if (runLoop != null && runLoop.notifyRegionChanged(System.currentTimeMillis()))
			{
				latestProvenanceSignal = "room entered: timer started";
				syncShortestPathTarget();
				syncRoomTargetMapPoint();
			}
			refreshOverlaySummary();
		}

		if (enforcementActive())
		{
			RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(rogueRun);
			String signal = rules.warnLeaveRoom() && !rogueRun.currentRegionLegal()
				? "⚠ Outside legal room region!"
				: "";
			if (!signal.isEmpty() && !signal.equals(latestProvenanceSignal))
			{
				latestProvenanceSignal = signal;
				refreshSidePanel();
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (rogueRun != null && event != null && event.getGameState() == GameState.LOGGED_IN)
		{
			previousInventorySnapshot = currentInventorySnapshot();
			rogueRun.setStartSnapshot(previousInventorySnapshot);
			provenanceSignals.reset();
			latestProvenanceSignal = "baseline refreshed";
			refreshOverlaySummary();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event == null || !config.experimentalJournalTab())
		{
			return;
		}
		// When the real Collection Log (group 621) opens, dump its composition so we can clone it.
		// Delay a few ticks so the game's clientscripts have populated the components first.
		if (event.getGroupId() == 621)
		{
			clogDumpCountdown = 4;
			return;
		}
		if (event.getGroupId() != InterfaceID.SIDE_JOURNAL)
		{
			return;
		}
		// The side journal is rebuilt by the game's clientscripts each time it opens; inject now
		// (the per-tick ensureInjected re-adds our widgets if a later rebuild wipes them). Also
		// log the live widget tree to drive layout tuning.
		if (journalTabAdapter != null)
		{
			journalTabAdapter.ensureInjected();
		}
		logJournalDump();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event == null)
		{
			return;
		}
		String message = event.getMessage();
		if (ProvenanceSignalTracker.isLikelyDeathMessage(message))
		{
			if (runSession != null)
			{
				runSession.recordViolation("Observed death", RogueScapeRunSession.RunEnding.DEATH);
			}
			latestProvenanceSignal = "death observed";
			refreshOverlaySummary();
			return;
		}
		if (observeBossKillChat(message))
		{
			refreshOverlaySummary();
		}
		provenanceSignals.observeChat(message);
		if (!provenanceSignals.latestSignal().isEmpty())
		{
			latestProvenanceSignal = provenanceSignals.latestSignal();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event == null || rogueRun == null || runLoop == null)
		{
			return;
		}
		if (runLoop.phase() != RunPhase.ROOM_ACTIVE)
		{
			return;
		}
		String skillName = event.getSkill() == null ? "" : event.getSkill().getName();
		if (rogueRun.recordStatChanged(skillName, event.getXp()))
		{
			latestProvenanceSignal = "room task XP: " + skillName;
			refreshOverlaySummary();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Add a "RogueScape" right-click on the in-game Collection Log button (detected by its
		// "Collection log" menu option) that opens our custom window. Gated behind the flag.
		if (event != null
			&& config.experimentalJournalTab()
			&& event.getOption() != null
			&& event.getOption().toLowerCase().contains("collection log"))
		{
			client.createMenuEntry(-1)
				.setOption("RogueScape")
				.setTarget("")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> toggleWindow());
		}

		if (event != null
			&& customRoomEditorState != null
			&& customRoomEditorState.isEditing())
		{
			Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
			if (mapWidget != null && !mapWidget.isHidden())
			{
				int hovered = customRoomEditorState.getHoveredRegionId();
				if (hovered >= 0)
				{
					boolean already = false;
					for (MenuEntry existing : client.getMenuEntries())
					{
						if (TOGGLE_ROOM_MENU_OPTION.equals(existing.getOption())
							&& existing.getType() == MenuAction.RUNELITE
							&& existing.getTarget() != null
							&& existing.getTarget().startsWith(TOGGLE_ROOM_MENU_TARGET_PREFIX))
						{
							already = true;
							break;
						}
					}
					if (!already)
					{
						boolean alreadySelected = customRoomEditorState.selection().contains(hovered);
						String actionWord = alreadySelected ? "Remove" : "Add";
						client.createMenuEntry(-1)
							.setOption(TOGGLE_ROOM_MENU_OPTION)
							.setTarget(TOGGLE_ROOM_MENU_TARGET_PREFIX + actionWord + " Region " + hovered)
							.setType(MenuAction.RUNELITE)
							.onClick(e -> toggleHoveredCustomRoomRegionAndPersist());
					}
				}
			}
		}

		if (!enforcementActive())
		{
			return;
		}
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(rogueRun);
		boolean inside = rogueRun.currentRegionLegal();
		MenuEntry[] entries = client.getMenuEntries();
		if (entries == null || entries.length == 0)
		{
			return;
		}
		MenuEntry[] filtered = new MenuEntry[entries.length];
		int kept = 0;
		boolean removedAny = false;
		for (MenuEntry entry : entries)
		{
			MenuEnforcementDecision decision = MenuEnforcementEvaluator.evaluate(
				entry.getOption(), entry.getTarget(), rules, inside);
			if (decision == MenuEnforcementDecision.BLOCK)
			{
				removedAny = true;
				continue;
			}
			filtered[kept++] = entry;
		}
		if (removedAny)
		{
			MenuEntry[] trimmed = new MenuEntry[kept];
			System.arraycopy(filtered, 0, trimmed, 0, kept);
			client.setMenuEntries(trimmed);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event == null)
		{
			return;
		}
		if (event.getMenuAction() == MenuAction.RUNELITE
			&& TOGGLE_ROOM_MENU_OPTION.equals(event.getMenuOption())
			&& event.getMenuTarget() != null
			&& event.getMenuTarget().startsWith(TOGGLE_ROOM_MENU_TARGET_PREFIX))
		{
			// MenuEntry.onClick already runs the toggle; just consume so other handlers don't act on it.
			event.consume();
			return;
		}

		if (enforcementActive())
		{
			RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(rogueRun);
			MenuEnforcementDecision decision = MenuEnforcementEvaluator.evaluate(
				event.getMenuOption(), event.getMenuTarget(), rules, rogueRun.currentRegionLegal());
			if (decision == MenuEnforcementDecision.BLOCK)
			{
				event.consume();
				if (runSession != null)
				{
					runSession.recordRunLoopNote("Blocked: " + event.getMenuOption());
				}
				return;
			}
		}

		provenanceSignals.observeMenu(event.getMenuOption(), event.getMenuTarget());
		if (!provenanceSignals.latestSignal().isEmpty())
		{
			latestProvenanceSignal = provenanceSignals.latestSignal();
		}
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (window != null && event != null && event.getOverlay() == window
			&& RogueScapeWindowOverlay.CLOSE_OPTION.equals(event.getEntry().getOption()))
		{
			window.setOpen(false);
		}
	}

	@Provides
	RogueScapeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RogueScapeConfig.class);
	}

	private boolean enforcementActive()
	{
		if (rogueRun == null || runLoop == null || runSession == null)
		{
			return false;
		}
		if (runSession.runState() != RunState.ACTIVE)
		{
			return false;
		}
		RunPhase phase = runLoop.phase();
		return phase == RunPhase.TRAVEL_TO_STAGE || phase == RunPhase.ROOM_ACTIVE || phase == RunPhase.BOSS_ACTIVE;
	}

	private InventorySnapshot currentInventorySnapshot()
	{
		Map<String, Integer> quantities = new LinkedHashMap<>();
		if (client == null)
		{
			return new InventorySnapshot(quantities);
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return new InventorySnapshot(quantities);
		}

		Item[] items = inventory.getItems();
		if (items == null)
		{
			return new InventorySnapshot(quantities);
		}

		for (Item item : items)
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			String itemId = String.valueOf(item.getId());
			quantities.put(itemId, quantities.getOrDefault(itemId, 0) + item.getQuantity());
		}
		return new InventorySnapshot(quantities);
	}

	private ItemDelta withItemNameAndRegion(ItemDelta delta, ProvenanceHint consumedHint)
	{
		String itemId = delta.itemId();
		String itemName = delta.itemName();
		try
		{
			int numericId = Integer.parseInt(itemId);
			itemName = itemName(numericId);
		}
		catch (NumberFormatException ignored)
		{
			// InventorySnapshot keys are numeric in live RuneLite wiring, but pure tests may use names.
		}
		return new ItemDelta(itemId, itemName, delta.quantity(), currentRegionNote(), consumedHint);
	}

	private String itemName(int itemId)
	{
		if (client == null)
		{
			return String.valueOf(itemId);
		}
		try
		{
			ItemComposition composition = client.getItemDefinition(itemId);
			String name = composition != null ? composition.getName() : null;
			return name == null || name.isEmpty() ? String.valueOf(itemId) : name;
		}
		catch (RuntimeException ex)
		{
			return String.valueOf(itemId);
		}
	}

	private String currentRegionNote()
	{
		return currentRegionId.isEmpty() ? "unknown region" : "region " + currentRegionId;
	}

	private Set<String> currentAllowedRoomRegions()
	{
		if (config.useCustomRoomForCurrentRun() && customRoomEditorState != null && !customRoomEditorState.selection().isEmpty())
		{
			return customRoomEditorState.selection().selectedRegionIdStrings();
		}
		if (rogueRun == null || rogueRun.currentStageRule() == null || rogueRun.currentStageRule().allowedRegionIds() == null)
		{
			return Collections.emptySet();
		}
		return rogueRun.currentStageRule().allowedRegionIds();
	}

	private void toggleHoveredCustomRoomRegionAndPersist()
	{
		if (customRoomEditorState == null)
		{
			return;
		}
		int toggled = customRoomEditorState.toggleHovered();
		if (toggled >= 0)
		{
			persistCustomRoomSelection();
			refreshOverlaySummary();
		}
	}

	private void persistCustomRoomSelection()
	{
		if (customRoomEditorState == null || configManager == null)
		{
			return;
		}
		RogueScapeCustomRoomSelection selection = customRoomEditorState.selection();
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_CUSTOM_ROOM_NAME, selection.getName());
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_CUSTOM_ROOM_IDS, selection.toCsv());
	}

	private void activateCustomRoomForRun()
	{
		persistCustomRoomSelection();
		if (configManager != null)
		{
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_USE_CUSTOM_ROOM, true);
		}
		applyCustomRoomZoneToRun();
		refreshOverlaySummary();
	}

	private void applyCustomRoomZoneToRun()
	{
		if (rogueRun == null || runSession == null || customRoomEditorState == null
			|| customRoomEditorState.selection().isEmpty())
		{
			return;
		}
		if (config != null && !config.useCustomRoomForCurrentRun())
		{
			return;
		}
		Set<String> selectedRegions = customRoomEditorState.selection().selectedRegionIdStrings();
		for (RunStage stage : runSession.route().stages())
		{
			if (stage == null || stage.type() == RunStageType.BOSS)
			{
				continue;
			}
			StageRegionRule existing = rogueRun.regionPolicy().ruleFor(stage.id());
			RoomKind kind = existing == null ? RoomKind.REGION : existing.roomKind();
			rogueRun.setRegionRule(stage.id(), new StageRegionRule(kind, selectedRegions, true));
		}
		runSession.recordRunLoopNote("Applied custom zone: " + customRoomEditorState.selection().getName()
			+ " (" + customRoomEditorState.selection().size() + " regions)");
	}

	private void refreshOverlaySummary()
	{
		if (uiModel == null || rogueRun == null)
		{
			return;
		}
		uiModel.recordManualAction("RogueScape live: " + legalitySummary());
		refreshSidePanel();
	}

	private void refreshSidePanel()
	{
		if (panel == null)
		{
			return;
		}
		if (runSession == null || rogueRun == null)
		{
			panel.render(SidePanelViewModel.lobby(
				panel.selectedMode(),
				panel.selectedGoal(),
				formatRules()));
			return;
		}
		panel.render(SidePanelViewModel.active(runLoop, PanelTab.RUN));
	}

	/**
	 * Starts a fresh run using the panel's current selections (mode, goal, handpicked
	 * rooms + boss). Falls back to config values / an auto-generated route when the
	 * panel has no selection.
	 */
	private void startRun()
	{
		RunMode mode = panel != null ? panel.selectedMode() : ModePresetParser.parseMode(config.modePreset());
		String panelGoal = panel != null ? panel.selectedGoal() : "";
		String goal = (panelGoal == null || panelGoal.trim().isEmpty()) ? config.goalText() : panelGoal.trim();
		String rawSeed = panel != null ? panel.selectedSeed() : config.seedText();
		String seed = (rawSeed == null || rawSeed.trim().isEmpty()) ? null : rawSeed.trim();
		RunPreset preset = panel != null ? panel.selectedPreset() : ModePresetParser.parsePreset(config.runPreset());

		boolean custom = mode == RunMode.CUSTOM_CREATOR && panel != null;
		previousInventorySnapshot = currentInventorySnapshot();
		if (custom)
		{
			RogueScapeCustomRunFactory.StartedRun started = RogueScapeCustomRunFactory.start(
				RogueScapeCustomRunFactory.Config.builder()
					.goal(goal)
					.seed(seed)
					.customMode(panel.customBuilderGameMode())
					.loadout(panel.customBuilderLoadout())
					.roomIds(panel.selectedRoomIds())
					.roomAllowances(panel.selectedRoomAllowances())
					.bossId("")
					.modifierIds(panel.selectedModifierIds())
					.strictness(panel.customStrictness())
					.bankUnlocks(panel.customBankUnlocks())
					.preRunSupplyExpected(config.preRunSupplyExpected())
					.timeLimitMinutes(panel.customTimeLimitMinutes())
					.startedAtMillis(System.currentTimeMillis())
					.startSnapshot(previousInventorySnapshot));
			runSession = started.session();
			rogueRun = started.run();
			runLoop = started.loop();
			runLoop.setTravelGatedStages(true);
			applyCustomRoomZoneToRun();
			provenanceSignals.reset();
			latestObservedItem = "";
			latestProvenanceSignal = "run started: " + runModeName(mode);
			refreshSidePanel();
			return;
		}

		runSession = RogueScapeRunSession.start(goal, seed, mode, preset);
		rogueRun = RogueScapeRun.wrap(runSession)
			.declareStarterKit(RogueScapeCustomRunFactory.starterKitForLoadout(panel != null ? panel.customBuilderLoadout() : "Naked"))
			.setStrictness(config.strictnessMode())
			.setBankAccessAllowed(config.bankAccessAllowed())
			.setPreRunSupplyExpected(config.preRunSupplyExpected())
			.setStartSnapshot(previousInventorySnapshot);
		try
		{
			RunRouteBuilder.buildRoute(mode, preset, seed, runSession, rogueRun);
		}
		catch (RuntimeException ex)
		{
			runSession.recordRunLoopNote("Route build skipped: " + ex.getMessage());
			try
			{
				RunRouteBuilder.buildRoute(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED, seed, runSession, rogueRun);
				runSession.recordRunLoopNote("Fallback route built.");
			}
			catch (RuntimeException fallback)
			{
				runSession.recordRunLoopNote("Fallback route failed: " + fallback.getMessage());
			}
		}

		// Build the loop after the route so its initial phase reflects the first stage.
		runLoop = new RogueScapeRunLoop(rogueRun, System.currentTimeMillis());
		runLoop.setTravelGatedStages(true);
		applyCustomRoomZoneToRun();
		provenanceSignals.reset();
		latestObservedItem = "";
		latestProvenanceSignal = runSession.route().size() > 0
			? "run started: " + runModeName(mode)
			: "run start failed: no route";
		refreshSidePanel();
	}

	/** Abandons the active run and returns the panel to the lobby/selection state. */
	private void resetToLobby()
	{
		clearShortestPathTarget();
		runSession = null;
		rogueRun = null;
		runLoop = null;
		previousInventorySnapshot = new InventorySnapshot();
		provenanceSignals.reset();
		latestObservedItem = "";
		latestProvenanceSignal = "";
		currentRegionId = "";
		lastShortestPathTargetKey = "";
		shortestPathStatus = "";
		removeRoomTargetMapPoint();
	}

	void dispatchAction(PanelAction action)
	{
		if (action == null)
		{
			return;
		}
		long now = System.currentTimeMillis();
		switch (action)
		{
			case COMPLETE_STAGE:
				if (runLoop != null) runLoop.completeCurrentStage(now);
				break;
			case DEV_COMPLETE_STAGE:
				if (runLoop != null) runLoop.forceCompleteCurrentStage(now);
				break;
			case CHOOSE_REWARD:
			case CHOOSE_REWARD_1:
				chooseRewardByIndex(0, now);
				break;
			case CHOOSE_REWARD_2:
				chooseRewardByIndex(1, now);
				break;
			case CHOOSE_REWARD_3:
				chooseRewardByIndex(2, now);
				break;
			case SKIP_REWARD:
				if (runLoop != null) runLoop.skipReward(now);
				break;
			case NEXT_STAGE:
				if (runLoop != null) runLoop.startNextStage(now);
				break;
			case FAIL_RUN:
				if (runSession != null) runSession.recordViolation("Manual fail", RogueScapeRunSession.RunEnding.MANUAL_FAIL);
				if (runLoop != null) runLoop.markNow(now);
				break;
			case DEV_COMPLETE_RUN:
				if (runSession != null && runLoop != null)
				{
					runSession.completeRun("Dev: force complete", RunCompletionReason.MANUAL_SUCCESS);
					runLoop.markNow(now);
				}
				break;
			case DEV_LOG_JOURNAL:
				if (clientThread != null)
				{
					clientThread.invokeLater(this::logJournalDump);
				}
				break;
			case START_RUN:
				startRunOnClientThread();
				return;
			case RESET_RUN:
				resetToLobby();
				break;
		}
		refreshSidePanel();
	}

	private void startRunOnClientThread()
	{
		if (clientThread != null)
		{
			clientThread.invokeLater(() ->
			{
				startRun();
				refreshSidePanel();
			});
			return;
		}
		startRun();
		refreshSidePanel();
	}

	private void chooseRewardByIndex(int zeroBasedIndex, long nowMillis)
	{
		if (runLoop == null)
		{
			return;
		}
		RewardDraft draft = runLoop.pendingRewardDraft();
		if (draft == null || draft.options() == null || zeroBasedIndex < 0 || zeroBasedIndex >= draft.options().size())
		{
			return;
		}
		runLoop.chooseReward(draft.options().get(zeroBasedIndex).optionId(), nowMillis);
	}

	private List<String> overlayLines()
	{
		List<String> lines = new ArrayList<>();
		if (rogueRun == null || runSession == null)
		{
			lines.add("State: OFF");
			return lines;
		}

		OverlayViewModel view = OverlayViewModel.from(rogueRun);
		lines.add("Run: " + view.goal());
		lines.add("State: " + view.state());
		lines.add("Region: " + (currentRegionId.isEmpty() ? "unknown" : currentRegionId));
		String target = targetRegionLabel();
		if (!target.isEmpty())
		{
			lines.add("Target: " + target);
		}
		if (!shortestPathStatus.isEmpty())
		{
			lines.add("Path: " + shortestPathStatus);
		}
		lines.add("Strictness: " + rogueRun.strictness() + " | Bank unlocks: " + (rogueRun.bankAccessAllowed() ? "on" : "off"));
		if (!view.currentRoom().isEmpty())
		{
			lines.add("Room: " + view.currentRoom());
		}
		if (customRoomEditorState != null && config.useCustomRoomForCurrentRun())
		{
			lines.add("Custom room: " + customRoomEditorState.selection().getName()
				+ " (" + customRoomEditorState.selection().size() + " regions active)");
		}
		lines.add("Score/relics: " + view.score() + "/" + view.relicCount());
		lines.add(legalitySummary(view));
		if (provenanceSignals.hasPendingHint())
		{
			lines.add("Pending source: " + provenanceSignals.currentHint());
		}
		else if (!latestProvenanceSignal.isEmpty())
		{
			lines.add("Signal: " + latestProvenanceSignal);
		}
		if (!latestObservedItem.isEmpty())
		{
			lines.add("Latest item: " + latestObservedItem);
		}
		if (config.experimentalJournalTab() && journalProbe != null)
		{
			lines.add(journalProbe.probe().summaryLine());
		}
		for (String warning : view.warnings())
		{
			lines.add("Warn: " + warning);
		}
		lines.add("Run: " + runSession.overlaySummary());
		return lines;
	}

	private String legalitySummary()
	{
		if (rogueRun == null)
		{
			return "Items legal/suspicious/illegal: 0/0/0";
		}
		return "Items legal/suspicious/illegal: "
			+ rogueRun.legalCount()
			+ "/"
			+ rogueRun.suspiciousCount()
			+ "/"
			+ rogueRun.illegalCount();
	}

	private String legalitySummary(OverlayViewModel view)
	{
		return "Items legal/suspicious/illegal: "
			+ view.legalCount()
			+ "/"
			+ view.suspiciousCount()
			+ "/"
			+ view.illegalCount();
	}

	private RogueScapeObjectiveOverlay.View objectiveView()
	{
		if (rogueRun == null || runSession == null || runLoop == null || runSession.runState() != RunState.ACTIVE)
		{
			return null;
		}
		RunStage stage = rogueRun.currentEnteredStage();
		if (stage == null)
		{
			return null;
		}
		int total = countStages(false);
		int cleared = countStages(true);
		double progress = total > 0 ? (double) cleared / total : 0.0;
		String region = currentRegionId == null || currentRegionId.isEmpty() ? "unknown" : currentRegionId;
		String target = targetRegionLabel();
		if (!target.isEmpty())
		{
			region = runLoop.phase() == RunPhase.TRAVEL_TO_STAGE
				? region + " -> " + target
				: region + " / " + target;
		}
		String next = nextUnclearedStageName();
		if (next.equals(stage.name()))
		{
			next = "";
		}
		String objective = stage.objectiveProgressLabel();
		String score = "Score " + rogueRun.effectiveScore();
		if (runLoop.phase() == RunPhase.TRAVEL_TO_STAGE)
		{
			objective = "Travel to the allowed room region";
			score = runLoop.hasTimeLimit() ? "Timer " + runLoop.timeRemainingLabel() : score;
		}
		else if (runLoop.hasTimeLimit())
		{
			score = "Timer " + runLoop.timeRemainingLabel();
		}
		return new RogueScapeObjectiveOverlay.View(
			stage.name(),
			objective,
			next,
			region,
			runLoop.phase().getDisplayName(),
			score,
			progress,
			stage.objectiveComplete(),
			rogueRun.currentRegionLegal());
	}

	private String targetRegionLabel()
	{
		if (rogueRun == null || rogueRun.currentStageRule() == null)
		{
			return "";
		}
		StageRegionRule rule = rogueRun.currentStageRule();
		if (!rule.restrictsRegion() || rule.allowedRegionIds().isEmpty())
		{
			return "";
		}
		int count = rule.allowedRegionIds().size();
		String first = rule.allowedRegionIds().iterator().next();
		return count == 1 ? first : first + " +" + (count - 1);
	}

	private boolean activeRoomWorldMapEnabled()
	{
		return rogueRun != null
			&& runLoop != null
			&& runSession != null
			&& runSession.runState() == RunState.ACTIVE
			&& customRoomEditorState != null
			&& !customRoomEditorState.isEditing()
			&& !currentAllowedRoomRegions().isEmpty();
	}

	private String currentRoomName()
	{
		return rogueRun == null || rogueRun.currentRoomName() == null ? "" : rogueRun.currentRoomName();
	}

	private void syncRoomTargetMapPoint()
	{
		if (!activeRoomWorldMapEnabled() || worldMapPointManager == null)
		{
			removeRoomTargetMapPoint();
			return;
		}
		WorldPoint target = shortestPathTargetPoint();
		String key = target == null ? "" : target.getX() + "," + target.getY() + "," + target.getPlane();
		if (key.isEmpty())
		{
			removeRoomTargetMapPoint();
			return;
		}
		if (key.equals(lastRoomTargetMapPointKey) && roomTargetMapPoint != null)
		{
			return;
		}

		removeRoomTargetMapPoint();
		roomTargetMapPoint = new WorldMapPoint(target, roomTargetMarkerImage());
		roomTargetMapPoint.setName("RogueScape: " + currentRoomName());
		roomTargetMapPoint.setTarget(target);
		roomTargetMapPoint.setJumpOnClick(true);
		worldMapPointManager.add(roomTargetMapPoint);
		lastRoomTargetMapPointKey = key;
	}

	private void removeRoomTargetMapPoint()
	{
		if (worldMapPointManager != null && roomTargetMapPoint != null)
		{
			worldMapPointManager.remove(roomTargetMapPoint);
		}
		roomTargetMapPoint = null;
		lastRoomTargetMapPointKey = "";
	}

	private static BufferedImage roomTargetMarkerImage()
	{
		BufferedImage image = new BufferedImage(25, 25, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0, 0, 0, 165));
		g.fillOval(1, 1, 23, 23);
		g.setColor(new Color(0, 235, 110, 235));
		g.fillOval(4, 4, 17, 17);
		g.setColor(new Color(235, 255, 235, 255));
		g.drawOval(4, 4, 17, 17);
		g.drawLine(12, 6, 12, 18);
		g.drawLine(6, 12, 18, 12);
		g.dispose();
		return image;
	}

	private void syncShortestPathTarget()
	{
		if (runLoop == null || rogueRun == null || client == null)
		{
			return;
		}
		WorldPoint target = runLoop.phase() == RunPhase.TRAVEL_TO_STAGE ? shortestPathTargetPoint() : null;
		String key = target == null ? "" : target.getX() + "," + target.getY() + "," + target.getPlane();
		if (key.equals(lastShortestPathTargetKey))
		{
			return;
		}
		if (setShortestPathTarget(target))
		{
			lastShortestPathTargetKey = key;
			shortestPathStatus = target == null ? "cleared" : "Shortest Path target set";
		}
		else if (target != null)
		{
			if (shortestPathStatus == null || shortestPathStatus.isEmpty())
			{
				shortestPathStatus = "Shortest Path not found";
			}
		}
	}

	private void clearShortestPathTarget()
	{
		if (lastShortestPathTargetKey.isEmpty() && shortestPathStatus.isEmpty())
		{
			return;
		}
		if (setShortestPathTarget(null))
		{
			shortestPathStatus = "cleared";
		}
		lastShortestPathTargetKey = "";
	}

	private WorldPoint shortestPathTargetPoint()
	{
		if (rogueRun == null || rogueRun.currentStageRule() == null)
		{
			return null;
		}
		StageRegionRule rule = rogueRun.currentStageRule();
		if (!rule.restrictsRegion() || rule.allowedRegionIds().isEmpty())
		{
			return null;
		}
		String first = rule.allowedRegionIds().iterator().next();
		try
		{
			int regionId = Integer.parseInt(first);
			int regionX = regionId >> 8;
			int regionY = regionId & 0xFF;
			int plane = 0;
			Player player = client.getLocalPlayer();
			if (player != null && player.getWorldLocation() != null)
			{
				plane = player.getWorldLocation().getPlane();
			}
			return new WorldPoint(regionX * 64 + 32, regionY * 64 + 32, plane);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private boolean setShortestPathTarget(WorldPoint target)
	{
		Object shortestPath = shortestPathPluginInstance();
		if (shortestPath == null)
		{
			log.debug("RogueScape Shortest Path bridge: plugin not found");
			return false;
		}
		try
		{
			if (!shortestPathPluginActive(shortestPath))
			{
				shortestPathStatus = "Shortest Path is off";
				log.debug("RogueScape Shortest Path bridge: plugin found but inactive ({})",
					shortestPath.getClass().getName());
				return false;
			}
			java.lang.reflect.Method setTarget = findMethod(shortestPath.getClass(), "setTarget", WorldPoint.class);
			if (setTarget == null)
			{
				return setShortestPathTargetFields(shortestPath, target);
			}
			setTarget.setAccessible(true);
			setTarget.invoke(shortestPath, target);
			log.debug("RogueScape Shortest Path bridge: set target {}", target);
			return true;
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not call Shortest Path setTarget; trying field fallback", ex);
			return setShortestPathTargetFields(shortestPath, target);
		}
	}

	private Object shortestPathPluginInstance()
	{
		if (pluginManager == null)
		{
			return null;
		}
		try
		{
			java.lang.reflect.Method getPlugins = pluginManager.getClass().getMethod("getPlugins");
			Object plugins = getPlugins.invoke(pluginManager);
			if (!(plugins instanceof Iterable))
			{
				return null;
			}
			for (Object plugin : (Iterable<?>) plugins)
			{
				if (plugin == null)
				{
					continue;
				}
				Class<?> type = plugin.getClass();
				String name = type.getName();
				PluginDescriptor descriptor = type.getAnnotation(PluginDescriptor.class);
				String descriptorName = descriptor == null ? "" : descriptor.name();
				if ("Shortest Path".equalsIgnoreCase(descriptorName)
					|| "shortestpath.ShortestPathPlugin".equals(name)
					|| name.endsWith(".ShortestPathPlugin")
					|| "ShortestPathPlugin".equals(type.getSimpleName()))
				{
					log.debug("RogueScape Shortest Path bridge: found {} ({})", descriptorName, name);
					return plugin;
				}
			}
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not inspect loaded plugins for Shortest Path", ex);
		}
		return null;
	}

	private boolean shortestPathPluginActive(Object plugin)
	{
		if (pluginManager == null || plugin == null)
		{
			return false;
		}
		try
		{
			java.lang.reflect.Method isPluginActive = pluginManager.getClass().getMethod("isPluginActive", Plugin.class);
			Object active = isPluginActive.invoke(pluginManager, plugin);
			return Boolean.TRUE.equals(active);
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			log.debug("Could not check Shortest Path active state; assuming active", ex);
			return true;
		}
	}

	private boolean setShortestPathTargetFields(Object shortestPath, WorldPoint target)
	{
		try
		{
			java.lang.reflect.Field targetField = findField(shortestPath.getClass(), "target");
			if (targetField == null)
			{
				shortestPathStatus = "Shortest Path API changed";
				return false;
			}
			targetField.setAccessible(true);
			targetField.set(shortestPath, target);

			java.lang.reflect.Field updateField = findField(shortestPath.getClass(), "pathUpdateScheduled");
			if (updateField != null)
			{
				updateField.setAccessible(true);
				updateField.setBoolean(shortestPath, true);
			}
			log.debug("RogueScape Shortest Path bridge: field target {}", target);
			return true;
		}
		catch (ReflectiveOperationException | RuntimeException ex)
		{
			shortestPathStatus = "Shortest Path bridge failed";
			log.debug("Could not set Shortest Path target fields", ex);
			return false;
		}
	}

	private static java.lang.reflect.Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
	{
		Class<?> current = type;
		while (current != null)
		{
			try
			{
				return current.getDeclaredMethod(name, parameterTypes);
			}
			catch (NoSuchMethodException ignored)
			{
				current = current.getSuperclass();
			}
		}
		return null;
	}

	private static java.lang.reflect.Field findField(Class<?> type, String name)
	{
		Class<?> current = type;
		while (current != null)
		{
			try
			{
				return current.getDeclaredField(name);
			}
			catch (NoSuchFieldException ignored)
			{
				current = current.getSuperclass();
			}
		}
		return null;
	}

	private static String provenanceLine(ProvenanceHint hint)
	{
		return hint == null || hint == ProvenanceHint.UNKNOWN ? "unknown source" : hint.name();
	}

	private boolean observeBossKillChat(String message)
	{
		if (rogueRun == null || message == null)
		{
			return false;
		}
		RunStage stage = rogueRun.currentEnteredStage();
		if (stage == null || stage.type() != RunStageType.BOSS)
		{
			return false;
		}
		String clean = BossKillChatMatcher.clean(message);
		if (!BossKillChatMatcher.matches(stage.name(), clean))
		{
			return false;
		}
		boolean recorded = rogueRun.recordBossDefeatSignal("chat: " + clean);
		if (recorded)
		{
			latestProvenanceSignal = "boss defeated: " + stage.name();
		}
		return recorded;
	}

	private String formatRules()
	{
		return "No bank access\nNo trading / GE\nStay in room regions\nStarter kit only\nRewards after rooms\nDeath = fail";
	}

	/** Toggles the custom in-game RogueScape pop-out window. */
	private void toggleWindow()
	{
		if (window != null)
		{
			window.toggle();
		}
	}

	/** Tabs shown in the in-game RogueScape window (Collection-Log-style top tabs). */
	private List<RogueScapeWindowOverlay.Tab> windowTabs()
	{
		List<RogueScapeWindowOverlay.Tab> tabs = new ArrayList<>();
		if (rogueRun == null || runSession == null || runLoop == null)
		{
			List<RogueScapeWindowOverlay.Block> lobby = new ArrayList<>();
			RunMode mode = panel != null ? panel.selectedMode() : ModePresetParser.parseMode(config.modePreset());
			RunPreset preset = panel != null ? panel.selectedPreset() : ModePresetParser.parsePreset(config.runPreset());
			String runTitle = panel != null && panel.selectedGoal() != null && !panel.selectedGoal().trim().isEmpty()
				? panel.selectedGoal().trim()
				: runModeName(mode);
			String seed = panel != null && panel.selectedSeed() != null && !panel.selectedSeed().trim().isEmpty()
				? panel.selectedSeed().trim()
				: config.seedText();
			lobby.add(RogueScapeWindowOverlay.Block.heading("RUN BUILDER"));
			lobby.add(RogueScapeWindowOverlay.Block.modeTiles(lobbyModeTiles(mode, seed)));
			lobby.add(RogueScapeWindowOverlay.Block.gap());
			lobby.add(RogueScapeWindowOverlay.Block.badge("Selected: " + runModeName(mode), startLine(mode, preset, seed),
				RogueScapeTheme.ACCENT, 0));
			lobby.add(RogueScapeWindowOverlay.Block.text("Run: " + runTitle, RogueScapeTheme.TEXT_PRIMARY));
			lobby.add(RogueScapeWindowOverlay.Block.modeTiles(startRunTile(mode)));
			lobby.add(RogueScapeWindowOverlay.Block.gap());
			List<String> preview = RunRouteBuilder.campaignPreviewRows(preset);
			if (mode == RunMode.SEEDED_RACE)
			{
				lobby.add(RogueScapeWindowOverlay.Block.heading("BALANCED SEED ROUTE"));
				lobby.add(RogueScapeWindowOverlay.Block.text("Seed creates the path; preset sets length and shape.",
					RogueScapeTheme.TEXT_PRIMARY));
			}
			else if (!preview.isEmpty())
			{
				lobby.add(RogueScapeWindowOverlay.Block.heading("CAMPAIGN PATH"));
				for (String row : preview)
				{
					lobby.add(RogueScapeWindowOverlay.Block.text(row,
						row.contains("[BOSS]") ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_PRIMARY));
				}
			}
			else
			{
				lobby.add(RogueScapeWindowOverlay.Block.heading("BALANCED ROUTE"));
				lobby.add(RogueScapeWindowOverlay.Block.text("Auto-builds region, supply, gear, and boss stages.",
					RogueScapeTheme.TEXT_PRIMARY));
			}
			tabs.add(new RogueScapeWindowOverlay.Tab("RUN BUILDER", lobby));

			List<RogueScapeWindowOverlay.Block> rules = new ArrayList<>();
			rules.add(RogueScapeWindowOverlay.Block.heading("RUN RULES"));
			for (String line : formatRules().split("\\n"))
			{
				rules.add(RogueScapeWindowOverlay.Block.text("- " + line, RogueScapeTheme.TEXT_PRIMARY));
			}
			tabs.add(new RogueScapeWindowOverlay.Tab("RULES", rules));

			List<RogueScapeWindowOverlay.Block> rewards = new ArrayList<>();
			rewards.add(RogueScapeWindowOverlay.Block.heading("RELIC BUILD"));
			rewards.add(RogueScapeWindowOverlay.Block.text("Rooms award relic choices after completion.", RogueScapeTheme.TEXT_PRIMARY));
			rewards.add(RogueScapeWindowOverlay.Block.text("Artifacts, modifiers, and score bonuses appear here once the run starts.",
				RogueScapeTheme.TEXT_MUTED));
			tabs.add(new RogueScapeWindowOverlay.Tab("BUILD", rewards));
			return tabs;
		}

		SidePanelViewModel vm = SidePanelViewModel.active(runLoop, PanelTab.RUN);

		// RUN CONTROL.
		List<RogueScapeWindowOverlay.Block> runCtl = new ArrayList<>();
		runCtl.add(RogueScapeWindowOverlay.Block.heading("RUN CONTROL"));
		addTextBlocks(runCtl, vm.headerRows());
		tabs.add(new RogueScapeWindowOverlay.Tab("RUN CONTROL", runCtl));

		// LIVE RUN — a progress bar, pending reward cards (if any), and the live status lines.
		tabs.add(new RogueScapeWindowOverlay.Tab("LIVE RUN", liveRunBlocks(vm)));

		// BUILD — the real relic-loadout aggregate (no fabricated archetype/synergy system exists).
		tabs.add(new RogueScapeWindowOverlay.Tab("BUILD", buildBlocks()));

		// ARTIFACTS — the held relics as an icon grid + names.
		tabs.add(new RogueScapeWindowOverlay.Tab("ARTIFACTS", artifactsBlocks()));

		// MODIFIERS.
		List<RogueScapeWindowOverlay.Block> mods = new ArrayList<>();
		mods.add(RogueScapeWindowOverlay.Block.heading("MODIFIERS"));
		addTextBlocks(mods, vm.modifierRows());
		tabs.add(new RogueScapeWindowOverlay.Tab("MODIFIERS", mods));

		// PROGRESSION.
		List<RogueScapeWindowOverlay.Block> progression = new ArrayList<>();
		progression.add(RogueScapeWindowOverlay.Block.heading("PROGRESSION"));
		addTextBlocks(progression, vm.progressionRows());
		tabs.add(new RogueScapeWindowOverlay.Tab("PROGRESSION", progression));

		return tabs;
	}

	private static List<RogueScapeWindowOverlay.ModeTile> lobbyModeTiles(RunMode selectedMode, String seed)
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Scavenger", "Scavenge rooms, draft power, fight bosses.",
			"Fresh source", RogueScapeTheme.POSITIVE, selectedMode == RunMode.FRESH_SOURCE || selectedMode == RunMode.UNSPECIFIED,
			"scavenger"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Rewarded", "Bosses unlock random gear and supplies.",
			"Campaign", RogueScapeTheme.RARITY_LEGENDARY, selectedMode == RunMode.BANK_DRAFT,
			"rewarded"));
		tiles.add(new RogueScapeWindowOverlay.ModeTile("Custom", "Build route, zones, and mods.",
			"Open builder", RogueScapeTheme.RARITY_EPIC, selectedMode == RunMode.CUSTOM_CREATOR,
			"custom"));
		return tiles;
	}

	private void selectRunBuilderMode(String actionId)
	{
		if (panel == null || runSession != null || rogueRun != null)
		{
			return;
		}
		if ("start-run".equals(actionId))
		{
			dispatchAction(PanelAction.START_RUN);
			return;
		}
		panel.selectRunBuilderMode(actionId);
		if ("custom".equals(actionId))
		{
			if (customBuilderWidgetWindow != null)
			{
				customBuilderWidgetWindow.setOpen(true);
			}
		}
		refreshSidePanel();
	}

	private List<RogueScapeWindowOverlay.ModeTile> startRunTile(RunMode mode)
	{
		List<RogueScapeWindowOverlay.ModeTile> tiles = new ArrayList<>();
		tiles.add(new RogueScapeWindowOverlay.ModeTile("START RUN",
			mode == RunMode.CUSTOM_CREATOR ? "Begin custom route" : "Begin selected route",
			"Click to launch", RogueScapeTheme.POSITIVE, false, "start-run"));
		return tiles;
	}

	private static String runModeName(RunMode mode)
	{
		if (mode == RunMode.BANK_DRAFT) return "Rewarded";
		if (mode == RunMode.CUSTOM_CREATOR) return "Custom";
		return "Scavenger";
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
		roomsTab.add(RogueScapeWindowOverlay.Block.badge("Room", panel == null ? "(none)" : panel.customSelectedRoomLabel(),
			RogueScapeTheme.INFO, 0));
		roomsTab.add(RogueScapeWindowOverlay.Block.badge("Allowed Collection", panel == null ? "All" : panel.customSelectedAllowanceLabel(),
			RogueScapeTheme.RARITY_EPIC, 0));
		roomsTab.add(RogueScapeWindowOverlay.Block.modeTiles(customRoomSelectorActions()));
		roomsTab.add(RogueScapeWindowOverlay.Block.gap());
		addConfirmedRouteBlocks(roomsTab);
		tabs.add(new RogueScapeWindowOverlay.Tab("ROOMS", roomsTab));

		List<RogueScapeWindowOverlay.Block> bosses = new ArrayList<>();
		bosses.add(RogueScapeWindowOverlay.Block.heading("BOSS SELECTOR"));
		bosses.add(RogueScapeWindowOverlay.Block.badge("Boss", panel == null ? "(auto)" : panel.customSelectedBossLabel(),
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
		if (customRoomEditorState == null || customRoomEditorState.selection().isEmpty())
		{
			zones.add(RogueScapeWindowOverlay.Block.text("No custom zone saved yet.", RogueScapeTheme.TEXT_MUTED));
			zones.add(RogueScapeWindowOverlay.Block.text("Use the world-map zone builder to paint region IDs for this run.",
				RogueScapeTheme.TEXT_PRIMARY));
		}
		else
		{
			zones.add(RogueScapeWindowOverlay.Block.badge(customRoomEditorState.selection().getName(),
				customRoomEditorState.selection().size() + " region ids selected", RogueScapeTheme.INFO, 0));
			zones.add(RogueScapeWindowOverlay.Block.text(customRoomEditorState.selection().toCsv(), RogueScapeTheme.TEXT_MUTED));
		}
		tabs.add(new RogueScapeWindowOverlay.Tab("ZONES", zones));

		List<RogueScapeWindowOverlay.Block> modBlocks = new ArrayList<>();
		modBlocks.add(RogueScapeWindowOverlay.Block.heading("STARTING MODIFIERS"));
		List<String> selectedMods = panel == null ? Collections.emptyList() : panel.selectedModifierIds();
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
		seed.add(RogueScapeWindowOverlay.Block.badge("Seed Preview",
			panel == null ? "mode=Scavenger;loadout=Naked" : panel.customSeedPreview(),
			RogueScapeTheme.ACCENT, 0));
		tabs.add(new RogueScapeWindowOverlay.Tab("SEED", seed));

		return tabs;
	}

	private RogueScapeCustomBuilderWidgetWindow.View customBuilderWidgetView()
	{
		if (panel == null)
		{
			return RogueScapeCustomBuilderWidgetWindow.View.empty();
		}
		return new RogueScapeCustomBuilderWidgetWindow.View(
			panel.customBuilderGameMode(),
			panel.customBuilderLoadout(),
			RogueScapeCustomRunFactory.loadoutKitLines(panel.customBuilderLoadout()),
			panel.customStrictness(),
			panel.customBankUnlocks(),
			panel.customTimeLimitMinutes(),
			panel.customBossLimit(),
			panel.customSelectedRoomLabel(),
			panel.customSelectedAllowanceLabel(),
			panel.customSelectedBossLabel(),
			panel.customRoomOptionLabels(),
			panel.customAllowanceOptionLabels(),
			panel.customBossOptionLabels(),
			panel.customModifierOptionLabels(),
			panel.selectedModifierLabels(),
			panel.selectedModifierIndexes(),
			panel.customModifierPageStart(),
			panel.customSelectedRoomIndex(),
			panel.customSelectedAllowanceIndex(),
			panel.customSelectedBossIndex(),
			panel.customRouteLabels(),
			panel.selectedRouteIndex(),
			panel.customSeedPreview()
		);
	}

	private void addConfirmedRouteBlocks(List<RogueScapeWindowOverlay.Block> out)
	{
		List<String> rooms = panel == null ? Collections.emptyList() : panel.selectedRoomLabels();
		if (rooms.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.text("Confirmed route is empty. Add rooms, then add a boss.",
				RogueScapeTheme.TEXT_MUTED));
		}
		else
		{
			int i = 0;
			int selectedIdx = panel == null ? -1 : panel.selectedRouteIndex();
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
		if (panel == null)
		{
			return "(auto)";
		}
		String boss = panel.selectedBossId();
		return boss == null || boss.isEmpty() ? "(auto)" : boss;
	}

	private String customGameModeLabel()
	{
		return panel == null ? "Scavenger" : panel.customBuilderGameMode();
	}

	private String customLoadoutLabel()
	{
		return panel == null ? "Naked" : panel.customBuilderLoadout();
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

	private void handleCustomBuilderAction(String actionId)
	{
		if (panel == null || actionId == null || runSession != null || rogueRun != null)
		{
			return;
		}
		if ("custom:mode-scavenger".equals(actionId))
		{
			panel.setCustomBuilderGameMode("Scavenger");
		}
		else if ("custom:mode-rewarded".equals(actionId))
		{
			panel.setCustomBuilderGameMode("Rewarded");
		}
		else if ("custom:loadout-naked".equals(actionId))
		{
			panel.setCustomBuilderLoadout("Naked");
		}
		else if ("custom:loadout-low".equals(actionId))
		{
			panel.setCustomBuilderLoadout("Low Gear");
		}
		else if ("custom:loadout-mid".equals(actionId))
		{
			panel.setCustomBuilderLoadout("Mid Gear");
		}
		else if ("custom:loadout-custom".equals(actionId))
		{
			panel.setCustomBuilderLoadout("Custom Kit");
		}
		else if ("custom:add-all".equals(actionId))
		{
			panel.addRoomForAllowance("All");
		}
		else if ("custom:add-supply".equals(actionId))
		{
			panel.addRoomForAllowance("Supply");
		}
		else if ("custom:add-armour".equals(actionId))
		{
			panel.addRoomForAllowance("Armour");
		}
		else if ("custom:add-weapons".equals(actionId))
		{
			panel.addRoomForAllowance("Weapons");
		}
		else if ("custom:add-skilling".equals(actionId))
		{
			panel.addRoomForAllowance("Skilling");
		}
		else if ("custom:add-shopping".equals(actionId))
		{
			panel.addRoomForAllowance("Shopping");
		}
		else if ("custom:room-prev".equals(actionId))
		{
			panel.customPreviousRoom();
		}
		else if ("custom:room-next".equals(actionId))
		{
			panel.customNextRoom();
		}
		else if (actionId.startsWith("custom:room-index:"))
		{
			panel.selectCustomRoomIndex(parseTrailingInt(actionId));
		}
		else if ("custom:room-page-up".equals(actionId))
		{
			panel.pageCustomRoomIndex(-10);
		}
		else if ("custom:room-page-down".equals(actionId))
		{
			panel.pageCustomRoomIndex(10);
		}
		else if ("custom:type-prev".equals(actionId))
		{
			panel.customPreviousAllowance();
		}
		else if ("custom:type-next".equals(actionId))
		{
			panel.customNextAllowance();
		}
		else if (actionId.startsWith("custom:type-index:"))
		{
			panel.selectCustomAllowanceIndex(parseTrailingInt(actionId));
		}
		else if ("custom:add-selected-room".equals(actionId))
		{
			panel.addSelectedCustomRoom();
		}
		else if ("custom:boss-prev".equals(actionId))
		{
			panel.customPreviousBoss();
		}
		else if ("custom:boss-next".equals(actionId))
		{
			panel.customNextBoss();
		}
		else if (actionId.startsWith("custom:boss-index:"))
		{
			panel.selectCustomBossIndex(parseTrailingInt(actionId));
		}
		else if ("custom:boss-page-up".equals(actionId))
		{
			panel.pageCustomBossIndex(-10);
		}
		else if ("custom:boss-page-down".equals(actionId))
		{
			panel.pageCustomBossIndex(10);
		}
		else if ("custom:add-selected-boss".equals(actionId))
		{
			panel.addSelectedCustomBoss();
		}
		else if ("custom:select-up".equals(actionId))
		{
			panel.selectPreviousRouteRow();
		}
		else if ("custom:select-down".equals(actionId))
		{
			panel.selectNextRouteRow();
		}
		else if (actionId.startsWith("custom:route-index:"))
		{
			panel.selectRouteRow(parseTrailingInt(actionId));
		}
		else if ("custom:move-up".equals(actionId))
		{
			panel.moveSelectedRouteUp();
		}
		else if ("custom:move-down".equals(actionId))
		{
			panel.moveSelectedRouteDown();
		}
		else if ("custom:remove-room".equals(actionId))
		{
			panel.removeLastCustomRoom();
		}
		else if ("custom:clear-route".equals(actionId))
		{
			panel.clearCustomRoute();
		}
		else if (actionId.startsWith("custom:modifier-index:"))
		{
			panel.toggleCustomModifierIndex(parseTrailingInt(actionId));
		}
		else if ("custom:modifier-page-up".equals(actionId))
		{
			panel.pageCustomModifierIndex(-10);
		}
		else if ("custom:modifier-page-down".equals(actionId))
		{
			panel.pageCustomModifierIndex(10);
		}
		else if ("custom:clear-modifiers".equals(actionId))
		{
			panel.clearCustomModifiers();
		}
		else if ("custom:cycle-strictness".equals(actionId))
		{
			panel.cycleCustomStrictness();
		}
		else if ("custom:toggle-bank".equals(actionId))
		{
			panel.toggleCustomBankUnlocks();
		}
		else if ("custom:cycle-time".equals(actionId))
		{
			panel.cycleCustomTimeLimit();
		}
		else if ("custom:cycle-boss-limit".equals(actionId))
		{
			panel.cycleCustomBossLimit();
		}
		else if ("custom:load-seed".equals(actionId))
		{
			panel.applyCustomSeed(panel.selectedSeed());
		}
		else if ("custom:start-run".equals(actionId))
		{
			panel.selectRunBuilderMode("custom");
			if (customBuilderWidgetWindow != null)
			{
				customBuilderWidgetWindow.setOpen(false);
			}
			startRunOnClientThread();
			return;
		}
		else if ("custom:first-boss".equals(actionId))
		{
			panel.selectFirstBoss();
		}
		else if ("custom:edit-zone".equals(actionId) && customRoomEditorState != null)
		{
			customRoomEditorState.setEditing(!customRoomEditorState.isEditing());
		}
		else if ("custom:use-zone".equals(actionId))
		{
			activateCustomRoomForRun();
		}
		refreshSidePanel();
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

	private List<RogueScapeWindowOverlay.Block> liveRunBlocks(SidePanelViewModel vm)
	{
		List<RogueScapeWindowOverlay.Block> live = new ArrayList<>();
		live.add(RogueScapeWindowOverlay.Block.heading("LIVE RUN"));
		int total = countStages(false);
		int cleared = countStages(true);
		double prog = total > 0 ? (double) cleared / total : 0.0;
		live.add(RogueScapeWindowOverlay.Block.statBar("Route", prog, cleared + " / " + total,
			RogueScapeTheme.BAR_PROGRESS));

		if (runLoop != null)
		{
			live.add(RogueScapeWindowOverlay.Block.badge(runLoop.phase().getDisplayName(),
				"Time " + runLoop.runElapsedLabel(), RogueScapeTheme.ACCENT, 0));
		}

		RunStage stage = rogueRun == null ? null : rogueRun.currentEnteredStage();
		if (stage != null)
		{
			live.add(RogueScapeWindowOverlay.Block.heading("CURRENT STAGE"));
			live.add(RogueScapeWindowOverlay.Block.badge(stage.name(), stage.type().name(),
				stage.type() == RunStageType.BOSS ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.GOLD, 0));
			live.add(RogueScapeWindowOverlay.Block.text("Objective: " + stage.objectiveProgressLabel(),
				stage.objectiveComplete() ? RogueScapeTheme.POSITIVE : RogueScapeTheme.TEXT_PRIMARY));
		}

		String next = nextUnclearedStageName();
		if (!next.isEmpty() && (stage == null || !next.equals(stage.name())))
		{
			live.add(RogueScapeWindowOverlay.Block.text("Next: " + next, RogueScapeTheme.TEXT_MUTED));
		}

		List<RogueScapeRewardOverlay.Card> pendingCards = pendingRewardCards();
		if (pendingCards != null)
		{
			live.add(RogueScapeWindowOverlay.Block.heading("REWARD READY"));
			live.add(RogueScapeWindowOverlay.Block.cards(pendingCards));
			return live;
		}

		live.add(RogueScapeWindowOverlay.Block.heading("BUILD STATE"));
		live.add(RogueScapeWindowOverlay.Block.text("Score: " + (rogueRun == null ? 0 : rogueRun.effectiveScore()),
			RogueScapeTheme.GOLD));
		live.add(RogueScapeWindowOverlay.Block.text("Relics: " + (rogueRun == null ? 0 : rogueRun.heldRelics().size())
			+ "   Legal/Illegal: " + (rogueRun == null ? "0/0" : rogueRun.legalCount() + "/" + rogueRun.illegalCount()),
			rogueRun != null && rogueRun.illegalCount() > 0 ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_PRIMARY));

		for (String row : vm.statusRows())
		{
			String lower = row == null ? "" : row.toLowerCase();
			if (lower.contains("over ") || lower.contains("outside") || lower.contains("illegal")
				|| lower.contains("blocked") || lower.contains("cannot"))
			{
				live.add(RogueScapeWindowOverlay.Block.text(row, RogueScapeTheme.NEGATIVE));
			}
		}
		if (!latestProvenanceSignal.isEmpty())
		{
			live.add(RogueScapeWindowOverlay.Block.text("Signal: " + latestProvenanceSignal,
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
	private int countStages(boolean clearedOnly)
	{
		if (runSession == null)
		{
			return 0;
		}
		int n = 0;
		for (com.pluginideahub.roguescape.core.RunStage stage : runSession.route().stages())
		{
			if (!clearedOnly || stage.isCleared())
			{
				n++;
			}
		}
		return n;
	}

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
		List<Relic> held = rogueRun.heldRelics();
		out.add(RogueScapeWindowOverlay.Block.heading("ARTIFACTS  (" + held.size() + ")"));
		if (held.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.text("No artifacts yet.", RogueScapeTheme.TEXT_MUTED));
			out.add(RogueScapeWindowOverlay.Block.text(
				"Claim relics from reward chests to build your collection.", RogueScapeTheme.TEXT_MUTED));
			return out;
		}
		// Pad to whole rows of 6 so empty slots read as "room for more" (no fake locks).
		int slots = Math.max(12, ((held.size() + 5) / 6) * 6);
		int[] ids = new int[slots];
		for (int i = 0; i < held.size(); i++)
		{
			ids[i] = relicIcon(held.get(i));
		}
		out.add(RogueScapeWindowOverlay.Block.itemGrid(ids, Integer.MAX_VALUE));
		out.add(RogueScapeWindowOverlay.Block.gap());
		for (Relic relic : held)
		{
			out.add(RogueScapeWindowOverlay.Block.text("- " + relic.name(), RogueScapeTheme.TEXT_PRIMARY));
		}
		return out;
	}

	/** BUILD tab — the aggregate effect of the held relic loadout (all real, relic-derived data). */
	private List<RogueScapeWindowOverlay.Block> buildBlocks()
	{
		List<RogueScapeWindowOverlay.Block> out = new ArrayList<>();
		out.add(RogueScapeWindowOverlay.Block.heading("BUILD - RELIC LOADOUT"));
		List<Relic> held = rogueRun.heldRelics();
		out.add(RogueScapeWindowOverlay.Block.text("Relics held: " + held.size(), RogueScapeTheme.TEXT_PRIMARY));
		int bonus = rogueRun.relicEngine().scoreBonus();
		out.add(RogueScapeWindowOverlay.Block.text("Relic score bonus: +" + bonus,
			bonus > 0 ? RogueScapeTheme.POSITIVE : RogueScapeTheme.TEXT_MUTED));

		java.util.Set<BankItemCategory> restricted = rogueRun.relicRestrictedCategories();
		if (!restricted.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.gap());
			out.add(RogueScapeWindowOverlay.Block.text("Forbidden categories:", RogueScapeTheme.TEXT_PRIMARY));
			for (BankItemCategory c : restricted)
			{
				out.add(RogueScapeWindowOverlay.Block.text("  - " + humanCategory(c), RogueScapeTheme.NEGATIVE));
			}
		}

		java.util.Map<BankItemCategory, Integer> limits = rogueRun.relicCategoryLimits();
		if (!limits.isEmpty())
		{
			java.util.Set<BankItemCategory> over = rogueRun.relicOverLimit();
			out.add(RogueScapeWindowOverlay.Block.gap());
			out.add(RogueScapeWindowOverlay.Block.text("Category limits:", RogueScapeTheme.TEXT_PRIMARY));
			for (java.util.Map.Entry<BankItemCategory, Integer> e : limits.entrySet())
			{
				int have = rogueRun.relicEngine().categoryCount(e.getKey());
				boolean isOver = over.contains(e.getKey());
				out.add(RogueScapeWindowOverlay.Block.text(
					"  Max " + e.getValue() + " " + humanCategory(e.getKey()) + "  (have " + have + ")",
					isOver ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_MUTED));
			}
		}

		if (held.isEmpty())
		{
			out.add(RogueScapeWindowOverlay.Block.gap());
			out.add(RogueScapeWindowOverlay.Block.text(
				"No relics chosen yet - your build is wide open.", RogueScapeTheme.TEXT_MUTED));
		}
		return out;
	}

	private static String humanCategory(BankItemCategory c)
	{
		return c == null ? "" : c.name().toLowerCase().replace('_', ' ');
	}

	/** Maps a reward-card index to its choose action (reuses the panel-action dispatch). */
	private PanelAction rewardActionForIndex(int idx)
	{
		switch (idx)
		{
			case 0: return PanelAction.CHOOSE_REWARD_1;
			case 1: return PanelAction.CHOOSE_REWARD_2;
			default: return PanelAction.CHOOSE_REWARD_3;
		}
	}

	/** ROLL SUPPLIES content; non-null only while a reward choice is actually pending. */
	private RogueScapeRewardOverlay.RewardView rewardView()
	{
		if (!config.experimentalJournalTab() || runLoop == null)
		{
			return null;
		}
		if (runLoop.phase() != RunPhase.BASE_REWARD || runLoop.baseRewardResolved())
		{
			return null;
		}
		RewardDraft draft = runLoop.pendingRewardDraft();
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
			return "ROLL REWARD";
		}
		switch (draft.chestType())
		{
			case RELIC: return "CLAIM YOUR RELIC";
			case SUPPLY: return "ROLL SUPPLIES";
			case UNLOCK: return "CHOOSE AN UNLOCK";
			case BANK_UNLOCK: return "UNLOCK BANK ITEM";
			default: return "ROLL REWARD";
		}
	}

	private static String rewardSubtitle(RewardDraft draft)
	{
		if (draft == null || draft.chestType() == null)
		{
			return "Choose one reward for the next stage";
		}
		switch (draft.chestType())
		{
			case RELIC: return "Choose one power for the next stage";
			case SUPPLY: return "Choose one random supply bundle";
			case UNLOCK: return "Choose one system to unlock for the run";
			case BANK_UNLOCK: return "Choose one bank item to make legal";
			default: return "Choose one reward for the next stage";
		}
	}

	private List<String> rewardRailRows()
	{
		List<String> rows = new ArrayList<>();
		if (runSession != null)
		{
			rows.add("Floor: " + countStages(true) + " / " + countStages(false));
			String completed = stageName(runLoop == null ? null : runLoop.completedStageId());
			if (!completed.isEmpty())
			{
				rows.add("Cleared: " + completed);
			}
			String next = nextUnclearedStageName();
			if (!next.isEmpty())
			{
				rows.add("Next: " + next);
			}
		}
		rows.add("Build:");
		rows.add(rogueRun == null || rogueRun.heldRelics().isEmpty()
			? "No relic build yet"
			: rogueRun.heldRelics().get(rogueRun.heldRelics().size() - 1).name());
		rows.add("");
		rows.add("Score: " + (rogueRun == null ? 0 : rogueRun.effectiveScore()));
		rows.add("Relics: " + (rogueRun == null ? 0 : rogueRun.heldRelics().size()));
		rows.add("Legal: " + (rogueRun == null ? 0 : rogueRun.legalCount()));
		rows.add("Illegal: " + (rogueRun == null ? 0 : rogueRun.illegalCount()));
		List<String> modifiers = rewardModifierRows();
		if (!modifiers.isEmpty())
		{
			rows.add("");
			rows.add("Modifiers:");
			for (String modifier : modifiers)
			{
				rows.add(modifier);
			}
		}
		if (!latestProvenanceSignal.isEmpty())
		{
			rows.add("");
			rows.add("Signal:");
			rows.add(latestProvenanceSignal);
		}
		return rows;
	}

	private String stageName(String stageId)
	{
		if (runSession == null || stageId == null || stageId.isEmpty())
		{
			return "";
		}
		com.pluginideahub.roguescape.core.RunStage stage = runSession.route().stageById(stageId);
		return stage == null ? "" : stage.name();
	}

	private String nextUnclearedStageName()
	{
		if (runSession == null)
		{
			return "";
		}
		for (com.pluginideahub.roguescape.core.RunStage stage : runSession.route().stages())
		{
			if (!stage.isCleared())
			{
				return stage.name();
			}
		}
		return "";
	}

	private List<String> rewardModifierRows()
	{
		List<String> rows = new ArrayList<>();
		if (rogueRun == null)
		{
			return rows;
		}
		rows.add("Strictness: " + rogueRun.strictness());
		if (rogueRun.bankAccessAllowed())
		{
			rows.add("Bank unlocks active");
		}
		int relicCount = 0;
		for (Relic relic : rogueRun.heldRelics())
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
		if (rogueRun == null)
		{
			return ids;
		}
		for (Relic relic : rogueRun.heldRelics())
		{
			ids.add(relicIcon(relic));
		}
		return ids;
	}

	/** Reward cards for an unresolved pending draft, or {@code null} if none is pending. */
	private List<RogueScapeRewardOverlay.Card> pendingRewardCards()
	{
		if (runLoop == null || runLoop.phase() != RunPhase.BASE_REWARD || runLoop.baseRewardResolved())
		{
			return null;
		}
		RewardDraft draft = runLoop.pendingRewardDraft();
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
			return ITEM_DRAGONSTONE;
		}
		switch (option.chestType())
		{
			case FOOD: return ITEM_SHARK;
			case POTION: return ITEM_PRAYER_POTION;
			case AMMO: return ITEM_BRONZE_ARROW;
			case UTILITY: return ITEM_GLORY;
			case UNLOCK: return ITEM_DRAGONSTONE;
			case BANK_UNLOCK: return ITEM_COINS;
			default: return ITEM_DRAGONSTONE;
		}
	}

	/** Display rarity heuristic (placeholder until rewards carry a real value tier): more effects = rarer. */
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
			return ITEM_DRAGONSTONE;
		}
		switch (kind)
		{
			case SCORING_BIAS: return ITEM_COINS;
			case CATEGORY_LIMIT: return ITEM_SHARK;
			case ONE_SHOT_MERCY: return ITEM_GLORY;
			default: return ITEM_DRAGONSTONE;
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
	private void logJournalDump()
	{
		if (journalProbe == null)
		{
			return;
		}
		for (String line : journalProbe.dumpLines())
		{
			log.info("[RogueScape journal] {}", line);
		}
	}
}
