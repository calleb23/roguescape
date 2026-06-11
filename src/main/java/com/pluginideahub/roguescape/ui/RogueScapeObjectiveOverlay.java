package com.pluginideahub.roguescape.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public final class RogueScapeObjectiveOverlay extends Overlay
{
	private static final int WIDTH = 246;
	private static final int PAD = 9;
	private static final int BAR_H = 9;

	public static final class View
	{
		final String stage;
		final String objective;
		final String next;
		final String region;
		final String phase;
		final String score;
		final double progress;
		final boolean objectiveReady;
		final boolean regionLegal;

		public View(String stage, String objective, String next, String region, String phase, String score,
			double progress, boolean objectiveReady, boolean regionLegal)
		{
			this.stage = stage == null ? "" : stage;
			this.objective = objective == null ? "" : objective;
			this.next = next == null ? "" : next;
			this.region = region == null ? "" : region;
			this.phase = phase == null ? "" : phase;
			this.score = score == null ? "" : score;
			this.progress = Math.max(0, Math.min(1, progress));
			this.objectiveReady = objectiveReady;
			this.regionLegal = regionLegal;
		}
	}

	private final Supplier<View> supplier;

	public RogueScapeObjectiveOverlay(Supplier<View> supplier)
	{
		this.supplier = supplier;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setMovable(true);
		setSnappable(true);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		View view = supplier == null ? null : supplier.get();
		if (view == null)
		{
			return null;
		}

		int height = view.next.isEmpty() ? 92 : 108;
		g.setPaint(new GradientPaint(0, 0, RogueScapeTheme.SECTION_HEADER_BG,
			0, height, RogueScapeTheme.PANEL_BG));
		g.fillRoundRect(0, 0, WIDTH, height, 8, 8);
		g.setColor(new Color(0, 0, 0, 120));
		g.drawRoundRect(1, 1, WIDTH - 3, height - 3, 8, 8);
		g.setColor(RogueScapeTheme.BORDER_BRIGHT);
		g.drawRoundRect(0, 0, WIDTH - 1, height - 1, 8, 8);

		g.setFont(FontManager.getRunescapeBoldFont());
		FontMetrics bold = g.getFontMetrics();
		String title = clip(view.stage, bold, WIDTH - PAD * 2 - 44);
		g.setColor(RogueScapeTheme.GOLD);
		g.drawString(title, PAD, 18);

		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		g.setColor(view.objectiveReady ? RogueScapeTheme.POSITIVE : RogueScapeTheme.TEXT_PRIMARY);
		g.drawString(clip(view.objective, fm, WIDTH - PAD * 2), PAD, 36);

		int barY = 45;
		g.setColor(RogueScapeTheme.BAR_TRACK);
		g.fillRoundRect(PAD, barY, WIDTH - PAD * 2, BAR_H, 5, 5);
		int fill = (int) Math.round((WIDTH - PAD * 2 - 2) * view.progress);
		if (fill > 0)
		{
			g.setColor(RogueScapeTheme.BAR_PROGRESS);
			g.fillRoundRect(PAD + 1, barY + 1, fill, BAR_H - 2, 4, 4);
		}
		g.setColor(RogueScapeTheme.BORDER);
		g.drawRoundRect(PAD, barY, WIDTH - PAD * 2, BAR_H, 5, 5);

		int y = 70;
		drawPill(g, PAD, y - 13, view.phase, RogueScapeTheme.ACCENT);
		drawRight(g, view.score, WIDTH - PAD, y, RogueScapeTheme.GOLD);
		y += 16;
		g.setColor(view.regionLegal ? RogueScapeTheme.POSITIVE : RogueScapeTheme.NEGATIVE);
		g.drawString(clip("Region: " + view.region, fm, WIDTH - PAD * 2), PAD, y);
		if (!view.next.isEmpty())
		{
			y += 16;
			g.setColor(RogueScapeTheme.TEXT_MUTED);
			g.drawString(clip("Next: " + view.next, fm, WIDTH - PAD * 2), PAD, y);
		}
		return new Dimension(WIDTH, height);
	}

	private static void drawPill(Graphics2D g, int x, int y, String text, Color color)
	{
		String label = text == null || text.isEmpty() ? "Running" : text;
		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(label) + 14;
		g.setColor(RogueScapeFrame.darken(color, 65));
		g.fillRoundRect(x, y, w, 16, 8, 8);
		g.setColor(color);
		g.drawRoundRect(x, y, w, 16, 8, 8);
		g.setColor(RogueScapeTheme.TEXT_PRIMARY);
		g.drawString(label, x + 7, y + 12);
	}

	private static void drawRight(Graphics2D g, String text, int right, int baseline, Color color)
	{
		g.setFont(FontManager.getRunescapeFont());
		FontMetrics fm = g.getFontMetrics();
		g.setColor(color);
		g.drawString(text, right - fm.stringWidth(text), baseline);
	}

	private static String clip(String text, FontMetrics fm, int maxWidth)
	{
		String s = RogueScapeWindowOverlay.ascii(text == null ? "" : text);
		if (fm.stringWidth(s) <= maxWidth)
		{
			return s;
		}
		while (s.length() > 1 && fm.stringWidth(s + ".") > maxWidth)
		{
			s = s.substring(0, s.length() - 1);
		}
		return s + ".";
	}
}
