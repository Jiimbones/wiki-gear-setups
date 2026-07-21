package com.wikigearsetups.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import net.runelite.client.ui.ColorScheme;

/**
 * Scroll content that always matches the viewport width and keeps its
 * natural height. Without this a child wider than the sidebar makes the
 * whole column lay out at that width and get clipped on the right, since
 * the horizontal scrollbar is disabled.
 */
class ScrollableWrapper extends JPanel implements Scrollable
{
	ScrollableWrapper(Component content)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(content, BorderLayout.NORTH);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 16;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 64;
	}

	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
}
