package com.wikigearsetups.bank;

import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.RankedItem;
import lombok.Value;

/**
 * The outcome for one equipment slot: which recommendation was satisfied and
 * by which specific item id. owned=false means a goal item the player lacks
 * (only produced under MissingGearBehavior.SHOW_BEST_UNOWNED).
 */
@Value
public class SlotPick
{
	EquipSlot slot;
	RankedItem item;
	int itemId;
	boolean owned;
	boolean topRecommendation;
}
