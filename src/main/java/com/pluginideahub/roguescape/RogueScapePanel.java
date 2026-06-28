package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.CustomRunSpec;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import com.pluginideahub.roguescape.core.RunSeedCodec;
import com.pluginideahub.roguescape.core.region.BossLibrary;
import com.pluginideahub.roguescape.core.region.RoomDefinition;
import com.pluginideahub.roguescape.core.region.RoomKind;
import com.pluginideahub.roguescape.core.region.RoomLibrary;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.Relic;
import com.pluginideahub.roguescape.core.ui.PanelAction;
import com.pluginideahub.roguescape.core.ui.SidePanelViewModel;
import com.pluginideahub.roguescape.ui.CollapsibleSection;
import com.pluginideahub.roguescape.ui.PanelActionPresenter;
import com.pluginideahub.roguescape.ui.PanelWidgetFactory;
import com.pluginideahub.roguescape.ui.RelicCatalogSection;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomEditorState;
import com.pluginideahub.roguescape.ui.RogueScapeTheme;
import com.pluginideahub.roguescape.ui.StatBar;
import com.pluginideahub.roguescape.ui.ZoneBuilderSection;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
	private final JComboBox<String> roomCombo = new JComboBox<>();
	private final JComboBox<String> bossCombo = new JComboBox<>();
	private final JTextArea selectedRoomsArea = new JTextArea(4, 14);
	private final JPanel routePickerPanel = new JPanel();
	private final JPanel routePreviewPanel = new JPanel();
	// Swing-free run-builder state (extracted; the panel delegates its builder API to this).
	private final CustomRunSpec customRunSpec = new CustomRunSpec();

	// STARTING CURSES widgets
	private final List<Relic> modifierDefs = new ArrayList<>();
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

	// The custom-zone builder card (owns its own widgets + editor-state wiring).
	private final ZoneBuilderSection zoneBuilderSection;

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
		this.zoneBuilderSection = new ZoneBuilderSection(roomEditorState, saveRoomRequest, useRoomRequest);

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
		column.add(RelicCatalogSection.build());
		column.add(devSection);
		column.add(buildSettingsSection());
		column.add(Box.createVerticalGlue());

		add(column, BorderLayout.NORTH);

		if (this.roomEditorState != null)
		{
			this.roomEditorState.onChange(() -> javax.swing.SwingUtilities.invokeLater(zoneBuilderSection::update));
		}

		zoneBuilderSection.update();
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
			case 3: return RunMode.SEEDED_RACE;
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
		else if ("seeded".equals(actionId))
		{
			selectRunBuilderState(3, 0, "");
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
		content.add(zoneBuilderSection.buildTab(), "Zone");
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
		return PanelWidgetFactory.builderTab();
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
		return customRunSpec.selectedRoomIds();
	}

	public List<String> selectedRoomLabels()
	{
		return customRunSpec.selectedRoomLabels();
	}

	public List<String> customRouteLabels()
	{
		return customRunSpec.customRouteLabels();
	}

	public List<String> selectedRoomAllowances()
	{
		return customRunSpec.selectedRoomAllowances();
	}

	public int selectedRouteIndex()
	{
		return customRunSpec.selectedRouteIndex();
	}

	public String customBuilderGameMode()
	{
		return customRunSpec.customBuilderGameMode();
	}

	public void setCustomBuilderGameMode(String customBuilderGameMode)
	{
		customRunSpec.setCustomBuilderGameMode(customBuilderGameMode);
	}

	public String customBuilderLoadout()
	{
		return customRunSpec.customBuilderLoadout();
	}

	public void setCustomBuilderLoadout(String customBuilderLoadout)
	{
		customRunSpec.setCustomBuilderLoadout(customBuilderLoadout);
	}

	public String customStrictness()
	{
		return customRunSpec.customStrictness();
	}

	public boolean customBankUnlocks()
	{
		return customRunSpec.customBankUnlocks();
	}

	public int customTimeLimitMinutes()
	{
		return customRunSpec.customTimeLimitMinutes();
	}

	public int customBossLimit()
	{
		return customRunSpec.customBossLimit();
	}

	public void cycleCustomStrictness()
	{
		customRunSpec.cycleCustomStrictness();
	}

	public void toggleCustomBankUnlocks()
	{
		customRunSpec.toggleCustomBankUnlocks();
	}

	public void cycleCustomTimeLimit()
	{
		customRunSpec.cycleCustomTimeLimit();
	}

	public void cycleCustomBossLimit()
	{
		customRunSpec.cycleCustomBossLimit();
	}

	public void addFirstRoomOfKind(RoomKind kind)
	{
		if (customRunSpec.addFirstRoomOfKind(kind))
		{
			refreshSelectedRoomsArea();
		}
	}

	public void addFirstRoomOfKind(RoomKind kind, String allowance)
	{
		if (customRunSpec.addFirstRoomOfKind(kind, allowance))
		{
			refreshSelectedRoomsArea();
		}
	}

	public void addRoomForAllowance(String allowance)
	{
		if (customRunSpec.addRoomForAllowance(allowance))
		{
			refreshSelectedRoomsArea();
		}
	}

	public String customSelectedRoomLabel()
	{
		return customRunSpec.customSelectedRoomLabel();
	}

	public List<String> customRoomOptionLabels()
	{
		return customRunSpec.customRoomOptionLabels();
	}

	public int customSelectedRoomIndex()
	{
		return customRunSpec.customSelectedRoomIndex();
	}

	public void selectCustomRoomIndex(int index)
	{
		customRunSpec.selectCustomRoomIndex(index);
	}

	public void pageCustomRoomIndex(int delta)
	{
		customRunSpec.pageCustomRoomIndex(delta);
	}

	public String customSelectedAllowanceLabel()
	{
		return customRunSpec.customSelectedAllowanceLabel();
	}

	public List<String> customAllowanceOptionLabels()
	{
		return customRunSpec.customAllowanceOptionLabels();
	}

	public int customSelectedAllowanceIndex()
	{
		return customRunSpec.customSelectedAllowanceIndex();
	}

	public void selectCustomAllowanceIndex(int index)
	{
		customRunSpec.selectCustomAllowanceIndex(index);
	}

	public String customSelectedBossLabel()
	{
		return customRunSpec.customSelectedBossLabel();
	}

	public List<String> customBossOptionLabels()
	{
		return customRunSpec.customBossOptionLabels();
	}

	public int customSelectedBossIndex()
	{
		return customRunSpec.customSelectedBossIndex();
	}

	public void selectCustomBossIndex(int index)
	{
		customRunSpec.selectCustomBossIndex(index);
	}

	public void pageCustomBossIndex(int delta)
	{
		customRunSpec.pageCustomBossIndex(delta);
	}

	public void customPreviousRoom()
	{
		customRunSpec.customPreviousRoom();
	}

	public void customNextRoom()
	{
		customRunSpec.customNextRoom();
	}

	public void customPreviousAllowance()
	{
		customRunSpec.customPreviousAllowance();
	}

	public void customNextAllowance()
	{
		customRunSpec.customNextAllowance();
	}

	public void customPreviousBoss()
	{
		customRunSpec.customPreviousBoss();
	}

	public void customNextBoss()
	{
		customRunSpec.customNextBoss();
	}

	public void addSelectedCustomRoom()
	{
		if (customRunSpec.addSelectedCustomRoom())
		{
			refreshSelectedRoomsArea();
		}
	}

	public void addSelectedCustomBoss()
	{
		if (customRunSpec.addSelectedCustomBoss())
		{
			refreshSelectedRoomsArea();
		}
	}

	public void removeLastCustomRoom()
	{
		if (customRunSpec.removeLastCustomRoom())
		{
			refreshSelectedRoomsArea();
		}
	}

	public void clearCustomRoute()
	{
		clearSelectedRooms();
	}

	public void selectPreviousRouteRow()
	{
		customRunSpec.selectPreviousRouteRow();
		refreshSelectedRoomsArea();
	}

	public void selectNextRouteRow()
	{
		customRunSpec.selectNextRouteRow();
		refreshSelectedRoomsArea();
	}

	public void selectRouteRow(int index)
	{
		customRunSpec.selectRouteRow(index);
		refreshSelectedRoomsArea();
	}

	public void moveSelectedRouteUp()
	{
		if (customRunSpec.moveSelectedRouteUp())
		{
			refreshSelectedRoomsArea();
		}
	}

	public void moveSelectedRouteDown()
	{
		if (customRunSpec.moveSelectedRouteDown())
		{
			refreshSelectedRoomsArea();
		}
	}

	public String customSeedPreview()
	{
		return customRunSpec.customSeedPreview();
	}

	public void applyCustomSeed(String seed)
	{
		Map<String, String> fields = RunSeedCodec.parseFields(seed);
		if (fields.isEmpty())
		{
			return;
		}
		setCustomBuilderGameMode(fields.get("mode"));
		setCustomBuilderLoadout(fields.get("loadout"));

		customRunSpec.applyRouteFromSeed(fields.get("rooms"), fields.get("boss"));

		customRunSpec.applyModifierIdsFromCsv(fields.get("mods"));
		String strictness = fields.get("strictness");
		if ("Trust".equalsIgnoreCase(strictness)) customRunSpec.setCustomStrictness("Trust");
		else if ("Strict".equalsIgnoreCase(strictness)) customRunSpec.setCustomStrictness("Strict");
		else if ("Balanced".equalsIgnoreCase(strictness)) customRunSpec.setCustomStrictness("Balanced");
		String bank = fields.get("bank");
		if (bank != null)
		{
			customRunSpec.setCustomBankUnlocks("on".equalsIgnoreCase(bank) || "true".equalsIgnoreCase(bank));
		}
		String time = fields.get("time");
		if (time != null)
		{
			customRunSpec.setCustomTimeLimitMinutes(RunSeedCodec.parseTimeMinutes(time));
		}
		String bossCap = fields.get("bosscap");
		if (bossCap != null)
		{
			customRunSpec.setCustomBossLimit(RunSeedCodec.parseBossLimit(bossCap));
		}
		refreshSelectedRoomsArea();
		refreshSelectedModifiersArea();
	}

	public void selectFirstBoss()
	{
		if (customRunSpec.selectFirstBoss())
		{
			refreshSelectedRoomsArea();
		}
	}

	/** First boss ID included in the mixed route, or "" for none. */
	public String selectedBossId()
	{
		return customRunSpec.selectedBossId();
	}

	/** Relic IDs of the curses/modifiers to apply when the run STARTs (empty = none). */
	public List<String> selectedModifierIds()
	{
		return customRunSpec.selectedModifierIds();
	}

	public List<String> customModifierOptionLabels()
	{
		return customRunSpec.customModifierOptionLabels();
	}

	public int customModifierPageStart()
	{
		return customRunSpec.customModifierPageStart();
	}

	public void pageCustomModifierIndex(int delta)
	{
		customRunSpec.pageCustomModifierIndex(delta);
	}

	public List<String> selectedModifierLabels()
	{
		return customRunSpec.selectedModifierLabels();
	}

	public List<Integer> selectedModifierIndexes()
	{
		return customRunSpec.selectedModifierIndexes();
	}

	public void toggleCustomModifierIndex(int index)
	{
		customRunSpec.toggleModifierIndex(index);
		refreshSelectedModifiersArea();
	}

	public void clearCustomModifiers()
	{
		customRunSpec.clearModifiers();
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
		modeCombo.addItem("Seeded Race");
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
		if (customRunSpec.addRoute(id, CustomRunSpec.allowanceLabel(roomDefs.get(idx).kind())))
		{
			refreshSelectedRoomsArea();
		}
	}

	private void removeLastRoom()
	{
		if (customRunSpec.removeLastRoom())
		{
			refreshSelectedRoomsArea();
		}
	}

	private void clearSelectedRooms()
	{
		customRunSpec.clearRoute();
		refreshSelectedRoomsArea();
	}

	private void refreshSelectedRoomsArea()
	{
		StringBuilder sb = new StringBuilder();
		int n = 1;
		List<String> ids = customRunSpec.selectedRoomIds();
		List<String> allowances = customRunSpec.selectedRoomAllowances();
		int selected = customRunSpec.selectedRouteIndex();
		for (int i = 0; i < ids.size(); i++)
		{
			String marker = i == selected ? "> " : "";
			String allowance = i < allowances.size() ? allowances.get(i) : "All";
			sb.append(marker).append(n++).append(". ").append(roomName(ids.get(i)))
				.append(" [").append(allowance).append("]\n");
		}
		selectedRoomsArea.setText(sb.toString());
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
		return PanelWidgetFactory.routeRow(text);
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
		clear.addActionListener(e -> { customRunSpec.clearModifiers(); refreshSelectedModifiersArea(); });
		c.add(clear);
		return c;
	}

	private void addSelectedModifier()
	{
		int idx = modifierCombo.getSelectedIndex();
		if (idx < 0 || idx >= modifierDefs.size()) return;
		String id = modifierDefs.get(idx).relicId();
		if (customRunSpec.addModifierIdIfAbsent(id))
		{
			refreshSelectedModifiersArea();
		}
	}

	private void refreshSelectedModifiersArea()
	{
		StringBuilder sb = new StringBuilder();
		for (String id : customRunSpec.selectedModifierIds())
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
		return PanelWidgetFactory.keyValueRow(line);
	}

	// ------------------------------------------------------------ ZONE BUILDER

	// ------------------------------------------------------------ RELICS

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
		JButton btn = new JButton(PanelActionPresenter.label(action));
		styleButton(btn, PanelActionPresenter.roleFor(action, primary));
		btn.addActionListener(e -> dispatch(action));
		return btn;
	}

	/** Backwards-compatible boolean styling: primary -> gold, else neutral. */
	private void styleButton(JButton btn, boolean primary)
	{
		PanelWidgetFactory.styleButton(btn, primary);
	}

	private void styleButton(JButton btn, RogueScapeTheme.ButtonRole role)
	{
		PanelWidgetFactory.styleButton(btn, role);
	}

	/** A label-left / value-right stat row for the LIVE RUN section. */
	private JPanel statRow(String label, String value, Color valueColor)
	{
		return PanelWidgetFactory.statRow(label, value, valueColor);
	}

	/** A single detail line (e.g. "You CAN:", "  ✓ Fight monsters"). */
	private JPanel detailRow(String text)
	{
		return PanelWidgetFactory.detailRow(text);
	}

	private JPanel mutedRow(String text)
	{
		return PanelWidgetFactory.mutedRow(text);
	}

	private JLabel fieldLabel(String text)
	{
		return PanelWidgetFactory.fieldLabel(text);
	}

	private JScrollPane readOnlyArea(JTextArea area, Color fg, int height)
	{
		return PanelWidgetFactory.readOnlyArea(area, fg, height);
	}

	private JPanel hintBox(String[] lines)
	{
		return PanelWidgetFactory.hintBox(lines);
	}

	private JPanel featureBox(String title, String[] lines)
	{
		return PanelWidgetFactory.featureBox(title, lines);
	}

	private JPanel vGap(int h)
	{
		return PanelWidgetFactory.vGap(h);
	}

	private static String escape(String s)
	{
		return PanelWidgetFactory.escape(s);
	}

}
