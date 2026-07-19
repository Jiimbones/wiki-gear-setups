package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Extracts {{Recommended equipment}} templates from raw wikitext.
 *
 * Deliberately tolerant: a malformed slot value is logged and skipped, never
 * fatal for the page. Only {{plink}}-family targets count as items — bare
 * wiki links in slot values are prose (see docs/wiki-format.md).
 */
@Slf4j
public class GearSetupParser
{
	private static final String TEMPLATE_NAME = "recommended equipment";
	private static final Pattern SLOT_PARAM = Pattern.compile("([a-z]+)([0-9]+)");
	private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	private static final Pattern PIPED_LINK = Pattern.compile("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]");
	private static final Pattern PLAIN_LINK = Pattern.compile("\\[\\[([^\\]|]*)\\]\\]");
	private static final Pattern NESTED_TEMPLATE = Pattern.compile("\\{\\{[^{}]*\\}\\}");

	public List<GearSetup> parse(String wikitext)
	{
		List<GearSetup> setups = new ArrayList<>();
		if (wikitext == null || wikitext.isEmpty())
		{
			return setups;
		}

		int i = 0;
		while ((i = wikitext.indexOf("{{", i)) >= 0)
		{
			int end = findTemplateEnd(wikitext, i);
			if (end < 0)
			{
				i += 2;
				continue;
			}

			String body = wikitext.substring(i + 2, end - 2);
			if (templateName(body).equalsIgnoreCase(TEMPLATE_NAME))
			{
				GearSetup setup = parseSetup(body, setups.size() + 1);
				if (setup != null)
				{
					setups.add(setup);
				}
				i = end;
			}
			else
			{
				// step past the opening braces only, so templates nested
				// inside non-matching wrappers (e.g. tabber helpers) are found
				i += 2;
			}
		}
		return setups;
	}

	private GearSetup parseSetup(String templateBody, int index)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		String style = null;
		Map<EquipSlot, TreeMap<Integer, List<RankedItem>>> ranked = new EnumMap<>(EquipSlot.class);

		for (int p = 1; p < parts.size(); p++)
		{
			String part = parts.get(p);
			int eq = indexOfTopLevel(part, '=');
			if (eq < 0)
			{
				continue;
			}
			String name = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String value = part.substring(eq + 1);

			try
			{
				if (name.equals("style"))
				{
					String s = stripMarkup(value);
					if (!s.isEmpty())
					{
						style = s;
					}
					continue;
				}

				Matcher m = SLOT_PARAM.matcher(name);
				if (!m.matches())
				{
					continue;
				}
				EquipSlot slot = EquipSlot.fromWikiParam(m.group(1));
				if (slot == null)
				{
					continue;
				}
				int rank = Integer.parseInt(m.group(2));

				List<RankedItem> items = parseItems(value, rank);
				if (!items.isEmpty())
				{
					ranked.computeIfAbsent(slot, k -> new TreeMap<>()).put(rank, items);
				}
			}
			catch (RuntimeException e)
			{
				log.warn("Skipping malformed slot param '{}'", name, e);
			}
		}

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		int total = 0;
		for (Map.Entry<EquipSlot, TreeMap<Integer, List<RankedItem>>> e : ranked.entrySet())
		{
			List<RankedItem> flat = new ArrayList<>();
			for (List<RankedItem> tier : e.getValue().values())
			{
				flat.addAll(tier);
			}
			slots.put(e.getKey(), flat);
			total += flat.size();
		}

