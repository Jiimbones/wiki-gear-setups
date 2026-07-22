package com.wikigearsetups.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Shown in place of the search UI once per session, and on demand from the
 * footer. In-panel rather than a startup dialog so it can never interrupt
 * play.
 *
 * Everything sits in one scrolling column — logo, title, body, accept,
 * personal note — so the button follows the text instead of being stranded
 * at the bottom of a tall sidebar. Wrapped paragraphs re-render at the
 * panel's real width whenever it changes, since a hard-coded pixel width
 * is what clipped them before.
 */
@Slf4j
class DisclaimerPanel extends JPanel
{
	private static final Color HEADING = ColorScheme.BRAND_ORANGE;
	/** RuneLite reserves 17px for the sidebar scrollbar; leave clearance for it. */
	private static final int SIDE_PADDING = 24;

	private final List<Paragraph> paragraphs = new ArrayList<>();
	private final JPanel content;
	private int renderedWidth = -1;

	private static final class Paragraph
	{
		final JLabel label;
		final String text;

		Paragraph(JLabel label, String text)
		{
			this.label = label;
			this.text = text;
		}
	}

	DisclaimerPanel(Icon banner, Runnable onAccept)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		if (banner != null)
		{
			JLabel bannerLabel = new JLabel(banner);
			bannerLabel.setAlignmentX(LEFT_ALIGNMENT);
			content.add(bannerLabel);
			content.add(Box.createVerticalStrut(14));
		}

		JLabel title = new JLabel("Before you start");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(10));

		addSection(content, "Suspected BIS, not gospel",
			"Recommended items are only suspected best-in-slot. Always do your own research "
				+ "for your account and the content you're doing.");
		addSection(content, "Give each setup a once-over",
			"Wiki pages list several options per slot, so now and then a layout includes "
				+ "something that doesn't quite fit the rest of it — an off-hand next to a "
				+ "two-handed weapon, say.");
		addSection(content, "Your bank stays yours",
			"It's only read to work out what you own. Nothing is sent anywhere.");
		addSection(content, "Found a bug?",
			"Please report any bugs you find to the Git.");

		content.add(Box.createVerticalStrut(6));

		JButton accept = RoundedButton.accent("Got it");
		accept.setFont(accept.getFont().deriveFont(Font.BOLD));
		accept.setAlignmentX(LEFT_ALIGNMENT);
		accept.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		accept.addActionListener(e -> onAccept.run());
		content.add(accept);
		content.add(Box.createVerticalStrut(14));

		JLabel note = paragraph(content, "If you enjoy the plugin, feel free to buy me a coffee "
			+ "(Donate button below) or share it with a friend :)");
		note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		content.add(Box.createVerticalStrut(4));
		JLabel signature = new JLabel("~ Jiimbones");
		signature.setFont(FontManager.getRunescapeFont());
		signature.setForeground(HEADING);
		signature.setAlignmentX(LEFT_ALIGNMENT);
		content.add(signature);

		JScrollPane scroll = new JScrollPane(new ScrollableWrapper(content),
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		add(scroll, BorderLayout.CENTER);

		// listen on the labels' own parent: the outer panel is wider than the
		// column they actually get, by the scrollbar and viewport insets
		content.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				reflow();
			}
		});
	}

	private void addSection(JPanel content, String heading, String body)
	{
		JLabel label = new JLabel(heading);
		label.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		label.setForeground(HEADING);
		label.setAlignmentX(LEFT_ALIGNMENT);
		content.add(label);
		content.add(Box.createVerticalStrut(2));
		paragraph(content, body);
		content.add(Box.createVerticalStrut(10));
	}

	private JLabel paragraph(JPanel content, String text)
	{
		JLabel label = new JLabel();
		label.setFont(FontManager.getRunescapeFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		content.add(label);
		paragraphs.add(new Paragraph(label, text));
		return label;
	}

	/**
	 * Re-wraps every paragraph to the width the text column actually has.
	 * The label's maximum size is pinned to the same value so the layout
	 * cannot hand it more room than the text was wrapped for — that mismatch
	 * is what clipped the last character of every line.
	 */
	private void reflow()
	{
		int width = content.getWidth() - SIDE_PADDING;
		if (width <= 0 || width == renderedWidth)
		{
			return;
		}
		renderedWidth = width;
		log.debug("Disclaimer reflow: panel={} content={} wrapping at {}",
			getWidth(), content.getWidth(), width);
		for (Paragraph paragraph : paragraphs)
		{
			paragraph.label.setText("<html><div style='width:" + width + "px'>"
				+ paragraph.text.replace("<", "&lt;") + "</div></html>");
			paragraph.label.setMaximumSize(new Dimension(width, Integer.MAX_VALUE));
		}
		revalidate();
	}
}
