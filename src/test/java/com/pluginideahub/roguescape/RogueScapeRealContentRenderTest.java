package com.pluginideahub.roguescape;

import com.pluginideahub.roguescape.core.RogueScapeRun;
import com.pluginideahub.roguescape.core.RogueScapeRunLoop;
import com.pluginideahub.roguescape.core.RogueScapeRunSession;
import com.pluginideahub.roguescape.core.RunMode;
import com.pluginideahub.roguescape.core.RunPreset;
import com.pluginideahub.roguescape.core.RunRouteBuilder;
import com.pluginideahub.roguescape.core.relic.ModifierLibrary;
import com.pluginideahub.roguescape.core.relic.RelicLibrary;
import com.pluginideahub.roguescape.core.region.RogueScapeCustomRoomSelection;
import com.pluginideahub.roguescape.ui.RogueScapeCustomRoomEditorState;
import com.pluginideahub.roguescape.ui.RogueScapeWindowOverlay;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Renders the in-game window through the REAL {@link RogueScapeWindowContent} provider — a
 * live run with relics, the lobby, and the custom builder — instead of the hand-built sample
 * tabs used by the overlay render test. This is what verifies the production content builders
 * actually produce sane visuals.
 */
public class RogueScapeRealContentRenderTest
{
	private static RogueScapePlugin pluginShell()
	{
		RogueScapePlugin plugin = new RogueScapePlugin();
		plugin.config = new RogueScapeConfig() { };
		plugin.customRoomEditorState = new RogueScapeCustomRoomEditorState(new RogueScapeCustomRoomSelection());
		plugin.panel = new RogueScapePanel(plugin.customRoomEditorState, () -> { }, () -> { }, action -> { }, () -> true);
		return plugin;
	}

	private static void startRun(RogueScapePlugin plugin)
	{
		RogueScapeRunSession session = RogueScapeRunSession.start(
			"UI verification run", "ui-seed", RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED);
		RogueScapeRun run = RogueScapeRun.wrap(session);
		RunRouteBuilder.buildRoute(RunMode.FRESH_SOURCE, RunPreset.UNSPECIFIED, "ui-seed", session, run);
		run.chooseRelic(RelicLibrary.all().get(0));
		run.chooseRelic(RelicLibrary.all().get(1));
		run.chooseRelic(ModifierLibrary.all().get(0));
		plugin.runSession = session;
		plugin.rogueRun = run;
		plugin.runLoop = new RogueScapeRunLoop(run, 0L);
	}

	@Test
	public void rendersRealRunTabs() throws Exception
	{
		RogueScapePlugin plugin = pluginShell();
		startRun(plugin);
		RogueScapeWindowContent content = new RogueScapeWindowContent(plugin);

		List<RogueScapeWindowOverlay.Tab> tabs = content.windowTabs();
		assertFalse("active run should produce window tabs", tabs.isEmpty());
		writeAll(tabs, "real-run");

		// Force a stage completion: the loop must offer a real reward draft, and the
		// LIVE RUN tab must surface it as cards.
		plugin.runLoop.forceCompleteCurrentStage(60_000L);
		assertTrue("stage completion should offer a reward draft",
			plugin.runLoop.pendingRewardDraft() != null);
		tabs = content.windowTabs();
		writePng(tabs, 0, new File("build/ui-preview/real-run-control-reward.png"));
		writePng(tabs, 1, new File("build/ui-preview/real-run-reward.png"));
	}

	@Test
	public void rendersRealLobbyAndCustomBuilderTabs() throws Exception
	{
		RogueScapePlugin plugin = pluginShell();
		RogueScapeWindowContent content = new RogueScapeWindowContent(plugin);

		List<RogueScapeWindowOverlay.Tab> lobby = content.windowTabs();
		assertFalse("lobby should produce window tabs", lobby.isEmpty());
		writePng(lobby, 0, new File("build/ui-preview/real-lobby.png"));

		// Custom mode now shows the coming-soon contract page (old builder retired for rework).
		clickNamedButton(plugin.panel, "roguescape.modeTile.custom");
		List<RogueScapeWindowOverlay.Tab> custom = content.windowTabs();
		assertTrue("custom mode shows the single coming-soon contract spread", custom.size() == 1);
		writeAll(custom, "real-custom");
	}

