package com.wikigearsetups.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
		CANDIDATES.put("combo food", Arrays.asList(
			"Cooked karambwan", "Potato with cheese"));
	}

	private PlaceholderItems()
	{
	}

	/**
	 * Joins candidates a choice template listed for itself. Mirrors
	 * {@code InventoryParser.CHOICE_SEPARATOR} — kept here as a literal so the
	 * items package doesn't depend on the wiki package.
	 */
	private static final String CHOICE_SEPARATOR = " / ";

	/**
	 * Concrete candidates for a placeholder name, or empty if not a
	 * placeholder at all.
	 *
	 * Two sources: the table above, for templates that name a class of item
	 * without listing it ({{Cheap food}}), and the slot value itself, for
	 * templates that carry their own list ({{MinPrice|Manta ray|Tuna
	 * potato|…}}) and are joined by the parser.
	 */
	public static List<String> candidatesFor(String name)
	{
		List<String> candidates = CANDIDATES.get(ItemNames.normalize(name));
		if (candidates != null)
		{
			return candidates;
		}
		return name.contains(CHOICE_SEPARATOR)
			? Arrays.asList(name.split(Pattern.quote(CHOICE_SEPARATOR)))
			: Collections.emptyList();
	}
}
