package com.wikigearsetups.layout;

import com.wikigearsetups.WikiGearSetupsConfig;
import com.wikigearsetups.bank.InventoryPick;
import com.wikigearsetups.bank.InventoryResolution;
import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.wiki.EquipSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.banktags.tabs.TabManager;
import net.runelite.client.plugins.banktags.tabs.TagTab;
import net.runelite.client.util.Text;

/**
 * Writes resolved setups into core Bank Tags: tags each picked item, saves
 * the layout, and creates (or updates) the tag tab. Multiple gear setups
 * import as one tab with a layout block per style.
 *
 * Must run on the client thread. Re-importing the same tag name overwrites:
 * the old tag is cleared first so removed items don't linger.
 *
 * Refuses to run while the Bank Tags plugin is disabled — TabManager's
 * in-memory tab list is only loaded by an enabled Bank Tags plugin, and
 * calling save() against an unloaded list would wipe the user's real tabs.
 */
@Slf4j
@Singleton
public class BankTagImporter
{
	static final int DEFAULT_ICON_ITEM_ID = 952; // spade

	/** Newline-separated tag names this plugin has written, per profile. */
	static final String IMPORTED_TAGS_KEY = "importedTags";

	private final TagManager tagManager;
	private final LayoutManager layoutManager;
	private final TabManager tabManager;
	private final PluginManager pluginManager;
	private final ConfigManager configManager;

	@Inject
	public BankTagImporter(TagManager tagManager, LayoutManager layoutManager,
		TabManager tabManager, PluginManager pluginManager, ConfigManager configManager)
	{
		this.tagManager = tagManager;
		this.layoutManager = layoutManager;
		this.tabManager = tabManager;
		this.pluginManager = pluginManager;
		this.configManager = configManager;
	}

	public boolean isBankTagsEnabled()
	{
		return pluginManager.getPlugins().stream()
			.filter(p -> p instanceof BankTagsPlugin)
			.findFirst()
			.map(pluginManager::isPluginEnabled)
			.orElse(false);
	}

	/** What is already sitting under a tag name, so the user can be asked first. */
	public enum Existing
	{
		/** Nothing — importing creates something new. */
		NONE,
		/** A tab this plugin wrote before; re-importing is the normal refresh path. */
		OURS,
		/** A tab or layout the plugin didn't create — almost certainly hand-made. */
		FOREIGN,
	}

	/**
	 * Whether importing under {@code tag} would replace something, and whose
	 * it is. Client thread only — reads the Bank Tags in-memory state.
	 *
	 * Ownership is tracked rather than inferred: a tag we have imported before
	 * is recorded in config, so anything else that exists is the player's.
	 */
	public Existing existing(String tag)
	{
		boolean present = tabManager.find(tag) != null || layoutManager.loadLayout(tag) != null;
		if (!present)
		{
			return Existing.NONE;
		}
		return importedTags().contains(tag) ? Existing.OURS : Existing.FOREIGN;
	}

	/**
	 * Whether this plugin created the tab under {@code tag}. Standardized on
	 * both sides because core hands back the tag as the player sees it.
	 */
	public boolean wasImported(String tag)
	{
		if (tag == null || tag.isEmpty())
		{
			return false;
		}
		String wanted = Text.standardize(tag);
		for (String imported : importedTags())
		{
			if (wanted.equals(Text.standardize(imported)))
			{
				return true;
			}
		}
		return false;
	}

	private Set<String> importedTags()
	{
		String stored = configManager.getConfiguration(WikiGearSetupsConfig.GROUP, IMPORTED_TAGS_KEY);
		if (stored == null || stored.isEmpty())
		{
			return new LinkedHashSet<>();
		}
		return new LinkedHashSet<>(Arrays.asList(stored.split("\n")));
	}

