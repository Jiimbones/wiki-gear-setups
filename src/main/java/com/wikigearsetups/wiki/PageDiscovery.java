package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Gear setups usually live on a "/Strategies" subpage of the page users
 * actually search for; some raids keep curated loadouts on a
 * "Guide:&lt;name&gt; setups" page instead (Theatre of Blood). Candidate
 * titles are tried in order until one parses to a non-empty setup list —
 * a missing Guide page fails fast and falls through.
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
		// a curated Guide page outranks even an explicit subpage — search
		// returns "/Strategies" results, but ToB's real setups live on
		// "Guide:Theatre of Blood setups"; missing guides fail fast
		if (!base.startsWith("Guide:"))
		{
			candidates.add("Guide:" + base + " setups");
		}
		if (slash > 0)
		{
			candidates.add(title);
		}
		candidates.add(base + "/Strategies");
		candidates.add(base);
		return new ArrayList<>(candidates);
	}
}
