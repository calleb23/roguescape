import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Throwaway concept-art painter for a paper "adventurer's journal" RogueScape UI.
 * Not plugin code — just produces mockup PNGs.
 */
public class PaperConcepts
{
	// Palette
	static final Color PAPER = new Color(0xC9B584);
	static final Color PAPER_DARK = new Color(0xB8A271);
	static final Color PAPER_CARD = new Color(0xD4C193);
	static final Color INK = new Color(0x33241454, true);
	static final Color INK_SOLID = new Color(0x332414);
	static final Color INK_FADED = new Color(0x6E5A3E);
	static final Color WAX_RED = new Color(0xA42C1E);
	static final Color WAX_GREEN = new Color(0x4F7A2B);
	static final Color WAX_BLUE = new Color(0x2F5E8C);
	static final Color WAX_GOLD = new Color(0xB98A2C);
	static final Color STAMP_RED = new Color(0xB03224);

	static Random rng = new Random(7);

	public static void main(String[] args) throws Exception
	{
		ImageIO.write(sidePanel(), "png", new File("/tmp/concepts/concept-journal-panel.png"));
		ImageIO.write(rewardSpread(), "png", new File("/tmp/concepts/concept-journal-reward.png"));
		ImageIO.write(liveRunPage(), "png", new File("/tmp/concepts/concept-journal-live.png"));
		ImageIO.write(longRunPage(), "png", new File("/tmp/concepts/concept-journal-longrun.png"));
		System.out.println("done");
	}

	// ------------------------------------------------------------ side panel: the journal page
	static BufferedImage sidePanel()
	{
		int w = 225, h = 920;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = gfx(img);
		// RuneLite dark client behind the paper
		g.setColor(new Color(0x1B1B1B));
		g.fillRect(0, 0, w, h);
		paper(g, 4, 4, w - 8, h - 8, 3);

		int x = 16, y = 38;
		// Header: ink title + wax seal
		g.setFont(serif(Font.BOLD, 22));
		g.setColor(INK_SOLID);
		g.drawString("RogueScape", x, y);
		g.setFont(serifItalic(13));
		g.setColor(INK_FADED);
		g.drawString("an adventurer's journal", x, y + 16);
		waxSeal(g, w - 34, 38, 16, WAX_RED);
		y += 34;
		inkRule(g, x, y, w - 32); y += 24;

		// Contracts
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("— PICK YOUR CONTRACT —", x + 8, y); y += 10;
		String[][] contracts = {
			{"Scavenger", "earn power room by room"},
			{"Rewarded", "short prep, boss loot"},
			{"Custom", "draw your own route"},
			{"Seeded Race", "same seed, same fate"},
		};
		Color[] seals = {WAX_GREEN, WAX_GOLD, WAX_BLUE, WAX_RED};
		for (int i = 0; i < contracts.length; i++)
		{
			contractCard(g, x, y, w - 32, 52, contracts[i][0], contracts[i][1], seals[i], i == 0, (i % 2 == 0) ? -0.8 : 0.7);
			y += 60;
		}
		y += 8;

		// The route as a table of contents: chapters with passport-stamp slots.
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("— CHAPTERS —", x + 8, y); y += 14;
		String[][] chapters = {
			{"I", "Lumbridge Swamp", "done"},
			{"II", "Canifis", "current"},
			{"III", "Dwarven Mine", ""},
			{"Final", "GIANT MOLE", "boss"},
		};
		for (String[] ch : chapters)
		{
			chapterLine(g, x, y, w - 32, ch[0], ch[1], ch[2]);
			y += 30;
		}
		y += 10;

		// Seed line
		g.setFont(serifItalic(13));
		g.setColor(INK_SOLID);
		g.drawString("Seed:", x, y + 2);
		g.setColor(INK_FADED);
		dashedLine(g, x + 40, y + 4, w - 20, y + 4);
		g.setFont(serifItalic(12));
		g.drawString("rat-king-42", x + 48, y);
		y += 26;

		// Start stamp
		stampButton(g, x + 6, y, w - 44, 40, "BEGIN THE RUN", -2.0);
		y += 64;
		inkRule(g, x, y, w - 32); y += 22;

		// Ledger preview (live state when running)
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("— THE LEDGER —", x + 8, y); y += 18;
		g.setFont(serif(Font.PLAIN, 12));
		ledgerLine(g, x, y, w - 32, "No run underway.", ""); y += 24;
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("Sign a contract, stamp it, begin.", x, y); y += 30;

		// Relic pockets: paper slots with stitched edges
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("— RELIC POCKETS —", x + 8, y); y += 10;
		for (int i = 0; i < 3; i++)
		{
			pocket(g, x + i * 64, y, 56, 56, i == 0);
		}
		y += 70;
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("Relics you claim are tucked here.", x, y);

		g.dispose();
		return img;
	}

