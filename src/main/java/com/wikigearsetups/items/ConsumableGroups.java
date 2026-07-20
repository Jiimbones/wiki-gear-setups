package com.wikigearsetups.items;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interchangeable-consumable classes, best first. When a recommended
 * consumable isn't owned, the best owned member of its class substitutes —
 * an unowned Shark slot becomes your Monkfish rather than a goal item.
 * Groups are keyed by every member (dose-stripped), so "Prayer potion"
 * finds the restore group regardless of how the wiki wrote the dose.
 */
public final class ConsumableGroups
{
	private static final Map<String, List<String>> BY_MEMBER = new HashMap<>();

	static
	{
		group(
			"Anglerfish", "Dark crab", "Manta ray", "Sea turtle", "Shark",
			"Monkfish", "Potato with cheese", "Bass", "Lobster", "Swordfish",
			"Tuna", "Salmon", "Trout", "Cake");
		group("Super restore(4)", "Sanfew serum(4)", "Prayer potion(4)");
	}

	private static void group(String... members)
	{
		List<String> list = Arrays.asList(members);
		for (String member : members)
		{
			BY_MEMBER.put(key(member), list);
		}
	}

	private static String key(String name)
	{
		return ItemNames.stripChargeDecorations(ItemNames.normalize(name));
	}

	private ConsumableGroups()
	{
	}

	/** The item's class, best first, or empty if it isn't a grouped consumable. */
	public static List<String> groupFor(String name)
	{
		List<String> group = BY_MEMBER.get(key(name));
		return group == null ? Collections.emptyList() : group;
	}
}