	private void rememberImported(String tag)
	{
		Set<String> tags = importedTags();
		if (tags.add(tag))
		{
			configManager.setConfiguration(WikiGearSetupsConfig.GROUP, IMPORTED_TAGS_KEY,
				String.join("\n", tags));
		}
	}

	/** Single-setup convenience. */
	public void importSetup(String tag, SetupResolution resolution, LayoutStyle layoutStyle)
	{
		importSetups(tag, Collections.singletonList(resolution), null,
			Collections.emptyList(), layoutStyle, null);
	}

	/** Convenience without extras. */
	public void importSetups(String tag, List<SetupResolution> resolutions,
		@Nullable InventoryResolution inventory, LayoutStyle layoutStyle, @Nullable Integer iconItemId)
	{
		importSetups(tag, resolutions, inventory, Collections.emptyList(), layoutStyle, iconItemId);
	}

	/**
	 * Imports the setups (and optional inventory) as bank tag {@code tag};
	 * {@code extraItemIds} (owned alternates) land below the layout. Client
	 * thread only.
	 */
	public void importSetups(String tag, List<SetupResolution> resolutions,
		@Nullable InventoryResolution inventory, List<Integer> extraItemIds,
		LayoutStyle layoutStyle, @Nullable Integer iconItemId)
	{
		if (!isBankTagsEnabled())
		{
			throw new IllegalStateException("The Bank Tags plugin must be enabled to import a setup");
		}

		Set<Integer> itemIds = new LinkedHashSet<>();
		List<List<SlotPick>> gearBlocks = new ArrayList<>();
		for (SetupResolution resolution : resolutions)
		{
			gearBlocks.add(resolution.getPicks());
			for (SlotPick pick : resolution.getPicks())
			{
				itemIds.add(pick.getItemId());
			}
		}
		List<InventoryPick> inventoryPicks = inventory == null
			? Collections.emptyList()
			: inventory.getPicks();
		List<InventoryPick> runePicks = inventory == null
			? Collections.emptyList()
			: inventory.getRunePicks();
		for (InventoryPick pick : inventoryPicks)
		{
			itemIds.add(pick.getItemId());
		}
		for (InventoryPick pick : runePicks)
		{
			itemIds.add(pick.getItemId());
		}
		itemIds.addAll(extraItemIds);

		if (itemIds.isEmpty())
		{
			throw new IllegalArgumentException("Nothing to import: no items resolved for this setup");
		}

		// overwrite semantics: clear any previous import under this name
		tagManager.removeTag(tag);
		for (int itemId : itemIds)
		{
			tagManager.addTag(itemId, tag, false);
		}

		Layout layout = LayoutBuilder.build(tag, gearBlocks, inventoryPicks, runePicks, extraItemIds, layoutStyle);
		layoutManager.saveLayout(layout);

		int icon = iconItemId != null ? iconItemId : iconFor(resolutions);
		TagTab existing = tabManager.find(tag);
		if (existing == null)
		{
			TagTab tab = new TagTab();
			tab.setTag(tag);
			tab.setIconItemId(icon);
			tabManager.add(tab);
		}
		else
		{
			existing.setIconItemId(icon);
		}
		tabManager.save();
		rememberImported(tag);

		log.debug("Imported bank tag '{}': {} items ({} gear blocks, {} inventory slots)",
			tag, itemIds.size(), gearBlocks.size(), inventoryPicks.size());
	}

	/** Tab icon: the first picked weapon across setups, else the first pick. */
	public static int iconFor(List<SetupResolution> resolutions)
	{
		for (SetupResolution resolution : resolutions)
		{
			for (SlotPick pick : resolution.getPicks())
			{
				if (pick.getSlot() == EquipSlot.WEAPON)
				{
					return pick.getItemId();
				}
			}
		}
		for (SetupResolution resolution : resolutions)
		{
			if (!resolution.getPicks().isEmpty())
			{
				return resolution.getPicks().get(0).getItemId();
			}
		}
		return DEFAULT_ICON_ITEM_ID;
	}
}
