package com.wikigearsetups.layout;

import net.runelite.client.plugins.banktags.tabs.Layout;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LayoutScrollTrimmerTest
{
	@Test
	public void emptyLayoutHasNoRows()
	{
		assertEquals(-1, LayoutScrollTrimmer.lastUsedRow(new Layout("t")));
	}

	@Test
	public void singleItemIsOneRow()
	{
		Layout layout = new Layout("t");
		layout.setItemAtPos(1234, 0);
		assertEquals(0, LayoutScrollTrimmer.lastUsedRow(layout));
	}

	@Test
	public void lastRowIsTheRowOfTheLastItem()
	{
		Layout layout = new Layout("t");
		layout.setItemAtPos(1234, 0);
		layout.setItemAtPos(5678, 81); // row 10 of an 8-wide grid
		assertEquals(10, LayoutScrollTrimmer.lastUsedRow(layout));
	}

	/**
	 * The array can carry -1s past the last item once core appends and removes
	 * entries, so the scan must skip them rather than trust {@code size()}.
	 */
	@Test
	public void trailingEmptySlotsAreIgnored()
	{
		Layout layout = new Layout("t");
		layout.setItemAtPos(1234, 8);
		layout.setItemAtPos(5678, 40);
		layout.removeItemAtPos(40);
		assertEquals(1, LayoutScrollTrimmer.lastUsedRow(layout));
	}

	@Test
	public void rowBoundaryCountsAsTheNextRow()
	{
		Layout layout = new Layout("t");
		layout.setItemAtPos(1234, 7);  // last column of row 0
		assertEquals(0, LayoutScrollTrimmer.lastUsedRow(layout));

		layout.setItemAtPos(5678, 8);  // first column of row 1
		assertEquals(1, LayoutScrollTrimmer.lastUsedRow(layout));
	}
}
