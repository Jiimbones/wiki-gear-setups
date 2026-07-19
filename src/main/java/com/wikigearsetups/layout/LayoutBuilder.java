package com.wikigearsetups.layout;

import com.wikigearsetups.bank.SlotPick;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.client.plugins.banktags.tabs.Layout;

/**
 * Produces the bank tag Layout for a resolved setup: one item per equipment
 * slot, packed in EquipSlot declaration order (head, cape, neck, ammo,
 * weapon, body, shield, legs, hands, feet, ring, special). Deterministic so
 * re-imports never shuffle positions.
 */
public final class LayoutBuilder
{
	private LayoutBuilder()
	{
	}

	public static Layout build(String tag, List<SlotPick> picks)
	{
		List<SlotPick> ordered = new ArrayList<>(picks);
		ordered.sort(Comparator.comparingInt(p -> p.getSlot().ordinal()));

		Layout layout = new Layout(tag);
		int pos = 0;
		for (SlotPick pick : ordered)
		{
			layout.setItemAtPos(pick.getItemId(), pos++);
		}
		return layout;
	}
}
