package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import com.pluginideahub.roguescape.core.region.BossLibrary;
import com.pluginideahub.roguescape.core.region.RoomDefinition;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.RoomLibrary;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.ui.PanelAction;
import com.pluginideahub.roguescape.core.ui.SidePanelViewModel;
import com.pluginideahub.roguescape.ui.CollapsibleSection;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomEditorState;
import com.pluginideahub.roguescape.ui.RogueScapeTheme;
import com.pluginideahub.roguescape.ui.StatBar;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.runelite.client.ui.PluginPanel;

/**
 * Vertical, section-based RogueScape side panel.
 *
 * <p>Replaces the old tabbed layout with a single scrolling column of
 * {@link CollapsibleSection}s, matching the target design: RUN CONTROL,
 * LIVE RUN, BUILD, ARTIFACTS, MODIFIERS, PROGRESSION, ZONE BUILDER, RELICS,
 * SETTINGS.
 */
public class RogueScapePanel extends PluginPanel
{
	private final Consumer<PanelAction> actionHandler;
	private final RogueScapeCustomRoomEditorState roomEditorState;
	private final Runnable saveRoomRequest;
	private final Runnable useRoomRequest;
	private final BooleanSupplier devModeSupplier;

	private final JPanel column = new JPanel();

	// RUN CONTROL widgets
	private final JComboBox<String> modeCombo = new JComboBox<>();
	private final JComboBox<String> presetCombo = new JComboBox<>();
	private final JTextField seedField = new JTextField("");
	private final JPanel runControlActions = new JPanel();
	private final JPanel modeTilesPanel = new JPanel();
	private final JPanel presetCardsPanel = new JPanel();
	private final JPanel campaignPreviewPanel = new JPanel();

	// ROUTE widgets
	private final List<RoomDefinition> roomDefs = new ArrayList<>();
	private final List<RoomDefinition> bossDefs = new ArrayList<>();
	private final List<String> selectedRoomIds = new ArrayList<>();
	private final List<String> selectedRoomAllowances = new ArrayList<>();
	private final JComboBox<String> roomCombo = new JComboBox<>();
	private final JComboBox<String> bossCombo = new JComboBox<>();
	private final JTextArea selectedRoomsArea = new JTextArea(4, 14);
	private final JPanel routePickerPanel = new JPanel();
	private final JPanel routePreviewPanel = new JPanel();
	private int selectedRouteIndex = -1;
	private int customRoomCursor = 0;
	private int customBossCursor = 0;
	private int customAllowanceCursor = 0;
	private int customModifierCursor = 0;
	private String customBuilderGameMode = "Scavenger";
	private String customBuilderLoadout = "Naked";
	private String customStrictness = "Balanced";
	private boolean customBankUnlocks;
	private int customTimeLimitMinutes;
	private int customBossLimit;
	private static final String[] CUSTOM_ALLOWANCES = {"Supply", "Armour", "Weapons", "Skilling", "All", "Shopping"};

	// STARTING CURSES widgets
	private final List<Relic> modifierDefs = new ArrayList<>();
	private final List<String> selectedModifierIds = new ArrayList<>();
	private final JComboBox<String> modifierCombo = new JComboBox<>();
	private final JTextArea selectedModifiersArea = new JTextArea(3, 14);

	// LIVE RUN content (rebuilt each render)
	private final JPanel liveRunContent = new JPanel();

	// BUILD / ARTIFACTS / MODIFIERS / PROGRESSION content (rebuilt each render)
	private final JPanel buildContent = new JPanel();
	private final JPanel artifactsContent = new JPanel();
	private final JPanel modifiersContent = new JPanel();
	private final JPanel progressionContent = new JPanel();

	// DEV TOOLS section (visibility tracks developer mode)
	private CollapsibleSection devSection;

	// ZONE BUILDER widgets
	private final JTextField zoneNameField = new JTextField("My Zone");
	private final JButton addZoneToggleBtn = new JButton("Start adding regions");
	private final JTextArea zoneStatusArea = new JTextArea(4, 14);
	private final JTextArea zoneRegionsArea = new JTextArea(4, 14);
	private final JButton saveZoneBtn = new JButton("Save Zone");
	private final JButton clearZoneBtn = new JButton("Clear regions");
	private final JButton useZoneBtn = new JButton("Use zone for current run");

	private SidePanelViewModel lastModel = null;

	public RogueScapePanel(RogueScapeCustomRoomEditorState roomEditorState,
		Runnable saveRoomRequest,
		Runnable useRoomRequest,
		Consumer<PanelAction> actionHandler,
		BooleanSupplier devModeSupplier)
	{
		super();
		this.roomEditorState = roomEditorState;
		this.saveRoomRequest = saveRoomRequest;
		this.useRoomRequest = useRoomRequest;
		this.actionHandler = actionHandler;
		this.devModeSupplier = devModeSupplier;

		if (this.roomEditorState != null)
		{
			zoneNameField.setText(this.roomEditorState.selection().getName());
		}

		setLayout(new BorderLayout());
		setBackground(RogueScapeTheme.PANEL_BG);
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
		column.setBackground(RogueScapeTheme.PANEL_BG);

		this.devSection = buildDevToolsSection();

		column.add(buildPluginHeader());
		column.add(vGap(4));
		column.add(buildRunBuilderSection());
		column.add(buildLiveRunSection());
		column.add(buildBuildSection());
		column.add(buildArtifactsSection());
		column.add(buildModifiersSection());
		column.add(buildProgressionSection());
		column.add(buildRelicsSection());
		column.add(devSection);
		column.add(buildSettingsSection());
		column.add(Box.createVerticalGlue());

		add(column, BorderLayout.NORTH);

		if (this.roomEditorState != null)
		{
			this.roomEditorState.onChange(() -> javax.swing.SwingUtilities.invokeLater(this::updateZoneBuilder));
		}

		updateZoneBuilder();
		updateCampaignPreview();
		updateRoute(null);
	}

	public void render(SidePanelViewModel model)
	{
		this.lastModel = model;
		updateRunControl(model);
		updateRoute(model);
		updateLiveRun(model);
		updateBuild(model);
		updateArtifacts(model);
		updateModifiers(model);
		updateProgression(model);
		updateDevTools(model);
		column.revalidate();
		column.repaint();
	}

	public RunMode selectedMode()
	{
		switch (modeCombo.getSelectedIndex())
		{
			case 0: return RunMode.FRESH_SOURCE;
			case 1: return RunMode.BANK_DRAFT;
			case 2: return RunMode.CUSTOM_CREATOR;
			default: return RunMode.UNSPECIFIED;
		}
	}

	public RunPreset selectedPreset()
	{
		switch (presetCombo.getSelectedIndex())
		{
			case 1: return RunPreset.GOBLIN_RAT;
			case 2: return RunPreset.IRON_SCRAPPER;
			case 3: return RunPreset.MAGE_SPARK;
			case 4: return RunPreset.ARCHERS_GAMBLE;
			case 5: return RunPreset.MONK_MODE;
			case 6: return RunPreset.WILDERNESS_RAT;
			case 7: return RunPreset.CLUE_GREMLIN;
			case 8: return RunPreset.MAX_MAIN_DRAFT;
			default: return RunPreset.UNSPECIFIED;
		}
	}

	public String selectedGoal()
	{
		return selectedRunTitle();
	}

	public String selectedSeed()
	{
		String seed = seedField.getText();
		return seed == null ? "" : seed.trim();
	}

	public void selectRunBuilderMode(String actionId)
	{
		if ("scavenger".equals(actionId))
		{
			selectRunBuilderState(0, 0, "");
		}
		else if ("rewarded".equals(actionId))
		{
			selectRunBuilderState(1, 0, "");
		}
		else if ("custom".equals(actionId))
		{
			selectRunBuilderState(2, 0, "");
		}
	}

	private CollapsibleSection buildRunBuilderSection()
	{
		CollapsibleSection section = new CollapsibleSection("Run Builder");
		JPanel shell = new JPanel();
		shell.setLayout(new BoxLayout(shell, BoxLayout.Y_AXIS));
		shell.setBackground(RogueScapeTheme.SECTION_BG);
		shell.setAlignmentX(Component.LEFT_ALIGNMENT);

		CardLayout cards = new CardLayout();
		JPanel content = new JPanel(cards);
		content.setBackground(RogueScapeTheme.SECTION_BG);
		content.setAlignmentX(Component.LEFT_ALIGNMENT);
		content.putClientProperty("roguescape.builderCard", "Type");
		content.add(buildRunControlTab(), "Type");
		content.add(buildRouteTab(), "Route");
		content.add(buildZoneBuilderTab(), "Zone");
		content.add(buildCursesTab(), "Mods");

		JPanel tabRow = new JPanel(new java.awt.GridLayout(1, 4, 3, 0));
		tabRow.setBackground(RogueScapeTheme.SECTION_BG);
		tabRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		tabRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		tabRow.add(builderTabButton("Run", "Type", cards, content));
		tabRow.add(builderTabButton("Map", "Route", cards, content));
		tabRow.add(builderTabButton("Zone", "Zone", cards, content));
		tabRow.add(builderTabButton("Cur", "Mods", cards, content));

		shell.add(tabRow);
		shell.add(content);
		section.content().add(shell);
		return section;
	}

