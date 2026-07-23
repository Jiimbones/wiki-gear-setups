package com.wikigearsetups.wiki;

import javax.annotation.Nullable;

/**
 * What kind of page a search result is, told from its title alone.
 *
 * A third of the pages carrying {{Recommended equipment}} aren't boss gear —
 * 15 are Slayer task pages, and there are prose guides too. Both are served
 * well but look identical to a boss page in the results list. A one-word tag
 * beside the result makes the well-served categories discoverable and warns
 * before someone opens a 42-style equipment guide.
 *
 * Title-only on purpose: search runs on the network path and must stay cheap,
 * so Combat Achievement task pages (which follow no title convention —
 * "Budget Cutter", "Morytania Only") are deliberately left unlabelled rather
 * than pay a category lookup per result.
 */
public enum PageKind
{
	SLAYER_TASK("Slayer task"),
	GUIDE("Guide"),
	NONE(null);

	@Nullable
	private final String label;

	PageKind(@Nullable String label)
	{
		this.label = label;
	}

	@Nullable
	public String getLabel()
	{
		return label;
	}

	public static PageKind of(String title)
	{
		if (title == null)
		{
			return NONE;
		}
		if (title.startsWith("Slayer task/"))
		{
			return SLAYER_TASK;
		}
		if (title.startsWith("Guide:") || title.contains("Guide/"))
		{
			return GUIDE;
		}
		return NONE;
	}
}
