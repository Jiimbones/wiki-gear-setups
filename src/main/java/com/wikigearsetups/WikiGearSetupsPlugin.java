package com.wikigearsetups;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.wikigearsetups.bank.BankStateCache;
import com.wikigearsetups.items.ClientItemNameIndex;
import com.wikigearsetups.items.ItemNameIndex;
import com.wikigearsetups.ui.WikiGearSetupsPanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Wiki Gear Setups",
	description = "Import recommended gear setups from the OSRS Wiki as bank tag layouts, built from the gear you own",
	tags = {"bank", "tag", "layout", "gear", "equipment", "wiki", "setup"}
)
@PluginDependency(BankTagsPlugin.class)
public class WikiGearSetupsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private WikiGearSetupsConfig config;

	@Inject
	private ClientItemNameIndex itemNameIndex;

	@Inject
	private BankStateCache bankStateCache;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private WikiGearSetupsPanel panel;

	private NavigationButton navButton;

	@Override
	public void configure(Binder binder)
	{
		binder.bind(ItemNameIndex.class).to(ClientItemNameIndex.class);
	}

	@Override
	protected void startUp()
	{
		migrateDefaults();

		BufferedImage icon = ImageUtil.loadImageResource(WikiGearSetupsPlugin.class, "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Wiki Gear Setups")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		if (client.getGameState() == GameState.LOGGED_IN)
		{
			itemNameIndex.ensureBuilt();
			bankStateCache.loadSnapshot();
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			itemNameIndex.ensureBuilt();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// fires once the RS profile is known post-login, and on world hops
		// across profile boundaries (e.g. leagues) — reload the right snapshot
		bankStateCache.loadSnapshot();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			bankStateCache.updateFromContainer(event.getItemContainer());
		}
	}

	/**
	 * Pre-release installs stored OMIT under the old default; the intended
	 * default is now SHOW_BEST_UNOWNED. One-time flip, marker-guarded so a
	 * deliberate later choice of OMIT sticks.
	 */
	private void migrateDefaults()
	{
		String marker = configManager.getConfiguration(WikiGearSetupsConfig.GROUP, "migratedShowBestDefault");
		if (marker == null)
		{
			String stored = configManager.getConfiguration(WikiGearSetupsConfig.GROUP, "missingGearBehavior");
			if ("OMIT".equals(stored))
			{
				configManager.setConfiguration(WikiGearSetupsConfig.GROUP, "missingGearBehavior",
					WikiGearSetupsConfig.MissingGearBehavior.SHOW_BEST_UNOWNED);
			}
			configManager.setConfiguration(WikiGearSetupsConfig.GROUP, "migratedShowBestDefault", "1");
		}

		// keys from settings that no longer exist: the slayer choice moved into
		// the panel, and the disclaimer now shows once per session
		configManager.unsetConfiguration(WikiGearSetupsConfig.GROUP, "assumeSlayerTask");
		configManager.unsetConfiguration(WikiGearSetupsConfig.GROUP, "disclaimerAccepted");
	}

	@Provides
	WikiGearSetupsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiGearSetupsConfig.class);
	}
}
