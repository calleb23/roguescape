package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/** Harness: crops candidate slice rects from the asset sheet into one labelled contact sheet. */
public class SheetSliceTest
{
	// name, x, y, w, h — tweak these until each crop frames its component cleanly.
	private static final Object[][] SLICES = {
		{"divider", 905, 975, 200, 22},
		{"corners", 1170, 958, 170, 48},
		{"hover-hi", 600, 290, 170, 40},
	};

	@Test
	public void writeContactSheet() throws Exception
	{
		BufferedImage sheet;
		try (InputStream in = getClass().getResourceAsStream("/com/pluginideahub/roguescape/ui/ui-sheet.png"))
		{
			assertNotNull(in);
			sheet = ImageIO.read(in);
		}

		int scale = 2;
		int pad = 18;
		int x = pad;
		int maxH = 0;
		int totalW = pad;
		for (Object[] s : SLICES)
		{
			totalW += (int) s[3] * scale + pad;
			maxH = Math.max(maxH, (int) s[4] * scale);
		}

		BufferedImage out = new BufferedImage(totalW, maxH + 40, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(0x202020));
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.setFont(new Font("SansSerif", Font.PLAIN, 10));

		for (Object[] s : SLICES)
		{
			String name = (String) s[0];
			Rectangle r = new Rectangle((int) s[1], (int) s[2], (int) s[3], (int) s[4]);
			BufferedImage crop = sheet.getSubimage(
				Math.max(0, r.x), Math.max(0, r.y),
				Math.min(r.width, sheet.getWidth() - r.x),
				Math.min(r.height, sheet.getHeight() - r.y));
			g.drawImage(crop, x, 30, r.width * scale, r.height * scale, null);
			g.setColor(Color.MAGENTA);
			g.drawRect(x, 30, r.width * scale, r.height * scale);
			g.setColor(Color.WHITE);
			g.drawString(name, x, 14);
			g.drawString(r.x + "," + r.y + " " + r.width + "x" + r.height, x, 26);
			x += r.width * scale + pad;
		}
		g.dispose();

		File dir = new File("build/ui-preview");
		dir.mkdirs();
		ImageIO.write(out, "png", new File(dir, "slices.png"));
	}
}