	// ------------------------------------------------------------ reward: open journal spread
	static BufferedImage rewardSpread()
	{
		int w = 680, h = 440;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = gfx(img);
		g.setColor(new Color(0x2A2620));
		g.fillRect(0, 0, w, h);

		// Two pages + spine shadow
		paper(g, 14, 12, w / 2 - 18, h - 24, 2);
		paper(g, w / 2 + 4, 12, w / 2 - 18, h - 24, 2);
		GradientPaint spine = new GradientPaint(w / 2 - 18, 0, new Color(0, 0, 0, 0), w / 2, 0, new Color(0, 0, 0, 70));
		g.setPaint(spine);
		g.fillRect(w / 2 - 18, 12, 18, h - 24);
		GradientPaint spine2 = new GradientPaint(w / 2, 0, new Color(0, 0, 0, 70), w / 2 + 18, 0, new Color(0, 0, 0, 0));
		g.setPaint(spine2);
		g.fillRect(w / 2, 12, 18, h - 24);

		// LEFT PAGE — the chest
		int lx = 34, ly = 44;
		g.setFont(serif(Font.BOLD, 19));
		g.setColor(INK_SOLID);
		g.drawString("Floor 2 — the chest opens", lx, ly);
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("choose one, the rest crumble to dust", lx, ly + 16);
		inkRule(g, lx, ly + 26, w / 2 - 70);

		String[][] loot = {
			{"One Potion Max", "Curse: at most one potion.", "curse"},
			{"Cape Collection", "Capes score +4 each at recap.", "relic"},
			{"No Runes", "Curse: runes are forbidden.", "curse"},
		};
		Color[] lootSeal = {WAX_RED, WAX_GOLD, WAX_RED};
		for (int i = 0; i < 3; i++)
		{
			lootCard(g, lx + i * 96, ly + 44, 86, 220, loot[i][0], loot[i][1], lootSeal[i], i == 1, (i - 1) * 1.6);
		}
		// chosen note
		g.setFont(serifItalic(12));
		g.setColor(STAMP_RED);
		g.drawString("« the gold seal calls to you »", lx + 30, ly + 290);
		stampButton(g, lx + 16, ly + 305, 150, 34, "TAKE IT", -1.5);
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("or leave the chest…", lx + 185, ly + 327);

		// RIGHT PAGE — the ledger
		int rx = w / 2 + 26, ry = 44;
		g.setFont(serif(Font.BOLD, 17));
		g.setColor(INK_SOLID);
		g.drawString("The Ledger", rx, ry);
		inkRule(g, rx, ry + 10, w / 2 - 60);
		g.setFont(serif(Font.PLAIN, 13));
		int yy = ry + 32;
		ledgerLine(g, rx, yy, w / 2 - 60, "Time afoot", "06:18"); yy += 22;
		ledgerLine(g, rx, yy, w / 2 - 60, "Rooms cleared", "2 of 4"); yy += 22;
		ledgerLine(g, rx, yy, w / 2 - 60, "Standing", "all lawful"); yy += 22;
		// score as tally marks
		g.setColor(INK_SOLID);
		g.drawString("Score", rx, yy + 12);
		tally(g, rx + 90, yy + 2, 18);
		yy += 34;
		inkRule(g, rx, yy, w / 2 - 60); yy += 22;
		g.setFont(smallCaps(12));
		g.drawString("RELICS IN POCKET", rx, yy); yy += 12;
		String[] relics = {"One Bank Mercy", "Four-Food Limit", "No Food (curse)"};
		Color[] rseal = {WAX_BLUE, WAX_GREEN, WAX_RED};
		for (int i = 0; i < relics.length; i++)
		{
			waxSeal(g, rx + 10, yy + 12, 9, rseal[i]);
			g.setFont(serif(Font.PLAIN, 13));
			g.setColor(INK_SOLID);
			g.drawString(relics[i], rx + 28, yy + 16);
			yy += 26;
		}
		yy += 6;
		// footnote
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("the next page is blank — for now.", rx, h - 40);

		g.dispose();
		return img;
	}

