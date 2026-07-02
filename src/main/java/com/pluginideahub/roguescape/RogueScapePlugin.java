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
	static final int ITEM_DRAGONSTONE = 1631; // Uncut dragonstone
	static final int ITEM_COINS = 995;        // Coins
	static final int ITEM_SHARK = 385;        // Shark
	static final int ITEM_GLORY = 1712;       // Amulet of glory(6)
	static final int ITEM_PRAYER_POTION = 2434;
	static final int ITEM_BRONZE_ARROW = 882;

	@Inject
	RogueScapeConfig config;

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
	private EventBus eventBus;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	private ShortestPathBridge shortestPathBridge;
	private RogueScapeWindowContent windowContent;

	private RogueScapeIcons icons;
	private PluginIdeaHubUiModel uiModel;
	private PluginIdeaHubOverlay overlay;
	private RogueScapeObjectiveOverlay objectiveOverlay;
	private RogueScapeRoomMaskOverlay roomMaskOverlay;
	private RogueScapeActiveRoomWorldMapOverlay activeRoomWorldMapOverlay;
	private RogueScapeCustomRoomWorldMapOverlay customRoomWorldMapOverlay;
	RogueScapeCustomRoomEditorState customRoomEditorState;
	private RogueScapeJournalWidgetProbe journalProbe;
	private RogueScapeJournalTabAdapter journalTabAdapter;
	RogueScapeWindowOverlay window;
	RogueScapeCustomBuilderWidgetWindow customBuilderWidgetWindow;
	private RogueScapeWidgetWindow widgetWindow;
	private int clogDumpCountdown = -1;
	private RogueScapeRewardOverlay rewardOverlay;
	RogueScapePanel panel;
	private NavigationButton navigationButton;
	RogueScapeRunSession runSession;
	RogueScapeRun rogueRun;
	RogueScapeRunLoop runLoop;
	private InventorySnapshot previousInventorySnapshot = new InventorySnapshot();
	private final ProvenanceSignalTracker provenanceSignals = new ProvenanceSignalTracker();
	private final RunHistory runHistory = new RunHistory();
	private RunRecap lastRecap;
	private boolean recapRecorded;
	private int bankPoolSizeNoted = -1;
	private String latestObservedItem = "";
	String latestProvenanceSignal = "";
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
		shortestPathBridge = new ShortestPathBridge(eventBus);
		windowContent = new RogueScapeWindowContent(this);
		// Overlays repaint every game frame, so no explicit repaint hook is needed.
		icons = new RogueScapeIcons(itemManager, spriteManager, null);
		window = new RogueScapeWindowOverlay(windowContent::windowTabs, icons);
		window.setBookMode(windowContent::isBookSpread);
		window.setModeTileHandler(windowContent::selectRunBuilderMode);
		window.setCanvasSize(() -> new Dimension(client.getCanvasWidth(), client.getCanvasHeight()));
		overlayManager.add(window);
		mouseManager.registerMouseListener(window);
		customBuilderWidgetWindow = new RogueScapeCustomBuilderWidgetWindow(client, clientThread, config::customBuilderWindow,
			windowContent::customBuilderWidgetView, windowContent::handleCustomBuilderAction);
		mouseManager.registerMouseListener(customBuilderWidgetWindow);
		// SPIKE: real widget-based window (proves the Collection-Log-style approach).
		widgetWindow = new RogueScapeWidgetWindow(client, clientThread, config::experimentalJournalTab, windowContent::windowTabs);
		mouseManager.registerMouseListener(widgetWindow);
		rewardOverlay = new RogueScapeRewardOverlay(
			windowContent::rewardView,
			icons,
			idx -> dispatchAction(windowContent.rewardActionForIndex(idx)),
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
		shortestPathBridge = null;
		windowContent = null;
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
		if (rogueRun == null || event == null)
		{
			return;
		}
		if (event.getContainerId() == InventoryID.BANK.getId())
		{
			populateBankDraftPool(event.getItemContainer());
			return;
		}
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
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

	/**
	 * Feeds the run's {@link com.pluginideahub.roguescape.core.reward.BankDraftPool} from the
	 * real bank whenever it is open. Ids are numeric strings so unlock checks line up with the
	 * live {@code ItemDelta} wiring; categories/tiers come from the heuristic classifier and
	 * live GE prices, which is what the fairness policy draws on.
	 */
	private void populateBankDraftPool(net.runelite.api.ItemContainer bank)
	{
		if (bank == null || rogueRun == null)
		{
			return;
		}
		for (net.runelite.api.Item item : bank.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			String name = itemName(item.getId());
			if (name == null || name.isEmpty() || "null".equals(name))
			{
				continue;
			}
			long price = itemManager != null ? itemManager.getItemPrice(item.getId()) : 0L;
			rogueRun.bankPool().add(new BankItem(
				Integer.toString(item.getId()),
				name,
				BankItemClassifier.guessCategory(name),
				ValueTier.ofPrice(price),
				price));
		}
		int size = rogueRun.bankPool().size();
		if (size != bankPoolSizeNoted)
		{
			bankPoolSizeNoted = size;
			latestProvenanceSignal = "Bank read — " + size + " items eligible for the draft.";
			refreshSidePanel();
		}
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
			String signal = rules.warnLeaveRoom() && !rogueRun.currentRegionAllowed()
				? "⚠ Outside allowed room region!"
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

		if (!enforcementActive() || !config.enforcementBlocking())
		{
			return;
		}
		RogueScapeEnforcementRules rules = RogueScapeEnforcementRules.forRun(rogueRun);
		boolean inside = rogueRun.currentRegionAllowed();
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
				event.getMenuOption(), event.getMenuTarget(), rules, rogueRun.currentRegionAllowed());
			if (decision == MenuEnforcementDecision.BLOCK)
			{
				if (config.enforcementBlocking())
				{
					event.consume();
					if (runSession != null)
					{
						runSession.recordRunLoopNote("Blocked: " + event.getMenuOption());
					}
					return;
				}
				if (runSession != null)
				{
					runSession.recordRunLoopNote("Forbidden (warn-only): " + event.getMenuOption());
					latestProvenanceSignal = "Forbidden action: " + event.getMenuOption();
				}
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

	void activateCustomRoomForRun()
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
			RoomKind kind = existing == null ? RoomKind.SUPPLY : existing.roomKind();
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
		uiModel.recordManualAction("RogueScape live: " + itemsSummary());
		refreshSidePanel();
	}

	void refreshSidePanel()
	{
		maybeRecordRecap();
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
	 * Snapshots the immutable {@link RunRecap} the first time a run is seen in a terminal
	 * state, and adds it to the session {@link RunHistory} (personal bests, export).
	 */
	private void maybeRecordRecap()
	{
		if (recapRecorded || runSession == null || rogueRun == null || runLoop == null
			|| runSession.runState() == RunState.ACTIVE)
		{
			return;
		}
		lastRecap = RunRecap.snapshot(rogueRun, rogueRun.relicEngine(), runLoop.runElapsedMillis());
		runHistory.add(lastRecap);
		recapRecorded = true;
	}

	private void exportRecapToClipboard()
	{
		if (lastRecap == null)
		{
			return;
		}
		try
		{
			String markdown = RecapExport.toMarkdown(lastRecap);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(markdown), null);
			latestProvenanceSignal = "Recap copied — run " + runHistory.size() + " in this journal.";
		}
		catch (RuntimeException ex)
		{
			log.debug("Could not copy recap to clipboard", ex);
			latestProvenanceSignal = "Recap copy failed";
		}
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
		// No user seed -> start the exact catalogue route the Contract previews, so the run is
		// always the one you signed for, never a surprise re-roll.
		String seed = (rawSeed == null || rawSeed.trim().isEmpty())
			? (windowContent != null ? windowContent.selectedRouteSeed() : null)
			: rawSeed.trim();
		RunPreset preset = panel != null ? panel.selectedPreset() : RunPreset.UNSPECIFIED;

		boolean custom = mode == RunMode.CUSTOM_CREATOR && panel != null;
		recapRecorded = false;
		bankPoolSizeNoted = -1;
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
					.bankUnlocks(panel.customBankUnlocks())
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
			.setBankAccessAllowed(config.bankAccessAllowed())
			.setCurses(windowContent != null ? windowContent.selectedCurses() : java.util.Collections.emptySet())
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
			case DEV_ENTER_ROOM:
				// Emulate walking into the current stage's allowed region — skips travel.
				if (rogueRun != null && runLoop != null)
				{
					StageRegionRule rule = rogueRun.currentStageRule();
					if (rule != null && rule.restrictsRegion() && !rule.allowedRegionIds().isEmpty())
					{
						rogueRun.moveToRegion(rule.allowedRegionIds().iterator().next());
					}
					runLoop.markNow(now);
				}
				break;
			case DEV_BOSS_KILL:
				// Emulate the boss falling — same signal the chat matcher fires.
				if (rogueRun != null && runLoop != null)
				{
					rogueRun.recordBossDefeatSignal("dev");
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
			case EXPORT_RECAP:
				exportRecapToClipboard();
				break;
			case RESET_RUN:
				resetToLobby();
				break;
		}
		refreshSidePanel();
	}

	void startRunOnClientThread()
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
		lines.add("Bank unlocks: " + (rogueRun.bankAccessAllowed() ? "on" : "off"));
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
		lines.add(itemsSummary(view));
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

	private String itemsSummary()
	{
		if (rogueRun == null)
		{
			return "Items collected: 0";
		}
		return "Items collected: " + rogueRun.itemsCollected();
	}

	private String itemsSummary(OverlayViewModel view)
	{
		return "Items collected: " + view.itemsCollected();
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
			rogueRun.currentRegionAllowed());
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
		if (shortestPathBridge != null && shortestPathBridge.setTarget(target))
		{
			lastShortestPathTargetKey = key;
			shortestPathStatus = shortestPathBridge.status();
		}
	}

	private void clearShortestPathTarget()
	{
		if (lastShortestPathTargetKey.isEmpty() && shortestPathStatus.isEmpty())
		{
			return;
		}
		if (shortestPathBridge != null)
		{
			shortestPathBridge.clear();
			shortestPathStatus = shortestPathBridge.status();
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

	String formatRules()
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
	int countStages(boolean clearedOnly)
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

	String nextUnclearedStageName()
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

	static String runModeName(RunMode mode)
	{
		if (mode == RunMode.BANK_DRAFT) return "Boss Ladder";
		if (mode == RunMode.CUSTOM_CREATOR) return "Custom";
		return "Dungeon Crawl";
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
