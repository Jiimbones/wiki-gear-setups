package com.wikigearsetups.ui;

import com.wikigearsetups.WikiGearSetupsConfig;
import com.wikigearsetups.bank.BankStateCache;
import com.wikigearsetups.bank.ExampleGearEnricher;
import com.wikigearsetups.bank.InventoryPick;
import com.wikigearsetups.bank.InventoryResolution;
import com.wikigearsetups.bank.InventoryResolver;
import com.wikigearsetups.bank.OwnershipResolver;
import com.wikigearsetups.bank.PickStrategy;
import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.items.ItemNameIndex;
import com.wikigearsetups.items.ItemNames;
import com.wikigearsetups.layout.BankTagImporter;
import com.wikigearsetups.layout.LayoutBuilder;
import com.wikigearsetups.layout.TagNames;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.DropsParser;
import com.wikigearsetups.wiki.ExampleSetup;
import com.wikigearsetups.wiki.ExampleSetupParser;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.GearSetupParser;
import com.wikigearsetups.wiki.GearSetups;
import com.wikigearsetups.wiki.InventoryParser;
import com.wikigearsetups.wiki.InventorySetup;
import com.wikigearsetups.wiki.PageDiscovery;
import com.wikigearsetups.wiki.PageKind;
import com.wikigearsetups.wiki.SearchResult;
import com.wikigearsetups.wiki.SetupPairing;
import com.wikigearsetups.wiki.SourceScorer;
import com.wikigearsetups.wiki.WikiClient;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

/**
 * Search → pick page → pick setup (or "All styles") → optional inventory →
 * grid + text preview → import. Network work runs on OkHttp threads,
 * game-state reads on the client thread, and every UI mutation hops back to
 * the EDT.
 */
@Slf4j
@Singleton
public class WikiGearSetupsPanel extends PluginPanel
{
	private static final Color OWNED = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color SUBSTITUTE = ColorScheme.PROGRESS_INPROGRESS_COLOR;
	private static final Color MISSING = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final String NO_INVENTORY = "No inventory";
	private static final int GRID_COLS = 8;
	private static final int CELL_W = 26;
	private static final int CELL_H = 24;
	private static final String DEVELOPER_NAME = "Jiimbones";
	private static final String CARD_MAIN = "main";
	private static final String CARD_DISCLAIMER = "disclaimer";
	private static final String DONATION_URL = "https://www.paypal.com/ncp/payment/FQTFP8N8H7P2W";
	private static final String BUG_REPORT_URL = "https://github.com/Jiimbones/wiki-gear-setups/issues/new";

	private final WikiClient wikiClient;
	private final GearSetupParser parser = new GearSetupParser();
	private final InventoryParser inventoryParser = new InventoryParser();
	private final ExampleSetupParser exampleSetupParser = new ExampleSetupParser();
	private final DropsParser dropsParser = new DropsParser();
	private final OwnershipResolver ownershipResolver;
	private final InventoryResolver inventoryResolver;
	private final BankStateCache bankStateCache;
	private final BankTagImporter importer;
	private final ItemNameIndex itemNameIndex;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final WikiGearSetupsConfig config;
	private final CardLayout cards = new CardLayout();
	private ImageIcon bannerIcon;
	private boolean bannerLoaded;

	private final FlatTextField searchField = new FlatTextField();
	private final JLabel statusLabel = new JLabel(" ");
	private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private final JList<String> resultsList = new JList<>(resultsModel);
	private final JButton wikiLinkButton = new RoundedButton("Open wiki page");
	private final JComboBox<String> sourceCombo = new JComboBox<>();
	private final JComboBox<String> modeCombo = new JComboBox<>();
	private final JComboBox<String> tierCombo = new JComboBox<>();
	private final JComboBox<String> setupCombo = new JComboBox<>();
	private final JComboBox<String> inventoryCombo = new JComboBox<>();
	private final JCheckBox slayerToggle = new JCheckBox("On this task");
	private final JCheckBox recommendedToggle = new JCheckBox("Show wiki BIS (ignore my gear)");
	private final JPanel gridPanel = new JPanel();
	private final JPanel previewPanel = new JPanel();
	private final FlatTextField tagNameField = new FlatTextField();
	private final IconPicker iconPicker;
	private final JButton importButton = RoundedButton.accent("Import as bank tag");

	private List<SearchResult> searchResults = Collections.emptyList();
	private String currentPageTitle;
	// everything the loaded page offers; the source selector picks which view
	private List<GearSetup> pageSetups = Collections.emptyList();
	// single-item annotation setups filtered out of the dropdown; their owned
	// items land below the layout as extras
	private List<GearSetup> pageSwitches = Collections.emptyList();
	private List<InventorySetup> pageInventories = Collections.emptyList();
	private List<ExampleSetup> pageExamples = Collections.emptyList();
	// boss uniques/pet offered as tab icons; empty until the drop table is seen
	private List<String> pageDropNames = Collections.emptyList();
	private Set<String> petNames = Collections.emptySet();
	private List<GearSetup> currentSetups;
	// non-empty = example mode: complete gear+inventory columns from the page,
	// chosen through the mode/tier/label cascade instead of the classic combos
	private List<ExampleSetup> currentExamples = Collections.emptyList();
	private List<InventorySetup> currentInventories = Collections.emptyList();
	private List<SetupResolution> currentResolutions;
	private InventoryResolution currentInventoryResolution;
	private List<Integer> currentExtras = Collections.emptyList();
	/** Inventory index per setup index from the page's order, or empty. */
	private int[] inventoryPairing = SetupPairing.NONE;
	private boolean updatingCombos;
	private boolean userPickedInventory;
	private boolean userEditedTag;
	private boolean userEditedIcon;

	@Inject
	public WikiGearSetupsPanel(WikiClient wikiClient, OwnershipResolver ownershipResolver,
		InventoryResolver inventoryResolver, BankStateCache bankStateCache,
		BankTagImporter importer, ItemNameIndex itemNameIndex, ItemManager itemManager,
		ClientThread clientThread, WikiGearSetupsConfig config)
	{
		super(false);
		this.iconPicker = new IconPicker(itemManager);
		this.wikiClient = wikiClient;
		this.ownershipResolver = ownershipResolver;
		this.inventoryResolver = inventoryResolver;
		this.bankStateCache = bankStateCache;
		this.importer = importer;
		this.itemNameIndex = itemNameIndex;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.config = config;

		setLayout(cards);
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// the whole search/preview/import UI lives on one card so the
		// first-run disclaimer can take the panel over without disturbing it
		JPanel main = new JPanel(new BorderLayout(0, 8));
		main.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// header: logo, search, status — always visible
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setOpaque(false);
		if (bannerIcon() != null)
		{
			JLabel banner = new JLabel(bannerIcon());
			banner.setAlignmentX(LEFT_ALIGNMENT);
			north.add(banner);
			north.add(Box.createVerticalStrut(8));
		}
		JPanel searchRow = new JPanel(new BorderLayout(4, 0));
		searchRow.setOpaque(false);
		searchRow.setAlignmentX(LEFT_ALIGNMENT);
		JButton searchButton = new RoundedButton("Search");
		styleFlatField(searchField);
		searchField.setPreferredSize(new Dimension(0, 30));
		searchField.addActionListener(e -> doSearch());
		searchButton.addActionListener(e -> doSearch());
		JButton clearButton = new RoundedButton("✕");
		clearButton.setToolTipText("Clear search");
		clearButton.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		clearButton.addActionListener(e ->
		{
			searchField.setText("");
			searchField.requestFocusInWindow();
		});
		JPanel searchButtons = new JPanel();
		searchButtons.setOpaque(false);
		searchButtons.setLayout(new BoxLayout(searchButtons, BoxLayout.X_AXIS));
		searchButtons.add(clearButton);
		searchButtons.add(Box.createHorizontalStrut(4));
		searchButtons.add(searchButton);
		searchRow.add(searchField, BorderLayout.CENTER);
		searchRow.add(searchButtons, BorderLayout.EAST);
		searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
		north.add(searchRow);
		north.add(Box.createVerticalStrut(4));
		statusLabel.setAlignmentX(LEFT_ALIGNMENT);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		north.add(statusLabel);
		main.add(north, BorderLayout.NORTH);

		// middle: everything else scrolls when the client window is short
		JPanel flow = new JPanel();
		flow.setLayout(new BoxLayout(flow, BoxLayout.Y_AXIS));
		flow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsList.setVisibleRowCount(6);
		resultsList.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		// explicit renderer — the client LAF's list colors are dark-on-dark here
		resultsList.setCellRenderer(new DarkListRenderer());
		resultsList.addListSelectionListener(e ->
		{
			int idx = resultsList.getSelectedIndex();
			if (!e.getValueIsAdjusting() && idx >= 0 && idx < searchResults.size())
			{
				loadPage(searchResults.get(idx).getTitle());
			}
		});
		JScrollPane resultsScroll = new JScrollPane(resultsList,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		resultsScroll.setAlignmentX(LEFT_ALIGNMENT);
		resultsScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR));
		SlimScrollBarUI.apply(resultsScroll.getVerticalScrollBar());
		flow.add(resultsScroll);
		flow.add(Box.createVerticalStrut(6));

