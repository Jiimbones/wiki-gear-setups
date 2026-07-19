package com.wikigearsetups.layout;

import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.wiki.EquipSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.client.plugins.banktags.tabs.Layout;

/**
 * Produces the bank tag Layout for a resolved setup. Deterministic so
 * re-imports never shuffle positions.
 *
 * EQUIPMENT_SCREEN mirrors the worn-equipment interface in the left half of
 * the 8-wide bank grid (head top-centre; cape/neck/ammo; weapon/body/shield;
 * legs; hands/feet/ring), leaving columns 4-7 free for future inventory
 * items. COMPACT packs one item per slot in EquipSlot declaration order.
 */
public final class LayoutBuilder
{
	private static final int BANK_WIDTH = 8;

	private static final Map<EquipSlot, Integer> EQUIPMENT_SCREEN_POS = new EnumMap<>(EquipSlot.class);

	static
	{
		EQUIPMENT_SCREEN_POS.put(EquipSlot.HEAD, 1);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.CAPE, BANK_WIDTH);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.NECK, BANK_WIDTH + 1);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.AMMO, BANK_WIDTH + 2);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.WEAPON, 2 * BANK_WIDTH);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.BODY, 2 * BANK_WIDTH + 1);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.SHIELD, 2 * BANK_WIDTH + 2);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.SPECIAL, 2 * BANK_WIDTH + 3);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.LEGS, 3 * BANK_WIDTH + 1);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.HANDS, 4 * BANK_WIDTH);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.FEET, 4 * BANK_WIDTH + 1);
		EQUIPMENT_SCREEN_POS.put(EquipSlot.RING, 4 * BANK_WIDTH + 2);
	}

	private LayoutBuilder()
	{
	}

	public static Layout build(String tag, List<SlotPick> picks, LayoutStyle style)
	{
		Layout layout = new Layout(tag);
		List<SlotPick> ordered = new ArrayList<>(picks);
		ordered.sort(Comparator.comparingInt(p -> p.getSlot().ordinal()));

		if (style == LayoutStyle.EQUIPMENT_SCREEN)
		{
			for (SlotPick pick : ordered)
			{
				layout.setItemAtPos(pick.getItemId(), EQUIPMENT_SCREEN_POS.get(pick.getSlot()));
			}
		}
		else
		{
			int pos = 0;
			for (SlotPick pick : ordered)
			{
				layout.setItemAtPos(pick.getItemId(), pos++);
			}
		}
		return layout;
	}
}
