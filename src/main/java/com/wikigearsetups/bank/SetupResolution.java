package com.wikigearsetups.bank;

import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * A gear setup resolved against what the player owns. Slots the player has
 * nothing for land in emptySlots (unless config asked for goal items).
 * unresolvedNames holds wiki names the item index couldn't map — surfaced in
 * the preview so failures are never silent.
 */
@Value
public class SetupResolution
{
	GearSetup setup;
	List<SlotPick> picks;
	Map<EquipSlot, List<String>> unresolvedNames;
	List<EquipSlot> emptySlots;
}
