package com.pluginideahub.roguescape.ui;

import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.Test;

public class RogueScapeCustomBuilderWidgetPreviewTest
{
	@Test
	public void writesCustomBuilderWidgetPreview() throws Exception
	{
		RogueScapeCustomBuilderWidgetWindow.View view = new RogueScapeCustomBuilderWidgetWindow.View(
			"Scavenger",
			"Naked",
			Arrays.asList("No starter items"),
			false,
			0,
			2,
			"Lumbridge Swamp",
			"Supply",
			"Bryophyta",
			Arrays.asList("Lumbridge Swamp", "Draynor Village", "Barbarian Village", "Edgeville",
				"Varrock East", "Varrock West", "Grand Exchange", "Falador", "Port Sarim",
				"Rimmington", "Karamja Jungle", "Al-Kharid"),
			Arrays.asList("Supply", "Armour", "Weapons", "Skilling", "All", "Shopping"),
			Arrays.asList("Giant Mole", "King Black Dragon", "Chaos Elemental", "Scorpia", "Venenatis"),
			Arrays.asList("No Food", "No Potions", "No Teleports", "No Shields", "No Magic", "No Ranged"),
			Arrays.asList("No Food", "No Teleports"),
			Arrays.asList(0, 2),
			0,
			4,
			2,
			1,
			Arrays.asList("Lumbridge Swamp [Supply]", "Varrock Sewers [Weapons]", "Bryophyta [Boss]"),
			1,
			"mode=Scavenger;loadout=Naked;rooms=lumbridge-swamp:Supply,varrock-sewers:Weapons,boss-bryophyta:Boss"
		);

		BufferedImage img = new BufferedImage(680, 430, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			Dimension size = RogueScapeCustomBuilderWidgetWindow.renderPreview(g, view);
			assertTrue(size.width > 0);
		}
		finally
		{
			g.dispose();
		}

		File dir = new File("build/ui-preview");
		assertTrue(dir.mkdirs() || dir.isDirectory());
		File out = new File(dir, "widget-custom-builder.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}
}
