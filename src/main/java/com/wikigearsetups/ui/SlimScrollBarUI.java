package com.wikigearsetups.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import net.runelite.client.ui.ColorScheme;

/**
 * Slim (8px) flat scrollbar: no arrow buttons, dark track, rounded thumb —
 * the stock scrollbar eats sidebar width and clashes with the dark theme.
 */
class SlimScrollBarUI extends BasicScrollBarUI
{
	private static final int THICKNESS = 8;
	private static final Color THUMB = new Color(0x5A, 0x5A, 0x5A);
	private static final Color THUMB_HOVER = new Color(0x78, 0x78, 0x78);

	static void apply(JScrollBar bar)
	{
		bar.setUI(new SlimScrollBarUI());
		bar.setPreferredSize(bar.getOrientation() == JScrollBar.VERTICAL
			? new Dimension(THICKNESS, 0)
			: new Dimension(0, THICKNESS));
		bar.setUnitIncrement(16);
	}

	@Override
	protected JButton createDecreaseButton(int orientation)
	{
		return zeroButton();
	}

	@Override
	protected JButton createIncreaseButton(int orientation)
	{
		return zeroButton();
	}

	private static JButton zeroButton()
	{
		JButton button = new JButton();
		Dimension zero = new Dimension(0, 0);
		button.setPreferredSize(zero);
		button.setMinimumSize(zero);
		button.setMaximumSize(zero);
		return button;
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
	{
		g.setColor(ColorScheme.DARK_GRAY_COLOR);
		g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
	{
		if (thumbBounds.isEmpty() || !scrollbar.isEnabled())
		{
			return;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(isThumbRollover() ? THUMB_HOVER : THUMB);
		g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1,
			thumbBounds.width - 2, thumbBounds.height - 2, THICKNESS, THICKNESS);
		g2.dispose();
	}
}
