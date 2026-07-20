package com.wikigearsetups.wiki;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One complete example loadout column from a raid page: concrete worn gear
 * paired with its inventory (and rune pouch). {@code path} is the section
 * headings above the table (e.g. ["Theatre of Blood (Hard Mode)",
 * "Standard"]) and {@code label} the column header ("Magic", "Minimum
 * (0-100 Invocation)").
 */
@Value
public class ExampleSetup
{
	List<String> path;
	String label;
	GearSetup gear;
	@Nullable
	InventorySetup inventory;
}
