package com.pluginideahub.roguescape.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;

public class RogueScapeWidgetSkinPreviewTest
{
	@Test
	public void writesBookBackgroundPreview() throws Exception
	{
		BufferedImage img = RogueScapeWidgetSkin.bookBackground(
			RogueScapeWidgetSkin.BOOK_W, RogueScapeWidgetSkin.BOOK_H);
		assertEquals(RogueScapeWidgetSkin.BOOK_W, img.getWidth());
		assertEquals(RogueScapeWidgetSkin.BOOK_H, img.getHeight());

		// The spread must be fully opaque paper — a transparent pixel would let the game world
		// bleed through the widget window.
		int cx = img.getWidth() / 2;
		int cy = img.getHeight() / 2;
		assertEquals(0xFF, (img.getRGB(cx, cy) >>> 24));
		assertEquals(0xFF, (img.getRGB(10, 10) >>> 24));

		File dir = new File("build/ui-preview");
		assertTrue(dir.mkdirs() || dir.isDirectory());
		File out = new File(dir, "widget-skin-book-bg.png");
		ImageIO.write(img, "png", out);
		assertTrue(out.exists());
	}

	@Test
	public void ribbonHasTransparentCorners()
	{
		BufferedImage img = RogueScapeWidgetSkin.ribbonImage(16, 34);
		// Ribbon is a down-pointing triangle notch at the bottom centre; the bottom-centre
		// pixel above the notch tip is inside it, the very corner pixels of the image are not.
		assertEquals(0, (img.getRGB(0, img.getHeight() - 1) >>> 24));
	}
}
