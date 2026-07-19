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
import com.wikigearsetups.items.ItemNames;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
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
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ImageIcon;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
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

	private final JTextField searchField = new JTextField();
	private final JLabel statusLabel = new JLabel(" ");
	private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private final JList<String> resultsList = new JList<>(resultsModel);
	private final JButton wikiLinkButton = new JButton("Open wiki page");
	private final JComboBox<String> setupCombo = new JComboBox<>();
	private final JComboBox<String> inventoryCombo = new JComboBox<>();
	private final JPanel gridPanel = new JPanel();
	private final JPanel previewPanel = new JPanel();
	private final JTextField tagNameField = new JTextField();
	private final JComboBox<String> iconCombo = new JComboBox<>();
	private final JButton importButton = new JButton("Import as bank tag");

	private List<SearchResult> searchResults = Collections.emptyList();
	private String currentPageTitle;
	private List<GearSetup> currentSetups;
	private List<InventorySetup> currentInventories = Collections.emptyList();
	private List<SetupResolution> currentResolutions;
	private InventoryResolution currentInventoryResolution;
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
		this.wikiClient = wikiClient;
		this.ownershipResolver = ownershipResolver;
		this.inventoryResolver = inventoryResolver;
		this.bankStateCache = bankStateCache;
		this.importer = importer;
		this.itemNameIndex = itemNameIndex;
		this.itemManager = itemManager;
		this.clientThread = clientThread;
		this.config = config;

		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel north = new JPanel(new BorderLayout(0, 4));
		JButton searchButton = new JButton("Search");
		searchField.addActionListener(e -> doSearch());
		searchButton.addActionListener(e -> doSearch());
		north.add(searchField, BorderLayout.CENTER);
		north.add(searchButton, BorderLayout.EAST);
		north.add(statusLabel, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsList.setVisibleRowCount(6);
		resultsList.addListSelectionListener(e ->
		{
			int idx = resultsList.getSelectedIndex();
			if (!e.getValueIsAdjusting() && idx >= 0 && idx < searchResults.size())
			{
				loadPage(searchResults.get(idx).getTitle());
			}
		});
		JScrollPane resultsScroll = new JScrollPane(resultsList);
		resultsScroll.setAlignmentX(LEFT_ALIGNMENT);
		center.add(resultsScroll);
		center.add(Box.createVerticalStrut(4));

		wikiLinkButton.setAlignmentX(LEFT_ALIGNMENT);
		wikiLinkButton.setVisible(false);
		wikiLinkButton.addActionListener(e -> openWikiPage());
		center.add(wikiLinkButton);
		center.add(Box.createVerticalStrut(4));

		setupCombo.setAlignmentX(LEFT_ALIGNMENT);
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
		center.add(setupCombo);
		center.add(Box.createVerticalStrut(4));

		inventoryCombo.setAlignmentX(LEFT_ALIGNMENT);
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
		center.add(inventoryCombo);
		center.add(Box.createVerticalStrut(6));

		gridPanel.setAlignmentX(LEFT_ALIGNMENT);
		gridPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		center.add(gridPanel);
		center.add(Box.createVerticalStrut(6));

		previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.setAlignmentX(LEFT_ALIGNMENT);
		center.add(previewPanel);

		add(center, BorderLayout.CENTER);

		JPanel south = new JPanel();
		south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
		tagNameField.getDocument().addDocumentListener(userEditListener(() -> tagNameField.hasFocus(), () -> userEditedTag = true));
		south.add(labeledRow("Tag name", tagNameField));
		south.add(Box.createVerticalStrut(4));
		iconCombo.setEditable(true);
		iconCombo.addActionListener(e ->
		{
			if (!updatingCombos)
			{
				userEditedIcon = true;
			}
		});
		south.add(labeledRow("Tab icon", iconCombo));
		south.add(Box.createVerticalStrut(6));
		importButton.setEnabled(false);
		importButton.setAlignmentX(LEFT_ALIGNMENT);
		importButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
		importButton.addActionListener(e -> doImport());
		south.add(importButton);
		add(south, BorderLayout.SOUTH);
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
		String style = currentSetups.get(setupIdx).getStyleLabel().toLowerCase(Locale.ROOT);
		for (int i = 0; i < currentInventories.size(); i++)
		{
			String label = currentInventories.get(i).getLabel().toLowerCase(Locale.ROOT);
			if (label.contains(style) || style.contains(label))
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
			Layout previewLayout = LayoutBuilder.build("preview", gearBlocks, invPicks, runePicks, config.layoutStyle());
			Set<Integer> unownedIds = collectUnowned(resolutions, invPicks, runePicks);

			SwingUtilities.invokeLater(() ->
				showPreview(resolutions, invResolution, hasSnapshot, previewLayout.getLayout(), unownedIds));
		});
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
		boolean hasSnapshot, int[] layoutArr, Set<Integer> unownedIds)
	{
		currentResolutions = resolutions;
		currentInventoryResolution = invResolution;
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
				addRow(slotName(pick.getSlot()) + ": " + name + suffix, color, false);
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

	/** Prefills tag name and icon combo, respecting in-progress user edits. */
	private void refreshTagAndIcon(List<SetupResolution> resolutions)
	{
		String autoTag = autoTagName(resolutions);
		if (!userEditedTag)
		{
			tagNameField.setText(autoTag);
		}

		LinkedHashSet<String> pickNames = new LinkedHashSet<>();
		String autoIconName = itemNameIndex.nameById(BankTagImporter.iconFor(resolutions));
		if (autoIconName != null)
		{
			pickNames.add(autoIconName);
		}
		for (SetupResolution r : resolutions)
		{
			for (SlotPick pick : r.getPicks())
			{
				String n = itemNameIndex.nameById(pick.getItemId());
				if (n != null)
				{
					pickNames.add(n);
				}
			}
		}

		String previous = selectedIconName();
		updatingCombos = true;
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		for (String n : pickNames)
		{
			model.addElement(n);
		}
		iconCombo.setModel(model);
		if (userEditedIcon && previous != null && !previous.isEmpty())
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

	private String selectedIconName()
	{
		Object item = iconCombo.getEditor() != null ? iconCombo.getEditor().getItem() : iconCombo.getSelectedItem();
		return item != null ? item.toString().trim() : "";
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
		String pageTitle = currentPageTitle;
		if (resolutions == null || resolutions.isEmpty() || pageTitle == null)
		{
			return;
		}
		String customTag = TagNames.sanitize(tagNameField.getText());
		String tag = customTag.isEmpty() ? autoTagName(resolutions) : customTag;

		Integer iconId = null;
		String iconName = selectedIconName();
		if (!iconName.isEmpty())
		{
			List<Integer> iconIds = itemNameIndex.idsByName(ItemNames.normalize(iconName));
			if (!iconIds.isEmpty())
			{
				iconId = iconIds.get(0);
			}
		}
		Integer icon = iconId;

		importButton.setEnabled(false);
		clientThread.invokeLater(() ->
		{
			try
			{
				importer.importSetups(tag, resolutions, invResolution, config.layoutStyle(), icon);
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

	private void status(String text, Color color)
	{
		statusLabel.setText("<html>" + text + "</html>");
		statusLabel.setForeground(color != null ? color : ColorScheme.LIGHT_GRAY_COLOR);
	}

	private static JPanel labeledRow(String label, java.awt.Component field)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
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
