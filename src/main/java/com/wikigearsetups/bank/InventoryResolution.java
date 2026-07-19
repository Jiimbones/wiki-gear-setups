package com.wikigearsetups.bank;

import com.wikigearsetups.wiki.InventorySetup;
import java.util.List;
import lombok.Value;

@Value
public class InventoryResolution
{
	InventorySetup setup;
	List<InventoryPick> picks;
	List<String> unresolvedNames;
	int missingCount;
}
