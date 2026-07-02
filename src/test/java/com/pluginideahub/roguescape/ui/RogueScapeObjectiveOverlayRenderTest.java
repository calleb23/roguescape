package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RogueScapeObjectiveOverlayRenderTest
{
	@Test
	public void rendersObjectiveHudToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		RogueScapeObjectiveOverlay.View view = new RogueScapeObjectiveOverlay.View(
			"Barbarian Village",
			"Find a weapon upgrade (0 / 1)",
			"Giant Mole",
			"12341",
			"Room Active",
			"Score 18",
			0.4,
			false,
			true);
		RogueScapeObjectiveOverlay overlay = new RogueScapeObjectiveOverlay(() -> view);

		BufferedImage scratch = new BufferedImage(320, 160, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		Dimension d = overlay.render(sg);
		sg.dispose();

		BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x3A4A2E));
		g.fillRect(0, 0, d.width, d.height);
		overlay.render(g);
		g.dispose();

		File out = new File(dir, "objective-hud.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}
}
