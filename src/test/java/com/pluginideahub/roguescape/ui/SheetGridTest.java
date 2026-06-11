package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;

/** Harness: overlays a labelled coordinate grid on the UI asset sheet to read off slice rects. */
public class SheetGridTest
{
	@Test
	public void writeGrid() throws Exception
	{
		BufferedImage sheet;
		try (InputStream in = getClass().getResourceAsStream("/com/pluginideahub/roguescape/ui/ui-sheet.png"))
		{
			assertNotNull("ui-sheet.png must be on the classpath", in);
			sheet = ImageIO.read(in);
		}

		BufferedImage out = new BufferedImage(sheet.getWidth(), sheet.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.drawImage(sheet, 0, 0, null);
		g.setFont(new Font("SansSerif", Font.BOLD, 11));

		for (int x = 0; x < sheet.getWidth(); x += 32)
		{
			boolean major = x % 128 == 0;
			g.setColor(major ? new Color(255, 255, 0, 160) : new Color(0, 255, 255, 60));
			g.drawLine(x, 0, x, sheet.getHeight());
			if (major)
			{
				g.setColor(Color.YELLOW);
				g.drawString(String.valueOf(x), x + 2, 12);
			}
		}
		for (int y = 0; y < sheet.getHeight(); y += 32)
		{
			boolean major = y % 128 == 0;
			g.setColor(major ? new Color(255, 255, 0, 160) : new Color(0, 255, 255, 60));
			g.drawLine(0, y, sheet.getWidth(), y);
			if (major)
			{
				g.setColor(Color.YELLOW);
				g.drawString(String.valueOf(y), 2, y + 12);
			}
		}
		g.dispose();

		File dir = new File("build/ui-preview");
		dir.mkdirs();
		ImageIO.write(out, "png", new File(dir, "sheet-grid.png"));
	}
}
