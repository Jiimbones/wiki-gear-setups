package com.wikigearsetups.wiki;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One parsed {{Recommended equipment}} template: a combat-style label and,
 * per equipment slot, the recommended items ordered by rank ascending
 * (rank 1 = best), appearance order within a rank.
 *
 * {@code paneLabel} is the tabber pane or section heading the template sat
 * under, kept as a fallback for when {@code styleLabel} (the template's own
 * {@code |style=}) is missing or duplicated on the page.
 */
@Value
public class GearSetup
{
	String styleLabel;
	Map<EquipSlot, List<RankedItem>> slots;
	@Nullable
	String paneLabel;

	public GearSetup(String styleLabel, Map<EquipSlot, List<RankedItem>> slots)
	{
		this(styleLabel, slots, null);
	}

	public GearSetup(String styleLabel, Map<EquipSlot, List<RankedItem>> slots, @Nullable String paneLabel)
	{
		this.styleLabel = styleLabel;
		this.slots = slots;
		this.paneLabel = paneLabel;
	}
}
