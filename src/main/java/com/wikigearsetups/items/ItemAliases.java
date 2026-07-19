package com.wikigearsetups.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two kinds of wiki-name knowledge the variation mapping can't express:
 *
 * Generic names — the wiki writes "Imbued god cape" but no item carries that
 * name; any of the concrete god capes satisfies it.
 *
 * Upgrades — distinct items (separate variation groups) where owning the
 * better one satisfies a recommendation for the lesser: a Divine rune pouch
 * IS a rune pouch. Chains are resolved transitively and must stay acyclic.
 */
public final class ItemAliases
{
	private static final Map<String, List<String>> GENERIC = new HashMap<>();
	private static final Map<String, List<String>> UPGRADES = new HashMap<>();

	static
	{
		GENERIC.put("god cape", Arrays.asList(
			"Saradomin cape", "Guthix cape", "Zamorak cape"));
		GENERIC.put("imbued god cape", Arrays.asList(
			"Imbued saradomin cape", "Imbued guthix cape", "Imbued zamorak cape",
			"Imbued saradomin max cape", "Imbued guthix max cape", "Imbued zamorak max cape"));
		GENERIC.put("god book", Arrays.asList(
			"Holy book", "Book of balance", "Unholy book",
			"Book of law", "Book of war", "Book of darkness"));

		UPGRADES.put("rune pouch", Arrays.asList(
			"Rune pouch (l)", "Divine rune pouch", "Divine rune pouch (l)"));
		UPGRADES.put("divine rune pouch", Collections.singletonList(
			"Divine rune pouch (l)"));
		UPGRADES.put("saradomin cape", Collections.singletonList("Imbued saradomin cape"));
		UPGRADES.put("guthix cape", Collections.singletonList("Imbued guthix cape"));
		UPGRADES.put("zamorak cape", Collections.singletonList("Imbued zamorak cape"));
	}

	private ItemAliases()
	{
	}

	/** Concrete item names a generic wiki name stands for, or empty. */
	public static List<String> genericFor(String name)
	{
		List<String> concrete = GENERIC.get(ItemNames.normalize(name));
		return concrete == null ? Collections.emptyList() : concrete;
	}

	/** Additional item names that also satisfy a recommendation for {@code name}, or empty. */
	public static List<String> upgradesFor(String normalizedName)
	{
		List<String> upgrades = UPGRADES.get(normalizedName);
		return upgrades == null ? Collections.emptyList() : upgrades;
	}
}
