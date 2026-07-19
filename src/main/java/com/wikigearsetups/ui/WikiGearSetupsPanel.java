package com.wikigearsetups.ui;

import com.wikigearsetups.WikiGearSetupsConfig;
import com.wikigearsetups.bank.BankStateCache;
import com.wikigearsetups.bank.InventoryPick;
import com.wikigearsetups.bank.InventoryResolution;
import com.wikigearsetups.bank.InventoryResolver;
import com.wikigearsetups.bank.OwnershipResolver;
import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.items.ItemNameIndex;
import com.wikigearsetups.layout.BankTagImporter;
import com.wikigearsetups.layout.LayoutBuilder;
import com.wikigearsetups.layout.TagNames;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.GearSetupParser;
import com.wikigearsetups.wiki.InventoryParser;
import com.wikigearsetups.wiki.InventorySetup;
import com.wikigearsetups.wiki.PageDiscovery;
import com.wikigearsetups.wiki.SearchResult;
import com.wikigearsetups.wiki.WikiClient;
import java.awt.BorderLayout;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
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
	private static final String ALL_STYLES = "All styles (one tab)";
	private static final String NO_INVENTORY = "No inventory";
	private static final int GRID_COLS = 8;
	private static final int CELL_W = 26;
	private static final int CELL_H = 24;
	private static final String DEVELOPER_NAME = "Jiimbones";
	// placeholder until a real donation link exists
	private static final String DONATION_URL = "https://example.com/donate";

	private final WikiClient wikiClient;
	private final GearSetupParser parser = new GearSetupParser();
	private final InventoryParser inventoryParser = new InventoryParser();
	private final OwnershipResolver ownershipResolver;
	private final InventoryResolver inventoryResolver;
	private final BankStateCache bankStateCache;
	private final BankTagImporter importer;
	private final ItemNameIndex itemNameIndex;
	private final ItemManager itemManager;
	private final ClientThread clientThread;
	private final WikiGearSetupsConfig config;

	private final FlatTextField searchField = new FlatTextField();
	private final JLabel statusLabel = new JLabel(" ");
	private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private final JList<String> resultsList = new JList<>(resultsModel);
	private final JButton wikiLinkButton = new RoundedButton("Open wiki page");
	private final JComboBox<String> setupCombo = new JComboBox<>();
	private final JComboBox<String> inventoryCombo = new JComboBox<>();
	private final JPanel gridPanel = new JPanel();
	private final JPanel previewPanel = new JPanel();
	private final FlatTextField tagNameField = new FlatTextField();
	private final JComboBox<IconOption> iconCombo = new JComboBox<>();
	private final JButton importButton = RoundedButton.accent("Import as bank tag");

	private List<SearchResult> searchResults = Collections.emptyList();
	private String currentPageTitle;
	private List<GearSetup> currentSetups;
	private List<InventorySetup> currentInventories = Collections.emptyList();
	private List<SetupResolution> currentResolutions;
	private InventoryResolution currentInventoryResolution;
	private List<Integer> currentExtras = Collections.emptyList();
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
		this.wikiClient = wikiClient;
		this.ownershipResolver = ownershipResolver;
		this.inventoryResolver = inventoryResolver;
		this.bankStateCache = bankStateCache;
		this.importer = importer;
		this.itemNameIndex = itemNameIndex;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.config = config;

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// header: logo, search, status — always visible
		JPanel north = new JPanel();
		north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
		north.setOpaque(false);
		JLabel banner = loadBanner();
		if (banner != null)
		{
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
		add(north, BorderLayout.NORTH);

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

		gridPanel.setAlignmentX(LEFT_ALIGNMENT);
		gridPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		gridPanel.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR));
		flow.add(gridPanel);
		flow.add(Box.createVerticalStrut(8));

		previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.setOpaque(false);
		previewPanel.setAlignmentX(LEFT_ALIGNMENT);
		flow.add(previewPanel);
		flow.add(Box.createVerticalStrut(8));

		tagNameField.getDocument().addDocumentListener(userEditListener(
			() -> tagNameField.getTextField().hasFocus(), () -> userEditedTag = true));
		styleFlatField(tagNameField);
		tagNameField.setPreferredSize(new Dimension(0, 26));
		flow.add(labeledRow("Tag name", tagNameField));
		flow.add(Box.createVerticalStrut(4));
		iconCombo.setRenderer(new IconOptionRenderer());
		iconCombo.setMaximumRowCount(12);
		styleCombo(iconCombo);
		iconCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedIcon = true;
			}
		});
		flow.add(labeledRow("Tab icon", iconCombo));
		flow.add(Box.createVerticalStrut(8));
		importButton.setEnabled(false);
		importButton.setToolTipText("Creates or updates a bank tag tab with this layout");
		importButton.setAlignmentX(LEFT_ALIGNMENT);
		importButton.setFont(importButton.getFont().deriveFont(Font.BOLD));
		importButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
		importButton.addActionListener(e -> doImport());
		flow.add(importButton);

		// Scrollable wrapper: tracks the viewport width so nothing can push
		// past the panel edge, while keeping natural height for scrolling
		JScrollPane scroll = new JScrollPane(new ScrollableWrapper(flow),
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(null);
		scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		SlimScrollBarUI.apply(scroll.getVerticalScrollBar());
		add(scroll, BorderLayout.CENTER);

		// footer pinned to the very bottom regardless of content height
		add(footer(), BorderLayout.SOUTH);
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

	/** Scroll content that always matches the viewport width, natural height. */
	private static final class ScrollableWrapper extends JPanel implements Scrollable
	{
		ScrollableWrapper(java.awt.Component content)
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
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction)
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

	/** Plugin logo shipped as resources/com/wikigearsetups/banner.png, or null when absent. */
	private JLabel loadBanner()
	{
		try
		{
			BufferedImage img = ImageUtil.loadImageResource(WikiGearSetupsPanel.class, "/com/wikigearsetups/banner.png");
			int width = PluginPanel.PANEL_WIDTH - 16;
			int height = Math.max(1, img.getHeight() * width / img.getWidth());
			return new JLabel(new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
		}
		catch (RuntimeException e)
		{
			log.debug("No banner resource", e);
			return null;
		}
	}

	private JPanel footer()
	{
		JPanel footer = new JPanel(new BorderLayout(4, 0));
		footer.setOpaque(false);
		footer.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.DARKER_GRAY_HOVER_COLOR),
			BorderFactory.createEmptyBorder(6, 0, 0, 0)));
		JLabel dev = new JLabel("Developed by " + DEVELOPER_NAME);
		dev.setFont(FontManager.getRunescapeSmallFont());
		dev.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		footer.add(dev, BorderLayout.WEST);
		JButton donate = new RoundedButton("Donate ♥");
		donate.setFont(FontManager.getRunescapeSmallFont());
		donate.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
		donate.addActionListener(e -> LinkBrowser.browse(DONATION_URL));
		footer.add(donate, BorderLayout.EAST);
		return footer;
	}

	/** A pickable tab icon: item id + display name, rendered with its sprite. */
	private static final class IconOption
	{
		private final int itemId;
		private final String name;

		IconOption(int itemId, String name)
		{
			this.itemId = itemId;
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof IconOption && ((IconOption) o).itemId == itemId;
		}

		@Override
		public int hashCode()
		{
			return itemId;
		}
	}

	private final class IconOptionRenderer extends DarkListRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof IconOption)
			{
				IconOption option = (IconOption) value;
				label.setText(option.name);
				AsyncBufferedImage img = itemManager.getImage(option.itemId);
				label.setIcon(new ImageIcon(img.getScaledInstance(20, 18, Image.SCALE_SMOOTH)));
				label.setIconTextGap(6);
			}
			return label;
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
					resultsModel.addElement(displayTitle(r.getTitle()));
				}
				clearSetups();
				status(results.isEmpty() ? "No pages with gear setups found" : results.size() + " results", null);
			}));
	}

	/** "Zulrah/Strategies" reads better as just "Zulrah". */
	private static String displayTitle(String title)
	{
		return title.endsWith("/Strategies")
			? title.substring(0, title.length() - "/Strategies".length())
			: title;
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
			List<GearSetup> setups = parser.parse(wikitext);
			if (setups.isEmpty())
			{
				tryCandidates(candidates, index + 1);
				return;
			}
			List<InventorySetup> inventories = inventoryParser.parse(wikitext);
			SwingUtilities.invokeLater(() -> showSetups(candidate, setups, inventories));
		});
	}

	private void showSetups(String pageTitle, List<GearSetup> setups, List<InventorySetup> inventories)
	{
		currentPageTitle = pageTitle;
		currentSetups = setups;
		currentInventories = inventories;
		userPickedInventory = false;
		userEditedTag = false;
		userEditedIcon = false;

		updatingCombos = true;
		DefaultComboBoxModel<String> setupModel = new DefaultComboBoxModel<>();
		for (GearSetup setup : setups)
		{
			setupModel.addElement(setup.getStyleLabel());
		}
		if (setups.size() > 1)
		{
			setupModel.addElement(ALL_STYLES);
		}
		setupCombo.setModel(setupModel);
		setupCombo.setVisible(true);
		wikiLinkButton.setVisible(true);

		DefaultComboBoxModel<String> invModel = new DefaultComboBoxModel<>();
		invModel.addElement(NO_INVENTORY);
		for (InventorySetup inventory : inventories)
		{
			invModel.addElement("Inv: " + inventory.getLabel());
		}
		inventoryCombo.setModel(invModel);
		inventoryCombo.setVisible(!inventories.isEmpty());
		autoSelectInventory();
		updatingCombos = false;

		status(setups.size() + (setups.size() == 1 ? " setup on " : " setups on ") + displayTitle(pageTitle), null);
		resolveSelection();
	}

	/**
	 * A lone inventory is obviously the right one — select it. With several,
	 * pick the one whose label matches the chosen style; otherwise leave the
	 * manual choice alone.
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
		String style = currentSetups.get(setupIdx).getStyleLabel();
		// combat-style tokens first: "Mage" gear finds a "Magic switches" inventory
		for (int i = 0; i < currentInventories.size(); i++)
		{
			if (StyleTokens.stylesOverlap(style, currentInventories.get(i).getLabel()))
			{
				inventoryCombo.setSelectedIndex(i + 1);
				return;
			}
		}
		String styleLower = style.toLowerCase(Locale.ROOT);
		for (int i = 0; i < currentInventories.size(); i++)
		{
			String label = currentInventories.get(i).getLabel().toLowerCase(Locale.ROOT);
			if (label.contains(styleLower) || styleLower.contains(label))
			{
				inventoryCombo.setSelectedIndex(i + 1);
				return;
			}
		}
	}

	private void resolveSelection()
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
		List<GearSetup> selectedSetups = allStyles
			? currentSetups
			: Collections.singletonList(currentSetups.get(setupIdx));

		int invIdx = inventoryCombo.isVisible() ? inventoryCombo.getSelectedIndex() : 0;
		InventorySetup selectedInventory = invIdx > 0 && invIdx <= currentInventories.size()
			? currentInventories.get(invIdx - 1)
			: null;

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

			List<SetupResolution> resolutions = new ArrayList<>();
			List<List<SlotPick>> gearBlocks = new ArrayList<>();
			for (GearSetup setup : selectedSetups)
			{
				SetupResolution r = ownershipResolver.resolve(setup, owned,
					config.missingGearBehavior(), config.assumeSlayerTask());
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
				: inventoryResolver.resolve(selectedInventory, owned, config.missingGearBehavior());

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
			List<Integer> extras = new ArrayList<>();
			for (GearSetup setup : selectedSetups)
			{
				for (int id : ownershipResolver.ownedAlternatives(setup, owned, config.assumeSlayerTask(), excluded))
				{
					extras.add(id);
					excluded.add(id);
				}
			}

			Layout previewLayout = LayoutBuilder.build("preview", gearBlocks, invPicks, runePicks, extras, config.layoutStyle());
			Set<Integer> unownedIds = collectUnowned(resolutions, invPicks, runePicks);

			SwingUtilities.invokeLater(() ->
				showPreview(resolutions, invResolution, extras, hasSnapshot, previewLayout.getLayout(), unownedIds));
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
					suffix = "  (goal)";
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
				addRow(slotName(slot) + ": nothing owned", ColorScheme.MEDIUM_GRAY_COLOR, false);
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
			if (!invResolution.getUnresolvedNames().isEmpty())
			{
				addRow("Unrecognized: " + String.join(", ", invResolution.getUnresolvedNames()),
					ColorScheme.MEDIUM_GRAY_COLOR, true);
			}
		}

		if (!extras.isEmpty())
		{
			addRow("+" + extras.size() + " owned alternates below the layout", ColorScheme.LIGHT_GRAY_COLOR, true);
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
			summary.append(", ").append(goals).append(" goals");
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

		LinkedHashSet<IconOption> options = new LinkedHashSet<>();
		int autoIconId = BankTagImporter.iconFor(resolutions);
		String autoIconName = itemNameIndex.nameById(autoIconId);
		if (autoIconName != null)
		{
			options.add(new IconOption(autoIconId, autoIconName));
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

		IconOption previous = (IconOption) iconCombo.getSelectedItem();
		updatingCombos = true;
		DefaultComboBoxModel<IconOption> model = new DefaultComboBoxModel<>();
		for (IconOption option : options)
		{
			model.addElement(option);
			// repaint when the sprite finishes loading so the picker isn't blank
			itemManager.getImage(option.itemId).onLoaded(() ->
				SwingUtilities.invokeLater(iconCombo::repaint));
		}
		iconCombo.setModel(model);
		if (userEditedIcon && previous != null && options.contains(previous))
		{
			iconCombo.setSelectedItem(previous);
		}
		else if (model.getSize() > 0)
		{
			iconCombo.setSelectedIndex(0);
			userEditedIcon = false;
		}
		updatingCombos = false;
	}

	private String autoTagName(List<SetupResolution> resolutions)
	{
		if (currentPageTitle == null || resolutions == null || resolutions.isEmpty())
		{
			return "";
		}
		String styleLabel = resolutions.size() > 1
			? "all"
			: resolutions.get(0).getSetup().getStyleLabel();
		return TagNames.build(currentPageTitle, styleLabel);
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

		IconOption selectedIcon = (IconOption) iconCombo.getSelectedItem();
		Integer icon = selectedIcon != null ? selectedIcon.itemId : null;

		importButton.setEnabled(false);
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
		currentInventories = Collections.emptyList();
		currentResolutions = null;
		currentInventoryResolution = null;
		currentExtras = Collections.emptyList();
		userPickedInventory = false;
		userEditedTag = false;
		userEditedIcon = false;
		setupCombo.setVisible(false);
		inventoryCombo.setVisible(false);
		wikiLinkButton.setVisible(false);
		tagNameField.setText("");
		iconCombo.setModel(new DefaultComboBoxModel<>());
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
		return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
	}
}
