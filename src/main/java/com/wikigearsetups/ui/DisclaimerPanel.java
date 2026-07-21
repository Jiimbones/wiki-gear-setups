package com.wikigearsetups.ui;

import java.awt.BorderLayout;
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
 * it can never interrupt what the player is doing.
 */
class DisclaimerPanel extends JPanel
{
	private static final int TEXT_WIDTH = PluginPanel.PANEL_WIDTH - 32;

	DisclaimerPanel(Icon banner, Runnable onReportBug, Runnable onAccept)
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
			content.add(Box.createVerticalStrut(10));
		}

		JLabel title = new JLabel("Before you start");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(java.awt.Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(8));

		content.add(paragraph("Recommended items are only <b>suspected</b> BIS. Setups come from the "
			+ "community-maintained OSRS Wiki and reflect its authors' opinions — do your own "
			+ "research for your account and situation. This plugin is a helper, not gospel."));
		content.add(Box.createVerticalStrut(6));
		content.add(paragraph("Your bank is only read to work out what you own. Nothing is sent anywhere."));
		content.add(Box.createVerticalStrut(6));
		content.add(paragraph("Found a bug, or a page that imports wrong? Please report it — "
			+ "include the page name."));
		content.add(Box.createVerticalStrut(6));

		JButton reportBug = new RoundedButton("Report a bug on GitHub");
		reportBug.setAlignmentX(LEFT_ALIGNMENT);
		reportBug.setMaximumSize(new Dimension(Integer.MAX_VALUE, reportBug.getPreferredSize().height));
		reportBug.addActionListener(e -> onReportBug.run());
		content.add(reportBug);
		content.add(Box.createVerticalStrut(10));

		content.add(paragraph("If you enjoy the plugin, feel free to buy me a coffee (Donate button "
			+ "below) or share it with a friend :)"));
		content.add(Box.createVerticalStrut(4));

		JLabel signature = new JLabel("~ Jiimbones");
		signature.setFont(FontManager.getRunescapeSmallFont());
		signature.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		signature.setAlignmentX(LEFT_ALIGNMENT);
		content.add(signature);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		wrapper.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(wrapper,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		add(scroll, BorderLayout.CENTER);

		JButton accept = RoundedButton.accent("Got it");
		accept.setFont(accept.getFont().deriveFont(Font.BOLD));
		accept.addActionListener(e -> onAccept.run());
		add(accept, BorderLayout.SOUTH);
	}

	private static JLabel paragraph(String html)
	{
		JLabel label = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px'>" + html + "</div></html>");
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
