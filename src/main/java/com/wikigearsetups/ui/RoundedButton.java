package com.wikigearsetups.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.ColorScheme;

/**
 * Flat dark button with rounded corners and hover/press feedback — Swing's
 * stock button chrome fights the dark sidebar theme.
 */
class RoundedButton extends JButton
{
	private static final int ARC = 10;

	private final Color base;
	private final Color hover;
	private final Color pressed;

	RoundedButton(String text)
	{
		this(text, ColorScheme.DARKER_GRAY_COLOR, ColorScheme.DARKER_GRAY_HOVER_COLOR,
			ColorScheme.DARK_GRAY_COLOR, Color.WHITE);
	}

	/** Accent variant for the primary action. */
	static RoundedButton accent(String text)
	{
		Color base = new Color(0x9A6C1B);
		return new RoundedButton(text, base, base.brighter(), base.darker(), Color.WHITE);
	}

	private RoundedButton(String text, Color base, Color hover, Color pressed, Color foreground)
	{
		super(text);
		this.base = base;
		this.hover = hover;
		this.pressed = pressed;
		setForeground(foreground);
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Color fill = base;
		if (!isEnabled())
		{
			fill = ColorScheme.DARKER_GRAY_COLOR;
		}
		else if (getModel().isPressed())
		{
			fill = pressed;
		}
		else if (getModel().isRollover())
		{
			fill = hover;
		}
		g2.setColor(fill);
		g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
		g2.dispose();
		super.paintComponent(g);
	}
}