		wikiLinkButton.setAlignmentX(LEFT_ALIGNMENT);
		wikiLinkButton.setVisible(false);
		wikiLinkButton.addActionListener(e -> openWikiPage());
		flow.add(wikiLinkButton);
		flow.add(Box.createVerticalStrut(6));

		sourceCombo.setAlignmentX(LEFT_ALIGNMENT);
		sourceCombo.setToolTipText("Complete wiki example loadouts, or gear built from the ranked recommendations");
		styleCombo(sourceCombo);
		sourceCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedTag = false;
				userEditedIcon = false;
				userPickedInventory = false;
				applySource();
			}
		});
		sourceCombo.setVisible(false);
		flow.add(sourceCombo);
		flow.add(Box.createVerticalStrut(4));

		modeCombo.setAlignmentX(LEFT_ALIGNMENT);
		modeCombo.setToolTipText("Mode section on the wiki page");
		styleCombo(modeCombo);
		modeCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedTag = false;
				userEditedIcon = false;
				updatingCombos = true;
				rebuildExampleTiers();
				rebuildExampleLabels();
				updatingCombos = false;
				resolveSelection();
			}
		});
		modeCombo.setVisible(false);
		flow.add(modeCombo);
		flow.add(Box.createVerticalStrut(4));

		tierCombo.setAlignmentX(LEFT_ALIGNMENT);
		tierCombo.setToolTipText("Tier within the selected mode");
		styleCombo(tierCombo);
		tierCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedTag = false;
				userEditedIcon = false;
				updatingCombos = true;
				rebuildExampleLabels();
				updatingCombos = false;
				resolveSelection();
			}
		});
		tierCombo.setVisible(false);
		flow.add(tierCombo);
		flow.add(Box.createVerticalStrut(4));

		setupCombo.setAlignmentX(LEFT_ALIGNMENT);
		setupCombo.setToolTipText("Gear style from the wiki page");
		styleCombo(setupCombo);
		setupCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedTag = false;
				userEditedIcon = false;
				updatingCombos = true;
				autoSelectInventory();
				updatingCombos = false;
				resolveSelection();
			}
		});
		setupCombo.setVisible(false);
		flow.add(setupCombo);
		flow.add(Box.createVerticalStrut(4));

		inventoryCombo.setAlignmentX(LEFT_ALIGNMENT);
		inventoryCombo.setToolTipText("Inventory table to include on the right half");
		styleCombo(inventoryCombo);
		inventoryCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userPickedInventory = true;
				userEditedTag = false;
				resolveSelection();
			}
		});
		inventoryCombo.setVisible(false);
		flow.add(inventoryCombo);
		flow.add(Box.createVerticalStrut(8));

		// only shown when the page actually has a task-conditional
		// recommendation, so it never appears as dead weight
		slayerToggle.setAlignmentX(LEFT_ALIGNMENT);
		slayerToggle.setOpaque(false);
		slayerToggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		slayerToggle.setFocusPainted(false);
		// tooltip is set per page from the wiki's own note wording
		slayerToggle.setToolTipText("Include recommendations that only apply while on this task");
		CheckBoxIcon.apply(slayerToggle);
		slayerToggle.addActionListener(e ->
		{
			userEditedTag = false;
			userEditedIcon = false;
			resolveSelection();
		});
		slayerToggle.setVisible(false);
		flow.add(slayerToggle);
		flow.add(Box.createVerticalStrut(4));

		recommendedToggle.setAlignmentX(LEFT_ALIGNMENT);
		recommendedToggle.setOpaque(false);
		recommendedToggle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		recommendedToggle.setFocusPainted(false);
		CheckBoxIcon.apply(recommendedToggle);
		recommendedToggle.setToolTipText("Show the wiki's own first choice for every slot, "
			+ "marking what you own. Also changes what Import writes.");
		// installation default is adaptive (off); the config can flip that
		recommendedToggle.setSelected(config.defaultToWikiBis());
		recommendedToggle.addActionListener(e ->
		{
			userEditedTag = false;
			userEditedIcon = false;
			updateImportLabel();
			resolveSelection();
		});
		recommendedToggle.setVisible(false);
		flow.add(recommendedToggle);
		flow.add(Box.createVerticalStrut(4));

		gridPanel.setAlignmentX(LEFT_ALIGNMENT);
		gridPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		gridPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR));
		flow.add(gridPanel);
		flow.add(Box.createVerticalStrut(8));

		// import controls sit directly under the grid; the detail list follows
		tagNameField.getDocument().addDocumentListener(userEditListener(
			() -> tagNameField.getTextField().hasFocus(), () -> userEditedTag = true));
		styleFlatField(tagNameField);
		tagNameField.setPreferredSize(new Dimension(0, 26));
		flow.add(labeledRow("Name", tagNameField));
		flow.add(Box.createVerticalStrut(4));
		iconPicker.setOnUserChange(() -> userEditedIcon = true);
		flow.add(labeledRow("Icon", iconPicker));
		flow.add(Box.createVerticalStrut(8));
		importButton.setEnabled(false);
		importButton.setToolTipText("Creates or updates a bank tag tab with this layout");
		importButton.setAlignmentX(LEFT_ALIGNMENT);
		importButton.setFont(importButton.getFont().deriveFont(Font.BOLD));
		importButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
		importButton.addActionListener(e -> doImport());
		flow.add(importButton);
		flow.add(Box.createVerticalStrut(8));

		previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.setOpaque(false);
		previewPanel.setAlignmentX(LEFT_ALIGNMENT);
		flow.add(previewPanel);

		// Scrollable wrapper: tracks the viewport width so nothing can push
		// past the panel edge, while keeping natural height for scrolling
		JScrollPane scroll = new JScrollPane(new ScrollableWrapper(flow),
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		main.add(scroll, BorderLayout.CENTER);

		// footer pinned to the very bottom regardless of content height
		main.add(footer(), BorderLayout.SOUTH);

		add(main, CARD_MAIN);
		add(new DisclaimerPanel(bannerIcon(), this::acceptDisclaimer), CARD_DISCLAIMER);
		// shown once per session: the panel is built when the plugin starts,
		// so a fresh client always opens on it
		cards.show(this, CARD_DISCLAIMER);
	}

	private void acceptDisclaimer()
	{
		cards.show(this, CARD_MAIN);
	}

	private void showDisclaimer()
	{
		cards.show(this, CARD_DISCLAIMER);
	}

	/** The LAF paints field text dark — force light text on the inner field. */
	private static void styleFlatField(FlatTextField field)
	{
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		field.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		field.getTextField().setForeground(Color.WHITE);
		field.getTextField().setCaretColor(Color.WHITE);
		field.getTextField().setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
	}

	/**
	 * Caps the width so long labels can't push the combo past the panel edge
	 * (the popup follows the combo width; labels ellipsize) and renders
	 * entries with explicit light-on-dark colors.
	 */
	private static void styleCombo(JComboBox<?> combo)
	{
		combo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		combo.setForeground(Color.WHITE);
		int height = Math.max(26, combo.getPreferredSize().height);
		combo.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 24, height));
		combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		if (!(combo.getRenderer() instanceof DarkListRenderer))
		{
			combo.setRenderer(new DarkListRenderer());
		}
	}

	/** Explicit light-on-dark cell colors for lists and combo popups. */
	private static class DarkListRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setOpaque(true);
			label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			label.setForeground(isSelected ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
			label.setBackground(isSelected ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			return label;
		}
	}

	/**
	 * Plugin logo from resources/com/wikigearsetups/banner.png, scaled once
	 * and shared by every card. Null when the resource is absent.
	 */
	private ImageIcon bannerIcon()
	{
		if (!bannerLoaded)
		{
			bannerLoaded = true;
			try
			{
				BufferedImage img = ImageUtil.loadImageResource(WikiGearSetupsPanel.class,
					"/com/wikigearsetups/banner.png");
				int width = PluginPanel.PANEL_WIDTH - 16;
				int height = Math.max(1, img.getHeight() * width / img.getWidth());
				bannerIcon = new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
			}
			catch (RuntimeException e)
			{
				log.debug("No banner resource", e);
			}
		}
		return bannerIcon;
	}

	/** Full-width stacked buttons: side by side they crowded and overlapped. */
	private JPanel footer()
	{
		JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
		footer.setOpaque(false);
		footer.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARKER_GRAY_HOVER_COLOR),
			BorderFactory.createEmptyBorder(8, 0, 0, 0)));

		JButton bugReport = new RoundedButton("Report a bug");
		bugReport.setToolTipText("Open a GitHub issue — include the page name and a screenshot");
		bugReport.addActionListener(e -> LinkBrowser.browse(bugReportUrl()));
		footer.add(fullWidth(bugReport));
		footer.add(Box.createVerticalStrut(5));

		JButton donate = new RoundedButton("Donate ♥");
		donate.addActionListener(e -> LinkBrowser.browse(DONATION_URL));
		footer.add(fullWidth(donate));
		footer.add(Box.createVerticalStrut(7));

		// also the permanent way back to the disclaimer
		JLabel dev = new JLabel("Developed by " + DEVELOPER_NAME, SwingConstants.CENTER);
		dev.setFont(FontManager.getRunescapeFont());
		dev.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dev.setToolTipText("About / disclaimer");
		dev.setAlignmentX(LEFT_ALIGNMENT);
		dev.setMaximumSize(new Dimension(Integer.MAX_VALUE, dev.getPreferredSize().height));
		dev.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		dev.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				showDisclaimer();
			}

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e)
			{
				dev.setForeground(Color.WHITE);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e)
			{
				dev.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});
		footer.add(dev);
		return footer;
	}

	private static JButton fullWidth(JButton button)
	{
		button.setAlignmentX(LEFT_ALIGNMENT);
		button.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		return button;
	}

	/** Prefills the issue with the page being viewed so reports are actionable. */
	private String bugReportUrl()
	{
		if (currentPageTitle == null)
		{
			return BUG_REPORT_URL;
		}
		try
		{
			return BUG_REPORT_URL + "?title=" + URLEncoder.encode("[" + currentPageTitle + "] ", "UTF-8")
				+ "&body=" + URLEncoder.encode("Wiki page: " + currentPageTitle
					+ "\nWhat happened:\n\nWhat I expected:\n", "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return BUG_REPORT_URL;
		}
	}

	private void doSearch()
	{
		String query = searchField.getText().trim();
		if (query.isEmpty())
		{
			return;
		}
		status("Searching…", null);
		wikiClient.search(query).whenComplete((results, err) ->
			SwingUtilities.invokeLater(() ->
			{
				if (err != null)
				{
					status("Search failed: " + rootMessage(err), MISSING);
					return;
				}
				searchResults = results;
				resultsModel.clear();
				for (SearchResult r : results)
				{
					resultsModel.addElement(resultLabel(r.getTitle()));
				}
				clearSetups();
				status(results.isEmpty() ? "No pages with gear setups found" : results.size() + " results", null);
			}));
	}

	/**
	 * A search result's line: the base page name, its kind when it has a
	 * telling one, and — for a slayer task — just the monster ("Slayer
	 * task/Abyssal demons" reads as "Abyssal demons · Slayer task").
	 */
	static String resultLabel(String title)
	{
		PageKind kind = PageKind.of(title);
		String base = displayTitle(title);
		if (kind == PageKind.SLAYER_TASK && base.startsWith("Slayer task/"))
		{
			base = base.substring("Slayer task/".length());
		}
		return kind.getLabel() == null ? base : base + "  ·  " + kind.getLabel();
	}

	/** "Zulrah/Strategies" and "Guide:Theatre of Blood setups" read as the base name. */
	private static String displayTitle(String title)
	{
		String t = title;
		if (t.startsWith("Guide:"))
		{
			t = t.substring("Guide:".length());
		}
		if (t.endsWith(" setups"))
		{
			t = t.substring(0, t.length() - " setups".length());
		}
		if (t.endsWith("/Strategies"))
		{
			t = t.substring(0, t.length() - "/Strategies".length());
		}
		return t;
	}

	private void openWikiPage()
	{
		if (currentPageTitle == null)
		{
			return;
		}
		try
		{
			LinkBrowser.browse("https://oldschool.runescape.wiki/w/"
				+ URLEncoder.encode(currentPageTitle.replace(' ', '_'), "UTF-8"));
		}
		catch (UnsupportedEncodingException e)
		{
			// UTF-8 always exists
		}
	}

	private void loadPage(String title)
	{
		status("Loading " + displayTitle(title) + "…", null);
		tryCandidates(PageDiscovery.candidatesFor(title), 0);
	}

	private void tryCandidates(List<String> candidates, int index)
	{
		if (index >= candidates.size())
		{
			SwingUtilities.invokeLater(() ->
			{
				clearSetups();
				status("No gear setups found on that page", MISSING);
			});
			return;
		}

		String candidate = candidates.get(index);
		wikiClient.getPageWikitext(candidate).whenComplete((wikitext, err) ->
		{
			if (err != null)
			{
				tryCandidates(candidates, index + 1);
				return;
			}
			// complete example loadouts (raid pages) take precedence over the
			// ranked recommendation tables
			List<ExampleSetup> examples = exampleSetupParser.parse(wikitext);
			List<GearSetup> setups = parser.parse(wikitext);
			if (examples.isEmpty() && setups.isEmpty())
			{
				tryCandidates(candidates, index + 1);
				return;
			}
			// example columns own their inventories; they shouldn't also appear
			// as standalone choices for the ranked setups
			List<InventorySetup> inventories = ExampleSetupParser.withoutExampleInventories(
				inventoryParser.parse(wikitext), examples);
			// which inventory belongs to which setup, from their page order
			int[] pairing = SetupPairing.pair(wikitext, setups.size(), inventories.size());
			List<String> drops = dropsParser.parse(wikitext);
			SwingUtilities.invokeLater(() ->
			{
				showSetups(candidate, setups, inventories, examples, pairing);
				applyDrops(candidate, drops);
			});
			if (drops.isEmpty())
			{
				loadDropsFromBasePage(candidate);
			}
		});
	}

	/**
	 * Setups usually live on a "/Strategies" subpage while the drop table
	 * stays on the boss's own page, so fall back to it for icon candidates.
	 */
	private void loadDropsFromBasePage(String loadedTitle)
	{
		String base = basePageTitle(loadedTitle);
		if (base.equals(loadedTitle))
		{
			return;
		}
		wikiClient.getPageWikitext(base).whenComplete((wikitext, err) ->
		{
			if (err != null)
			{
				return;
			}
			List<String> drops = dropsParser.parse(wikitext);
			if (!drops.isEmpty())
			{
				SwingUtilities.invokeLater(() -> applyDrops(loadedTitle, drops));
			}
		});
	}

	static String basePageTitle(String title)
	{
		String base = title;
		if (base.startsWith("Guide:"))
		{
			base = base.substring("Guide:".length());
		}
		if (base.endsWith(" setups"))
		{
			base = base.substring(0, base.length() - " setups".length());
		}
		int slash = base.indexOf('/');
		return slash > 0 ? base.substring(0, slash) : base;
	}

	/** Late-arriving drop data refreshes the icon choices, if the page is still shown. */
	private void applyDrops(String forPage, List<String> drops)
	{
		if (!forPage.equals(currentPageTitle) || drops.isEmpty())
		{
			return;
		}
		pageDropNames = drops;
		if (currentResolutions != null)
		{
			refreshTagAndIcon(currentResolutions);
		}
		wikiClient.petNames().whenComplete((pets, err) -> SwingUtilities.invokeLater(() ->
		{
			if (err != null || pets.isEmpty() || !forPage.equals(currentPageTitle))
			{
				return;
			}
			petNames = pets;
			if (currentResolutions != null)
			{
				refreshTagAndIcon(currentResolutions);
			}
		}));
	}

	private void showSetups(String pageTitle, List<GearSetup> setups, List<InventorySetup> inventories,
		List<ExampleSetup> examples, int[] pairing)
	{
		currentPageTitle = pageTitle;
		// single-item "for this room" annotations aren't loadouts; keep them out
		// of the dropdown and let their owned items fall below the layout as
		// extras (see decision #12 — they ride the owned-only channel, so BIS
		// never shows them). Score and pair on the FULL list first, then remap.
		GearSetups.Partition split = GearSetups.partition(setups);
		pageSetups = disambiguate(split.getLoadouts());
		pageSwitches = split.getSwitches();
		inventoryPairing = split.remapPairing(pairing);
		pageInventories = inventories;
		pageExamples = examples;
		pageDropNames = Collections.emptyList();
		userPickedInventory = false;
		userEditedTag = false;
		userEditedIcon = false;
		wikiLinkButton.setVisible(true);
		recommendedToggle.setVisible(true);
		updateImportLabel();

		// both formats present: default to whichever is more complete on this
		// page, and say so when the margin is worth mentioning
		updatingCombos = true;
		SourceScorer.Result score = SourceScorer.score(examples, setups, pageInventories);
		String note = score.isDecisive() ? "  (more complete)" : "";
		DefaultComboBoxModel<String> sourceModel = new DefaultComboBoxModel<>();
		sourceModel.addElement("Example setups" + (score.isPreferExamples() ? note : ""));
		sourceModel.addElement("Gear by style" + (score.isPreferExamples() ? "" : note));
		sourceCombo.setModel(sourceModel);
		sourceCombo.setSelectedIndex(score.isPreferExamples() ? 0 : 1);
		sourceCombo.setVisible(!examples.isEmpty() && !setups.isEmpty());
		updatingCombos = false;

		applySource();
	}

	private void applySource()
	{
		boolean exampleMode = !pageExamples.isEmpty()
			&& (!sourceCombo.isVisible() || sourceCombo.getSelectedIndex() == 0);

		if (exampleMode)
		{
			// example mode: complete gear+inventory columns, no separate
			// inventory choice — mode/tier combos appear only when the page
			// actually has those levels (ToB guide), otherwise it's a single
			// dropdown (CoX/ToA)
			currentExamples = pageExamples;
			currentSetups = null;
			currentInventories = Collections.emptyList();

			updatingCombos = true;
			List<String> modes = distinctLevel(pageExamples, 0);
			modeCombo.setModel(toModel(modes));
			modeCombo.setVisible(modes.size() > 1);
			rebuildExampleTiers();
			rebuildExampleLabels();
			inventoryCombo.setVisible(false);
			updatingCombos = false;

			status(pageExamples.size() + " example setups on " + displayTitle(currentPageTitle), null);
			resolveSelection();
			return;
		}

		currentExamples = Collections.emptyList();
		modeCombo.setVisible(false);
		tierCombo.setVisible(false);
		currentSetups = pageSetups;
		currentInventories = pageInventories;

		updatingCombos = true;
		DefaultComboBoxModel<String> setupModel = new DefaultComboBoxModel<>();
		for (GearSetup setup : pageSetups)
		{
			setupModel.addElement(setup.getStyleLabel());
		}
		if (pageSetups.size() > 1)
		{
			// name the real cost — combining N styles stacks N gear blocks
			setupModel.addElement("All " + pageSetups.size() + " styles · ~"
				+ LayoutBuilder.estimatedRows(pageSetups.size()) + " rows");
		}
		setupCombo.setModel(setupModel);
		setupCombo.setVisible(true);

		DefaultComboBoxModel<String> invModel = new DefaultComboBoxModel<>();
		invModel.addElement(NO_INVENTORY);
		List<String> invLabels = new ArrayList<>(pageInventories.size());
		for (InventorySetup inventory : pageInventories)
		{
			invLabels.add(inventory.getLabel());
		}
		for (String label : uniquify(invLabels))
		{
			invModel.addElement("Inv: " + label);
		}
		inventoryCombo.setModel(invModel);
		inventoryCombo.setVisible(!pageInventories.isEmpty());
		autoSelectInventory();
		updatingCombos = false;

		status(pageSetups.size() + (pageSetups.size() == 1 ? " setup on " : " setups on ")
			+ displayTitle(currentPageTitle), null);
		resolveSelection();
	}

	/** The button must say what it will actually write — the toggle changes that. */
	private void updateImportLabel()
	{
		importButton.setText(recommendedToggle.isSelected()
			? "Import BIS as bank tag" : "Import as bank tag");
	}

	private static DefaultComboBoxModel<String> toModel(List<String> values)
	{
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		for (String value : values)
		{
			model.addElement(value);
		}
		return model;
	}

	private static String pathLevel(ExampleSetup example, int level)
	{
		return level < example.getPath().size() ? example.getPath().get(level) : "";
	}

	private static List<String> distinctLevel(List<ExampleSetup> examples, int level)
	{
		List<String> values = new ArrayList<>();
		for (ExampleSetup example : examples)
		{
			String value = pathLevel(example, level);
			if (!values.contains(value))
			{
				values.add(value);
			}
		}
		return values;
	}

	/** Examples matching the visible mode/tier selections. */
	private List<ExampleSetup> filteredExamples()
	{
		List<ExampleSetup> filtered = new ArrayList<>();
		Object mode = modeCombo.isVisible() ? modeCombo.getSelectedItem() : null;
		Object tier = tierCombo.isVisible() ? tierCombo.getSelectedItem() : null;
		for (ExampleSetup example : currentExamples)
		{
			if (mode != null && !mode.equals(pathLevel(example, 0)))
			{
				continue;
			}
			if (tier != null && !tier.equals(pathLevel(example, 1)))
			{
				continue;
			}
			filtered.add(example);
		}
		return filtered;
	}

	private void rebuildExampleTiers()
	{
		List<ExampleSetup> modeFiltered = new ArrayList<>();
		Object mode = modeCombo.isVisible() ? modeCombo.getSelectedItem() : null;
		for (ExampleSetup example : currentExamples)
		{
			if (mode == null || mode.equals(pathLevel(example, 0)))
			{
				modeFiltered.add(example);
			}
		}
		List<String> tiers = distinctLevel(modeFiltered, 1);
		tierCombo.setModel(toModel(tiers));
		tierCombo.setVisible(tiers.size() > 1);
	}

	private void rebuildExampleLabels()
	{
		List<String> labels = new ArrayList<>();
		for (ExampleSetup example : filteredExamples())
		{
			labels.add(example.getLabel());
		}
		setupCombo.setModel(toModel(labels));
		setupCombo.setVisible(!labels.isEmpty());
	}

	private ExampleSetup selectedExample()
	{
		List<ExampleSetup> filtered = filteredExamples();
		int idx = setupCombo.getSelectedIndex();
		return idx >= 0 && idx < filtered.size() ? filtered.get(idx) : null;
	}

	/**
	 * A lone inventory is obviously the right one — select it. With several,
	 * prefer the one the page's own order pairs to this setup, since half the
	 * pages label every inventory "Inventory" and the labels can't tell them
	 * apart. Fall back to label matching, then leave the choice alone.
	 */
	private void autoSelectInventory()
	{
		if (userPickedInventory || currentInventories.isEmpty())
		{
			return;
		}
		if (currentInventories.size() == 1)
		{
			inventoryCombo.setSelectedIndex(1);
			return;
		}
		int setupIdx = setupCombo.getSelectedIndex();
		if (currentSetups == null || setupIdx < 0 || setupIdx >= currentSetups.size())
		{
			return;
		}

		if (setupIdx < inventoryPairing.length)
		{
			int paired = inventoryPairing[setupIdx];
			if (paired >= 0 && paired < currentInventories.size())
			{
				inventoryCombo.setSelectedIndex(paired + 1);
				return;
			}
		}

		List<String> labels = new ArrayList<>();
		for (InventorySetup inventory : currentInventories)
		{
			labels.add(inventory.getLabel());
		}
		int match = InventoryMatcher.bestMatch(currentSetups.get(setupIdx).getStyleLabel(), labels);
		if (match >= 0)
		{
			inventoryCombo.setSelectedIndex(match + 1);
		}
	}

	private static final Pattern AUTO_STYLE = Pattern.compile("Setup \\d+");

	/**
	 * The checkbox tooltip, built from the wiki's own note wording so a
	 * monster-specific condition ("Only on Fossil Island wyvern task") is
	 * shown verbatim rather than flattened to "on a slayer task".
	 */
	private static String slayerTooltip(List<String> notes)
	{
		if (notes.isEmpty())
		{
			return "Include recommendations that only apply while on this task";
		}
		StringBuilder html = new StringBuilder("<html>Tick to include items the wiki lists "
			+ "only for this task:<br>");
		for (String note : notes)
		{
			html.append("• ").append(escapeHtml(note)).append("<br>");
		}
		return html.append("</html>").toString();
	}

	/**
	 * Chooses a distinct, meaningful display label for every setup.
	 *
	 * The template's own {@code |style=} is used when it's present and unique.
	 * When it's missing (the parser fell back to "Setup 1") or duplicated
	 * ("Melee" four times on Gemstone Crab), the tabber pane the template sat
	 * under is the better distinguisher ("Dharok's", "Obsidian", "Blood
	 * moon") — so it's used instead when one exists. Whatever remains is then
	 * numbered, so no two entries are ever identical: identical entries are
	 * unpickable and slug to the same tag, silently overwriting each other.
	 */
	static List<GearSetup> disambiguate(List<GearSetup> setups)
	{
		Map<String, Integer> styleCount = new HashMap<>();
		for (GearSetup setup : setups)
		{
			styleCount.merge(setup.getStyleLabel(), 1, Integer::sum);
		}

		List<String> labels = new ArrayList<>(setups.size());
		for (GearSetup setup : setups)
		{
			String style = setup.getStyleLabel();
			boolean weak = styleCount.get(style) > 1 || AUTO_STYLE.matcher(style).matches();
			String pane = setup.getPaneLabel();
			labels.add(weak && pane != null && !pane.isEmpty() ? pane : style);
		}
		labels = uniquify(labels);

		List<GearSetup> out = new ArrayList<>(setups.size());
		for (int i = 0; i < setups.size(); i++)
		{
			GearSetup setup = setups.get(i);
			out.add(labels.get(i).equals(setup.getStyleLabel())
				? setup
				: new GearSetup(labels.get(i), setup.getSlots(), setup.getPaneLabel()));
		}
		return out;
	}

	/**
	 * Numbers repeats so every entry in a dropdown is distinct. Pages do give
	 * two things the same name — Corporeal Beast lists "Best in slot" twice —
	 * and an entry you can't tell apart from its neighbour is one you can't
	 * deliberately choose.
	 */
	static List<String> uniquify(List<String> labels)
	{
		Map<String, Integer> seen = new HashMap<>();
		List<String> out = new ArrayList<>(labels.size());
		for (String label : labels)
		{
			int count = seen.merge(label, 1, Integer::sum);
			out.add(count == 1 ? label : label + " (" + count + ")");
		}
		return out;
	}

	private void resolveSelection()
	{
		List<GearSetup> selectedSetups;
		InventorySetup selectedInventory;
		if (!currentExamples.isEmpty())
		{
			ExampleSetup example = selectedExample();
			if (example == null)
			{
				return;
			}
			// unowned example items fall back through the page's ranked
			// ladders so the tab still adapts to the player's gear
			selectedSetups = Collections.singletonList(
				ExampleGearEnricher.enrich(example.getGear(), pageSetups));
			selectedInventory = example.getInventory();
		}
		else
		{
			if (currentSetups == null)
			{
				return;
			}
			int setupIdx = setupCombo.getSelectedIndex();
			if (setupIdx < 0)
			{
				return;
			}
			boolean allStyles = setupIdx == currentSetups.size();
			selectedSetups = allStyles
				? currentSetups
				: Collections.singletonList(currentSetups.get(setupIdx));

			int invIdx = inventoryCombo.isVisible() ? inventoryCombo.getSelectedIndex() : 0;
			selectedInventory = invIdx > 0 && invIdx <= currentInventories.size()
				? currentInventories.get(invIdx - 1)
				: null;
		}

		clientThread.invokeLater(() ->
		{
			if (!itemNameIndex.isReady())
			{
				SwingUtilities.invokeLater(() ->
					status("Item index still building — log in and retry shortly", SUBSTITUTE));
				return;
			}
			Set<Integer> owned = bankStateCache.ownedItemIds();
			boolean hasSnapshot = bankStateCache.hasSnapshot();
			PickStrategy strategy = recommendedToggle.isSelected()
				? PickStrategy.TOP_RECOMMENDATION : PickStrategy.BEST_OWNED;
			boolean onSlayerTask = slayerToggle.isSelected();
			// the enriched setups are what actually gets resolved, so ask them
			// (not the raw page) whether a task-conditional item is in play, and
			// collect the wiki's own wording for the checkbox tooltip
			List<String> slayerNotes = new ArrayList<>();
			for (GearSetup setup : selectedSetups)
			{
				for (String note : OwnershipResolver.slayerConditionalNotes(setup))
				{
					if (!slayerNotes.contains(note))
					{
						slayerNotes.add(note);
					}
				}
			}
			boolean showSlayerToggle = !slayerNotes.isEmpty();
			String slayerTooltip = slayerTooltip(slayerNotes);

			List<SetupResolution> resolutions = new ArrayList<>();
			List<List<SlotPick>> gearBlocks = new ArrayList<>();
			for (GearSetup setup : selectedSetups)
			{
				SetupResolution r = ownershipResolver.resolve(setup, owned,
					config.missingGearBehavior(), onSlayerTask, strategy);
				List<SlotPick> picks = OwnershipResolver.dropShieldForTwoHanded(r.getPicks(), this::isTwoHanded);
				if (picks != r.getPicks())
				{
					r = new SetupResolution(r.getSetup(), picks, r.getUnresolvedNames(), r.getEmptySlots());
				}
				resolutions.add(r);
				gearBlocks.add(r.getPicks());
			}
			InventoryResolution invResolution = selectedInventory == null
				? null
				: inventoryResolver.resolve(selectedInventory, owned, config.missingGearBehavior(),
					pageSetups, strategy);

			List<InventoryPick> invPicks = invResolution == null
				? Collections.emptyList() : invResolution.getPicks();
			List<InventoryPick> runePicks = invResolution == null
				? Collections.emptyList() : invResolution.getRunePicks();

			// owned alternates from the wiki tables fill the space below the layout
			Set<Integer> excluded = new HashSet<>();
			for (List<SlotPick> block : gearBlocks)
			{
				for (SlotPick pick : block)
				{
					excluded.add(pick.getItemId());
				}
			}
			for (InventoryPick pick : invPicks)
			{
				excluded.add(pick.getItemId());
			}
			for (InventoryPick pick : runePicks)
			{
				excluded.add(pick.getItemId());
			}
			// owned alternates are an owned-mode concept — the BIS view shows
			// the wiki's list and nothing else (decision #12). The page's
			// single-item switch annotations ride the same owned-only channel,
			// so they too are absent from BIS.
			List<Integer> extras = new ArrayList<>();
			if (strategy == PickStrategy.BEST_OWNED && config.includeExtras())
			{
				for (GearSetup setup : selectedSetups)
				{
					for (int id : ownershipResolver.ownedAlternatives(setup, owned, onSlayerTask, excluded))
					{
						extras.add(id);
						excluded.add(id);
					}
				}
				for (GearSetup switchSetup : pageSwitches)
				{
					for (int id : ownershipResolver.ownedAlternatives(switchSetup, owned, onSlayerTask, excluded))
					{
						extras.add(id);
						excluded.add(id);
					}
				}
			}

			Layout previewLayout = LayoutBuilder.build("preview", gearBlocks, invPicks, runePicks, extras, config.layoutStyle());
			Set<Integer> unownedIds = collectUnowned(resolutions, invPicks, runePicks);

			SwingUtilities.invokeLater(() ->
			{
				slayerToggle.setVisible(showSlayerToggle);
				if (showSlayerToggle)
				{
					slayerToggle.setToolTipText(slayerTooltip);
				}
				showPreview(resolutions, invResolution, extras, hasSnapshot, previewLayout.getLayout(), unownedIds);
			});
		});
	}

	/** Client thread. Unknown stats count as one-handed — never drop a shield on a guess. */
	private boolean isTwoHanded(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId);
		return stats != null && stats.getEquipment() != null && stats.getEquipment().isTwoHanded();
	}

	private static Set<Integer> collectUnowned(List<SetupResolution> resolutions,
		List<InventoryPick> invPicks, List<InventoryPick> runePicks)
	{
		Set<Integer> unowned = new HashSet<>();
		for (SetupResolution r : resolutions)
		{
			for (SlotPick pick : r.getPicks())
			{
				if (!pick.isOwned())
				{
					unowned.add(pick.getItemId());
				}
			}
		}
		for (InventoryPick pick : invPicks)
		{
			if (!pick.isOwned())
			{
				unowned.add(pick.getItemId());
			}
		}
		for (InventoryPick pick : runePicks)
		{
			if (!pick.isOwned())
			{
				unowned.add(pick.getItemId());
			}
		}
		return unowned;
	}

	private void showPreview(List<SetupResolution> resolutions, InventoryResolution invResolution,
		List<Integer> extras, boolean hasSnapshot, int[] layoutArr, Set<Integer> unownedIds)
	{
		currentResolutions = resolutions;
		currentInventoryResolution = invResolution;
		currentExtras = extras;
		previewPanel.removeAll();

		renderGrid(layoutArr, unownedIds);

		if (!hasSnapshot)
		{
			addRow("Open your bank once so owned gear is known", SUBSTITUTE, true);
		}

		int totalPicks = 0;
		int substitutes = 0;
		int goals = 0;
		boolean multi = resolutions.size() > 1;
		for (SetupResolution resolution : resolutions)
		{
			if (multi)
			{
				addRow("— " + resolution.getSetup().getStyleLabel() + " —", ColorScheme.LIGHT_GRAY_COLOR, false);
			}
			for (SlotPick pick : resolution.getPicks())
			{
				totalPicks++;
				String name = itemNameIndex.nameById(pick.getItemId());
				if (name == null)
				{
					name = pick.getItem().getName();
				}
				Color color;
				String suffix = "";
				if (!pick.isOwned())
				{
					color = MISSING;
					suffix = recommendedToggle.isSelected() ? "  (not owned)" : "  (goal)";
					goals++;
				}
				else if (pick.isTopRecommendation())
				{
					color = OWNED;
				}
				else
				{
					color = SUBSTITUTE;
					suffix = "  (alt)";
					substitutes++;
				}
				addItemRow(pick.getItemId(), slotName(pick.getSlot()) + ": " + name + suffix, color);
			}
			for (EquipSlot slot : resolution.getEmptySlots())
			{
				addRow(slotName(slot) + (recommendedToggle.isSelected() ? ": nothing recognized" : ": nothing owned"),
					ColorScheme.MEDIUM_GRAY_COLOR, false);
			}
			for (Map.Entry<EquipSlot, List<String>> entry : resolution.getUnresolvedNames().entrySet())
			{
				addRow("Unrecognized (" + slotName(entry.getKey()) + "): "
					+ String.join(", ", entry.getValue()), ColorScheme.MEDIUM_GRAY_COLOR, true);
			}
		}

		if (invResolution != null)
		{
			addRow("— Inventory: " + invResolution.getSetup().getLabel() + " —", ColorScheme.LIGHT_GRAY_COLOR, false);
			int placed = invResolution.getPicks().size();
			StringBuilder inv = new StringBuilder().append(placed).append(" slots");
			if (invResolution.getMissingCount() > 0)
			{
				inv.append(", ").append(invResolution.getMissingCount()).append(" missing");
			}
			addRow(inv.toString(), placed > 0 ? OWNED : ColorScheme.MEDIUM_GRAY_COLOR, false);
			if (!invResolution.getRunePicks().isEmpty())
			{
				List<String> runeNames = new ArrayList<>();
				for (InventoryPick rune : invResolution.getRunePicks())
				{
					String rn = itemNameIndex.nameById(rune.getItemId());
					runeNames.add(rn != null ? rn : "?");
				}
				addRow("Runes: " + String.join(", ", runeNames), OWNED, false);
			}
			if (!invResolution.getMissingNames().isEmpty())
			{
				addRow("— Missing items —", ColorScheme.LIGHT_GRAY_COLOR, false);
				for (String missingName : invResolution.getMissingNames())
				{
					addRow(missingName, MISSING, false);
				}
			}
			if (!invResolution.getUnresolvedNames().isEmpty())
			{
				addRow("Unrecognized: " + String.join(", ", invResolution.getUnresolvedNames()),
					ColorScheme.MEDIUM_GRAY_COLOR, true);
			}
		}

		if (!extras.isEmpty())
		{
			addRow("+" + extras.size() + " owned extras below the layout (alternates, switches)",
				ColorScheme.LIGHT_GRAY_COLOR, true);
		}

		boolean importable = totalPicks > 0
			|| (invResolution != null && !invResolution.getPicks().isEmpty());
		importButton.setEnabled(importable);

		refreshTagAndIcon(resolutions);

		StringBuilder summary = new StringBuilder().append(totalPicks).append(" gear items");
		if (substitutes > 0)
		{
			summary.append(", ").append(substitutes).append(" alternatives");
		}
		if (goals > 0)
		{
			summary.append(", ").append(goals).append(recommendedToggle.isSelected() ? " not owned" : " goals");
		}
		status(summary.toString(), null);

		previewPanel.revalidate();
		previewPanel.repaint();
	}

	/** Item-icon grid mirroring the real bank tab layout. Unowned items render dimmed. */
	private void renderGrid(int[] layoutArr, Set<Integer> unownedIds)
	{
		gridPanel.removeAll();

		int last = -1;
		for (int i = 0; i < layoutArr.length; i++)
		{
			if (layoutArr[i] != -1)
			{
				last = i;
			}
		}
		if (last < 0)
		{
			gridPanel.setVisible(false);
			return;
		}
		int rows = last / GRID_COLS + 1;
		gridPanel.setVisible(true);
		gridPanel.setLayout(new GridLayout(rows, GRID_COLS, 1, 1));
		gridPanel.setMaximumSize(new Dimension(GRID_COLS * (CELL_W + 1), rows * (CELL_H + 1)));

		for (int pos = 0; pos < rows * GRID_COLS; pos++)
		{
			JLabel cell = new JLabel();
			cell.setHorizontalAlignment(SwingConstants.CENTER);
			cell.setPreferredSize(new Dimension(CELL_W, CELL_H));
			cell.setOpaque(true);
			cell.setBackground(ColorScheme.DARK_GRAY_COLOR);
			int id = pos < layoutArr.length ? layoutArr[pos] : -1;
			if (id != -1)
			{
				boolean dim = unownedIds.contains(id);
				AsyncBufferedImage img = itemManager.getImage(id);
				Runnable apply = () ->
				{
					Image scaled = img.getScaledInstance(CELL_W, CELL_H, Image.SCALE_SMOOTH);
					ImageIcon icon = new ImageIcon(scaled);
					cell.setIcon(icon);
					if (dim)
					{
						cell.setEnabled(false); // Swing renders disabled icons greyed
					}
					cell.revalidate();
					cell.repaint();
				};
				apply.run();
				img.onLoaded(() -> SwingUtilities.invokeLater(apply));
			}
			gridPanel.add(cell);
		}
		gridPanel.revalidate();
		gridPanel.repaint();
	}

	/** Prefills tag name and icon picker, respecting in-progress user edits. */
	private void refreshTagAndIcon(List<SetupResolution> resolutions)
	{
		String autoTag = autoTagName(resolutions);
		if (!userEditedTag)
		{
			tagNameField.setText(autoTag);
		}

		// boss drops lead the grid: they're the icons that identify a tab at a
		// glance, and the pet is the default when we can spot one
		LinkedHashSet<IconOption> options = new LinkedHashSet<>();
		IconOption petOption = null;
		for (String drop : pageDropNames)
		{
			List<Integer> ids = itemNameIndex.idsByName(ItemNames.normalize(drop));
			if (ids.isEmpty())
			{
				continue;
			}
			IconOption option = new IconOption(ids.get(0), drop);
			if (petOption == null && petNames.contains(drop))
			{
				petOption = option;
			}
			options.add(option);
		}

		int autoIconId = BankTagImporter.iconFor(resolutions);
		String autoIconName = itemNameIndex.nameById(autoIconId);
		IconOption weaponOption = autoIconName == null ? null : new IconOption(autoIconId, autoIconName);
		if (weaponOption != null)
		{
			options.add(weaponOption);
		}
		for (SetupResolution r : resolutions)
		{
			for (SlotPick pick : r.getPicks())
			{
				String n = itemNameIndex.nameById(pick.getItemId());
				if (n != null)
				{
					options.add(new IconOption(pick.getItemId(), n));
				}
			}
		}
		if (currentInventoryResolution != null)
		{
			for (InventoryPick pick : currentInventoryResolution.getPicks())
			{
				String n = itemNameIndex.nameById(pick.getItemId());
				if (n != null)
				{
					options.add(new IconOption(pick.getItemId(), n));
				}
			}
		}

		IconOption previous = iconPicker.getSelected();
		iconPicker.setOptions(new ArrayList<>(options));
		if (userEditedIcon && previous != null && options.contains(previous))
		{
			iconPicker.setSelected(previous);
		}
		else
		{
			IconOption preferred = petOption != null ? petOption : weaponOption;
			if (preferred == null && !options.isEmpty())
			{
				preferred = options.iterator().next();
			}
			iconPicker.setSelected(preferred);
			userEditedIcon = false;
		}
	}

	private String autoTagName(List<SetupResolution> resolutions)
	{
		if (currentPageTitle == null || resolutions == null || resolutions.isEmpty())
		{
			return "";
		}
		String styleLabel;
		if (!currentExamples.isEmpty())
		{
			styleLabel = exampleStyleLabel();
		}
		else if (resolutions.size() > 1)
		{
			styleLabel = "all";
		}
		else
		{
			styleLabel = resolutions.get(0).getSetup().getStyleLabel();
		}
		// a BIS tab is a different thing from your gear tab — default it to a
		// different tag so importing one never silently overwrites the other
		if (recommendedToggle.isSelected())
		{
			styleLabel = styleLabel + " bis";
		}
		return withTagPrefix(TagNames.build(displayTitle(currentPageTitle), styleLabel));
	}

	/**
	 * Prepends the configured prefix so every tab this plugin creates sorts
	 * together and stays clear of the player's own tabs.
	 */
	private String withTagPrefix(String tag)
	{
		return TagNames.withPrefix(config.tagPrefix(), tag);
	}

	/**
	 * The visible cascade selections joined, with the page name scrubbed out
	 * of section labels ("Theatre of Blood (Hard Mode)" contributes just
	 * "Hard Mode" to a Theatre of Blood tag).
	 */
	private String exampleStyleLabel()
	{
		StringBuilder label = new StringBuilder();
		String page = displayTitle(currentPageTitle).toLowerCase(Locale.ROOT);
		for (JComboBox<String> combo : Arrays.asList(modeCombo, tierCombo, setupCombo))
		{
			Object selected = combo.isVisible() ? combo.getSelectedItem() : null;
			if (selected == null)
			{
				continue;
			}
			String part = selected.toString();
			StringBuilder scrubbed = new StringBuilder();
			for (String word : part.split("[\\s()]+"))
			{
				if (!word.isEmpty() && !page.contains(word.toLowerCase(Locale.ROOT)))
				{
					scrubbed.append(word).append(' ');
				}
			}
			label.append(scrubbed.length() > 0 ? scrubbed : part).append(' ');
		}
		return label.toString().trim();
	}

	private void doImport()
	{
		List<SetupResolution> resolutions = currentResolutions;
		InventoryResolution invResolution = currentInventoryResolution;
		List<Integer> extras = currentExtras;
		String pageTitle = currentPageTitle;
		if (resolutions == null || resolutions.isEmpty() || pageTitle == null)
		{
			return;
		}
		String customTag = TagNames.sanitize(tagNameField.getText());
		String tag = customTag.isEmpty() ? autoTagName(resolutions) : customTag;

		IconOption selectedIcon = iconPicker.getSelected();
		Integer icon = selectedIcon != null ? selectedIcon.itemId : null;

		importButton.setEnabled(false);
		// what's already under that name has to be read on the client thread,
		// but asking about it has to happen on the EDT
		clientThread.invokeLater(() ->
		{
			BankTagImporter.Existing existing;
			try
			{
				existing = importer.existing(tag);
			}
			catch (Exception e)
			{
				log.warn("Could not check for an existing tab", e);
				existing = BankTagImporter.Existing.NONE;
			}
			BankTagImporter.Existing found = existing;
			SwingUtilities.invokeLater(() ->
			{
				if (found != BankTagImporter.Existing.NONE && config.confirmOverwrite()
					&& !askOverwrite(tag, found))
				{
					status("Import cancelled — '" + tag + "' left alone", null);
					importButton.setEnabled(true);
					return;
				}
				runImport(tag, resolutions, invResolution, extras, icon);
			});
		});
	}

	/**
	 * Asks before replacing a tab. Overwriting is usually what the player
	 * wants — it's how a setup gets refreshed after an upgrade — but a tab
	 * they built by hand is not recoverable, so the wording leads with whose
	 * it is and the safe option is the default.
	 */
	private boolean askOverwrite(String tag, BankTagImporter.Existing existing)
	{
		boolean ours = existing == BankTagImporter.Existing.OURS;
		String message = "<html><body style='width:260px'>"
			+ (ours
				? "The bank tag <b>" + escapeHtml(tag) + "</b> was imported by this plugin before."
					+ "<br><br>Importing again replaces its items and layout with the setup shown here."
				: "There is already a bank tag named <b>" + escapeHtml(tag) + "</b> that this plugin"
					+ " didn't create.<br><br>Importing replaces its items and layout. If you arranged"
					+ " that tab yourself, those changes can't be recovered.")
			+ "<br><br>You can import under a different name by editing the Name field first."
			+ "</body></html>";

		String[] options = {ours ? "Replace" : "Replace anyway", "Cancel"};
		int choice = JOptionPane.showOptionDialog(this, message,
			ours ? "Update this tab?" : "Overwrite an existing tab?",
			JOptionPane.YES_NO_OPTION, ours ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE,
			null, options, options[1]);
		return choice == JOptionPane.YES_OPTION;
	}

	private static String escapeHtml(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void runImport(String tag, List<SetupResolution> resolutions,
		InventoryResolution invResolution, List<Integer> extras, Integer icon)
	{
		clientThread.invokeLater(() ->
		{
			try
			{
				importer.importSetups(tag, resolutions, invResolution, extras, config.layoutStyle(), icon);
				SwingUtilities.invokeLater(() ->
				{
					status("Imported '" + tag + "' — open your bank to see the tab", OWNED);
					importButton.setEnabled(true);
				});
			}
			catch (Exception e)
			{
				log.warn("Import failed", e);
				SwingUtilities.invokeLater(() ->
				{
					status(rootMessage(e), MISSING);
					importButton.setEnabled(true);
				});
			}
		});
	}

	private void clearSetups()
	{
		currentPageTitle = null;
		currentSetups = null;
		pageSetups = Collections.emptyList();
		pageSwitches = Collections.emptyList();
		inventoryPairing = SetupPairing.NONE;
		pageInventories = Collections.emptyList();
		pageExamples = Collections.emptyList();
		pageDropNames = Collections.emptyList();
		currentExamples = Collections.emptyList();
		currentInventories = Collections.emptyList();
		currentResolutions = null;
		currentInventoryResolution = null;
		currentExtras = Collections.emptyList();
		userPickedInventory = false;
		userEditedTag = false;
		userEditedIcon = false;
		sourceCombo.setVisible(false);
		modeCombo.setVisible(false);
		tierCombo.setVisible(false);
		setupCombo.setVisible(false);
		inventoryCombo.setVisible(false);
		slayerToggle.setVisible(false);
		recommendedToggle.setVisible(false);
		wikiLinkButton.setVisible(false);
		tagNameField.setText("");
		iconPicker.setOptions(Collections.emptyList());
		iconPicker.setSelected(null);
		gridPanel.removeAll();
		gridPanel.setVisible(false);
		previewPanel.removeAll();
		previewPanel.revalidate();
		previewPanel.repaint();
		importButton.setEnabled(false);
	}

	private void addRow(String text, Color color, boolean small)
	{
		JLabel label = new JLabel("<html>" + text.replace("<", "&lt;") + "</html>");
		label.setForeground(color);
		if (small)
		{
			label.setFont(label.getFont().deriveFont(Font.ITALIC, label.getFont().getSize() - 1f));
		}
		label.setAlignmentX(LEFT_ALIGNMENT);
		previewPanel.add(label);
	}

	/** A preview row led by the item's sprite. */
	private void addItemRow(int itemId, String text, Color color)
	{
		JLabel label = new JLabel("<html>" + text.replace("<", "&lt;") + "</html>");
		label.setForeground(color);
		AsyncBufferedImage img = itemManager.getImage(itemId);
		label.setIcon(new ImageIcon(img.getScaledInstance(18, 16, Image.SCALE_SMOOTH)));
		label.setIconTextGap(4);
		img.onLoaded(() -> SwingUtilities.invokeLater(() ->
		{
			label.setIcon(new ImageIcon(img.getScaledInstance(18, 16, Image.SCALE_SMOOTH)));
			label.repaint();
		}));
		label.setAlignmentX(LEFT_ALIGNMENT);
		previewPanel.add(label);
	}

	private void status(String text, Color color)
	{
		statusLabel.setText("<html>" + text + "</html>");
		statusLabel.setForeground(color != null ? color : ColorScheme.LIGHT_GRAY_COLOR);
	}

	private static JPanel labeledRow(String label, java.awt.Component field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel l = new JLabel(label);
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(l, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 4));
		return row;
	}

	private static DocumentListener userEditListener(java.util.function.BooleanSupplier hasFocus, Runnable onEdit)
	{
		return new DocumentListener()
		{
			private void changed()
			{
				if (hasFocus.getAsBoolean())
				{
					onEdit.run();
				}
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changed();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				changed();
			}
		};
	}

	private static String slotName(EquipSlot slot)
	{
		String s = slot.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static String rootMessage(Throwable t)
	{
		while (t.getCause() != null && t.getCause() != t)
		{
			t = t.getCause();
		}
		if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException
			|| t instanceof java.net.SocketTimeoutException || t instanceof java.io.InterruptedIOException)
		{
			return "Can't reach the OSRS Wiki — check your connection and try again";
		}
		return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
	}
}
