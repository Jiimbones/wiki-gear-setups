package com.wikigearsetups;

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
}
