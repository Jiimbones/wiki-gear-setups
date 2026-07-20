package com.wikigearsetups.bank;

import com.wikigearsetups.items.ItemNames;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Example loadouts name one concrete item per slot, which alone would lose
 * ownership personalization. Most raid pages also carry ranked
 * recommendation tables — each example item is looked up in those ladders
 * and its slot gains the ladder as fallback alternatives. The example item
 * stays first (owned → placed, unowned → the goal), so tier intent is
 * preserved while unowned slots degrade to the best gear the player has.
 *
 * The anchor lookup also picks the right style implicitly: a melee example
 * weapon is only found in the melee setup's weapon ladder.
 */
public final class ExampleGearEnricher
{
	private ExampleGearEnricher()
	{
	}

	public static GearSetup enrich(GearSetup example, List<GearSetup> rankedSetups)
	{
		if (rankedSetups == null || rankedSetups.isEmpty())
		{
			return example;
		}
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		for (Map.Entry<EquipSlot, List<RankedItem>> entry : example.getSlots().entrySet())
		{
			RankedItem anchor = entry.getValue().get(0);
			List<RankedItem> merged = new ArrayList<>();
			merged.add(anchor);
			List<RankedItem> ladder = findLadder(entry.getKey(), anchor, rankedSetups);
			if (ladder != null)
			{
				String anchorName = ItemNames.normalize(anchor.getName());
				for (RankedItem alternative : ladder)
				{
					if (!ItemNames.normalize(alternative.getName()).equals(anchorName))
					{
						merged.add(alternative);
					}
				}
			}
			slots.put(entry.getKey(), merged);
		}
		return new GearSetup(example.getStyleLabel(), slots);
	}

	/** The first ranked slot list containing the anchor item, or null. */
	private static List<RankedItem> findLadder(EquipSlot slot, RankedItem anchor, List<GearSetup> rankedSetups)
	{
		String anchorName = ItemNames.normalize(anchor.getName());
		for (GearSetup setup : rankedSetups)
		{
			List<RankedItem> list = setup.getSlots().get(slot);
			if (list == null)
			{
				continue;
			}
			for (RankedItem item : list)
			{
				if (ItemNames.normalize(item.getName()).equals(anchorName))
				{
					return list;
				}
			}
		}
		return null;
	}
}
