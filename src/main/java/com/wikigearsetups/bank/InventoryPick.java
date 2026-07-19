package com.wikigearsetups.bank;

import lombok.Value;

/** One inventory slot resolved to a concrete item id. slot is 0-based (0-27). */
@Value
public class InventoryPick
{
	int slot;
	int itemId;
	boolean owned;
}
