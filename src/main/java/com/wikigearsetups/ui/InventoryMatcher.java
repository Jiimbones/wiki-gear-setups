package com.wikigearsetups.ui;

import java.util.List;
import java.util.Locale;

/**
 * Chooses which inventory belongs to a gear style, in decreasing order of
 * confidence:
 *
 * 1. an exact label match — "Ranged" gear with a "Ranged" inventory beats a
 *    "Max Magic &amp; Ranged" one that merely mentions the style;
 * 2. a shared combat-style token, so "Mage" finds "Magic switches";
 * 3. either label containing the other.
 */
final class InventoryMatcher
{
	private InventoryMatcher()
	{
	}

	/** Index of the best inventory for {@code styleLabel}, or -1 if none fits. */
	static int bestMatch(String styleLabel, List<String> inventoryLabels)
	{
		if (styleLabel == null || inventoryLabels.isEmpty())
		{
			return -1;
		}
		String style = normalize(styleLabel);

		for (int i = 0; i < inventoryLabels.size(); i++)
		{
			if (normalize(inventoryLabels.get(i)).equals(style))
			{
				return i;
			}
		}
		for (int i = 0; i < inventoryLabels.size(); i++)
		{
			if (StyleTokens.stylesOverlap(styleLabel, inventoryLabels.get(i)))
			{
				return i;
			}
		}
		for (int i = 0; i < inventoryLabels.size(); i++)
		{
			String label = normalize(inventoryLabels.get(i));
			if (!label.isEmpty() && (label.contains(style) || style.contains(label)))
			{
				return i;
			}
		}
		return -1;
	}

	private static String normalize(String label)
	{
		return label == null ? "" : label.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
