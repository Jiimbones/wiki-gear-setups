package com.wikigearsetups.layout;

import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.wiki.EquipSlot;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.banktags.tabs.TabManager;
import net.runelite.client.plugins.banktags.tabs.TagTab;

/**
 * Writes a resolved setup into core Bank Tags: tags each picked item,
 * saves the layout, and creates (or updates) the tag tab.
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

	private final TagManager tagManager;
	private final LayoutManager layoutManager;
	private final TabManager tabManager;
	private final PluginManager pluginManager;

	@Inject
	public BankTagImporter(TagManager tagManager, LayoutManager layoutManager,
		TabManager tabManager, PluginManager pluginManager)
	{
		this.tagManager = tagManager;
		this.layoutManager = layoutManager;
		this.tabManager = tabManager;
		this.pluginManager = pluginManager;
	}

	public boolean isBankTagsEnabled()
	{
		return pluginManager.getPlugins().stream()
			.filter(p -> p instanceof BankTagsPlugin)
			.findFirst()
			.map(pluginManager::isPluginEnabled)
			.orElse(false);
	}

	/** Imports the setup as bank tag {@code tag}. Client thread only. */
	public void importSetup(String tag, SetupResolution resolution, LayoutStyle layoutStyle)
	{
		if (!isBankTagsEnabled())
		{
			throw new IllegalStateException("The Bank Tags plugin must be enabled to import a setup");
		}
		if (resolution.getPicks().isEmpty())
		{
			throw new IllegalArgumentException("Nothing to import: no items resolved for this setup");
		}

		// overwrite semantics: clear any previous import under this name
		tagManager.removeTag(tag);
		for (SlotPick pick : resolution.getPicks())
		{
			tagManager.addTag(pick.getItemId(), tag, false);
		}

		Layout layout = LayoutBuilder.build(tag, resolution.getPicks(), layoutStyle);
		layoutManager.saveLayout(layout);

		int icon = iconFor(resolution);
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

		log.debug("Imported bank tag '{}': {} items", tag, resolution.getPicks().size());
	}

	/** Tab icon: the picked weapon if there is one, else the first pick. */
	static int iconFor(SetupResolution resolution)
	{
		return resolution.getPicks().stream()
			.filter(p -> p.getSlot() == EquipSlot.WEAPON)
			.findFirst()
			.map(SlotPick::getItemId)
			.orElseGet(() -> resolution.getPicks().isEmpty()
				? DEFAULT_ICON_ITEM_ID
				: resolution.getPicks().get(0).getItemId());
	}
}