	@Test
	public void rendersRealSidePanelRunStates() throws Exception
	{
		RogueScapePlugin plugin = pluginShell();
		startRun(plugin);

		panelPng(plugin, com.pluginideahub.roguescape.core.ui.SidePanelViewModel.active(
			plugin.runLoop, com.pluginideahub.roguescape.core.ui.PanelTab.RUN), "side-panel-active.png");

		plugin.runLoop.forceCompleteCurrentStage(60_000L);
		panelPng(plugin, com.pluginideahub.roguescape.core.ui.SidePanelViewModel.active(
			plugin.runLoop, com.pluginideahub.roguescape.core.ui.PanelTab.RUN), "side-panel-reward.png");

		plugin.runSession.completeRun("UI verify done", com.pluginideahub.roguescape.core.RunCompletionReason.MANUAL_SUCCESS);
		plugin.runLoop.markNow(120_000L);
		panelPng(plugin, com.pluginideahub.roguescape.core.ui.SidePanelViewModel.active(
			plugin.runLoop, com.pluginideahub.roguescape.core.ui.PanelTab.RUN), "side-panel-complete.png");
	}

	@Test
	public void rendersLongRunChapterList() throws Exception
	{
		java.util.List<com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter> chapters = new java.util.ArrayList<>();
		String[] places = {"Lumbridge Swamp", "Draynor", "Varrock Sewers", "Edgeville", "Barbarian Village",
			"Al Kharid", "Falador", "Dwarven Mine", "Catacombs", "Brimhaven"};
		for (int i = 0; i < 68; i++)
		{
			boolean boss = (i + 1) % 10 == 0 || i == 67;
			chapters.add(new com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter(
				i == 67 ? "Final" : Integer.toString(i + 1),
				boss ? "BOSS " + (i + 1) : places[i % places.length],
				boss, i < 30, i == 30));
		}
		com.pluginideahub.roguescape.ui.ChapterList list = new com.pluginideahub.roguescape.ui.ChapterList();
		list.setChapters(chapters);
		Dimension d = list.getPreferredSize();
		list.setSize(d);
		BufferedImage img = new BufferedImage(d.width, d.height + 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(com.pluginideahub.roguescape.ui.RogueScapePaper.sheet(d.width, d.height + 8), 0, 0, null);
		list.printAll(g);
		g.dispose();
		File out = new File("build/ui-preview/chapters-longrun.png");
		out.getParentFile().mkdirs();
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}

	private static void panelPng(RogueScapePlugin plugin, com.pluginideahub.roguescape.core.ui.SidePanelViewModel vm,
		String fileName) throws Exception
	{
		plugin.panel.render(vm);
		Dimension size = new Dimension(net.runelite.client.ui.PluginPanel.PANEL_WIDTH, 1180);
		plugin.panel.setPreferredSize(size);
		plugin.panel.setMinimumSize(size);
		plugin.panel.setSize(size);
		layoutTree(plugin.panel);
		BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		plugin.panel.printAll(g);
		g.dispose();
		File out = new File("build/ui-preview/" + fileName);
		out.getParentFile().mkdirs();
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}

	private static void layoutTree(Container container)
	{
		// Headless: no EDT pumps revalidate(), so stale BoxLayout caches must be
		// invalidated by hand before laying out (the live client's EDT does this).
		invalidateTree(container);
		doLayoutTree(container);
		// A second pass settles sizes that depend on the first pass (html labels etc.).
		doLayoutTree(container);
	}

	private static void invalidateTree(Container container)
	{
		container.invalidate();
		for (Component child : container.getComponents())
		{
			child.invalidate();
			if (child instanceof Container)
			{
				invalidateTree((Container) child);
			}
		}
	}

	private static void doLayoutTree(Container container)
	{
		container.doLayout();
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				doLayoutTree((Container) child);
			}
		}
	}

	private static void writeAll(List<RogueScapeWindowOverlay.Tab> tabs, String prefix) throws Exception
	{
		for (int i = 0; i < tabs.size(); i++)
		{
			String name = tabs.get(i).name().toLowerCase().replace(' ', '-');
			writePng(tabs, i, new File("build/ui-preview/" + prefix + "-" + name + ".png"));
		}
	}

	private static void writePng(List<RogueScapeWindowOverlay.Tab> tabs, int tab, File out) throws Exception
	{
		out.getParentFile().mkdirs();
		RogueScapeWindowOverlay overlay = new RogueScapeWindowOverlay(() -> tabs);
		overlay.setOpen(true);
		overlay.setSelectedTab(tab);

		BufferedImage scratch = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		Dimension d = overlay.render(sg);
		sg.dispose();

		BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x3A4A2E));
		g.fillRect(0, 0, d.width, d.height);
		overlay.render(g);
		g.dispose();
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}

	private static boolean clickNamedButton(Component component, String name)
	{
		if (component instanceof AbstractButton && name.equals(component.getName()))
		{
			((AbstractButton) component).doClick();
			return true;
		}
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				if (clickNamedButton(child, name))
				{
					return true;
				}
			}
		}
		return false;
	}
}
