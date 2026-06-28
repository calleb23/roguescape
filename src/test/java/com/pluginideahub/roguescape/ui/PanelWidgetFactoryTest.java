package com.pluginideahub.roguescape.ui;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Characterizes {@link PanelWidgetFactory} — the stateless leaf Swing builders lifted out of
 * {@code RogueScapePanel}. Pins the parts the layout test only exercises indirectly: HTML escaping,
 * label uppercasing, spacer sizing, and the value/symbol-driven foreground colouring in
 * {@code statRow}/{@code keyValueRow}/{@code detailRow}/{@code routeRow}.
 */
public class PanelWidgetFactoryTest
{
	private static JLabel labelAt(JPanel row, String constraint)
	{
		return (JLabel) ((BorderLayout) row.getLayout()).getLayoutComponent(constraint);
	}

	@Test
	public void escapeReplacesHtmlMetacharacters()
	{
		assertEquals("a&lt;b&gt; &amp; c", PanelWidgetFactory.escape("a<b> & c"));
	}

	@Test
	public void fieldLabelUppercasesText()
	{
		assertEquals("SEED", PanelWidgetFactory.fieldLabel("Seed").getText());
	}

	@Test
	public void vGapUsesRequestedHeight()
	{
		assertEquals(12, PanelWidgetFactory.vGap(12).getPreferredSize().height);
	}

	@Test
	public void statRowPlacesLabelWestAndColouredValueEast()
	{
		JPanel row = PanelWidgetFactory.statRow("Score", "42", RogueScapeTheme.GOLD);
		assertEquals("Score", labelAt(row, BorderLayout.WEST).getText());
		JLabel value = labelAt(row, BorderLayout.EAST);
		assertEquals("42", value.getText());
		assertEquals(RogueScapeTheme.GOLD, value.getForeground());
	}

	@Test
	public void keyValueRowColoursByValueSemantics()
	{
		assertEquals(RogueScapeTheme.NEGATIVE,
			labelAt(PanelWidgetFactory.keyValueRow("Items: 3 illegal"), BorderLayout.EAST).getForeground());
		assertEquals(RogueScapeTheme.ACCENT,
			labelAt(PanelWidgetFactory.keyValueRow("Bank: on"), BorderLayout.EAST).getForeground());
		assertEquals(RogueScapeTheme.ACCENT,
			labelAt(PanelWidgetFactory.keyValueRow("Pickups: flagged"), BorderLayout.EAST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_MUTED,
			labelAt(PanelWidgetFactory.keyValueRow("Bank: off"), BorderLayout.EAST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_MUTED,
			labelAt(PanelWidgetFactory.keyValueRow("Trade: allowed"), BorderLayout.EAST).getForeground());
	}

	@Test
	public void keyValueRowWithoutDelimiterFallsBackToDetailRow()
	{
		// no ": " -> detailRow path: a BorderLayout row whose WEST label carries the whole text
		assertEquals("plain line",
			labelAt(PanelWidgetFactory.keyValueRow("plain line"), BorderLayout.WEST).getText());
	}

	@Test
	public void detailRowColoursBySymbolAndPrefix()
	{
		assertEquals(RogueScapeTheme.POSITIVE,
			labelAt(PanelWidgetFactory.detailRow("✓ yes"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.NEGATIVE,
			labelAt(PanelWidgetFactory.detailRow("✗ no"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_PRIMARY,
			labelAt(PanelWidgetFactory.detailRow("You CAN:"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_PRIMARY,
			labelAt(PanelWidgetFactory.detailRow("You CANNOT:"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_MUTED,
			labelAt(PanelWidgetFactory.detailRow("ordinary"), BorderLayout.WEST).getForeground());
	}

	@Test
	public void routeRowColoursBySymbol()
	{
		assertEquals(RogueScapeTheme.POSITIVE,
			labelAt(PanelWidgetFactory.routeRow("✓ done"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.ACCENT,
			labelAt(PanelWidgetFactory.routeRow("▶ active"), BorderLayout.WEST).getForeground());
		assertEquals(RogueScapeTheme.TEXT_MUTED,
			labelAt(PanelWidgetFactory.routeRow("pending"), BorderLayout.WEST).getForeground());
	}
}