		if (total == 0)
		{
			return null;
		}
		return new GearSetup(style != null ? style : "Setup " + index, slots);
	}

	private List<RankedItem> parseItems(String value, int rank)
	{
		List<RankedItem> items = new ArrayList<>();
		String v = HTML_COMMENT.matcher(value).replaceAll("");

		int i = 0;
		while (i < v.length())
		{
			if (!v.startsWith("{{", i))
			{
				i++;
				continue;
			}
			int end = findTemplateEnd(v, i);
			if (end < 0)
			{
				break;
			}
			String inner = v.substring(i + 2, end - 2);
			String name = templateName(inner).toLowerCase(Locale.ROOT);

			if (name.equals("plink") || name.equals("plinkp") || name.equals("plinkt"))
			{
				String item = firstPositional(inner);
				if (item != null)
				{
					item = stripMarkup(item);
					if (!item.isEmpty() && !item.equalsIgnoreCase("n/a") && !item.equalsIgnoreCase("none"))
					{
						items.add(new RankedItem(item, rank, null));
					}
				}
			}
			else if (name.equals("efn") && !items.isEmpty())
			{
				String note = firstPositional(inner);
				if (note != null)
				{
					note = stripMarkup(note);
					if (!note.isEmpty())
					{
						RankedItem last = items.get(items.size() - 1);
						String merged = last.getNote() == null ? note : last.getNote() + "; " + note;
						items.set(items.size() - 1, new RankedItem(last.getName(), rank, merged));
					}
				}
			}
			i = end;
		}
		return items;
	}

	/**
	 * From the index of an opening "{{", returns the index just past its
	 * matching "}}", or -1 if unbalanced.
	 */
	static int findTemplateEnd(String s, int start)
	{
		int depth = 0;
		int i = start;
		while (i < s.length() - 1)
		{
			if (s.charAt(i) == '{' && s.charAt(i + 1) == '{')
			{
				depth++;
				i += 2;
			}
			else if (s.charAt(i) == '}' && s.charAt(i + 1) == '}')
			{
				depth--;
				i += 2;
				if (depth == 0)
				{
					return i;
				}
			}
			else
			{
				i++;
			}
		}
		return -1;
	}

	/** Template name: body text up to the first pipe (or whole body). */
	static String templateName(String templateBody)
	{
		int pipe = templateBody.indexOf('|');
		String name = pipe < 0 ? templateBody : templateBody.substring(0, pipe);
		return name.trim();
	}

	/** First non-empty positional (un-named) parameter of a template body, or null. */
	static String firstPositional(String templateBody)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		for (int i = 1; i < parts.size(); i++)
		{
			String part = parts.get(i);
			if (indexOfTopLevel(part, '=') < 0 && !part.trim().isEmpty())
			{
				return part.trim();
			}
		}
		return null;
	}

	/** Splits on sep, ignoring occurrences nested inside {{...}} or [[...]]. */
	static List<String> splitTopLevel(String s, char sep)
	{
		List<String> parts = new ArrayList<>();
		int braceDepth = 0;
		int bracketDepth = 0;
		StringBuilder cur = new StringBuilder();
		int i = 0;
		while (i < s.length())
		{
			if (s.startsWith("{{", i))
			{
				braceDepth++;
				cur.append("{{");
				i += 2;
			}
			else if (s.startsWith("}}", i))
			{
				braceDepth = Math.max(0, braceDepth - 1);
				cur.append("}}");
				i += 2;
			}
			else if (s.startsWith("[[", i))
			{
				bracketDepth++;
				cur.append("[[");
				i += 2;
			}
			else if (s.startsWith("]]", i))
			{
				bracketDepth = Math.max(0, bracketDepth - 1);
				cur.append("]]");
				i += 2;
			}
			else if (s.charAt(i) == sep && braceDepth == 0 && bracketDepth == 0)
			{
				parts.add(cur.toString());
				cur.setLength(0);
				i++;
			}
			else
			{
				cur.append(s.charAt(i));
				i++;
			}
		}
		parts.add(cur.toString());
		return parts;
	}

	/** Index of the first top-level occurrence of c, or -1. */
	static int indexOfTopLevel(String s, char c)
	{
		int braceDepth = 0;
		int bracketDepth = 0;
		int i = 0;
		while (i < s.length())
		{
			if (s.startsWith("{{", i))
			{
				braceDepth++;
				i += 2;
			}
			else if (s.startsWith("}}", i))
			{
				braceDepth = Math.max(0, braceDepth - 1);
				i += 2;
			}
			else if (s.startsWith("[[", i))
			{
				bracketDepth++;
				i += 2;
			}
			else if (s.startsWith("]]", i))
			{
				bracketDepth = Math.max(0, bracketDepth - 1);
				i += 2;
			}
			else
			{
				if (s.charAt(i) == c && braceDepth == 0 && bracketDepth == 0)
				{
					return i;
				}
				i++;
			}
		}
		return -1;
	}

	/**
	 * Reduces wiki markup to display text: [[a|b]] -> b, [[a]] -> a, nested
	 * templates dropped, br tags to spaces, whitespace collapsed.
	 */
	static String stripMarkup(String s)
	{
		String out = s;
		String prev;
		do
		{
			prev = out;
			out = NESTED_TEMPLATE.matcher(out).replaceAll("");
		}
		while (!out.equals(prev));
		out = PIPED_LINK.matcher(out).replaceAll("$1");
		out = PLAIN_LINK.matcher(out).replaceAll("$1");
		out = out.replaceAll("(?i)<br\\s*/?>", " ");
		out = out.replaceAll("''+", "");
		out = out.replaceAll("\\s+", " ");
		return out.trim();
	}
}
