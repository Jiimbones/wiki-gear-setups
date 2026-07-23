package com.wikigearsetups;

import com.wikigearsetups.layout.LayoutStyle;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WikiGearSetupsConfig.GROUP)
public interface WikiGearSetupsConfig extends Config
{
	String GROUP = "wikigearsetups";

	enum MissingGearBehavior
	{
		OMIT("Hide — only gear I own"),
		SHOW_BEST_UNOWNED("Show as a goal");

		private final String label;

		MissingGearBehavior(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "missingGearBehavior",
		name = "Unowned recommendations",
		description = "For a slot where you own none of the recommended items: "
			+ "'Show as a goal' fills it with the wiki's best pick, highlighted as unowned, "
			+ "so you can see what to aim for; 'Hide' leaves it empty so the tab holds only gear you own.",
		position = 0
	)
	default MissingGearBehavior missingGearBehavior()
	{
		return MissingGearBehavior.SHOW_BEST_UNOWNED;
	}

	@ConfigItem(
		keyName = "layoutStyle",
		name = "Layout shape",
		description = "Arrange imported tabs like the worn equipment screen (inventory-ready), or as packed rows",
		position = 1
	)
	default LayoutStyle layoutStyle()
	{
		return LayoutStyle.EQUIPMENT_SCREEN;
	}

	@ConfigItem(
		keyName = "defaultToWikiBis",
		name = "Start in Wiki BIS view",
		description = "Open each page with 'Show wiki BIS' already ticked, so setups show the wiki's ideal "
			+ "gear instead of adapting to your bank. Off shows the setup built from what you own.",
		position = 2
	)
	default boolean defaultToWikiBis()
	{
		return false;
	}

	@ConfigItem(
		keyName = "confirmOverwrite",
		name = "Confirm before overwriting",
		description = "Ask before an import replaces an existing bank tag. Turn off to import without the "
			+ "prompt — a tab you arranged by hand can't be recovered, so leave this on unless you re-import a lot.",
		position = 3
	)
	default boolean confirmOverwrite()
	{
		return true;
	}

	@ConfigItem(
		keyName = "includeExtras",
		name = "Extra owned gear",
		description = "Place owned alternatives and situational switches the wiki mentions below the main "
			+ "layout. Turn off for a tab of just the chosen loadout.",
		position = 4
	)
	default boolean includeExtras()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tagPrefix",
		name = "Tag name prefix",
		description = "Prepended to every tab this plugin creates (e.g. 'wgs') so they sort together in Bank "
			+ "Tags and never collide with your own tabs. Leave blank for no prefix.",
		position = 5
	)
	default String tagPrefix()
	{
		return "";
	}

}