	// ------------------------------------------------------------ live run: today's entry
	static BufferedImage liveRunPage()
	{
		int w = 680, h = 440;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = gfx(img);
		g.setColor(new Color(0x2A2620));
		g.fillRect(0, 0, w, h);
		paper(g, 14, 12, w - 28, h - 24, 2);

		// Red ribbon bookmark hanging over the page edge at the current chapter.
		g.setColor(new Color(0x9E2B1F));
		Polygon ribbon = new Polygon(new int[]{52, 76, 76, 64, 52}, new int[]{12, 12, 84, 72, 84}, 5);
		g.fillPolygon(ribbon);
		g.setColor(new Color(0, 0, 0, 60));
		g.drawPolygon(ribbon);

		int x = 96, y = 52;
		g.setFont(serif(Font.BOLD, 22));
		g.setColor(INK_SOLID);
		g.drawString("Chapter II — Canifis", x, y);
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("the ink is still wet on this one.", x, y + 17);
		inkRule(g, x, y + 28, w - 300);

		// The entry: written objective + progress as an inked sentence.
		int ex = 60, ey = y + 60;
		g.setFont(serif(Font.PLAIN, 14));
		g.setColor(INK_SOLID);
		g.drawString("Today I must find a lawful weapon upgrade", ex, ey);
		g.drawString("somewhere in this dreadful town.", ex, ey + 20);
		g.setFont(serifItalic(13));
		g.setColor(STAMP_RED);
		g.drawString("…nothing found yet.   ( 0 of 1 )", ex, ey + 44);

		// What the chapter allows / forbids, as journal margin notes.
		int ny = ey + 78;
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("THE RULES OF THIS PLACE", ex, ny); ny += 18;
		g.setFont(serif(Font.PLAIN, 13));
		g.setColor(new Color(0x4F7A2B));
		g.drawString("✓  fight, loot, and gather here", ex + 6, ny); ny += 19;
		g.drawString("✓  potions (unlocked last chapter)", ex + 6, ny); ny += 19;
		g.setColor(STAMP_RED);
		g.drawString("✗  the bank stays sealed", ex + 6, ny); ny += 19;
		g.drawString("✗  no trading with locals", ex + 6, ny); ny += 19;

		// Passport strip: previous chapters stamped, future ones empty.
		int px = w - 250, py = y + 50;
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("THE RECORD", px, py - 10);
		String[][] chapters = {
			{"I", "Lumbridge Swamp", "done"},
			{"II", "Canifis", "current"},
			{"III", "Dwarven Mine", ""},
			{"Final", "GIANT MOLE", "boss"},
		};
		for (String[] ch : chapters)
		{
			chapterLine(g, px, py, 200, ch[0], ch[1], ch[2]);
			py += 32;
		}

		// Hourglass + abandon stamp bottom right.
		int hx = w - 250, hy = h - 120;
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("THE HOURGLASS", hx, hy);
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(INK_FADED);
		g.drawLine(hx + 4, hy + 12, hx + 24, hy + 12);
		g.drawLine(hx + 4, hy + 42, hx + 24, hy + 42);
		g.drawLine(hx + 4, hy + 12, hx + 24, hy + 42);
		g.drawLine(hx + 24, hy + 12, hx + 4, hy + 42);
		g.setFont(serif(Font.BOLD, 18));
		g.setColor(INK_SOLID);
		g.drawString("06:18", hx + 36, hy + 32);
		stampButton(g, hx, hy + 54, 160, 34, "ABANDON RUN", 1.5);

		// Bottom-left: tear-off corner hint
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("finish the task to stamp this chapter and turn the page.", 60, h - 44);

		g.dispose();
		return img;
	}

