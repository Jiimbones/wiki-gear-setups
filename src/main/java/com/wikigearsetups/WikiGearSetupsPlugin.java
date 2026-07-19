package com.wikigearsetups;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
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
	private WikiGearSetupsConfig config;

	@Override
	protected void startUp()
	{
		log.debug("Wiki Gear Setups started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Wiki Gear Setups stopped");
	}

	@Provides
	WikiGearSetupsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiGearSetupsConfig.class);
	}
}
