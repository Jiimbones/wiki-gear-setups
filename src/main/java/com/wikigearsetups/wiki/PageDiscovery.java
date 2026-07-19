package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Gear setups usually live on a "/Strategies" subpage of the page users
 * actually search for. Candidate titles are tried in order until one parses
 * to a non-empty setup list.
 */
public final class PageDiscovery
{
	private PageDiscovery()
	{
	}

	public static List<String> candidatesFor(String title)
	{
		String base = title;
		int slash = title.indexOf('/');
		if (slash > 0)
		{
			base = title.substring(0, slash);
		}

		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		if (slash > 0)
		{
			candidates.add(title); // an explicitly chosen subpage wins
		}
		candidates.add(base + "/Strategies");
		candidates.add(base);
		return new ArrayList<>(candidates);
	}
}
