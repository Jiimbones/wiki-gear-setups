package com.wikigearsetups.items;

import java.util.Locale;

/** Name normalization shared by the index and the resolver. */
public final class ItemNames
{
	private ItemNames()
	{
	}

	/**
	 * Lowercase, straight apostrophes, collapsed whitespace, and no space
	 * before parentheses — the game is inconsistent ("Salve amulet(i)" but
	 * "Ring of suffering (i)") and the wiki always writes the space.
	 */
	public static String normalize(String name)
	{
		return name
			.replace('’', '\'')
			.toLowerCase(Locale.ROOT)
			.replaceAll("\\s+\\(", "(")
			.replaceAll("\\s+", " ")
			.trim();
	}

	/**
	 * Removes charge/degradation decorations so variants compare equal to
	 * their base: parenthesized numbers "(10)" and a trailing bare number
	 * ("Karil's leathertop 100"). Non-numeric parens like "(i)" are kept —
	 * they distinguish genuinely different items.
	 */
	public static String stripChargeDecorations(String normalizedName)
	{
		return normalizedName
			.replaceAll("\\(\\d+\\)", "")
			.replaceAll("\\s+\\d+$", "")
			.replaceAll("\\s+", " ")
			.trim();
	}
}
