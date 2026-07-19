package com.wikigearsetups.wiki;

import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * One parsed {{Recommended equipment}} template: a combat-style label and,
 * per equipment slot, the recommended items ordered by rank ascending
 * (rank 1 = best), appearance order within a rank.
 */
@Value
public class GearSetup
{
	String styleLabel;
	Map<EquipSlot, List<RankedItem>> slots;
}
