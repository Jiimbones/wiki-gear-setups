package com.wikigearsetups.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Shown in place of the search UI the first time the panel is opened, and
 * again on demand from the footer. In-panel rather than a startup dialog so
 * it can never interrupt play.
 *
 * Order: logo, title, body, accept, personal note.
 */
class DisclaimerPanel extends JPanel
{
	/** Leaves room for the panel border and the scrollbar so text never clips. */
	private static final int TEXT_WIDTH = PluginPanel.PANEL_WIDTH - 44;

	DisclaimerPanel(Icon banner, Runnable onAccept)
	{
		super(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		if (banner != null)
		{
			JLabel bannerLabel = new JLabel(banner);
			bannerLabel.setAlignmentX(LEFT_ALIGNMENT);
			content.add(bannerLabel);
			content.add(Box.createVerticalStrut(12));
		}

		JLabel title = new JLabel("Before you start");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(8));

		content.add(paragraph("Recommended items are only <b>suspected</b> BIS. Always do your own "
			+ "research for your account and the content you're doing — treat this plugin as a "
			+ "helper, not gospel."));
		content.add(Box.createVerticalStrut(8));
		content.add(paragraph("Wiki pages list several options per slot, so once in a while a "
			+ "layout will include something that doesn't quite fit the rest of it — an off-hand "
			+ "next to a two-handed weapon, say. Give each setup a quick once-over before you "
			+ "rely on it."));
		content.add(Box.createVerticalStrut(8));
		content.add(paragraph("Your bank is only read to work out what you own. "
			+ "Nothing is sent anywhere."));
		content.add(Box.createVerticalStrut(8));
		content.add(paragraph("Please report any bugs you find to the Git."));

		JScrollPane scroll = new JScrollPane(new ScrollableWrapper(content),
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		add(scroll, BorderLayout.CENTER);

		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
		bottom.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton accept = RoundedButton.accent("Got it");
		accept.setFont(accept.getFont().deriveFont(Font.BOLD));
		accept.setAlignmentX(LEFT_ALIGNMENT);
		accept.setMaximumSize(new Dimension(Integer.MAX_VALUE, accept.getPreferredSize().height));
		accept.addActionListener(e -> onAccept.run());
		bottom.add(accept);
		bottom.add(Box.createVerticalStrut(10));

		JLabel note = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px'>"
			+ "If you enjoy the plugin, feel free to buy me a coffee (Donate button below) "
			+ "or share it with a friend :)</div></html>");
		note.setFont(FontManager.getRunescapeSmallFont());
		note.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		note.setAlignmentX(LEFT_ALIGNMENT);
		bottom.add(note);

		JLabel signature = new JLabel("~ Jiimbones");
		signature.setFont(FontManager.getRunescapeSmallFont());
		signature.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		signature.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		signature.setAlignmentX(LEFT_ALIGNMENT);
		bottom.add(signature);

		add(bottom, BorderLayout.SOUTH);
	}

	private static JLabel paragraph(String html)
	{
		JLabel label = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px'>" + html + "</div></html>");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
