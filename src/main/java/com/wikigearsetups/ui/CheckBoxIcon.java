package com.wikigearsetups.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import net.runelite.client.ui.ColorScheme;

/**
 * Checkbox box-and-tick drawn explicitly: the stock dark-on-dark tick is
 * easy to miss, and this toggle changes what Import writes, so its state
 * has to read at a glance.
 */
class CheckBoxIcon implements Icon
{
	private static final int SIZE = 14;

	private final boolean checked;

	private CheckBoxIcon(boolean checked)
	{
		this.checked = checked;
	}

	static void apply(JCheckBox box)
	{
		box.setIcon(new CheckBoxIcon(false));
		box.setSelectedIcon(new CheckBoxIcon(true));
		box.setIconTextGap(6);
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(checked ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		g2.fillRoundRect(x, y, SIZE, SIZE, 4, 4);
		g2.setColor(checked ? Color.WHITE : ColorScheme.DARKER_GRAY_HOVER_COLOR);
		g2.drawRoundRect(x, y, SIZE, SIZE, 4, 4);

		if (checked)
		{
			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g2.drawLine(x + 3, y + 7, x + 6, y + 10);
			g2.drawLine(x + 6, y + 10, x + 11, y + 4);
		}
		g2.dispose();
	}

	@Override
	public int getIconWidth()
	{
		return SIZE;
	}

	@Override
	public int getIconHeight()
	{
		return SIZE;
	}
}
