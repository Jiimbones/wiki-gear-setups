package com.wikigearsetups;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.wikigearsetups.bank.BankStateCache;
import com.wikigearsetups.items.ClientItemNameIndex;
import com.wikigearsetups.items.ItemNameIndex;
import com.wikigearsetups.layout.LayoutScrollTrimmer;
import com.wikigearsetups.ui.WikiGearSetupsPanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ScriptPostFired;
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
	tags = {"bank", "tag", "layout", "gear", "equipment", "wiki", "setup",
		"bis", "best in slot", "loadout", "boss", "pvm", "raids"}
)
@PluginDependency(BankTagsPlugin.class)
public class WikiGearSetupsPlugin extends Plugin
{
	/**
	 * Shown in the disclaimer footer so a bug report can name the build.
	 *
	 * Hard-coded rather than derived from the build: the hub's `standard`
	 * build mode replaces build.gradle, so anything gradle injected at
	 * package time would be lost. Keep in step with build.gradle and
	 * runelite-plugin.properties when releasing.
	 */
	public static final String VERSION = "1.0";

	@Inject
	private Client client;

	@Inject
	private ClientItemNameIndex itemNameIndex;

	@Inject
	private BankStateCache bankStateCache;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WikiGearSetupsPanel panel;

	@Inject
	private LayoutScrollTrimmer layoutScrollTrimmer;

	private NavigationButton navButton;

	@Override
	public void configure(Binder binder)
	{
		binder.bind(ItemNameIndex.class).to(ClientItemNameIndex.class);
	}

	@Override
	protected void startUp()
	{
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
	public void onScriptPostFired(ScriptPostFired event)
	{
		// after the bank has finished building, so core Bank Tags has already
		// laid the tab out and the vanilla scrollbar height is in place
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			layoutScrollTrimmer.onBankBuilt();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			bankStateCache.updateFromContainer(event.getItemContainer());
		}
	}

	@Provides
	WikiGearSetupsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WikiGearSetupsConfig.class);
	}
}
