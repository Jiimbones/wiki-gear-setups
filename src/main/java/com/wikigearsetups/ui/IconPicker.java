package com.wikigearsetups.ui;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Tab icon chooser: a button showing the current icon that opens a grid of
 * item sprites. Icons only — names live in tooltips — so a long list of
 * candidates stays something you can scan at a glance rather than scroll
 * through.
 */
class IconPicker extends RoundedButton
{
	private static final int COLUMNS = 6;
	private static final int CELL = 30;
	private static final int MAX_POPUP_HEIGHT = 200;

	private final ItemManager itemManager;
	private final List<IconOption> options = new ArrayList<>();
	private IconOption selected;
	private Runnable onUserChange;

	IconPicker(ItemManager itemManager)
	{
		super("");
		this.itemManager = itemManager;
		setToolTipText("Choose the bank tab icon");
		setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
		addActionListener(e -> showPopup());
	}

	void setOnUserChange(Runnable onUserChange)
	{
		this.onUserChange = onUserChange;
	}

	void setOptions(List<IconOption> newOptions)
	{
		options.clear();
		options.addAll(newOptions);
		if (selected != null && !options.contains(selected))
		{
			selected = null;
		}
		setEnabled(!options.isEmpty());
	}

	IconOption getSelected()
	{
		return selected;
	}

	void setSelected(IconOption option)
	{
		selected = option;
		if (option == null)
		{
			setIcon(null);
			setText("");
			return;
		}
		setText("");
		applyIcon(option.itemId);
		setToolTipText(option.name);
	}

	private void applyIcon(int itemId)
	{
		AsyncBufferedImage img = itemManager.getImage(itemId);
		Runnable apply = () -> setIcon(new ImageIcon(img.getScaledInstance(24, 21, Image.SCALE_SMOOTH)));
		apply.run();
		img.onLoaded(() -> SwingUtilities.invokeLater(apply));
	}

	private void showPopup()
	{
		if (options.isEmpty())
		{
			return;
		}
		JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 2, 2));
		grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		grid.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JPopupMenu popup = new JPopupMenu();
		popup.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR));
		popup.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		for (IconOption option : options)
		{
			grid.add(cell(option, popup));
		}

		int rows = (options.size() + COLUMNS - 1) / COLUMNS;
		int height = Math.min(rows * (CELL + 2) + 8, MAX_POPUP_HEIGHT);
		JScrollPane scroll = new JScrollPane(grid,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		scroll.setPreferredSize(new Dimension(COLUMNS * (CELL + 2) + 16, height));

		popup.add(scroll);
		popup.show(this, 0, getHeight());
	}

	private JLabel cell(IconOption option, JPopupMenu popup)
	{
		JLabel cell = new JLabel();
		cell.setHorizontalAlignment(SwingConstants.CENTER);
		cell.setPreferredSize(new Dimension(CELL, CELL));
		cell.setOpaque(true);
		cell.setToolTipText(option.name);
		boolean isSelected = option.equals(selected);
		cell.setBackground(isSelected ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
		cell.setBorder(isSelected
			? BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE)
			: BorderFactory.createEmptyBorder(1, 1, 1, 1));

		AsyncBufferedImage img = itemManager.getImage(option.itemId);
		Runnable apply = () -> cell.setIcon(new ImageIcon(img.getScaledInstance(24, 21, Image.SCALE_SMOOTH)));
		apply.run();
		img.onLoaded(() -> SwingUtilities.invokeLater(apply));

		cell.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				setSelected(option);
				popup.setVisible(false);
				if (onUserChange != null)
				{
					onUserChange.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				cell.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				cell.setBackground(option.equals(selected)
					? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARK_GRAY_COLOR);
			}
		});
		return cell;
	}
}
