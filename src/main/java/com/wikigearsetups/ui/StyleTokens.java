package com.wikigearsetups.ui;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Combat-style tokens extracted from free-text labels so a "Mage" gear setup
 * matches a "Magic switches" inventory. The wiki mixes mage/magic and
 * range/ranged/ranging freely.
 */
final class StyleTokens
{
	private StyleTokens()
	{
	}

	static Set<String> tokens(String label)
	{
		String s = label.toLowerCase(Locale.ROOT);
		Set<String> tokens = new HashSet<>();
		if (s.contains("melee"))
		{
			tokens.add("melee");
		}
		if (s.contains("range") || s.contains("ranging"))
		{
			tokens.add("ranged");
		}
		if (s.contains("mage") || s.contains("magic"))
		{
			tokens.add("magic");
		}
		return tokens;
	}

	static boolean stylesOverlap(String a, String b)
	{
		Set<String> tokens = tokens(a);
		tokens.retainAll(tokens(b));
		return !tokens.isEmpty();
	}
}
