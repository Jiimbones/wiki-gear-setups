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
		description = "When you own no item for a slot: omit the slot, or show the wiki's best recommendation as a goal item",
		position = 0
	)
	default MissingGearBehavior missingGearBehavior()
	{
		return MissingGearBehavior.OMIT;
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
		keyName = "assumeSlayerTask",
		name = "Assume on slayer task",
		description = "Include recommendations that only apply while on a slayer task, such as the slayer helmet",
		position = 2
	)
	default boolean assumeSlayerTask()
	{
		return false;
	}
}
