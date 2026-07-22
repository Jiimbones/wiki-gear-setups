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
		OMIT("Best owned only"),
		SHOW_BEST_UNOWNED("Show best unowned as goal");

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
		name = "Missing gear",
		description = "When you own no item for a slot: show the wiki's best recommendation as a goal placeholder, or omit the slot",
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
