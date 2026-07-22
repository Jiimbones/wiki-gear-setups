package com.wikigearsetups.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The disclaimer clipped for several rounds because a CSS {@code width}
 * does not paint at the width it claims. These pin the property that
 * actually matters: every line fits the budget when measured the same way
 * it is drawn.
 */
public class DisclaimerWrapTest
{
	private static FontMetrics metrics()
	{
		// a real FontMetrics without needing a display
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		return img.createGraphics().getFontMetrics(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
	}

	private static String[] lines(String html)
	{
		return html.replace("<html>", "").replace("</html>", "").split("<br>");
	}

	@Test
	public void everyLineFitsTheWidthBudget()
	{
		FontMetrics fm = metrics();
		int width = 160;
		String text = "Recommended items are only suspected best-in-slot. Always do your own "
			+ "research for your account and the content you're doing.";

		for (String line : lines(DisclaimerPanel.wrapToWidth(text, fm, width)))
		{
			assertTrue("line wider than the column: '" + line + "' = " + fm.stringWidth(line),
				fm.stringWidth(line) <= width);
		}
	}

	@Test
	public void noWordsAreLostOrDuplicated()
	{
		FontMetrics fm = metrics();
		String text = "Wiki pages list several options per slot, so now and then a layout "
			+ "includes something that doesn't quite fit.";

		String rebuilt = String.join(" ", lines(DisclaimerPanel.wrapToWidth(text, fm, 140)))
			.replace("&#39;", "'");
		assertEquals(text, rebuilt);
	}

	@Test
	public void htmlSpecialCharactersAreEscaped()
	{
		String html = DisclaimerPanel.wrapToWidth("a < b & c > d", metrics(), 500);
		assertTrue(html, html.contains("&lt;") && html.contains("&amp;") && html.contains("&gt;"));
	}

	@Test
	public void aWordLongerThanTheColumnStillGetsItsOwnLine()
	{
		FontMetrics fm = metrics();
		String[] lines = lines(DisclaimerPanel.wrapToWidth("short Supercalifragilisticexpialidocious end", fm, 60));

		assertEquals(3, lines.length);
		assertEquals("Supercalifragilisticexpialidocious", lines[1]);
	}
}
