package com.pluginideahub.roguescape.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Small top-left HUD overlay that renders a title plus a list of summary lines supplied by the
 * plugin. Replaces the vendored {@code PluginIdeaHubOverlay} scaffolding with a RogueScape-owned
 * equivalent (same two-supplier contract).
 */
public class RogueScapeSummaryOverlay extends Overlay
{
	private final PanelComponent panelComponent = new PanelComponent();
	private final Supplier<String> titleSupplier;
	private final Supplier<List<String>> linesSupplier;

	public RogueScapeSummaryOverlay(Supplier<String> titleSupplier, Supplier<List<String>> linesSupplier)
	{
		this.titleSupplier = titleSupplier;
		this.linesSupplier = linesSupplier;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(LineComponent.builder()
			.left(titleSupplier.get())
			.build());
		for (String line : safeLines())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(line)
				.build());
		}
		return panelComponent.render(graphics);
	}

	private List<String> safeLines()
	{
		List<String> lines = linesSupplier.get();
		return lines == null ? Collections.emptyList() : lines;
	}
}
