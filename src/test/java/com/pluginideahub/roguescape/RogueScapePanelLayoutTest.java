package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import com.pluginideahub.roguescape.ui.CollapsibleSection;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomEditorState;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import net.runelite.client.ui.PluginPanel;
import org.junit.Test;

import static org.junit.Assert.*;

public class RogueScapePanelLayoutTest
{
	@Test
	public void sidePanelKeepsNonZeroBoundedPreferredHeight()
	{
		RogueScapePanel panel = newPanel();

		Dimension preferred = panel.getPreferredSize();
		Dimension minimum = panel.getMinimumSize();

		assertTrue("preferred width should stay near the RuneLite sidebar width",
			preferred.width >= PluginPanel.PANEL_WIDTH - 32 && preferred.width <= PluginPanel.PANEL_WIDTH + 128);
		assertTrue("preferred height should not collapse to zero", preferred.height > 0);
		assertTrue("minimum height should not collapse to zero", minimum.height > 0);
	}

	@Test
	public void sidePanelUsesRuneLitePluginPanelScrollWrapper()
	{
		RogueScapePanel panel = newPanel();
		JScrollPane wrapperScroll = directChildScrollPane(panel.getWrappedPanel());

		assertNotNull("RuneLite PluginPanel wrapper should provide the sidebar scroll host", wrapperScroll);
		assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, wrapperScroll.getHorizontalScrollBarPolicy());
		assertTrue("the navigation tab should receive PluginPanel's wrapped panel, not the raw content panel",
			panel.getWrappedPanel() != panel);
		assertEquals("raw RogueScape panel should not contain a nested top-level scroll host",
			0, Arrays.stream(panel.getComponents()).filter(JScrollPane.class::isInstance).count());
	}

	@Test
	public void verticalLayoutExposesTheExpectedSections()
	{
		RogueScapePanel panel = newPanel();
		List<CollapsibleSection> sections = collectSections(panel);

		assertTrue("expected the full vertical section stack from the design mockup",
			sections.size() >= 8);
	}

	@Test
	public void sectionsCanCollapseAndExpand()
	{
		RogueScapePanel panel = newPanel();
		CollapsibleSection section = collectSections(panel).get(0);

		boolean initiallyCollapsed = section.isCollapsed();
		section.setCollapsed(!initiallyCollapsed);
		assertEquals("toggling should flip the collapsed state", !initiallyCollapsed, section.isCollapsed());
		assertEquals("content visibility should track the collapsed state",
			initiallyCollapsed, section.content().isVisible());
	}

	@Test
	public void runBuilderSelectionsMapToRunSystemEnums()
	{
		RogueScapePanel panel = newPanel();

		// Mode is selected through the full-width cards; presets were removed, so the
		// preset selection is always UNSPECIFIED.
		clickNamedButton(panel, "roguescape.modeTile.custom");

		assertEquals(RunMode.CUSTOM_CREATOR, panel.selectedMode());
		assertEquals(RunPreset.UNSPECIFIED, panel.selectedPreset());
	}

	@Test
	public void runBuilderExposesTrimmedSeedForSeededRoutes()
	{
		RogueScapePanel panel = newPanel();
		JTextField seedField = collectComponents(panel, JTextField.class).stream()
			.filter(field -> "roguescape.seedField".equals(field.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("seed field not found"));

		seedField.setText("  weekly-42  ");

		assertEquals("weekly-42", panel.selectedSeed());
	}

	@Test
	public void compactRunBuilderButtonsShowExpectedCards()
	{
		RogueScapePanel panel = newPanel();

		clickButton(panel, "Route");
		assertEquals("Route", activeBuilderCard(panel));

		clickButton(panel, "Zone");
		assertEquals("Zone", activeBuilderCard(panel));

		clickButton(panel, "Curses");
		assertEquals("Mods", activeBuilderCard(panel));
	}

	@Test
	public void storyboardLobbyTilesDriveExistingRunSelections()
	{
		RogueScapePanel panel = newPanel();

		clickNamedButton(panel, "roguescape.modeTile.reward");
		assertEquals(RunMode.BANK_DRAFT, panel.selectedMode());
		assertEquals(RunPreset.UNSPECIFIED, panel.selectedPreset());

		clickNamedButton(panel, "roguescape.modeTile.custom");
		assertEquals(RunMode.CUSTOM_CREATOR, panel.selectedMode());
	}

	@Test
	public void popoutWindowModeActionsDrivePanelSelections()
	{
		RogueScapePanel panel = newPanel();

		panel.selectRunBuilderMode("rewarded");
		assertEquals(RunMode.BANK_DRAFT, panel.selectedMode());
		assertEquals(RunPreset.UNSPECIFIED, panel.selectedPreset());

		panel.selectRunBuilderMode("goal");
		assertEquals(RunMode.FRESH_SOURCE, panel.selectedMode());
		assertEquals(RunPreset.UNSPECIFIED, panel.selectedPreset());

		panel.selectRunBuilderMode("weekly");
		assertEquals(RunMode.FRESH_SOURCE, panel.selectedMode());
		assertEquals("", panel.selectedSeed());

		panel.selectRunBuilderMode("custom");
		assertEquals(RunMode.CUSTOM_CREATOR, panel.selectedMode());
	}

	@Test
	public void customBuilderBridgeMutatesRouteSelections()
	{
		RogueScapePanel panel = newPanel();

		panel.addFirstRoomOfKind(com.pluginideahub.roguescape.core.region.RoomKind.WEAPON);
		panel.addFirstRoomOfKind(com.pluginideahub.roguescape.core.region.RoomKind.SUPPLY);
		assertEquals(2, panel.selectedRoomIds().size());
		assertEquals(2, panel.selectedRoomLabels().size());

		panel.removeLastCustomRoom();
		assertEquals(1, panel.selectedRoomIds().size());

		panel.clearCustomRoute();
		assertTrue(panel.selectedRoomIds().isEmpty());
	}

	@Test
	public void customBuilderTracksModeLoadoutAllowanceAndRouteOrder()
	{
		RogueScapePanel panel = newPanel();

		panel.setCustomBuilderGameMode("Rewarded");
		panel.setCustomBuilderLoadout("Low Gear");
		panel.addRoomForAllowance("Supply");
		panel.addRoomForAllowance("Weapons");
		assertEquals("Rewarded", panel.customBuilderGameMode());
		assertEquals("Low Gear", panel.customBuilderLoadout());
		assertTrue(panel.selectedRoomLabels().get(0).contains("[Supply]"));
		assertTrue(panel.selectedRoomLabels().get(1).contains("[Weapon]"));

		panel.moveSelectedRouteUp();
		assertTrue(panel.selectedRoomLabels().get(0).contains("[Weapon]"));
		assertTrue(panel.customSeedPreview().contains("loadout=Low Gear"));
		assertTrue(panel.customSeedPreview().contains(":Weapon"));
	}

	@Test
	public void customBuilderSelectorFlowAddsRoomWithChosenAllowanceAndBoss()
	{
		RogueScapePanel panel = newPanel();

		assertEquals("Lumbridge Swamp", panel.customSelectedRoomLabel());
		assertEquals("Weapon", panel.customSelectedAllowanceLabel());
		panel.customNextRoom();
		panel.customNextAllowance();
		panel.addSelectedCustomRoom();

		assertEquals(1, panel.selectedRoomIds().size());
		assertTrue(panel.selectedRoomLabels().get(0).contains("[Armour]"));
		assertTrue(panel.selectedRoomLabels().get(0).contains("random unlock draft"));

		panel.customNextBoss();
		panel.addSelectedCustomBoss();
		assertFalse(panel.selectedBossId().isEmpty());
		assertTrue(panel.selectedRoomLabels().get(1).contains("random relic draft"));
		assertTrue(panel.customSeedPreview().contains(":Boss"));
	}

	@Test
	public void customBuilderDirectListSelectionDrivesRoomTypeBossAndRoute()
	{
		RogueScapePanel panel = newPanel();

		panel.selectCustomRoomIndex(4);
		panel.selectCustomAllowanceIndex(0);
		panel.addSelectedCustomRoom();

		assertEquals("Varrock East", panel.customSelectedRoomLabel());
		assertEquals("Weapon", panel.customSelectedAllowanceLabel());
		assertTrue(panel.selectedRoomLabels().get(0).contains("Varrock East"));
		assertTrue(panel.selectedRoomLabels().get(0).contains("[Weapon]"));

		panel.selectCustomBossIndex(1);
		panel.addSelectedCustomBoss();
		assertFalse(panel.selectedBossId().isEmpty());

		panel.addRoomForAllowance("Supply");
		panel.selectRouteRow(0);
		panel.moveSelectedRouteDown();
		assertTrue(panel.selectedRoomLabels().get(1).contains("Varrock East"));
	}

	@Test
	public void customBuilderModifierTogglesTrackStartingCurses()
	{
		RogueScapePanel panel = newPanel();

		panel.toggleCustomModifierIndex(0);
		panel.toggleCustomModifierIndex(2);
		assertEquals(2, panel.selectedModifierIds().size());
		assertEquals(2, panel.selectedModifierLabels().size());
		assertTrue(panel.selectedModifierIndexes().contains(0));
		assertTrue(panel.selectedModifierIndexes().contains(2));

		panel.toggleCustomModifierIndex(0);
		assertEquals(1, panel.selectedModifierIds().size());

		panel.clearCustomModifiers();
		assertTrue(panel.selectedModifierIds().isEmpty());
	}

	@Test
	public void customBuilderModifierPagingReachesLaterCurses()
	{
		RogueScapePanel panel = newPanel();

		assertEquals(0, panel.customModifierPageStart());
		panel.pageCustomModifierIndex(10);
		assertEquals(10, panel.customModifierPageStart());

		panel.toggleCustomModifierIndex(10);
		assertEquals(1, panel.selectedModifierIds().size());
		assertTrue(panel.selectedModifierIndexes().contains(10));

		panel.pageCustomModifierIndex(-10);
		assertEquals(0, panel.customModifierPageStart());
		assertTrue(panel.selectedModifierIndexes().contains(10));
	}

	@Test
	public void customBuilderConstraintChoicesAppearInSeedPreview()
	{
		RogueScapePanel panel = newPanel();

		panel.toggleCustomBankUnlocks();
		panel.cycleCustomTimeLimit();
		panel.cycleCustomBossLimit();

		assertTrue(panel.customBankUnlocks());
		assertEquals(30, panel.customTimeLimitMinutes());
		assertEquals(1, panel.customBossLimit());
		assertTrue(panel.customSeedPreview().contains("bank=on"));
		assertTrue(panel.customSeedPreview().contains("time=30m"));
		assertTrue(panel.customSeedPreview().contains("bosscap=1"));
	}

	@Test
	public void customBuilderBossLimitStopsExtraBossSteps()
	{
		RogueScapePanel panel = newPanel();

		panel.cycleCustomBossLimit();
		panel.selectCustomBossIndex(0);
		panel.addSelectedCustomBoss();
		panel.selectCustomBossIndex(1);
		panel.addSelectedCustomBoss();

		assertEquals(1, panel.selectedRoomAllowances().stream().filter("Boss"::equals).count());
	}

	@Test
	public void customBuilderRoomAndBossPagingMovesSelections()
	{
		RogueScapePanel panel = newPanel();

		String firstRoom = panel.customSelectedRoomLabel();
		panel.pageCustomRoomIndex(10);
		assertNotEquals(firstRoom, panel.customSelectedRoomLabel());
		panel.pageCustomRoomIndex(-10);
		assertEquals(firstRoom, panel.customSelectedRoomLabel());

		String firstBoss = panel.customSelectedBossLabel();
		panel.pageCustomBossIndex(10);
		assertNotEquals(firstBoss, panel.customSelectedBossLabel());
	}

	@Test
	public void customBuilderSeedRoundTripsIntoBuilderState()
	{
		RogueScapePanel panel = newPanel();

		panel.applyCustomSeed("mode=Rewarded;loadout=Mid Gear;rooms=lumbridge-swamp:Weapons,boss-king-black-dragon:Boss,edgeville:Shopping;"
			+ "mods=mod-no-food,mod-no-teleports;bank=on;time=60m;bosscap=2");

		assertEquals("Rewarded", panel.customBuilderGameMode());
		assertEquals("Mid Gear", panel.customBuilderLoadout());
		assertEquals(3, panel.selectedRoomIds().size());
		assertEquals("Weapons", panel.selectedRoomAllowances().get(0));
		assertEquals("Boss", panel.selectedRoomAllowances().get(1));
		assertEquals("Shopping", panel.selectedRoomAllowances().get(2));
		assertEquals("boss-king-black-dragon", panel.selectedBossId());
		assertTrue(panel.selectedModifierIds().contains("mod-no-food"));
		assertTrue(panel.selectedModifierIds().contains("mod-no-teleports"));
		assertTrue(panel.customBankUnlocks());
		assertEquals(60, panel.customTimeLimitMinutes());
		assertEquals(2, panel.customBossLimit());
	}

	@Test
	public void rendersSidePanelPreviewToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		RogueScapePanel panel = newPanel();
		panel.render(null);
		Dimension size = new Dimension(PluginPanel.PANEL_WIDTH, 920);
		panel.setPreferredSize(size);
		panel.setMinimumSize(size);
		panel.setSize(size);
		layoutTree(panel);

		BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		panel.printAll(g);
		g.dispose();

		File out = new File(dir, "side-panel-lobby.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}

	private static void layoutTree(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				Container nested = (Container) child;
				Dimension preferred = nested.getPreferredSize();
				if (preferred.width > 0 && preferred.height > 0 && (nested.getWidth() == 0 || nested.getHeight() == 0))
				{
					nested.setSize(Math.max(container.getWidth(), preferred.width), preferred.height);
				}
				layoutTree(nested);
			}
		}
	}

	private static RogueScapePanel newPanel()
	{
		RogueScapeCustomRoomSelection selection = new RogueScapeCustomRoomSelection();
		RogueScapeCustomRoomEditorState state = new RogueScapeCustomRoomEditorState(selection);
		return new RogueScapePanel(state, () -> { }, () -> { }, action -> { }, () -> true);
	}

	private static JScrollPane directChildScrollPane(Container container)
	{
		return Arrays.stream(container.getComponents())
			.filter(JScrollPane.class::isInstance)
			.map(JScrollPane.class::cast)
			.findFirst()
			.orElse(null);
	}

	private static List<CollapsibleSection> collectSections(Component component)
	{
		List<CollapsibleSection> found = new ArrayList<>();
		collectSections(component, found);
		return found;
	}

	private static void collectSections(Component component, List<CollapsibleSection> found)
	{
		if (component instanceof CollapsibleSection)
		{
			found.add((CollapsibleSection) component);
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				collectSections(child, found);
			}
		}
	}

	private static void clickButton(Component component, String text)
	{
		JButton button = collectComponents(component, JButton.class).stream()
			.filter(b -> text.equals(b.getText()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("button not found: " + text));
		button.doClick();
	}

	private static void clickNamedButton(Component component, String name)
	{
		JButton button = collectComponents(component, JButton.class).stream()
			.filter(b -> name.equals(b.getName()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("button not found: " + name));
		button.doClick();
	}

	private static Object activeBuilderCard(Component component)
	{
		return collectComponents(component, JPanel.class).stream()
			.map(panel -> panel.getClientProperty("roguescape.builderCard"))
			.filter(value -> value != null)
			.findFirst()
			.orElse(null);
	}

	private static <T extends Component> List<T> collectComponents(Component component, Class<T> type)
	{
		List<T> found = new ArrayList<>();
		collectComponents(component, type, found);
		return found;
	}

	private static <T extends Component> void collectComponents(Component component, Class<T> type, List<T> found)
	{
		if (type.isInstance(component))
		{
			found.add(type.cast(component));
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				collectComponents(child, type, found);
			}
		}
	}
}
