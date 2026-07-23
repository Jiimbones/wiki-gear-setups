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

}
