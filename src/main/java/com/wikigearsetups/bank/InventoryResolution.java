package com.wikigearsetups.bank;

import com.wikigearsetups.wiki.InventorySetup;
import java.util.List;
import lombok.Value;

/**
 * missingCount covers inventory slots and runes together. runePicks use
 * slot = index within the rune list (0-3), not a grid position.
 */
@Value
public class InventoryResolution
{
	InventorySetup setup;
	List<InventoryPick> picks;
	List<InventoryPick> runePicks;
	List<String> unresolvedNames;
	int missingCount;
}
