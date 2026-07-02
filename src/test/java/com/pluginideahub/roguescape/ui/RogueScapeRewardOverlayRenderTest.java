package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

/** Render harness for the ROLL SUPPLIES reward window — writes a PNG preview. */
public class RogueScapeRewardOverlayRenderTest
{
	@Test
	public void rendersRewardWindowToPng() throws Exception
	{
		File dir = new File("build/ui-preview");
		dir.mkdirs();

		List<RogueScapeRewardOverlay.Card> cards = Arrays.asList(
			new RogueScapeRewardOverlay.Card("Relic of Blood", "RELIC",
				RogueScapeRewardOverlay.Rarity.RARE, 0,
				Arrays.asList("Heals 2 HP on kill. Stacks up to 10 times.")),
			new RogueScapeRewardOverlay.Card("Twisted Souls", "RELIC",
				RogueScapeRewardOverlay.Rarity.EPIC, 0,
				Arrays.asList("Forbids armour.", "Max 4 food", "+5 score: weapon")),
			new RogueScapeRewardOverlay.Card("Dark Hunger", "RELIC",
				RogueScapeRewardOverlay.Rarity.LEGENDARY, 0,
				Arrays.asList("One-shot mercy.", "-25% food healing recorded at recap.")));

		RogueScapeRewardOverlay.RewardView view =
			new RogueScapeRewardOverlay.RewardView("D1", "The chest holds a relic", "choose one power — the rest crumble to dust", cards,
				Arrays.asList("Chapter: 7 of 50", "Stamped: Barbarian Village", "Next: Giant Mole",
					"Build:", "Bloodthirsty", "", "Score: 182",
					"Relics: 4", "Lawful: 18", "Forbidden: 0", "", "Curses:", "Twisted Souls",
					"Dark Hunger", "", "Noted:", "The chest is open."),
				Arrays.asList(1631, 995, 385, 1712, 1127, 4587, 6685));

		RogueScapeRewardOverlay overlay = new RogueScapeRewardOverlay(() -> view, null, i -> {}, () -> {});

		BufferedImage scratch = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = scratch.createGraphics();
		Dimension d = overlay.render(sg);
		sg.dispose();

		BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(new Color(0x3A4A2E));
		g.fillRect(0, 0, d.width, d.height);
		overlay.render(g);
		g.dispose();

		File out = new File(dir, "reward.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}
}
