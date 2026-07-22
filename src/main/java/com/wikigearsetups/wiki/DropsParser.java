package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls a boss's signature drops off its page for use as tab icons.
 *
 * Drop rows are {{DropsLine|name=X|rarity=1/1024|...}} grouped under
 * section headings. Only the "Uniques" and "Tertiary" sections are kept —
 * they hold the items people associate with the boss (its weapon pieces,
 * its pet) rather than the runes and herbs that make up the bulk of a
 * drop table.
 */
public class DropsParser
{
	private static final Pattern HEADING = Pattern.compile("^={2,6}\\s*(.+?)\\s*={2,6}\\s*$");
	private static final Pattern DROPS_LINE = Pattern.compile("\\{\\{DropsLine\\b([^{}]*(?:\\{\\{[^{}]*\\}\\}[^{}]*)*)\\}\\}",
		Pattern.CASE_INSENSITIVE);

	private static final Set<String> WANTED_SECTIONS = new LinkedHashSet<>();

	static
	{
		WANTED_SECTIONS.add("uniques");
		WANTED_SECTIONS.add("unique drops");
		WANTED_SECTIONS.add("tertiary");
	}

	/** Item names from the page's unique and tertiary drop sections, in page order. */
	public List<String> parse(String rawWikitext)
	{
		List<String> drops = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return drops;
		}

		String section = "";
		for (String line : GearSetupParser.normalizeLineEndings(rawWikitext).split("\n", -1))
		{
			Matcher heading = HEADING.matcher(line.trim());
			if (heading.matches())
			{
				section = GearSetupParser.stripMarkup(heading.group(1)).toLowerCase(Locale.ROOT).trim();
				continue;
			}
			if (!WANTED_SECTIONS.contains(section))
			{
				continue;
			}
			Matcher row = DROPS_LINE.matcher(line);
			while (row.find())
			{
				String name = GearSetupParser.namedParam(row.group(1), "name");
				if (name != null && !name.isEmpty() && !drops.contains(name))
				{
					drops.add(name);
				}
			}
		}
		return drops;
	}
}