	// ------------------------------------------------------------ long run: 68 chapters
	static BufferedImage longRunPage()
	{
		int w = 680, h = 440;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = gfx(img);
		g.setColor(new Color(0x2A2620));
		g.fillRect(0, 0, w, h);
		paper(g, 14, 12, w - 28, h - 24, 2);

		int x = 44, y = 52;
		g.setFont(serif(Font.BOLD, 20));
		g.setColor(INK_SOLID);
		g.drawString("The Record — a long campaign", x, y);
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("chapter 31 of 68 — the book is getting heavy.", x, y + 16);
		inkRule(g, x, y + 26, w - 90); y += 50;

		// Windowed chapters: a few lines around the bookmark, the rest condensed.
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("… 28 chapters already stamped …", x + 16, y); y += 20;
		chapterLine(g, x, y, 280, "XXIX", "Brimhaven Dungeon", "done"); y += 28;
		chapterLine(g, x, y, 280, "XXX", "KALPHITE QUEEN", "done"); y += 28;
		chapterLine(g, x, y, 280, "XXXI", "Catacombs of Kourend", "current"); y += 28;
		chapterLine(g, x, y, 280, "XXXII", "VORKATH", "boss"); y += 28;
		g.setFont(serifItalic(12));
		g.setColor(INK_FADED);
		g.drawString("… 35 chapters yet unwritten …", x + 16, y); y += 24;

		// The passport page: every chapter as a stamp slot in a grid.
		int gx = 370, gy = 96;
		g.setFont(smallCaps(12));
		g.setColor(INK_SOLID);
		g.drawString("THE PASSPORT PAGE", gx, gy - 10);
		int cols = 10, cell = 26;
		for (int i = 0; i < 68; i++)
		{
			int cxx = gx + (i % cols) * cell + 12;
			int cyy = gy + (i / cols) * cell + 12;
			if (i < 30)
			{
				Graphics2D c = (Graphics2D) g.create();
				c.rotate(Math.toRadians(-18 + rng.nextInt(36)), cxx, cyy);
				boolean bossCh = (i + 1) % 10 == 0;
				c.setColor(bossCh ? new Color(0x90B03224, true) : new Color(0x606E5A3E, true));
				c.setStroke(new BasicStroke(1.8f));
				c.drawOval(cxx - 9, cyy - 9, 18, 18);
				c.drawLine(cxx - 5, cyy, cxx + 5, cyy);
				c.dispose();
			}
			else if (i == 30)
			{
				g.setColor(new Color(0x9E2B1F));
				g.fillPolygon(new int[]{cxx - 5, cxx + 5, cxx}, new int[]{cyy - 8, cyy - 8, cyy + 7}, 3);
			}
			else
			{
				Stroke old = g.getStroke();
				g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{2.5f, 2.5f}, 0));
				boolean bossCh = (i + 1) % 10 == 0;
				g.setColor(bossCh ? new Color(0x70B03224, true) : new Color(0x506E5A3E, true));
				g.drawOval(cxx - 8, cyy - 8, 16, 16);
				g.setStroke(old);
			}
		}
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("every chapter, one stamp — bosses in red.", gx, gy + 7 * 26 + 26);

		// footer
		g.setFont(serifItalic(11));
		g.setColor(INK_FADED);
		g.drawString("the chapter list always shows where you are; the passport shows how far you've come.", x, h - 44);
		g.dispose();
		return img;
	}

	/** One table-of-contents line: numeral, dotted leader, name, stamp slot at the end. */
	static void chapterLine(Graphics2D g, int x, int y, int w, String numeral, String name, String state)
	{
		boolean done = "done".equals(state);
		boolean current = "current".equals(state);
		boolean boss = "boss".equals(state);
		g.setFont(serif(Font.BOLD, 13));
		g.setColor(boss ? STAMP_RED : INK_SOLID);
		g.drawString(numeral + ".", x, y + 12);
		g.setFont(boss ? serif(Font.BOLD, 13) : serif(Font.PLAIN, 13));
		g.setColor(done ? INK_FADED : boss ? STAMP_RED : INK_SOLID);
		g.drawString(name, x + 54, y + 12);
		FontMetrics fm = g.getFontMetrics();
		int nameEnd = x + 54 + fm.stringWidth(name);
		if (done)
		{
			// struck through once finished
			g.setStroke(new BasicStroke(1.4f));
			g.drawLine(x + 52, y + 8, nameEnd + 2, y + 8);
		}
		g.setColor(new Color(0x33241430, true));
		dashedLine(g, nameEnd + 6, y + 9, x + w - 30, y + 9);
		// the stamp slot
		int sx = x + w - 14, sy = y + 7;
		if (done)
		{
			// inked arrival stamp, slightly rotated
			Graphics2D c = (Graphics2D) g.create();
			c.rotate(Math.toRadians(-14), sx, sy);
			c.setColor(new Color(0x70B03224, true));
			c.setStroke(new BasicStroke(2f));
			c.drawOval(sx - 11, sy - 11, 22, 22);
			c.setFont(serif(Font.BOLD, 8));
			c.setColor(new Color(0xB0B03224, true));
			c.drawString("CLEAR", sx - 10, sy + 3);
			c.dispose();
		}
		else if (current)
		{
			// the ribbon points here: small red bookmark triangle
			g.setColor(new Color(0x9E2B1F));
			g.fillPolygon(new int[]{sx - 6, sx + 6, sx}, new int[]{sy - 9, sy - 9, sy + 8}, 3);
		}
		else
		{
			// empty dotted slot awaiting its stamp
			Stroke old = g.getStroke();
			g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0));
			g.setColor(boss ? new Color(0x80B03224, true) : new Color(0x806E5A3E, true));
			g.drawOval(sx - 10, sy - 10, 20, 20);
			g.setStroke(old);
		}
	}

	// ------------------------------------------------------------ helpers
	static Graphics2D gfx(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g;
	}

	static Font serif(int style, int size) { return new Font("Serif", style, size); }
	static Font serifItalic(int size) { return new Font("Serif", Font.ITALIC, size); }
	static Font smallCaps(int size) { return new Font("Serif", Font.BOLD, size); }

	/** Aged paper rectangle: base, fiber noise, stain blots, darkened deckled edges. */
	static void paper(Graphics2D g, int x, int y, int w, int h, int wobble)
	{
		g.setColor(new Color(0, 0, 0, 90));
		g.fillRoundRect(x + 3, y + 4, w, h, 6, 6);
		g.setColor(PAPER);
		g.fillRoundRect(x, y, w, h, 6, 6);
		// fiber noise — denser and darker for a worn sheet
		for (int i = 0; i < w * h / 45; i++)
		{
			int px = x + rng.nextInt(w), py = y + rng.nextInt(h);
			g.setColor(new Color(0x6E5A3E & 0xFFFFFF | (10 + rng.nextInt(26)) << 24, true));
			g.fillRect(px, py, 1 + rng.nextInt(2), 1);
		}
		// large uneven tone blotches (water damage)
		for (int i = 0; i < 7; i++)
		{
			int sx = x + rng.nextInt(w), sy = y + rng.nextInt(h), sr = 22 + rng.nextInt(52);
			g.setColor(new Color(0x8A744E & 0xFFFFFF | (10 + rng.nextInt(14)) << 24, true));
			g.fillOval(sx - sr, sy - sr / 2, sr * 2, sr);
		}
		// foxing: small rust-brown age spots
		for (int i = 0; i < w * h / 2600; i++)
		{
			int sx = x + rng.nextInt(w), sy = y + rng.nextInt(h), sr = 2 + rng.nextInt(4);
			g.setColor(new Color(0x7A5230 & 0xFFFFFF | (26 + rng.nextInt(40)) << 24, true));
			g.fillOval(sx, sy, sr, sr);
		}
		// edge vignette: darker, uneven burn creeping inward
		for (int i = 0; i < 3; i++)
		{
			g.setStroke(new BasicStroke(3.5f - i));
			g.setColor(new Color(0x5C4326 & 0xFFFFFF | (46 - i * 14) << 24, true));
			g.drawRoundRect(x + i * 2, y + i * 2, w - i * 4, h - i * 4, 7, 7);
		}
		// ragged nicks on the border
		g.setColor(new Color(0x5C4326));
		for (int i = 0; i < 14; i++)
		{
			boolean vert = rng.nextBoolean();
			int ex = vert ? (rng.nextBoolean() ? x : x + w - 2) : x + rng.nextInt(w);
			int ey = vert ? y + rng.nextInt(h) : (rng.nextBoolean() ? y : y + h - 2);
			g.fillRect(ex, ey, vert ? 2 : 3 + rng.nextInt(4), vert ? 3 + rng.nextInt(4) : 2);
		}
	}

	static void inkRule(Graphics2D g, int x, int y, int w)
	{
		g.setStroke(new BasicStroke(1.3f));
		g.setColor(new Color(0x33241470, true));
		g.drawLine(x, y, x + w, y);
		// flourish
		g.drawArc(x + w / 2 - 8, y - 3, 16, 6, 0, 180);
	}

	static void dashedLine(Graphics2D g, int x1, int y1, int x2, int y2)
	{
		Stroke old = g.getStroke();
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3f, 3f}, 0));
		g.drawLine(x1, y1, x2, y2);
		g.setStroke(old);
	}

	/** Wax seal: blob with rim, emboss star, highlight. */
	static void waxSeal(Graphics2D g, int cx, int cy, int r, Color wax)
	{
		g.setColor(new Color(0, 0, 0, 60));
		g.fillOval(cx - r + 1, cy - r + 2, r * 2, r * 2);
		g.setColor(wax);
		g.fillOval(cx - r, cy - r, r * 2, r * 2);
		// irregular blob edges
		for (int i = 0; i < 5; i++)
		{
			double a = rng.nextDouble() * Math.PI * 2;
			g.fillOval((int) (cx + Math.cos(a) * r * 0.8) - 3, (int) (cy + Math.sin(a) * r * 0.8) - 3, 7, 7);
		}
		g.setColor(wax.darker());
		g.drawOval(cx - r + 3, cy - r + 3, (r - 3) * 2, (r - 3) * 2);
		// emboss star
		g.setColor(new Color(255, 255, 255, 70));
		int sr = r - 6;
		for (int i = 0; i < 4; i++)
		{
			double a = Math.PI / 4 + i * Math.PI / 2;
			g.drawLine(cx, cy, (int) (cx + Math.cos(a) * sr), (int) (cy + Math.sin(a) * sr));
		}
	}

	/** A paper contract card with seal, slight rotation, tack pin; chosen gets a red circle. */
	static void contractCard(Graphics2D g, int x, int y, int w, int h, String title, String sub, Color seal, boolean chosen, double angleDeg)
	{
		Graphics2D c = (Graphics2D) g.create();
		c.rotate(Math.toRadians(angleDeg), x + w / 2.0, y + h / 2.0);
		c.setColor(new Color(0, 0, 0, 50));
		c.fillRect(x + 2, y + 3, w, h);
		c.setColor(PAPER_CARD);
		c.fillRect(x, y, w, h);
		c.setColor(new Color(0x8A744E));
		c.drawRect(x, y, w, h);
		// tack
		c.setColor(new Color(0x6E5A3E));
		c.fillOval(x + w / 2 - 3, y - 3, 7, 7);
		waxSeal(c, x + 22, y + h / 2, 13, seal);
		c.setFont(serif(Font.BOLD, 14));
		c.setColor(INK_SOLID);
		c.drawString(title, x + 44, y + 22);
		c.setFont(serifItalic(11));
		c.setColor(INK_FADED);
		c.drawString(sub, x + 44, y + 38);
		if (chosen)
		{
			c.setColor(STAMP_RED);
			c.setStroke(new BasicStroke(2.2f));
			c.drawOval(x + 6, y + h / 2 - 16, 32, 32);
			c.setFont(serif(Font.BOLD, 10));
			c.rotate(Math.toRadians(-8), x + w - 34, y + 14);
			c.drawString("CHOSEN", x + w - 52, y + 16);
		}
		c.dispose();
	}

	/** Small hand-drawn map box for the side panel. */
	static void mapBox(Graphics2D g, int x, int y, int w, int h)
	{
		g.setColor(PAPER_CARD);
		g.fillRect(x, y, w, h);
		g.setColor(new Color(0x8A744E));
		g.drawRect(x, y, w, h);
		int[][] nodes = {{x + 26, y + h - 28}, {x + 70, y + 56}, {x + 120, y + 96}, {x + w - 32, y + 30}};
		Stroke dash = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{1.5f, 6f}, 0);
		for (int i = 0; i < nodes.length - 1; i++)
		{
			g.setStroke(dash);
			g.setColor(INK_FADED);
			g.drawLine(nodes[i][0], nodes[i][1], nodes[i + 1][0], nodes[i + 1][1]);
		}
		g.setStroke(new BasicStroke(1.8f));
		for (int i = 0; i < nodes.length; i++)
		{
			boolean boss = i == nodes.length - 1;
			if (boss)
			{
				g.setColor(STAMP_RED);
				g.drawLine(nodes[i][0] - 6, nodes[i][1] - 6, nodes[i][0] + 6, nodes[i][1] + 6);
				g.drawLine(nodes[i][0] - 6, nodes[i][1] + 6, nodes[i][0] + 6, nodes[i][1] - 6);
			}
			else
			{
				g.setColor(PAPER);
				g.fillOval(nodes[i][0] - 7, nodes[i][1] - 7, 14, 14);
				g.setColor(INK_SOLID);
				g.drawOval(nodes[i][0] - 7, nodes[i][1] - 7, 14, 14);
			}
		}
		g.setFont(serifItalic(10));
		g.setColor(INK_FADED);
		g.drawString("3 rooms, then the beast", x + 10, y + 16);
	}

	/** Loot card: tall aged card, seal at top, ink diamond sketch, description. */
	static void lootCard(Graphics2D g, int x, int y, int w, int h, String title, String desc, Color seal, boolean chosen, double angleDeg)
	{
		Graphics2D c = (Graphics2D) g.create();
		c.rotate(Math.toRadians(angleDeg), x + w / 2.0, y + h / 2.0);
		c.setColor(new Color(0, 0, 0, 60));
		c.fillRoundRect(x + 2, y + 4, w, h, 8, 8);
		c.setColor(chosen ? new Color(0xF4E9CB) : PAPER_CARD);
		c.fillRoundRect(x, y, w, h, 8, 8);
		c.setStroke(new BasicStroke(chosen ? 2.4f : 1.4f));
		c.setColor(chosen ? STAMP_RED : new Color(0x8A744E));
		c.drawRoundRect(x, y, w, h, 8, 8);
		waxSeal(c, x + w / 2, y + 26, 14, seal);
		c.setFont(serif(Font.BOLD, 12));
		c.setColor(INK_SOLID);
		drawWrapped(c, title, x + 6, y + 56, w - 12, 14, true);
		// ink sketch: diamond w/ hatching
		int cx = x + w / 2, cy = y + 108;
		c.setStroke(new BasicStroke(1.6f));
		c.setColor(INK_SOLID);
		Polygon d = new Polygon(new int[]{cx, cx + 16, cx, cx - 16}, new int[]{cy - 18, cy, cy + 18, cy}, 4);
		c.draw(d);
		for (int i = -10; i <= 10; i += 5)
		{
			c.drawLine(cx + i - 4, cy + Math.abs(i) - 12, cx + i + 4, cy + Math.abs(i) - 4);
		}
		c.setFont(serifItalic(10));
		c.setColor(INK_FADED);
		drawWrapped(c, desc, x + 7, y + 146, w - 14, 12, false);
		if (chosen)
		{
			c.setFont(serif(Font.BOLD, 11));
			c.setColor(STAMP_RED);
			c.rotate(Math.toRadians(-10), x + w / 2, y + h - 18);
			c.drawString("CHOSEN", x + w / 2 - 24, y + h - 14);
		}
		c.dispose();
	}

	static void drawWrapped(Graphics2D g, String text, int x, int y, int w, int lineH, boolean center)
	{
		FontMetrics fm = g.getFontMetrics();
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		for (String word : words)
		{
			String cand = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(cand) > w && line.length() > 0)
			{
				g.drawString(line.toString(), center ? x + (w - fm.stringWidth(line.toString())) / 2 : x, y);
				y += lineH;
				line = new StringBuilder(word);
			}
			else line = new StringBuilder(cand);
		}
		g.drawString(line.toString(), center ? x + (w - fm.stringWidth(line.toString())) / 2 : x, y);
	}

	/** Red rubber-stamp button, slightly rotated, rough double border. */
	static void stampButton(Graphics2D g, int x, int y, int w, int h, String text, double angleDeg)
	{
		Graphics2D c = (Graphics2D) g.create();
		c.rotate(Math.toRadians(angleDeg), x + w / 2.0, y + h / 2.0);
		c.setColor(new Color(STAMP_RED.getRed(), STAMP_RED.getGreen(), STAMP_RED.getBlue(), 28));
		c.fillRoundRect(x, y, w, h, 10, 10);
		c.setStroke(new BasicStroke(2.6f));
		c.setColor(STAMP_RED);
		c.drawRoundRect(x, y, w, h, 10, 10);
		c.setStroke(new BasicStroke(1.2f));
		c.drawRoundRect(x + 4, y + 4, w - 8, h - 8, 7, 7);
		c.setFont(serif(Font.BOLD, 15));
		FontMetrics fm = c.getFontMetrics();
		c.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h / 2 + 5);
		// stamp grain: punch tiny paper-colored holes
		for (int i = 0; i < 60; i++)
		{
			c.setColor(new Color(PAPER.getRed(), PAPER.getGreen(), PAPER.getBlue(), 90));
			c.fillRect(x + 2 + rng.nextInt(w - 4), y + 2 + rng.nextInt(h - 4), 1, 1);
		}
		c.dispose();
	}

	static void ledgerLine(Graphics2D g, int x, int y, int w, String label, String value)
	{
		g.setColor(INK_SOLID);
		g.drawString(label, x, y + 12);
		FontMetrics fm = g.getFontMetrics();
		int lx = x + fm.stringWidth(label) + 6;
		int vx = x + w - fm.stringWidth(value);
		g.setColor(new Color(0x33241430, true));
		dashedLine(g, lx, y + 10, vx - 6, y + 10);
		g.setColor(INK_SOLID);
		g.drawString(value, vx, y + 12);
	}

	static void tally(Graphics2D g, int x, int y, int count)
	{
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(INK_SOLID);
		int groups = count / 5, rest = count % 5, gx = x;
		for (int i = 0; i < groups; i++)
		{
			for (int j = 0; j < 4; j++) g.drawLine(gx + j * 5, y, gx + j * 5, y + 14);
			g.drawLine(gx - 3, y + 12, gx + 18, y + 2);
			gx += 28;
		}
		for (int j = 0; j < rest; j++) g.drawLine(gx + j * 5, y, gx + j * 5, y + 14);
	}

	/** Stitched paper pocket slot for relics. */
	static void pocket(Graphics2D g, int x, int y, int w, int h, boolean filled)
	{
		g.setColor(PAPER_DARK);
		g.fillRoundRect(x, y, w, h, 10, 10);
		g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 3f}, 0));
		g.setColor(INK_FADED);
		g.drawRoundRect(x + 3, y + 3, w - 6, h - 6, 8, 8);
		if (filled)
		{
			waxSeal(g, x + w / 2, y + h / 2, 14, WAX_BLUE);
		}
		else
		{
			g.setFont(new Font("Serif", Font.ITALIC, 18));
			g.setColor(new Color(0x33241440, true));
			g.drawString("?", x + w / 2 - 4, y + h / 2 + 6);
		}
	}
}
