package com.wikigearsetups.layout;

import com.wikigearsetups.bank.InventoryPick;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.wiki.EquipSlot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.plugins.banktags.tabs.Layout;

/**
 * Produces the bank tag Layout for one or more resolved gear setups plus an
 * optional inventory. Deterministic so re-imports never shuffle positions.
 *
 * EQUIPMENT_SCREEN mirrors the worn-equipment interface in columns 0-3
 * (head; cape/neck/ammo; weapon/body/shield/special; legs; hands/feet/ring),
 * one 5-row block per gear setup stacked vertically — multi-style content
 * (ToA/CoX/ToB) imports as one tab with a block per style. The inventory's
 * 28 wiki slots map onto columns 4-7, rows 0-6, preserving wiki order.
 *
 * An item appearing in several gear blocks is placed only once (you bring
 * one pair of boots); an inventory item already placed as gear is skipped
 * (it's the same switch). Duplicates WITHIN the inventory are kept — the
 * wiki uses repeated slots (karambwans, food) deliberately.
 *
 * COMPACT packs gear blocks as rows separated by a blank row, inventory last.
 */
public final class LayoutBuilder
{
	private static final int BANK_WIDTH = 8;
	private static final int ROWS_PER_BLOCK = 5;
	private static final int INVENTORY_COL = 4;

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

	/** Single-setup convenience. */
	public static Layout build(String tag, List<SlotPick> picks, LayoutStyle style)
	{
		return build(tag, Collections.singletonList(picks), Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList(), style);
	}

	/** Convenience without extras. */
	public static Layout build(String tag, List<List<SlotPick>> gearBlocks,
		List<InventoryPick> inventory, List<InventoryPick> runes, LayoutStyle style)
	{
		return build(tag, gearBlocks, inventory, runes, Collections.emptyList(), style);
	}

	public static Layout build(String tag, List<List<SlotPick>> gearBlocks,
		List<InventoryPick> inventory, List<InventoryPick> runes,
		List<Integer> extras, LayoutStyle style)
	{
		Layout layout = new Layout(tag);
		Set<Integer> placed = new HashSet<>();

		if (style == LayoutStyle.EQUIPMENT_SCREEN)
		{
			int blockRow = 0;
			for (List<SlotPick> block : gearBlocks)
			{
				for (SlotPick pick : sorted(block))
				{
					if (placed.add(pick.getItemId()))
					{
						layout.setItemAtPos(pick.getItemId(),
							blockRow * BANK_WIDTH + EQUIPMENT_SCREEN_POS.get(pick.getSlot()));
					}
				}
				blockRow += ROWS_PER_BLOCK;
			}
			// runes sit directly under the last equipment block, left side
			boolean runesPlaced = false;
			for (InventoryPick rune : runes)
			{
				if (placed.add(rune.getItemId()))
				{
					layout.setItemAtPos(rune.getItemId(), blockRow * BANK_WIDTH + rune.getSlot());
					runesPlaced = true;
				}
			}
			int inventoryRows = 0;
			Set<Integer> inventoryIds = new HashSet<>();
			for (InventoryPick pick : inventory)
			{
				if (!placed.contains(pick.getItemId()))
				{
					int row = pick.getSlot() / 4;
					int col = pick.getSlot() % 4;
					layout.setItemAtPos(pick.getItemId(), row * BANK_WIDTH + INVENTORY_COL + col);
					inventoryIds.add(pick.getItemId());
					inventoryRows = Math.max(inventoryRows, row + 1);
				}
			}
			placed.addAll(inventoryIds);
			// extras (owned alternates) flow below everything, after a blank row
			int extrasRow = Math.max(blockRow + (runesPlaced ? 1 : 0), inventoryRows) + 1;
			int pos = extrasRow * BANK_WIDTH;
			for (int extra : extras)
			{
				if (placed.add(extra))
				{
					layout.setItemAtPos(extra, pos++);
				}
			}
		}
		else
		{
			int pos = 0;
			for (List<SlotPick> block : gearBlocks)
			{
				int placedInBlock = 0;
				for (SlotPick pick : sorted(block))
				{
					if (placed.add(pick.getItemId()))
					{
						layout.setItemAtPos(pick.getItemId(), pos++);
						placedInBlock++;
					}
				}
				if (placedInBlock > 0)
				{
					pos = ((pos + BANK_WIDTH - 1) / BANK_WIDTH + 1) * BANK_WIDTH; // next row + blank row
				}
			}
			for (InventoryPick pick : inventory)
			{
				if (!placed.contains(pick.getItemId()))
				{
					layout.setItemAtPos(pick.getItemId(), pos++);
				}
			}
			for (InventoryPick rune : runes)
			{
				if (placed.add(rune.getItemId()))
				{
					layout.setItemAtPos(rune.getItemId(), pos++);
				}
			}
			if (!extras.isEmpty() && pos > 0)
			{
				pos = ((pos + BANK_WIDTH - 1) / BANK_WIDTH + 1) * BANK_WIDTH;
			}
			for (int extra : extras)
			{
				if (placed.add(extra))
				{
					layout.setItemAtPos(extra, pos++);
				}
			}
		}
		return layout;
	}

	private static List<SlotPick> sorted(List<SlotPick> picks)
	{
		List<SlotPick> ordered = new ArrayList<>(picks);
		ordered.sort(Comparator.comparingInt(p -> p.getSlot().ordinal()));
		return ordered;
	}
}