	private JButton builderTabButton(String label, String cardName, CardLayout cards, JPanel content)
	{
		JButton button = new JButton(label);
		styleButton(button, RogueScapeTheme.ButtonRole.NEUTRAL);
		button.setMargin(new java.awt.Insets(2, 2, 2, 2));
		button.addActionListener(e ->
		{
			cards.show(content, cardName);
			content.putClientProperty("roguescape.builderCard", cardName);
		});
		return button;
	}

	private JPanel builderTab()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(RogueScapeTheme.SECTION_BG);
		p.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	private JPanel buildPluginHeader()
	{
		JPanel header = new JPanel()
		{
			@Override
			protected void paintComponent(Graphics graphics)
			{
				Graphics2D g = (Graphics2D) graphics;
				g.setPaint(new GradientPaint(0, 0, RogueScapeTheme.SECTION_HEADER_BG,
					0, getHeight(), RogueScapeTheme.PANEL_BG));
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(RogueScapeTheme.BORDER);
				g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
				g.setColor(RogueScapeTheme.BORDER_BRIGHT);
				g.drawLine(1, 1, getWidth() - 2, 1);
			}
		};
		header.setLayout(new BorderLayout(8, 0));
		header.setOpaque(false);
		header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
		header.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 58));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel crest = new JLabel("*");
		crest.setHorizontalAlignment(JLabel.CENTER);
		crest.setForeground(RogueScapeTheme.ACCENT);
		crest.setFont(RogueScapeTheme.header(crest.getFont()).deriveFont(24f));
		crest.setPreferredSize(new Dimension(24, 32));
		header.add(crest, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("RogueScape");
		title.setForeground(RogueScapeTheme.GOLD);
		title.setFont(RogueScapeTheme.header(title.getFont()));
		JLabel sub = new JLabel("Rooms, relics, rewards");
		sub.setForeground(RogueScapeTheme.TEXT_MUTED);
		sub.setFont(RogueScapeTheme.small(sub.getFont()));
		text.add(title);
		text.add(sub);
		header.add(text, BorderLayout.CENTER);

		JLabel status = new JLabel("ON");
		status.setForeground(RogueScapeTheme.POSITIVE);
		status.setFont(RogueScapeTheme.value(status.getFont()));
		header.add(status, BorderLayout.EAST);
		return header;
	}

	/** Ordered route IDs the player handpicked. Entries may be rooms or bosses. */
	public List<String> selectedRoomIds()
	{
		return new ArrayList<>(selectedRoomIds);
	}

	public List<String> selectedRoomLabels()
	{
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			String allowance = i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All";
			labels.add(roomName(selectedRoomIds.get(i)) + " [" + allowance + "] -> " + rewardPreviewForAllowance(allowance));
		}
		return labels;
	}

	public List<String> customRouteLabels()
	{
		return selectedRoomLabels();
	}

	public List<String> selectedRoomAllowances()
	{
		List<String> allowances = new ArrayList<>();
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			allowances.add(i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All");
		}
		return allowances;
	}

	public int selectedRouteIndex()
	{
		return selectedRouteIndex;
	}

	public String customBuilderGameMode()
	{
		return customBuilderGameMode;
	}

	public void setCustomBuilderGameMode(String customBuilderGameMode)
	{
		if (customBuilderGameMode != null && !customBuilderGameMode.trim().isEmpty())
		{
			this.customBuilderGameMode = customBuilderGameMode.trim();
		}
	}

	public String customBuilderLoadout()
	{
		return customBuilderLoadout;
	}

	public void setCustomBuilderLoadout(String customBuilderLoadout)
	{
		if (customBuilderLoadout != null && !customBuilderLoadout.trim().isEmpty())
		{
			this.customBuilderLoadout = customBuilderLoadout.trim();
		}
	}

	public String customStrictness()
	{
		return customStrictness;
	}

	public boolean customBankUnlocks()
	{
		return customBankUnlocks;
	}

	public int customTimeLimitMinutes()
	{
		return customTimeLimitMinutes;
	}

	public int customBossLimit()
	{
		return customBossLimit;
	}

	public void cycleCustomStrictness()
	{
		if ("Balanced".equals(customStrictness))
		{
			customStrictness = "Trust";
		}
		else if ("Trust".equals(customStrictness))
		{
			customStrictness = "Strict";
		}
		else
		{
			customStrictness = "Balanced";
		}
	}

	public void toggleCustomBankUnlocks()
	{
		customBankUnlocks = !customBankUnlocks;
	}

	public void cycleCustomTimeLimit()
	{
		if (customTimeLimitMinutes == 0)
		{
			customTimeLimitMinutes = 30;
		}
		else if (customTimeLimitMinutes == 30)
		{
			customTimeLimitMinutes = 60;
		}
		else if (customTimeLimitMinutes == 60)
		{
			customTimeLimitMinutes = 90;
		}
		else
		{
			customTimeLimitMinutes = 0;
		}
	}

	public void cycleCustomBossLimit()
	{
		if (customBossLimit == 0)
		{
			customBossLimit = 1;
		}
		else if (customBossLimit < 3)
		{
			customBossLimit++;
		}
		else
		{
			customBossLimit = 0;
		}
	}

	public void addFirstRoomOfKind(RoomKind kind)
	{
		addFirstRoomOfKind(kind, allowanceLabel(kind));
	}

	public void addFirstRoomOfKind(RoomKind kind, String allowance)
	{
		if (kind == null)
		{
			return;
		}
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() == kind && def.kind() != RoomKind.BOSS && !selectedRoomIds.contains(def.id()))
			{
				selectedRoomIds.add(def.id());
				selectedRoomAllowances.add(allowance == null || allowance.trim().isEmpty() ? allowanceLabel(kind) : allowance.trim());
				selectedRouteIndex = selectedRoomIds.size() - 1;
				refreshSelectedRoomsArea();
				return;
			}
		}
	}

	public void addRoomForAllowance(String allowance)
	{
		String normalized = allowance == null ? "" : allowance.trim().toLowerCase();
		if ("supply".equals(normalized))
		{
			addFirstRoomOfKind(RoomKind.SUPPLY, "Supply");
		}
		else if ("armour".equals(normalized) || "armor".equals(normalized))
		{
			addFirstRoomOfKind(RoomKind.ARMOUR, "Armour");
		}
		else if ("weapons".equals(normalized) || "weapon".equals(normalized))
		{
			addFirstRoomOfKind(RoomKind.WEAPON, "Weapons");
		}
		else if ("skilling".equals(normalized))
		{
			addFirstRoomOfKind(RoomKind.SKILLING, "Skilling");
		}
		else if ("shopping".equals(normalized) || "shop".equals(normalized))
		{
			addFirstRoomOfKind(RoomKind.SHOP, "Shopping");
		}
		else
		{
			addFirstRoomOfKind(RoomKind.REGION, "All");
		}
	}

	public String customSelectedRoomLabel()
	{
		List<RoomDefinition> rooms = customRoomOptions();
		if (rooms.isEmpty())
		{
			return "(no rooms)";
		}
		customRoomCursor = clamp(customRoomCursor, rooms.size());
		return rooms.get(customRoomCursor).name();
	}

	public List<String> customRoomOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (RoomDefinition def : customRoomOptions())
		{
			labels.add(def.name());
		}
		return labels;
	}

	public int customSelectedRoomIndex()
	{
		return clamp(customRoomCursor, customRoomOptions().size());
	}

	public void selectCustomRoomIndex(int index)
	{
		customRoomCursor = clamp(index, customRoomOptions().size());
	}

	public void pageCustomRoomIndex(int delta)
	{
		customRoomCursor = clamp(customRoomCursor + delta, customRoomOptions().size());
	}

	public String customSelectedAllowanceLabel()
	{
		customAllowanceCursor = clamp(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
		return CUSTOM_ALLOWANCES[customAllowanceCursor];
	}

	public List<String> customAllowanceOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (String allowance : CUSTOM_ALLOWANCES)
		{
			labels.add(allowance);
		}
		return labels;
	}

	public int customSelectedAllowanceIndex()
	{
		return clamp(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	public void selectCustomAllowanceIndex(int index)
	{
		customAllowanceCursor = clamp(index, CUSTOM_ALLOWANCES.length);
	}

	public String customSelectedBossLabel()
	{
		List<RoomDefinition> bosses = BossLibrary.all();
		if (bosses.isEmpty())
		{
			return "(no bosses)";
		}
		customBossCursor = clamp(customBossCursor, bosses.size());
		return bosses.get(customBossCursor).name();
	}

	public List<String> customBossOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (RoomDefinition def : BossLibrary.all())
		{
			labels.add(def.name());
		}
		return labels;
	}

	public int customSelectedBossIndex()
	{
		return clamp(customBossCursor, BossLibrary.all().size());
	}

	public void selectCustomBossIndex(int index)
	{
		customBossCursor = clamp(index, BossLibrary.all().size());
	}

	public void pageCustomBossIndex(int delta)
	{
		customBossCursor = clamp(customBossCursor + delta, BossLibrary.all().size());
	}

	public void customPreviousRoom()
	{
		customRoomCursor = previousIndex(customRoomCursor, customRoomOptions().size());
	}

	public void customNextRoom()
	{
		customRoomCursor = nextIndex(customRoomCursor, customRoomOptions().size());
	}

	public void customPreviousAllowance()
	{
		customAllowanceCursor = previousIndex(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	public void customNextAllowance()
	{
		customAllowanceCursor = nextIndex(customAllowanceCursor, CUSTOM_ALLOWANCES.length);
	}

	public void customPreviousBoss()
	{
		customBossCursor = previousIndex(customBossCursor, BossLibrary.all().size());
	}

	public void customNextBoss()
	{
		customBossCursor = nextIndex(customBossCursor, BossLibrary.all().size());
	}

	public void addSelectedCustomRoom()
	{
		List<RoomDefinition> rooms = customRoomOptions();
		if (rooms.isEmpty())
		{
			return;
		}
		customRoomCursor = clamp(customRoomCursor, rooms.size());
		RoomDefinition def = rooms.get(customRoomCursor);
		if (!selectedRoomIds.contains(def.id()))
		{
			selectedRoomIds.add(def.id());
			selectedRoomAllowances.add(customSelectedAllowanceLabel());
			selectedRouteIndex = selectedRoomIds.size() - 1;
			refreshSelectedRoomsArea();
		}
	}

	public void addSelectedCustomBoss()
	{
		List<RoomDefinition> bosses = BossLibrary.all();
		if (bosses.isEmpty())
		{
			return;
		}
		customBossCursor = clamp(customBossCursor, bosses.size());
		RoomDefinition boss = bosses.get(customBossCursor);
		if (customBossLimit > 0 && bossCountInCustomRoute() >= customBossLimit)
		{
			return;
		}
		if (!selectedRoomIds.contains(boss.id()))
		{
			selectedRoomIds.add(boss.id());
			selectedRoomAllowances.add("Boss");
			selectedRouteIndex = selectedRoomIds.size() - 1;
			refreshSelectedRoomsArea();
		}
	}

	public void removeLastCustomRoom()
	{
		if (selectedRoomIds.isEmpty())
		{
			return;
		}
		int idx = selectedRouteIndex >= 0 && selectedRouteIndex < selectedRoomIds.size()
			? selectedRouteIndex : selectedRoomIds.size() - 1;
		selectedRoomIds.remove(idx);
		if (idx < selectedRoomAllowances.size())
		{
			selectedRoomAllowances.remove(idx);
		}
		selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : Math.min(idx, selectedRoomIds.size() - 1);
		refreshSelectedRoomsArea();
	}

	public void clearCustomRoute()
	{
		clearSelectedRooms();
	}

	public void selectPreviousRouteRow()
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = selectedRouteIndex <= 0 ? 0 : selectedRouteIndex - 1;
		}
		refreshSelectedRoomsArea();
	}

	public void selectNextRouteRow()
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = selectedRouteIndex < 0 ? 0 : Math.min(selectedRoomIds.size() - 1, selectedRouteIndex + 1);
		}
		refreshSelectedRoomsArea();
	}

	public void selectRouteRow(int index)
	{
		if (selectedRoomIds.isEmpty())
		{
			selectedRouteIndex = -1;
		}
		else
		{
			selectedRouteIndex = clamp(index, selectedRoomIds.size());
		}
		refreshSelectedRoomsArea();
	}

	public void moveSelectedRouteUp()
	{
		if (selectedRouteIndex <= 0 || selectedRouteIndex >= selectedRoomIds.size())
		{
			return;
		}
		swapRouteRows(selectedRouteIndex, selectedRouteIndex - 1);
		selectedRouteIndex--;
		refreshSelectedRoomsArea();
	}

	public void moveSelectedRouteDown()
	{
		if (selectedRouteIndex < 0 || selectedRouteIndex >= selectedRoomIds.size() - 1)
		{
			return;
		}
		swapRouteRows(selectedRouteIndex, selectedRouteIndex + 1);
		selectedRouteIndex++;
		refreshSelectedRoomsArea();
	}

	public String customSeedPreview()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mode=").append(customBuilderGameMode);
		sb.append(";loadout=").append(customBuilderLoadout);
		sb.append(";rooms=");
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			if (i > 0) sb.append(",");
			sb.append(selectedRoomIds.get(i)).append(":");
			sb.append(i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All");
		}
		sb.append(";mods=").append(String.join(",", selectedModifierIds));
		sb.append(";strictness=").append(customStrictness);
		sb.append(";bank=").append(customBankUnlocks ? "on" : "off");
		sb.append(";time=").append(customTimeLimitMinutes == 0 ? "none" : customTimeLimitMinutes + "m");
		sb.append(";bosscap=").append(customBossLimit == 0 ? "none" : customBossLimit);
		return sb.toString();
	}

	public void applyCustomSeed(String seed)
	{
		Map<String, String> fields = parseSeedFields(seed);
		if (fields.isEmpty())
		{
			return;
		}
		setCustomBuilderGameMode(fields.get("mode"));
		setCustomBuilderLoadout(fields.get("loadout"));

		selectedRoomIds.clear();
		selectedRoomAllowances.clear();
		String rooms = fields.get("rooms");
		if (rooms != null && !rooms.trim().isEmpty())
		{
			for (String part : rooms.split(","))
			{
				String[] pair = part.split(":", 2);
				String id = pair.length > 0 ? pair[0].trim() : "";
				if (routeEntryExists(id))
				{
					selectedRoomIds.add(id);
					selectedRoomAllowances.add(pair.length > 1 && !pair[1].trim().isEmpty() ? pair[1].trim() : "All");
				}
			}
		}

		String boss = fields.get("boss");
		if (boss != null && !"auto".equalsIgnoreCase(boss) && !"none".equalsIgnoreCase(boss) && routeEntryExists(boss.trim())
			&& !selectedRoomIds.contains(boss.trim()))
		{
			selectedRoomIds.add(boss.trim());
			selectedRoomAllowances.add("Boss");
		}
		selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : selectedRoomIds.size() - 1;

		selectedModifierIds.clear();
		String mods = fields.get("mods");
		if (mods != null && !mods.trim().isEmpty())
		{
			for (String id : mods.split(","))
			{
				String modId = id.trim();
				if (modifierExists(modId) && !selectedModifierIds.contains(modId))
				{
					selectedModifierIds.add(modId);
				}
			}
		}
		String strictness = fields.get("strictness");
		if ("Trust".equalsIgnoreCase(strictness)) customStrictness = "Trust";
		else if ("Strict".equalsIgnoreCase(strictness)) customStrictness = "Strict";
		else if ("Balanced".equalsIgnoreCase(strictness)) customStrictness = "Balanced";
		String bank = fields.get("bank");
		if (bank != null)
		{
			customBankUnlocks = "on".equalsIgnoreCase(bank) || "true".equalsIgnoreCase(bank);
		}
		String time = fields.get("time");
		if (time != null)
		{
			customTimeLimitMinutes = parseTimeMinutes(time);
		}
		String bossCap = fields.get("bosscap");
		if (bossCap != null)
		{
			customBossLimit = parseBossLimit(bossCap);
		}
		refreshSelectedRoomsArea();
		refreshSelectedModifiersArea();
	}

	private List<RoomDefinition> customRoomOptions()
	{
		List<RoomDefinition> rooms = new ArrayList<>();
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() != RoomKind.BOSS)
			{
				rooms.add(def);
			}
		}
		return rooms;
	}

	private boolean roomExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (RoomDefinition def : customRoomOptions())
		{
			if (def.id().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private boolean routeEntryExists(String id)
	{
		return roomExists(id) || bossExists(id);
	}

	private boolean bossExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (RoomDefinition def : BossLibrary.all())
		{
			if (def.id().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private void selectBossById(String id)
	{
		if (id == null || id.isEmpty())
		{
			return;
		}
		List<RoomDefinition> bosses = BossLibrary.all();
		for (int i = 0; i < bosses.size(); i++)
		{
			if (bosses.get(i).id().equals(id))
			{
				customBossCursor = i;
				addSelectedCustomBoss();
				return;
			}
		}
	}

	private boolean modifierExists(String id)
	{
		if (id == null || id.isEmpty())
		{
			return false;
		}
		for (Relic relic : ModifierLibrary.all())
		{
			if (relic.relicId().equals(id))
			{
				return true;
			}
		}
		return false;
	}

	private static Map<String, String> parseSeedFields(String seed)
	{
		Map<String, String> fields = new LinkedHashMap<>();
		if (seed == null || seed.trim().isEmpty())
		{
			return fields;
		}
		for (String part : seed.split(";"))
		{
			int eq = part.indexOf('=');
			if (eq <= 0)
			{
				continue;
			}
			String key = part.substring(0, eq).trim().toLowerCase();
			String value = part.substring(eq + 1).trim();
			if (!key.isEmpty())
			{
				fields.put(key, value);
			}
		}
		return fields;
	}

	private static int parseTimeMinutes(String value)
	{
		if (value == null)
		{
			return 0;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.isEmpty() || "none".equals(normalized) || "off".equals(normalized))
		{
			return 0;
		}
		if (normalized.endsWith("m"))
		{
			normalized = normalized.substring(0, normalized.length() - 1).trim();
		}
		try
		{
			return Math.max(0, Integer.parseInt(normalized));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static int parseBossLimit(String value)
	{
		if (value == null)
		{
			return 0;
		}
		String normalized = value.trim().toLowerCase();
		if (normalized.isEmpty() || "none".equals(normalized) || "off".equals(normalized))
		{
			return 0;
		}
		try
		{
			return Math.max(0, Math.min(3, Integer.parseInt(normalized)));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private int bossCountInCustomRoute()
	{
		int count = 0;
		for (String id : selectedRoomIds)
		{
			if (bossExists(id))
			{
				count++;
			}
		}
		return count;
	}

	private static int previousIndex(int current, int size)
	{
		if (size <= 0) return 0;
		return current <= 0 ? size - 1 : current - 1;
	}

	private static int nextIndex(int current, int size)
	{
		if (size <= 0) return 0;
		return current >= size - 1 ? 0 : current + 1;
	}

	private static int clamp(int current, int size)
	{
		if (size <= 0) return 0;
		return Math.max(0, Math.min(size - 1, current));
	}

	public void selectFirstBoss()
	{
		selectCustomBossIndex(0);
		addSelectedCustomBoss();
	}

	/** First boss ID included in the mixed route, or "" for none. */
	public String selectedBossId()
	{
		for (String id : selectedRoomIds)
		{
			if (bossExists(id))
			{
				return id;
			}
		}
		return "";
	}

	/** Relic IDs of the curses/modifiers to apply when the run STARTs (empty = none). */
	public List<String> selectedModifierIds()
	{
		return new ArrayList<>(selectedModifierIds);
	}

	public List<String> customModifierOptionLabels()
	{
		List<String> labels = new ArrayList<>();
		for (Relic r : ModifierLibrary.all())
		{
			labels.add(r.name());
		}
		return labels;
	}

	public int customModifierPageStart()
	{
		return clamp(customModifierCursor, ModifierLibrary.all().size());
	}

	public void pageCustomModifierIndex(int delta)
	{
		customModifierCursor = clamp(customModifierCursor + delta, ModifierLibrary.all().size());
	}

	public List<String> selectedModifierLabels()
	{
		List<String> labels = new ArrayList<>();
		for (String id : selectedModifierIds)
		{
			labels.add(modifierName(id));
		}
		return labels;
	}

	public List<Integer> selectedModifierIndexes()
	{
		List<Integer> indexes = new ArrayList<>();
		List<Relic> modifiers = ModifierLibrary.all();
		for (int i = 0; i < modifiers.size(); i++)
		{
			if (selectedModifierIds.contains(modifiers.get(i).relicId()))
			{
				indexes.add(i);
			}
		}
		return indexes;
	}

	public void toggleCustomModifierIndex(int index)
	{
		List<Relic> modifiers = ModifierLibrary.all();
		if (modifiers.isEmpty())
		{
			return;
		}
		int idx = clamp(index, modifiers.size());
		String id = modifiers.get(idx).relicId();
		if (selectedModifierIds.contains(id))
		{
			selectedModifierIds.remove(id);
		}
		else
		{
			selectedModifierIds.add(id);
		}
		refreshSelectedModifiersArea();
	}

	public void clearCustomModifiers()
	{
		selectedModifierIds.clear();
		refreshSelectedModifiersArea();
	}

	// ------------------------------------------------------------ RUN CONTROL

	private CollapsibleSection buildRunControlSection()
	{
		CollapsibleSection section = new CollapsibleSection("Run Control");
		section.content().add(buildRunControlTab());
		return section;
	}

	private JPanel buildRunControlTab()
	{
		JPanel c = builderTab();
		c.add(fieldLabel("Choose Mode"));
		c.add(vGap(3));
		modeTilesPanel.setLayout(new java.awt.GridLayout(1, 3, 4, 4));
		modeTilesPanel.setBackground(RogueScapeTheme.SECTION_BG);
		modeTilesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
		modeTilesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		modeTilesPanel.add(modeTile("Scavenge", "earn gear", 0, 0, ""));
		modeTilesPanel.add(modeTile("Reward", "boss loot", 1, 0, ""));
		modeTilesPanel.add(modeTile("Custom", "build route", 2, 0, ""));
		c.add(modeTilesPanel);
		c.add(vGap(8));

		c.add(fieldLabel("Mode"));
		c.add(vGap(3));
		modeCombo.removeAllItems();
		modeCombo.addItem("Scavenger");
		modeCombo.addItem("Rewarded");
		modeCombo.addItem("Custom Creator");
		modeCombo.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		modeCombo.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		modeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		modeCombo.addActionListener(e -> updateCampaignPreview());
		c.add(modeCombo);
		c.add(vGap(6));

		c.add(fieldLabel("Preset"));
		c.add(vGap(3));
		presetCombo.removeAllItems();
		presetCombo.addItem("Auto / None");
		presetCombo.addItem("Goblin Rat");
		presetCombo.addItem("Iron Scrapper");
		presetCombo.addItem("Mage Spark");
		presetCombo.addItem("Archer's Gamble");
		presetCombo.addItem("Monk Mode");
		presetCombo.addItem("Wilderness Rat");
		presetCombo.addItem("Clue Gremlin");
		presetCombo.addItem("Max Main Draft");
		styleCombo(presetCombo);
		presetCombo.addActionListener(e -> updateCampaignPreview());
		c.add(presetCombo);
		c.add(vGap(6));

		c.add(fieldLabel("Featured Campaigns"));
		c.add(vGap(3));
		presetCardsPanel.setLayout(new java.awt.GridLayout(2, 2, 4, 4));
		presetCardsPanel.setBackground(RogueScapeTheme.SECTION_BG);
		presetCardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
		presetCardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		presetCardsPanel.add(presetCard("Goblin Rat", "starter hunt", 1));
		presetCardsPanel.add(presetCard("Iron Scrapper", "ore to claws", 2));
		presetCardsPanel.add(presetCard("Clue Gremlin", "odd path", 7));
		presetCardsPanel.add(presetCard("Max Draft", "big route", 8));
		c.add(presetCardsPanel);
		c.add(vGap(6));

		campaignPreviewPanel.setLayout(new BoxLayout(campaignPreviewPanel, BoxLayout.Y_AXIS));
		campaignPreviewPanel.setBackground(RogueScapeTheme.SECTION_BG);
		campaignPreviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		c.add(campaignPreviewPanel);
		c.add(vGap(6));

		c.add(fieldLabel("Seed"));
		c.add(vGap(3));
		seedField.setName("roguescape.seedField");
		seedField.setToolTipText("Optional: identical seeds produce the same generated route.");
		seedField.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		seedField.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		seedField.setCaretColor(RogueScapeTheme.TEXT_PRIMARY);
		seedField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		c.add(seedField);
		c.add(vGap(8));

		runControlActions.setLayout(new BoxLayout(runControlActions, BoxLayout.Y_AXIS));
		runControlActions.setBackground(RogueScapeTheme.SECTION_BG);
		c.add(runControlActions);

		c.add(vGap(6));
		c.add(hintBox(new String[]{
			"Tiles choose the run fantasy:",
			"Scavenger: start constrained, earn power",
			"Rewarded: short prep, boss reward pressure",
			"Custom: build route, rooms, bosses, constraints"
		}));
		return c;
	}

	private String selectedRunTitle()
	{
		RunPreset preset = selectedPreset();
		if (preset != RunPreset.UNSPECIFIED)
		{
			return presetLabel(preset);
		}
		switch (selectedMode())
		{
			case BANK_DRAFT: return "Rewarded Run";
			case CUSTOM_CREATOR: return "Custom Run";
			case FRESH_SOURCE:
			case UNSPECIFIED:
			default: return "Scavenger Run";
		}
	}

	private static String presetLabel(RunPreset preset)
	{
		switch (preset)
		{
			case GOBLIN_RAT: return "Goblin Rat";
			case IRON_SCRAPPER: return "Iron Scrapper";
			case MAGE_SPARK: return "Mage Spark";
			case ARCHERS_GAMBLE: return "Archer's Gamble";
			case MONK_MODE: return "Monk Mode";
			case WILDERNESS_RAT: return "Wilderness Rat";
			case CLUE_GREMLIN: return "Clue Gremlin";
			case MAX_MAIN_DRAFT: return "Max Main Draft";
			case UNSPECIFIED:
			default: return "Scavenger Run";
		}
	}

	private JButton modeTile(String title, String sub, int modeIndex, int presetIndex, String seed)
	{
		JButton button = new JButton("<html><body style='text-align:center'>"
			+ escape(title) + "<br><span style='font-size:8px'>" + escape(sub) + "</span></body></html>");
		button.setName("roguescape.modeTile." + title.toLowerCase().replace(" ", "-"));
		styleButton(button, RogueScapeTheme.ButtonRole.NEUTRAL);
		button.setMargin(new java.awt.Insets(4, 2, 4, 2));
		button.addActionListener(e ->
			selectRunBuilderState(modeIndex, presetIndex, seed));
		return button;
	}

	private void selectRunBuilderState(int modeIndex, int presetIndex, String seed)
	{
		modeCombo.setSelectedIndex(modeIndex);
		presetCombo.setSelectedIndex(presetIndex);
		if (seed != null)
		{
			seedField.setText(seed);
		}
		updateCampaignPreview();
		updateRunControl(lastModel);
	}

	private static String weeklySeed()
	{
		return "weekly-2026-06-01";
	}

	private void updateCampaignPreview()
	{
		if (campaignPreviewPanel == null)
		{
			return;
		}
		campaignPreviewPanel.removeAll();
		RunPreset preset = selectedPreset();
		RunMode mode = selectedMode();
		List<String> rows = RunRouteBuilder.campaignPreviewRows(preset);
		if (preset == RunPreset.UNSPECIFIED)
		{
			campaignPreviewPanel.add(featureBox("Balanced Route", new String[]{
				"Auto-builds a run from region, supply, gear, and boss stages.",
				"Pick a preset for an authored campaign path."
			}));
		}
		else if (mode == RunMode.SEEDED_RACE)
		{
			campaignPreviewPanel.add(featureBox("Seeded Race", new String[]{
				"Preset controls length and difficulty.",
				"Seed controls the balanced randomized route."
			}));
		}
		else if (!rows.isEmpty())
		{
			List<String> lines = new ArrayList<>();
			int limit = Math.min(rows.size(), 7);
			for (int i = 0; i < limit; i++)
			{
				lines.add(rows.get(i));
			}
			campaignPreviewPanel.add(featureBox("Campaign Path", lines.toArray(new String[0])));
		}
		campaignPreviewPanel.revalidate();
		campaignPreviewPanel.repaint();
	}

	private void updateRunControl(SidePanelViewModel model)
	{
		runControlActions.removeAll();
		boolean lobby = model == null || model.isLobby();

		if (lobby)
		{
			JButton start = actionButton(PanelAction.START_RUN, true);
			start.setText(startRunLabel());
			start.setEnabled(model == null || model.isActionEnabled(PanelAction.START_RUN));
			runControlActions.add(start);
		}
		else
		{
			// Contextual primary actions surfaced by the view model for the phase.
			PanelAction[] order = {
				PanelAction.COMPLETE_STAGE,
				PanelAction.CHOOSE_REWARD_1,
				PanelAction.CHOOSE_REWARD_2,
				PanelAction.CHOOSE_REWARD_3,
				PanelAction.SKIP_REWARD,
				PanelAction.NEXT_STAGE
			};
			for (PanelAction action : order)
			{
				if (!model.isActionEnabled(action)) continue;
				runControlActions.add(actionButton(action, true));
				runControlActions.add(vGap(3));
			}
			if (model.isActionEnabled(PanelAction.FAIL_RUN))
			{
				runControlActions.add(actionButton(PanelAction.FAIL_RUN, false));
				runControlActions.add(vGap(3));
			}
			if (model.isActionEnabled(PanelAction.RESET_RUN))
			{
				runControlActions.add(actionButton(PanelAction.RESET_RUN, false));
			}
		}
		runControlActions.revalidate();
		runControlActions.repaint();
	}

	private JButton presetCard(String title, String sub, int presetIndex)
	{
		JButton button = new JButton("<html><body style='text-align:center'>"
			+ escape(title) + "<br><span style='font-size:8px'>" + escape(sub) + "</span></body></html>");
		button.setName("roguescape.presetCard." + presetIndex);
		styleButton(button, RogueScapeTheme.ButtonRole.NEUTRAL);
		button.setMargin(new java.awt.Insets(3, 2, 3, 2));
		button.addActionListener(e ->
		{
			presetCombo.setSelectedIndex(presetIndex);
			updateCampaignPreview();
		});
		return button;
	}

	private String startRunLabel()
	{
		RunMode mode = selectedMode();
		RunPreset preset = selectedPreset();
		if (mode == RunMode.CUSTOM_CREATOR)
		{
			return "Begin Custom Run";
		}
		if (mode == RunMode.BANK_DRAFT)
		{
			return "Start Rewarded";
		}
		if (mode == RunMode.FRESH_SOURCE)
		{
			return "Start Scavenge";
		}
		if (preset != RunPreset.UNSPECIFIED)
		{
			return "Begin Campaign";
		}
		return "Begin Run";
	}

	// ------------------------------------------------------------ ROUTE

	private CollapsibleSection buildRouteSection()
	{
		CollapsibleSection section = new CollapsibleSection("Route");
		section.content().add(buildRouteTab());
		return section;
	}

	private JPanel buildRouteTab()
	{
		JPanel c = builderTab();
		// --- Picker (lobby): handpick rooms + boss ---
		routePickerPanel.setLayout(new BoxLayout(routePickerPanel, BoxLayout.Y_AXIS));
		routePickerPanel.setBackground(RogueScapeTheme.SECTION_BG);
		routePickerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		routePickerPanel.add(fieldLabel("Add Room"));
		routePickerPanel.add(vGap(3));
		roomDefs.clear();
		roomCombo.removeAllItems();
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.kind() != RoomKind.BOSS)
			{
				roomDefs.add(def);
				roomCombo.addItem(def.name());
			}
		}
		styleCombo(roomCombo);
		routePickerPanel.add(roomCombo);
		routePickerPanel.add(vGap(3));

		JButton addRoom = new JButton("+ Add room");
		styleButton(addRoom, false);
		addRoom.addActionListener(e -> addSelectedRoom());
		routePickerPanel.add(addRoom);
		routePickerPanel.add(vGap(6));

		routePickerPanel.add(fieldLabel("Mixed Route (in order)"));
		routePickerPanel.add(vGap(3));
		routePickerPanel.add(readOnlyArea(selectedRoomsArea, RogueScapeTheme.TEXT_PRIMARY, 70));
		routePickerPanel.add(vGap(3));

		JPanel roomBtns = new JPanel(new java.awt.GridLayout(1, 2, 4, 0));
		roomBtns.setBackground(RogueScapeTheme.SECTION_BG);
		roomBtns.setAlignmentX(Component.LEFT_ALIGNMENT);
		roomBtns.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		JButton removeRoom = new JButton("Remove last");
		styleButton(removeRoom, false);
		removeRoom.addActionListener(e -> removeLastRoom());
		JButton clearRooms = new JButton("Clear");
		styleButton(clearRooms, false);
		clearRooms.addActionListener(e -> clearSelectedRooms());
		roomBtns.add(removeRoom);
		roomBtns.add(clearRooms);
		routePickerPanel.add(roomBtns);
		routePickerPanel.add(vGap(6));

		routePickerPanel.add(fieldLabel("Add Boss Step"));
		routePickerPanel.add(vGap(3));
		bossDefs.clear();
		bossCombo.removeAllItems();
		for (RoomDefinition def : BossLibrary.all())
		{
			bossDefs.add(def);
			bossCombo.addItem(def.name());
		}
		styleCombo(bossCombo);
		routePickerPanel.add(bossCombo);
		routePickerPanel.add(vGap(3));
		JButton addBoss = new JButton("+ Add boss");
		styleButton(addBoss, false);
		addBoss.addActionListener(e ->
		{
			int idx = bossCombo.getSelectedIndex();
			if (idx >= 0)
			{
				selectCustomBossIndex(idx);
				addSelectedCustomBoss();
			}
		});
		routePickerPanel.add(addBoss);
		routePickerPanel.add(vGap(4));
		routePickerPanel.add(mutedRow("Bosses are route steps and can be reordered."));

		// --- Preview (active): read-only route progress ---
		routePreviewPanel.setLayout(new BoxLayout(routePreviewPanel, BoxLayout.Y_AXIS));
		routePreviewPanel.setBackground(RogueScapeTheme.SECTION_BG);
		routePreviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		c.add(routePickerPanel);
		c.add(routePreviewPanel);
		return c;
	}

	private void updateRoute(SidePanelViewModel model)
	{
		boolean lobby = model == null || model.isLobby();
		routePickerPanel.setVisible(lobby);
		routePreviewPanel.setVisible(!lobby);
		if (lobby)
		{
			return;
		}
		routePreviewPanel.removeAll();
		List<String> rows = model.routeRows();
		if (rows.isEmpty())
		{
			routePreviewPanel.add(mutedRow("No route stages."));
		}
		else
		{
			for (String row : rows)
			{
				routePreviewPanel.add(routeRow(row));
			}
		}
		routePreviewPanel.revalidate();
		routePreviewPanel.repaint();
	}

	private void addSelectedRoom()
	{
		int idx = roomCombo.getSelectedIndex();
		if (idx < 0 || idx >= roomDefs.size()) return;
		String id = roomDefs.get(idx).id();
		if (!selectedRoomIds.contains(id))
		{
			selectedRoomIds.add(id);
			selectedRoomAllowances.add(allowanceLabel(roomDefs.get(idx).kind()));
			selectedRouteIndex = selectedRoomIds.size() - 1;
			refreshSelectedRoomsArea();
		}
	}

	private void removeLastRoom()
	{
		if (!selectedRoomIds.isEmpty())
		{
			selectedRoomIds.remove(selectedRoomIds.size() - 1);
			if (!selectedRoomAllowances.isEmpty())
			{
				selectedRoomAllowances.remove(selectedRoomAllowances.size() - 1);
			}
			selectedRouteIndex = selectedRoomIds.isEmpty() ? -1 : Math.min(selectedRouteIndex, selectedRoomIds.size() - 1);
			refreshSelectedRoomsArea();
		}
	}

	private void clearSelectedRooms()
	{
		selectedRoomIds.clear();
		selectedRoomAllowances.clear();
		selectedRouteIndex = -1;
		refreshSelectedRoomsArea();
	}

	private void refreshSelectedRoomsArea()
	{
		StringBuilder sb = new StringBuilder();
		int n = 1;
		for (int i = 0; i < selectedRoomIds.size(); i++)
		{
			String marker = i == selectedRouteIndex ? "> " : "";
			String allowance = i < selectedRoomAllowances.size() ? selectedRoomAllowances.get(i) : "All";
			sb.append(marker).append(n++).append(". ").append(roomName(selectedRoomIds.get(i)))
				.append(" [").append(allowance).append("]\n");
		}
		selectedRoomsArea.setText(sb.toString());
	}

	private void swapRouteRows(int a, int b)
	{
		String room = selectedRoomIds.get(a);
		selectedRoomIds.set(a, selectedRoomIds.get(b));
		selectedRoomIds.set(b, room);
		if (a < selectedRoomAllowances.size() && b < selectedRoomAllowances.size())
		{
			String allowance = selectedRoomAllowances.get(a);
			selectedRoomAllowances.set(a, selectedRoomAllowances.get(b));
			selectedRoomAllowances.set(b, allowance);
		}
	}

	private static String allowanceLabel(RoomKind kind)
	{
		if (kind == RoomKind.SUPPLY) return "Supply";
		if (kind == RoomKind.ARMOUR) return "Armour";
		if (kind == RoomKind.WEAPON) return "Weapons";
		if (kind == RoomKind.SKILLING) return "Skilling";
		if (kind == RoomKind.SHOP) return "Shopping";
		return "All";
	}

	private static String rewardPreviewForAllowance(String allowance)
	{
		String value = allowance == null ? "" : allowance.trim().toLowerCase();
		if ("boss".equals(value)) return "random relic draft";
		if ("supply".equals(value)) return "random supply draft";
		if ("all".equals(value)) return "random relic draft";
		return "random unlock draft";
	}

	private String roomName(String id)
	{
		for (RoomDefinition def : roomDefs)
		{
			if (def.id().equals(id)) return def.name();
		}
		for (RoomDefinition def : BossLibrary.all())
		{
			if (def.id().equals(id)) return def.name();
		}
		for (RoomDefinition def : RoomLibrary.all())
		{
			if (def.id().equals(id)) return def.name();
		}
		return id;
	}

	private JPanel routeRow(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));
		if (text.startsWith("✓"))
		{
			lbl.setForeground(RogueScapeTheme.POSITIVE);
		}
		else if (text.startsWith("▶"))
		{
			lbl.setForeground(RogueScapeTheme.ACCENT);
		}
		else
		{
			lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		}
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	// ------------------------------------------------------------ STARTING CURSES

	private CollapsibleSection buildCursesSection()
	{
		CollapsibleSection section = new CollapsibleSection("Starting Curses", true);
		section.content().add(buildCursesTab());
		return section;
	}

	private JPanel buildCursesTab()
	{
		JPanel c = builderTab();
		c.add(mutedRow("Optional self-imposed curses applied when you START. Relics are earned from rewards."));
		c.add(vGap(6));

		c.add(fieldLabel("Add Curse"));
		c.add(vGap(3));
		modifierDefs.clear();
		modifierCombo.removeAllItems();
		for (Relic r : ModifierLibrary.all())
		{
			modifierDefs.add(r);
			modifierCombo.addItem(r.name());
		}
		styleCombo(modifierCombo);
		c.add(modifierCombo);
		c.add(vGap(3));

		JButton add = new JButton("+ Add curse");
		styleButton(add, false);
		add.addActionListener(e -> addSelectedModifier());
		c.add(add);
		c.add(vGap(6));

		c.add(fieldLabel("Selected Curses"));
		c.add(vGap(3));
		c.add(readOnlyArea(selectedModifiersArea, RogueScapeTheme.NEGATIVE, 56));
		c.add(vGap(3));

		JButton clear = new JButton("Clear curses");
		styleButton(clear, false);
		clear.addActionListener(e -> { selectedModifierIds.clear(); refreshSelectedModifiersArea(); });
		c.add(clear);
		return c;
	}

	private void addSelectedModifier()
	{
		int idx = modifierCombo.getSelectedIndex();
		if (idx < 0 || idx >= modifierDefs.size()) return;
		String id = modifierDefs.get(idx).relicId();
		if (!selectedModifierIds.contains(id))
		{
			selectedModifierIds.add(id);
			refreshSelectedModifiersArea();
		}
	}

	private void refreshSelectedModifiersArea()
	{
		StringBuilder sb = new StringBuilder();
		for (String id : selectedModifierIds)
		{
			sb.append("- ").append(modifierName(id)).append("\n");
		}
		selectedModifiersArea.setText(sb.toString());
	}

	private String modifierName(String id)
	{
		for (Relic r : modifierDefs)
		{
			if (r.relicId().equals(id)) return r.name();
		}
		return id;
	}

	// ------------------------------------------------------------ DEV TOOLS

	private CollapsibleSection buildDevToolsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Dev Tools", true);
		JPanel c = section.content();
		c.add(mutedRow("Simulate stepping a run to test UI states. Buttons drive the run loop through legal transitions."));
		c.add(vGap(4));
		c.add(devButton("Force Complete Stage", PanelAction.DEV_COMPLETE_STAGE));
		c.add(vGap(3));
		c.add(devButton("Pick Reward 1", PanelAction.CHOOSE_REWARD_1));
		c.add(vGap(3));
		c.add(devButton("Skip Reward", PanelAction.SKIP_REWARD));
		c.add(vGap(3));
		c.add(devButton("Next Stage", PanelAction.NEXT_STAGE));
		c.add(vGap(3));
		c.add(devButton("Force Complete Run", PanelAction.DEV_COMPLETE_RUN));
		c.add(vGap(3));
		JButton fail = devButton("Force Fail Run", PanelAction.FAIL_RUN);
		styleButton(fail, RogueScapeTheme.ButtonRole.DANGER);
		c.add(fail);
		c.add(vGap(6));
		c.add(mutedRow("Journal injection (needs 'Experimental Quest-tab UI' on):"));
		c.add(vGap(3));
		c.add(devButton("Log Journal Widgets", PanelAction.DEV_LOG_JOURNAL));
		return section;
	}

	private JButton devButton(String label, PanelAction action)
	{
		JButton btn = new JButton(label);
		styleButton(btn, false);
		btn.addActionListener(e -> dispatch(action));
		return btn;
	}

	private void updateDevTools(SidePanelViewModel model)
	{
		boolean dev = devModeSupplier == null || devModeSupplier.getAsBoolean();
		if (devSection != null)
		{
			devSection.setVisible(dev);
		}
	}

	private void styleCombo(JComboBox<String> combo)
	{
		combo.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		combo.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		combo.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	// ------------------------------------------------------------ LIVE RUN

	private CollapsibleSection buildLiveRunSection()
	{
		CollapsibleSection section = new CollapsibleSection("Live Run");
		liveRunContent.setLayout(new BoxLayout(liveRunContent, BoxLayout.Y_AXIS));
		liveRunContent.setBackground(RogueScapeTheme.SECTION_BG);
		section.content().add(liveRunContent);
		return section;
	}

	private void updateLiveRun(SidePanelViewModel model)
	{
		liveRunContent.removeAll();
		if (model == null || model.isLobby())
		{
			liveRunContent.add(mutedRow("Run not started."));
			liveRunContent.add(mutedRow("Pick a run, then START."));
			liveRunContent.revalidate();
			liveRunContent.repaint();
			return;
		}

		if (model.floorTotal() > 0)
		{
			String floorText = model.floorCurrent() + " / " + model.floorTotal();
			StatBar floorBar = new StatBar(RogueScapeTheme.BAR_PROGRESS);
			floorBar.setValue((double) model.floorCurrent() / model.floorTotal(), "Floor " + floorText);
			liveRunContent.add(floorBar);
			liveRunContent.add(vGap(4));
		}
		liveRunContent.add(statRow("Time", model.timerLabel(), RogueScapeTheme.INFO));
		liveRunContent.add(statRow("State", model.stateLabel(), RogueScapeTheme.ACCENT));
		if (!model.phaseLabel().isEmpty())
		{
			liveRunContent.add(statRow("Phase", model.phaseLabel(), RogueScapeTheme.TEXT_PRIMARY));
		}
		if (!model.roomName().isEmpty())
		{
			liveRunContent.add(statRow("Room", model.roomName(), RogueScapeTheme.TEXT_PRIMARY));
		}
		if (!model.regionId().isEmpty())
		{
			liveRunContent.add(statRow("Region", model.regionId(), RogueScapeTheme.TEXT_MUTED));
		}
		liveRunContent.add(statRow("Score", Integer.toString(model.score()), RogueScapeTheme.TEXT_PRIMARY));
		liveRunContent.add(statRow("Legal", Integer.toString(model.legalCount()), RogueScapeTheme.POSITIVE));
		liveRunContent.add(statRow("Illegal", Integer.toString(model.illegalCount()),
			model.illegalCount() > 0 ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_MUTED));

		// Phase detail rows (You CAN / You CANNOT) carried as status strings.
		boolean firstDetail = true;
		for (String row : model.statusRows())
		{
			if (row == null || row.trim().isEmpty()) continue;
			if (row.startsWith("Score:") || row.startsWith("Legal/Illegal:") || row.startsWith("CURRENT:")) continue;
			if (firstDetail)
			{
				liveRunContent.add(vGap(4));
				firstDetail = false;
			}
			liveRunContent.add(detailRow(row));
		}

		liveRunContent.revalidate();
		liveRunContent.repaint();
	}

	// ------------------------------------------------------------ BUILD / ARTIFACTS / MODIFIERS / PROGRESSION

	private CollapsibleSection buildBuildSection()
	{
		CollapsibleSection section = new CollapsibleSection("Build");
		buildContent.setLayout(new BoxLayout(buildContent, BoxLayout.Y_AXIS));
		buildContent.setBackground(RogueScapeTheme.SECTION_BG);
		section.content().add(buildContent);
		return section;
	}

	private void updateBuild(SidePanelViewModel model)
	{
		buildContent.removeAll();
		if (model == null || model.isLobby())
		{
			buildContent.add(mutedRow("Your build forms from claimed relics."));
			buildContent.add(mutedRow("Pick a preset, seed, route, and curses."));
		}
		else
		{
			buildContent.add(statRow("Score", Integer.toString(model.score()), RogueScapeTheme.GOLD));
			if (!model.phaseLabel().isEmpty())
			{
				buildContent.add(statRow("Phase", model.phaseLabel(), RogueScapeTheme.ACCENT));
			}
			if (model.bossesTotal() > 0)
			{
				buildContent.add(statRow("Bosses", model.bossesDefeated() + " / " + model.bossesTotal(),
					RogueScapeTheme.BAR_SYNERGY));
			}
			buildContent.add(vGap(4));
			if (model.relicRows().isEmpty() || (model.relicRows().size() == 1 && model.relicRows().get(0).startsWith("No ")))
			{
				buildContent.add(mutedRow("No relic effects active yet."));
			}
			else
			{
				for (String row : model.relicRows())
				{
					if (row.startsWith("Held artifacts:"))
					{
						buildContent.add(keyValueRow(row));
					}
					else if (row.startsWith("- "))
					{
						buildContent.add(detailRow(row.substring(2)));
					}
				}
			}
		}
		buildContent.revalidate();
		buildContent.repaint();
	}

	private CollapsibleSection buildArtifactsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Artifacts");
		artifactsContent.setLayout(new BoxLayout(artifactsContent, BoxLayout.Y_AXIS));
		artifactsContent.setBackground(RogueScapeTheme.SECTION_BG);
		section.content().add(artifactsContent);
		return section;
	}

	private void updateArtifacts(SidePanelViewModel model)
	{
		artifactsContent.removeAll();
		if (model == null || model.isLobby())
		{
			artifactsContent.add(mutedRow("Start a run, then claim relic rewards."));
		}
		else if (model.relicRows().isEmpty())
		{
			artifactsContent.add(mutedRow("No artifacts yet."));
		}
		else
		{
			for (String row : model.relicRows())
			{
				if (row.startsWith("~ "))
				{
					artifactsContent.add(mutedRow(row.substring(2)));
				}
				else
				{
					artifactsContent.add(detailRow(row));
				}
			}
		}
		artifactsContent.revalidate();
		artifactsContent.repaint();
	}

	private CollapsibleSection buildModifiersSection()
	{
		CollapsibleSection section = new CollapsibleSection("Modifiers");
		modifiersContent.setLayout(new BoxLayout(modifiersContent, BoxLayout.Y_AXIS));
		modifiersContent.setBackground(RogueScapeTheme.SECTION_BG);
		section.content().add(modifiersContent);
		return section;
	}

	private void updateModifiers(SidePanelViewModel model)
	{
		modifiersContent.removeAll();
		if (model == null || model.isLobby())
		{
			modifiersContent.add(mutedRow("Run not started."));
		}
		else if (model.modifierRows().isEmpty())
		{
			modifiersContent.add(mutedRow("No active modifiers."));
		}
		else
		{
			for (String row : model.modifierRows())
			{
				if (row.startsWith("~ "))
				{
					modifiersContent.add(mutedRow(row.substring(2)));
				}
				else
				{
					modifiersContent.add(keyValueRow(row));
				}
			}
		}
		modifiersContent.revalidate();
		modifiersContent.repaint();
	}

	private CollapsibleSection buildProgressionSection()
	{
		CollapsibleSection section = new CollapsibleSection("Progression");
		progressionContent.setLayout(new BoxLayout(progressionContent, BoxLayout.Y_AXIS));
		progressionContent.setBackground(RogueScapeTheme.SECTION_BG);
		section.content().add(progressionContent);
		return section;
	}

	private void updateProgression(SidePanelViewModel model)
	{
		progressionContent.removeAll();
		if (model == null || model.isLobby())
		{
			progressionContent.add(mutedRow("Run not started."));
			progressionContent.revalidate();
			progressionContent.repaint();
			return;
		}
		if (model.bossesTotal() > 0)
		{
			StatBar bossBar = new StatBar(RogueScapeTheme.BAR_SYNERGY);
			bossBar.setValue((double) model.bossesDefeated() / model.bossesTotal(),
				"Bosses " + model.bossesDefeated() + " / " + model.bossesTotal());
			progressionContent.add(bossBar);
			progressionContent.add(vGap(4));
		}
		for (String row : model.progressionRows())
		{
			if (row.startsWith("Bosses defeated:"))
			{
				continue; // shown as the bar above
			}
			progressionContent.add(keyValueRow(row));
		}
		progressionContent.revalidate();
		progressionContent.repaint();
	}

	/** Renders "Label: value" as a label-left/value-right stat row; falls back to a plain row. */
	private JPanel keyValueRow(String line)
	{
		int idx = line.indexOf(": ");
		if (idx <= 0)
		{
			return detailRow(line);
		}
		String label = line.substring(0, idx);
		String value = line.substring(idx + 2);
		String lower = value.toLowerCase();
		Color color = RogueScapeTheme.TEXT_PRIMARY;
		if (lower.contains("illegal"))
		{
			color = RogueScapeTheme.NEGATIVE;
		}
		else if (lower.equals("off") || lower.equals("allowed"))
		{
			color = RogueScapeTheme.TEXT_MUTED;
		}
		else if (lower.equals("on") || lower.equals("flagged"))
		{
			color = RogueScapeTheme.ACCENT;
		}
		return statRow(label, value, color);
	}

	// ------------------------------------------------------------ ZONE BUILDER

	private CollapsibleSection buildZoneBuilderSection()
	{
		CollapsibleSection section = new CollapsibleSection("Zone Builder", true);
		section.content().add(buildZoneBuilderTab());
		return section;
	}

	private JPanel buildZoneBuilderTab()
	{
		JPanel c = builderTab();
		c.add(fieldLabel("Zone Name"));
		c.add(vGap(3));
		zoneNameField.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		zoneNameField.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		zoneNameField.setCaretColor(RogueScapeTheme.TEXT_PRIMARY);
		zoneNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		c.add(zoneNameField);
		c.add(vGap(6));

		styleButton(addZoneToggleBtn, true);
		addZoneToggleBtn.addActionListener(e -> toggleZoneEditing());
		c.add(addZoneToggleBtn);
		c.add(vGap(6));

		c.add(fieldLabel("Status"));
		c.add(vGap(3));
		c.add(readOnlyArea(zoneStatusArea, RogueScapeTheme.TEXT_MUTED, 70));
		c.add(vGap(6));

		c.add(fieldLabel("Selected Regions"));
		c.add(vGap(3));
		c.add(readOnlyArea(zoneRegionsArea, RogueScapeTheme.TEXT_PRIMARY, 70));
		c.add(vGap(6));

		styleButton(saveZoneBtn, true);
		saveZoneBtn.addActionListener(e -> {
			syncZoneNameToSelection();
			if (saveRoomRequest != null) saveRoomRequest.run();
			if (roomEditorState != null)
			{
				roomEditorState.markChanged("Zone \"" + roomEditorState.selection().getName() + "\" saved");
			}
			updateZoneBuilder();
		});
		c.add(saveZoneBtn);
		c.add(vGap(4));

		styleButton(clearZoneBtn, false);
		clearZoneBtn.addActionListener(e -> {
			if (roomEditorState != null)
			{
				roomEditorState.selection().clear();
				roomEditorState.markChanged("Selected regions cleared");
				if (saveRoomRequest != null) saveRoomRequest.run();
			}
			updateZoneBuilder();
		});
		c.add(clearZoneBtn);
		c.add(vGap(4));

		styleButton(useZoneBtn, false);
		useZoneBtn.addActionListener(e -> {
			syncZoneNameToSelection();
			if (useRoomRequest != null && roomEditorState != null && !roomEditorState.selection().isEmpty())
			{
				useRoomRequest.run();
				roomEditorState.markChanged("Zone \"" + roomEditorState.selection().getName() + "\" activated");
			}
			updateZoneBuilder();
		});
		c.add(useZoneBtn);
		c.add(vGap(6));

		c.add(hintBox(new String[]{
			"How to add regions:",
			"1. Click 'Start adding regions'",
			"2. Open the world map",
			"3. Hover a region tile",
			"4. Right-click → Toggle region"
		}));

		return c;
	}

	private void updateZoneBuilder()
	{
		if (roomEditorState == null) return;
		boolean editing = roomEditorState.isEditing();
		addZoneToggleBtn.setText(editing ? "Stop adding regions" : "Start adding regions");
		addZoneToggleBtn.setBackground(editing ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.POSITIVE);

		int hoveredId = roomEditorState.getHoveredRegionId();
		int selectedCount = roomEditorState.selection().size();
		String lastSummary = roomEditorState.getLastToggleSummary();
		String zoneName = roomEditorState.selection().getName();

		StringBuilder status = new StringBuilder();
		status.append("Editing: ").append(editing ? "ON" : "OFF").append("\n");
		status.append("Zone: ").append(zoneName).append("\n");
		if (editing)
		{
			status.append("Hovered: ").append(hoveredId >= 0 ? Integer.toString(hoveredId) : "none").append("\n");
		}
		status.append("Selected: ").append(selectedCount).append(" region(s)\n");
		if (lastSummary != null && !lastSummary.isEmpty())
		{
			status.append("Last: ").append(lastSummary);
		}
		zoneStatusArea.setText(status.toString());

		clearZoneBtn.setEnabled(selectedCount > 0);
		useZoneBtn.setEnabled(selectedCount > 0);

		StringBuilder sb = new StringBuilder();
		for (String id : roomEditorState.selection().selectedRegionIdStrings())
		{
			sb.append(id).append("\n");
		}
		zoneRegionsArea.setText(sb.toString());
	}

	void syncZoneNameToSelection()
	{
		if (roomEditorState == null) return;
		roomEditorState.selection().setName(zoneNameField.getText());
	}

	private void toggleZoneEditing()
	{
		if (roomEditorState == null) return;
		roomEditorState.setEditing(!roomEditorState.isEditing());
		updateZoneBuilder();
	}

	// ------------------------------------------------------------ RELICS

	private CollapsibleSection buildRelicsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Relics", true);
		JPanel c = section.content();
		c.add(mutedRow("The relic & modifier catalog. Relics grant scoring bonuses; modifiers are curses that only restrict."));
		c.add(vGap(6));

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(RogueScapeTheme.SECTION_BG);

		list.add(catalogHeader("RELICS (" + RelicLibrary.all().size() + ")"));
		for (Relic r : RelicLibrary.all())
		{
			list.add(catalogEntry(r));
		}
		list.add(vGap(8));
		list.add(catalogHeader("MODIFIERS / CURSES (" + ModifierLibrary.all().size() + ")"));
		for (Relic r : ModifierLibrary.all())
		{
			list.add(catalogEntry(r));
		}

		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createLineBorder(RogueScapeTheme.BORDER));
		scroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, 280));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		scroll.getViewport().setBackground(RogueScapeTheme.SECTION_BG);
		scroll.getVerticalScrollBar().setUnitIncrement(12);
		c.add(scroll);
		return section;
	}

	private JLabel catalogHeader(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setForeground(RogueScapeTheme.GOLD);
		lbl.setFont(lbl.getFont().deriveFont(java.awt.Font.BOLD, 12f));
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		return lbl;
	}

	private JPanel catalogEntry(Relic relic)
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(RogueScapeTheme.SECTION_BG);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));

		JLabel name = new JLabel(relic.name());
		name.setForeground(RogueScapeTheme.ACCENT);
		name.setFont(RogueScapeTheme.value(name.getFont()));
		name.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.add(name);

		JLabel desc = new JLabel("<html><body style='width:165px'>" + escape(relic.description()) + "</body></html>");
		desc.setForeground(RogueScapeTheme.TEXT_MUTED);
		desc.setFont(RogueScapeTheme.small(desc.getFont()));
		desc.setAlignmentX(Component.LEFT_ALIGNMENT);
		p.add(desc);
		return p;
	}

	// ------------------------------------------------------------ SETTINGS

	private CollapsibleSection buildSettingsSection()
	{
		CollapsibleSection section = new CollapsibleSection("Settings", true);
		section.content().add(mutedRow("Overlay opacity, room mask, and other"));
		section.content().add(mutedRow("toggles live in RuneLite's config panel"));
		section.content().add(mutedRow("(gear icon on this plugin)."));
		return section;
	}

	// ------------------------------------------------------------ widget helpers

	private void dispatch(PanelAction action)
	{
		if (actionHandler != null)
		{
			actionHandler.accept(action);
		}
	}

	private JButton actionButton(PanelAction action, boolean primary)
	{
		JButton btn = new JButton(actionLabel(action));
		styleButton(btn, roleFor(action, primary));
		btn.addActionListener(e -> dispatch(action));
		return btn;
	}

	/** Maps an action to its semantic button role (matches the asset sheet's button states). */
	private static RogueScapeTheme.ButtonRole roleFor(PanelAction action, boolean primary)
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

	/** Backwards-compatible boolean styling: primary -> gold, else neutral. */
	private void styleButton(JButton btn, boolean primary)
	{
		styleButton(btn, primary ? RogueScapeTheme.ButtonRole.PRIMARY : RogueScapeTheme.ButtonRole.NEUTRAL);
	}

	private void styleButton(JButton btn, RogueScapeTheme.ButtonRole role)
	{
		Color base = RogueScapeTheme.buttonBg(role);
		Color hover = RogueScapeTheme.buttonHoverBg(role);
		btn.setFocusPainted(false);
		btn.setFont(RogueScapeTheme.button(btn.getFont()));
		btn.setAlignmentX(Component.LEFT_ALIGNMENT);
		btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		btn.setBackground(base);
		btn.setForeground(RogueScapeTheme.buttonText(role));
		btn.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER),
			BorderFactory.createEmptyBorder(3, 8, 3, 8)));
		btn.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				if (btn.isEnabled()) btn.setBackground(hover);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				btn.setBackground(base);
			}
		});
	}

	/** A label-left / value-right stat row for the LIVE RUN section. */
	private JPanel statRow(String label, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

		JLabel l = new JLabel(label);
		l.setForeground(RogueScapeTheme.TEXT_MUTED);
		l.setFont(RogueScapeTheme.label(l.getFont()));
		row.add(l, BorderLayout.WEST);

		JLabel v = new JLabel(value);
		v.setForeground(valueColor);
		v.setFont(RogueScapeTheme.value(v.getFont()));
		row.add(v, BorderLayout.EAST);
		return row;
	}

	/** A single detail line (e.g. "You CAN:", "  ✓ Fight monsters"). */
	private JPanel detailRow(String text)
	{
		JLabel lbl = new JLabel(text);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));
		if (text.startsWith("You CAN") || text.startsWith("You CANNOT"))
		{
			lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
			lbl.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		}
		else if (text.contains("✓"))
		{
			lbl.setForeground(RogueScapeTheme.POSITIVE);
		}
		else if (text.contains("✗"))
		{
			lbl.setForeground(RogueScapeTheme.NEGATIVE);
		}
		else
		{
			lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		}

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	private JPanel mutedRow(String text)
	{
		JLabel lbl = new JLabel("<html><body style='width:160px'>" + escape(text) + "</body></html>");
		lbl.setForeground(RogueScapeTheme.TEXT_MUTED);
		lbl.setFont(RogueScapeTheme.label(lbl.getFont()));

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(RogueScapeTheme.SECTION_BG);
		row.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
		row.add(lbl, BorderLayout.WEST);
		return row;
	}

	private JLabel fieldLabel(String text)
	{
		JLabel lbl = new JLabel(text.toUpperCase());
		lbl.setForeground(RogueScapeTheme.ACCENT);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
		lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		return lbl;
	}

	private JScrollPane readOnlyArea(JTextArea area, Color fg, int height)
	{
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		area.setForeground(fg);
		area.setFont(RogueScapeTheme.label(area.getFont()));
		area.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		JScrollPane scroll = new JScrollPane(area);
		scroll.setBorder(BorderFactory.createLineBorder(RogueScapeTheme.BORDER));
		scroll.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, height));
		scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
		return scroll;
	}

	private JPanel hintBox(String[] lines)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER),
			BorderFactory.createEmptyBorder(5, 8, 5, 8)
		));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (String line : lines)
		{
			JLabel hl = new JLabel(line);
			hl.setForeground(RogueScapeTheme.TEXT_MUTED);
			hl.setFont(hl.getFont().deriveFont(Font.PLAIN, 10f));
			box.add(hl);
		}
		return box;
	}

	private JPanel featureBox(String title, String[] lines)
	{
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		box.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(RogueScapeTheme.BORDER_BRIGHT),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)
		));
		box.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel head = new JLabel(title);
		head.setForeground(RogueScapeTheme.GOLD);
		head.setFont(RogueScapeTheme.value(head.getFont()).deriveFont(Font.BOLD));
		box.add(head);
		box.add(Box.createVerticalStrut(3));
		for (String line : lines)
		{
			JLabel row = new JLabel("<html><body style='width:170px'>" + escape(line) + "</body></html>");
			row.setForeground(line.contains("[BOSS]") ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.TEXT_PRIMARY);
			row.setFont(RogueScapeTheme.label(row.getFont()));
			box.add(row);
		}
		return box;
	}

	private JPanel vGap(int h)
	{
		JPanel p = new JPanel();
		p.setBackground(RogueScapeTheme.SECTION_BG);
		p.setPreferredSize(new Dimension(1, h));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
		return p;
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String actionLabel(PanelAction action)
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
}
