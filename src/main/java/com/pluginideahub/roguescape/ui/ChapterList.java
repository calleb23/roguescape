package com.pluginideahub.roguescape.ui;

import com.pluginideahub.roguescape.core.ui.SidePanelViewModel.Chapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import net.runelite.client.ui.PluginPanel;

/**
 * Paints the run's route as a journal table of contents: each chapter is a numeral + name with
 * a dotted leader and a stamp slot at the end — struck through and CLEAR-stamped once cleared,
 * marked with the ribbon bookmark at the current chapter, an empty dotted slot ahead, and the
 * boss finale in red. Long runs window the list (a few chapters around the bookmark) and add a
 * passport stamp-grid so the whole campaign still fits.
 */
public class ChapterList extends JComponent
{
	private static final int LINE_H = 28;
	private static final int WINDOW = 9;          // show all when at most this many chapters
	private static final int AROUND = 4;          // chapters shown around the bookmark when windowed
	private static final int CELL = 18;
	private static final int COLS = 10;

	private List<Chapter> chapters = new ArrayList<>();

	public ChapterList()
	{
		setOpaque(false);
		setAlignmentX(LEFT_ALIGNMENT);
		recompute();
	}

	public void setChapters(List<Chapter> chapters)
	{
		this.chapters = chapters == null ? new ArrayList<>() : new ArrayList<>(chapters);
		recompute();
		revalidate();
		repaint();
	}

	private int currentIndex()
	{
		for (int i = 0; i < chapters.size(); i++)
		{
			if (chapters.get(i).isCurrent())
			{
				return i;
			}
		}
		return -1;
	}

	private boolean windowed()
	{
		return chapters.size() > WINDOW;
	}

	private void recompute()
	{
		int w = PluginPanel.PANEL_WIDTH - 32;
		int h;
		if (chapters.isEmpty())
		{
			h = LINE_H;
		}
		else if (!windowed())
		{
			h = chapters.size() * LINE_H + 4;
		}
		else
		{
			int rows = (chapters.size() + COLS - 1) / COLS;
			// two ellipsis lines + the window of lines + passport header + grid + caption
			h = 2 * 18 + (AROUND + 1) * LINE_H + 16 + rows * CELL + 22;
		}
		Dimension d = new Dimension(w, h);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int w = getWidth();
		if (chapters.isEmpty())
		{
			g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
			g.setColor(RogueScapeTheme.INK_FADED);
			g.drawString("No route yet — sign a contract.", 4, 16);
			g.dispose();
			return;
		}

		int y = 0;
		if (!windowed())
		{
			for (Chapter c : chapters)
			{
				line(g, 0, y, w, c);
				y += LINE_H;
			}
			g.dispose();
			return;
		}

		// Windowed: a band of chapters around the bookmark, framed by ink ellipses.
		int cur = Math.max(0, currentIndex());
		int from = Math.max(0, cur - 1);
		int to = Math.min(chapters.size(), from + AROUND + 1);
		from = Math.max(0, to - (AROUND + 1));

		g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
		g.setColor(RogueScapeTheme.INK_FADED);
		if (from > 0)
		{
			g.drawString("… " + from + " chapters stamped …", 12, y + 13);
		}
		y += 18;
		for (int i = from; i < to; i++)
		{
			line(g, 0, y, w, chapters.get(i));
			y += LINE_H;
		}
		int ahead = chapters.size() - to;
		g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
		g.setColor(RogueScapeTheme.INK_FADED);
		if (ahead > 0)
		{
			g.drawString("… " + ahead + " chapters unwritten …", 12, y + 13);
		}
		y += 16;

		// Passport stamp grid: the whole campaign at a glance.
		g.setFont(new Font(Font.SERIF, Font.BOLD, 12));
		g.setColor(RogueScapeTheme.INK);
		g.drawString("THE PASSPORT", 0, y + 10);
		y += 16;
		passport(g, 4, y, cur);

		g.dispose();
	}

	/** One chapter line: numeral, name (struck if done), dotted leader, end stamp slot. */
	private void line(Graphics2D g, int x, int y, int w, Chapter c)
	{
		boolean done = c.isDone();
		boolean current = c.isCurrent();
		boolean boss = c.isBoss();

		g.setFont(new Font(Font.SERIF, Font.BOLD, 13));
		g.setColor(boss ? RogueScapeTheme.STAMP : RogueScapeTheme.INK);
		g.drawString(c.numeral() + ".", x, y + 12);

		g.setFont(new Font(Font.SERIF, boss ? Font.BOLD : Font.PLAIN, 13));
		g.setColor(done ? RogueScapeTheme.INK_FADED : boss ? RogueScapeTheme.STAMP : RogueScapeTheme.INK);
		int nameX = x + 46;
		String name = c.name();
		FontMetrics fm = g.getFontMetrics();
		// Trim long place names so the leader + slot always have room.
		int maxName = w - 46 - 34;
		while (fm.stringWidth(name) > maxName && name.length() > 4)
		{
			name = name.substring(0, name.length() - 2);
		}
		if (!name.equals(c.name()))
		{
			name = name + "…";
		}
		g.drawString(name, nameX, y + 12);
		int nameEnd = nameX + fm.stringWidth(name);
		if (done)
		{
			g.setStroke(new BasicStroke(1.4f));
			g.drawLine(x + 44, y + 8, nameEnd + 2, y + 8);
		}

		RogueScapePaper.leader(g, nameEnd + 6, y + 9, x + w - 28);

		int sx = x + w - 13;
		int sy = y + 7;
		if (done)
		{
			RogueScapePaper.clearStamp(g, sx, sy, 11, "CLEAR");
		}
		else if (current)
		{
			RogueScapePaper.ribbon(g, sx, y - 2, 13, 20);
		}
		else
		{
			RogueScapePaper.stampSlot(g, sx, sy, 10, boss);
		}
	}

	private void passport(Graphics2D g, int x, int y, int currentIdx)
	{
		for (int i = 0; i < chapters.size(); i++)
		{
			int cx = x + (i % COLS) * CELL + 9;
			int cy = y + (i / COLS) * CELL + 9;
			Chapter c = chapters.get(i);
			if (c.isDone())
			{
				RogueScapePaper.clearStamp(g, cx, cy, 7, "");
			}
			else if (i == currentIdx)
			{
				g.setColor(RogueScapeTheme.RIBBON);
				g.fillPolygon(new int[]{cx - 4, cx + 4, cx}, new int[]{cy - 6, cy - 6, cy + 5}, 3);
			}
			else
			{
				RogueScapePaper.stampSlot(g, cx, cy, 7, c.isBoss());
			}
		}
		int rows = (chapters.size() + COLS - 1) / COLS;
		g.setFont(new Font(Font.SERIF, Font.ITALIC, 11));
		g.setColor(RogueScapeTheme.INK_FADED);
		g.drawString("every chapter, one stamp — bosses in red.", x, y + rows * CELL + 12);
	}
}
