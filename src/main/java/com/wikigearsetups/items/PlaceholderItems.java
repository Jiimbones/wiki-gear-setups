package com.wikigearsetups.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The wiki fills some inventory slots with placeholder templates instead of
 * concrete items ({{Cheap food}}, {{Cheap prayer}}). Each maps to concrete
 * candidates, best-owned-first semantics applied by the resolver — so a
 * "Cheap food" slot becomes whichever of these the player actually banks.
 */
public final class PlaceholderItems
{
	private static final Map<String, List<String>> CANDIDATES = new HashMap<>();

	static
	{
		CANDIDATES.put("cheap food", Arrays.asList(
			"Shark", "Monkfish", "Lobster", "Swordfish", "Tuna", "Trout", "Cake"));
		CANDIDATES.put("high healing food", Arrays.asList(
			"Anglerfish", "Manta ray", "Dark crab", "Shark"));
		CANDIDATES.put("cheap prayer", Arrays.asList(
			"Prayer potion(4)", "Super restore(4)"));
	}

	private PlaceholderItems()
	{
	}

	/** Concrete candidates for a placeholder name, or empty if not a known placeholder. */
	public static List<String> candidatesFor(String name)
	{
		List<String> candidates = CANDIDATES.get(ItemNames.normalize(name));
		return candidates == null ? Collections.emptyList() : candidates;
	}
}
