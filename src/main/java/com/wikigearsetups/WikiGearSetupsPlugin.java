package com.wikigearsetups;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.wikigearsetups.items.ClientItemNameIndex;
import com.wikigearsetups.items.ItemNameIndex;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Wiki Gear Setups",
	description = "Import recommended gear setups from the OSRS Wiki as bank tag layouts, built from the gear you own",
	tags = {"bank", "tag", "layout", "gear", "equipment", "wiki", "setup"}
)
public class WikiGearSetupsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private WikiGearSetupsConfig config;

	@Inject
	private ClientItemNameIndex itemNameIndex;

	@Override
	public void configure(Binder binder)
	{
		binder.bind(ItemNameIndex.class).to(ClientItemNameIndex.class);
	}

	@Override
	protected void startUp()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			itemNameIndex.ensureBuilt();
		}
	}

	@Override
	protected void shutDown()
	{
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			itemNameIndex.ensureBuilt();
		}
	}

	@Provides
	WikiGearSetupsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiGearSetupsConfig.class);
	}
}
