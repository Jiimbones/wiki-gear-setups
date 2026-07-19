package com.wikigearsetups.wiki;

import java.util.List;
import lombok.Value;

/**
 * One parsed {{Inventory}} template: a label (tabber pane / heading it sat
 * under), up to 28 slot item names in wiki slot order (index = slot - 1,
 * empty slots are empty strings), and the runes of the {{Rune pouch}}
 * template that followed it, if any. Values that were placeholder templates
 * ({{Cheap food}}) become the template's name and will simply fail item
 * resolution, surfacing as "unrecognized" in the preview.
 */
@Value
public class InventorySetup
{
	String label;
	List<String> slots;
	List<String> runes;
}
