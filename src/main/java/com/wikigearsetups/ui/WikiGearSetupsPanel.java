package com.wikigearsetups.ui;

import com.wikigearsetups.WikiGearSetupsConfig;
import com.wikigearsetups.bank.BankStateCache;
import com.wikigearsetups.bank.OwnershipResolver;
import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.items.ItemNameIndex;
import com.wikigearsetups.layout.BankTagImporter;
import com.wikigearsetups.layout.TagNames;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.GearSetupParser;
import com.wikigearsetups.wiki.PageDiscovery;
import com.wikigearsetups.wiki.SearchResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Search → pick page → pick setup → preview owned/substituted/missing →
 * import. Network work runs on OkHttp threads, game-state reads on the
 * client thread, and every UI mutation hops back to the EDT.
 */
@Slf4j
@Singleton
public class WikiGearSetupsPanel extends PluginPanel
{
	private static final Color OWNED = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color SUBSTITUTE = ColorScheme.PROGRESS_INPROGRESS_COLOR;
	private static final Color MISSING = ColorScheme.PROGRESS_ERROR_COLOR;

	private final com.wikigearsetups.wiki.WikiClient wikiClient;
	private final GearSetupParser parser = new GearSetupParser();
	private final OwnershipResolver ownershipResolver;
	private final BankStateCache bankStateCache;
	private final BankTagImporter importer;
	private final ItemNameIndex itemNameIndex;
	private final ClientThread clientThread;
	private final WikiGearSetupsConfig config;

	private final JTextField searchField = new JTextField();
	private final JLabel statusLabel = new JLabel(" ");
	private final DefaultListModel<String> resultsModel = new DefaultListModel<>();
	private final JList<String> resultsList = new JList<>(resultsModel);
	private final JComboBox<String> setupCombo = new JComboBox<>();
	private final JPanel previewPanel = new JPanel();
	private final JButton importButton = new JButton("Import as bank tag");

	private String currentPageTitle;
	private List<GearSetup> currentSetups;
	private SetupResolution currentResolution;
	private boolean updatingCombo;

	@Inject
	public WikiGearSetupsPanel(com.wikigearsetups.wiki.WikiClient wikiClient,
		OwnershipResolver ownershipResolver, BankStateCache bankStateCache,
		BankTagImporter importer, ItemNameIndex itemNameIndex,
		ClientThread clientThread, WikiGearSetupsConfig config)
	{
		this.wikiClient = wikiClient;
		this.ownershipResolver = ownershipResolver;
		this.bankStateCache = bankStateCache;
		this.importer = importer;
		this.itemNameIndex = itemNameIndex;
		this.clientThread = clientThread;
		this.config = config;

		setLayout(new BorderLayout(0, 6));
		setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));

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
			if (!e.getValueIsAdjusting() && resultsList.getSelectedValue() != null)
			{
				loadPage(resultsList.getSelectedValue());
			}
		});
		JScrollPane resultsScroll = new JScrollPane(resultsList);
		resultsScroll.setAlignmentX(LEFT_ALIGNMENT);
		center.add(resultsScroll);
		center.add(Box.createVerticalStrut(6));

		setupCombo.setAlignmentX(LEFT_ALIGNMENT);
		setupCombo.addActionListener(e ->
		{
			if (!updatingCombo)
			{
				resolveSelectedSetup();
			}
		});
		setupCombo.setVisible(false);
		center.add(setupCombo);
		center.add(Box.createVerticalStrut(6));

		previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
		previewPanel.setAlignmentX(LEFT_ALIGNMENT);
		center.add(previewPanel);

		add(center, BorderLayout.CENTER);

		importButton.setEnabled(false);
		importButton.addActionListener(e -> doImport());
		add(importButton, BorderLayout.SOUTH);
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
				resultsModel.clear();
				for (SearchResult r : results)
				{
					resultsModel.addElement(r.getTitle());
				}
				clearSetups();
				status(results.isEmpty() ? "No results" : results.size() + " results", null);
			}));
	}

	private void loadPage(String title)
	{
		status("Loading " + title + "…", null);
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
			SwingUtilities.invokeLater(() -> showSetups(candidate, setups));
		});
	}

	private void showSetups(String pageTitle, List<GearSetup> setups)
	{
		currentPageTitle = pageTitle;
		currentSetups = setups;

		updatingCombo = true;
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
		for (GearSetup setup : setups)
		{
			model.addElement(setup.getStyleLabel());
		}
		setupCombo.setModel(model);
		setupCombo.setVisible(true);
		updatingCombo = false;

		status(setups.size() + (setups.size() == 1 ? " setup on " : " setups on ") + pageTitle, null);
		resolveSelectedSetup();
	}

	private void resolveSelectedSetup()
	{
		int selected = setupCombo.getSelectedIndex();
		if (currentSetups == null || selected < 0 || selected >= currentSetups.size())
		{
			return;
		}
		GearSetup setup = currentSetups.get(selected);

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
			SetupResolution resolution = ownershipResolver.resolve(setup, owned,
				config.missingGearBehavior(), config.assumeSlayerTask());
			SwingUtilities.invokeLater(() -> showPreview(resolution, hasSnapshot));
		});
	}

	private void showPreview(SetupResolution resolution, boolean hasSnapshot)
	{
		currentResolution = resolution;
		previewPanel.removeAll();

		if (!hasSnapshot)
		{
			addRow("Open your bank once so owned gear is known", SUBSTITUTE, true);
		}

		int substitutes = 0;
		int goals = 0;
		for (SlotPick pick : resolution.getPicks())
		{
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

		importButton.setEnabled(!resolution.getPicks().isEmpty());

		StringBuilder summary = new StringBuilder()
			.append(resolution.getPicks().size()).append(" items");
		if (substitutes > 0)
		{
			summary.append(", ").append(substitutes).append(" alternatives");
		}
		if (goals > 0)
		{
			summary.append(", ").append(goals).append(" goals");
		}
		if (!resolution.getEmptySlots().isEmpty())
		{
			summary.append(", ").append(resolution.getEmptySlots().size()).append(" empty");
		}
		status(summary.toString(), null);

		previewPanel.revalidate();
		previewPanel.repaint();
	}

	private void doImport()
	{
		SetupResolution resolution = currentResolution;
		String pageTitle = currentPageTitle;
		if (resolution == null || pageTitle == null)
		{
			return;
		}
		String tag = TagNames.build(pageTitle, resolution.getSetup().getStyleLabel());
		importButton.setEnabled(false);
		clientThread.invokeLater(() ->
		{
			try
			{
				importer.importSetup(tag, resolution, config.layoutStyle());
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
		currentResolution = null;
		setupCombo.setVisible(false);
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
