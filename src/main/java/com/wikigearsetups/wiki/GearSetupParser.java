package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Extracts {{Recommended equipment}} templates from raw wikitext.
 *
 * Deliberately tolerant: a malformed slot value is logged and skipped, never
 * fatal for the page. Only {{plink}}-family targets count as items — bare
 * wiki links in slot values are prose (see docs/wiki-format.md).
 *
 * Tolerance must not mean silence, though: a parameter that names no slot is
 * skipped, and if the wiki adds a slot we don't know, its recommendations
 * vanish without trace. That is how the two-handed column went unread for
 * months. {@link #unrecognisedParams} reports what a page would lose.
 */
@Slf4j
public class GearSetupParser
{
	private static final String TEMPLATE_NAME = "recommended equipment";
	/**
	 * A slot name, then its rank: "head1", "weapon12", "2h3". The name may
	 * start with a digit — the two-handed column is "2h" — but must end in a
	 * letter, or the rank's digits would be swallowed into it.
	 */
	private static final Pattern SLOT_PARAM = Pattern.compile("([a-z0-9]*[a-z])([0-9]+)");
	/**
	 * Parameters that shape the table rather than fill it. The template
	 * documents only "style" and "bucket"; the rest are older layout options
	 * some pages still carry. None of them names gear.
	 */
	private static final Set<String> LAYOUT_PARAMS = new HashSet<>(Arrays.asList(
		"style", "bucket", "hidecol", "align", "rows", "class", "buttons"));
	private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	/** A balanced parenthetical qualifying the item that precedes it: "(on task)". */
	private static final Pattern PARENTHETICAL = Pattern.compile("\\(([^()]+)\\)");
	private static final Pattern REF_BODY =
		Pattern.compile("<ref[^>]*>(.*?)</ref>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern PIPED_LINK = Pattern.compile("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]");
	private static final Pattern PLAIN_LINK = Pattern.compile("\\[\\[([^\\]|]*)\\]\\]");
	private static final Pattern NESTED_TEMPLATE = Pattern.compile("\\{\\{[^{}]*\\}\\}");

	public List<GearSetup> parse(String rawWikitext)
	{
		List<GearSetup> setups = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return setups;
		}
		String wikitext = normalizeLineEndings(rawWikitext);

		for (int[] call : templateCalls(wikitext))
		{
			GearSetup setup = parseSetup(wikitext.substring(call[0] + 2, call[1] - 2), setups.size() + 1,
				SectionLabels.precedingLabel(wikitext, call[0]));
			if (setup != null)
			{
				setups.add(setup);
			}
		}
		return setups;
	}

	/**
	 * Parameter names on the page's {{Recommended equipment}} calls that are
	 * neither a slot nor a known layout option — what {@link #parse} would
	 * skip. Empty means every recommendation on the page was read.
	 */
	public static Set<String> unrecognisedParams(String rawWikitext)
	{
		return paramNames(rawWikitext, GearSetupParser::isUnrecognised);
	}

	/**
	 * Slot names written without a rank ("weapon = ..."). The template only
	 * reads "weapon1"-"weapon5", so these never show on the wiki page either,
	 * and skipping them keeps the import true to the page. They are still
	 * worth knowing about: usually a typo for a rank, and fixable on the wiki.
	 */
	public static Set<String> unrankedSlotParams(String rawWikitext)
	{
		return paramNames(rawWikitext, name -> EquipSlot.fromWikiParam(name) != null);
	}

	private static Set<String> paramNames(String rawWikitext, Predicate<String> keep)
	{
		Set<String> names = new TreeSet<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return names;
		}
		String wikitext = normalizeLineEndings(rawWikitext);
		for (int[] call : templateCalls(wikitext))
		{
			List<String> parts = splitTopLevel(wikitext.substring(call[0] + 2, call[1] - 2), '|');
			for (int p = 1; p < parts.size(); p++)
			{
				String name = paramName(parts.get(p));
				if (name != null && keep.test(name))
				{
					names.add(name);
				}
			}
		}
		return names;
	}

	/**
	 * Start and end of every {{Recommended equipment}} call, in page order.
	 * Steps past the opening braces of anything else only, so calls nested
	 * inside non-matching wrappers (e.g. tabber helpers) are still found.
	 */
	private static List<int[]> templateCalls(String wikitext)
	{
		List<int[]> calls = new ArrayList<>();
		int i = 0;
		while ((i = wikitext.indexOf("{{", i)) >= 0)
		{
			int end = findTemplateEnd(wikitext, i);
			if (end < 0)
			{
				i += 2;
				continue;
			}
			if (templateName(wikitext.substring(i + 2, end - 2)).equalsIgnoreCase(TEMPLATE_NAME))
			{
				calls.add(new int[]{i, end});
				i = end;
			}
			else
			{
				i += 2;
			}
		}
		return calls;
	}

	/** A named parameter's name, trimmed and lower-cased; null for a positional one. */
	private static String paramName(String part)
	{
		int eq = indexOfTopLevel(part, '=');
		return eq < 0 ? null : part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Neither a slot nor a layout option; layout options may name a column
	 * ("hidecol5"). A slot name missing its rank is not counted: the template
	 * ignores it too (see {@link #unrankedSlotParams}).
	 */
	private static boolean isUnrecognised(String name)
	{
		return !LAYOUT_PARAMS.contains(name.replaceFirst("[0-9]+$", ""))
			&& slotParam(name) == null
			&& EquipSlot.fromWikiParam(name) == null;
	}

	private static final class SlotRank
	{
		final EquipSlot slot;
		final int rank;

		SlotRank(EquipSlot slot, int rank)
		{
			this.slot = slot;
			this.rank = rank;
		}
	}

	/** The slot and rank a parameter fills ("2h3" is the weapon slot, rank 3), or null. */
	private static SlotRank slotParam(String name)
	{
		Matcher m = SLOT_PARAM.matcher(name);
		if (!m.matches())
		{
			return null;
		}
		EquipSlot slot = EquipSlot.fromWikiParam(m.group(1));
		if (slot == null)
		{
			return null;
		}
		try
		{
			return new SlotRank(slot, Integer.parseInt(m.group(2)));
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	private GearSetup parseSetup(String templateBody, int index, String paneLabel)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		String style = null;
		Map<EquipSlot, TreeMap<Integer, List<RankedItem>>> ranked = new EnumMap<>(EquipSlot.class);

		for (int p = 1; p < parts.size(); p++)
		{
			String part = parts.get(p);
			String name = paramName(part);
			if (name == null)
			{
				continue;
			}
			String value = part.substring(indexOfTopLevel(part, '=') + 1);

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

				SlotRank slotRank = slotParam(name);
				if (slotRank == null)
				{
					if (EquipSlot.fromWikiParam(name) != null)
					{
						log.debug("Ignoring '{}' with no rank; the wiki doesn't show it either", name);
					}
					else if (isUnrecognised(name))
					{
						log.debug("Skipping unrecognised {{Recommended equipment}} parameter '{}'", name);
					}
					continue;
				}

				List<RankedItem> items = parseItems(value, slotRank.rank);
				if (!items.isEmpty())
				{
					// two names can fill one slot at one rank — "weapon1" and
					// "2h1" are both rank-one weapons — so add rather than
					// replace, keeping both in the order the page lists them
					ranked.computeIfAbsent(slotRank.slot, k -> new TreeMap<>())
						.computeIfAbsent(slotRank.rank, k -> new ArrayList<>())
						.addAll(items);
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
		return new GearSetup(style != null ? style : "Setup " + index, slots, paneLabel);
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
					// a "#" means the target is a page section, never an item —
					// the real name is in txt=, or before the anchor
					List<String> fallbacks = new ArrayList<>();
					addIfPresent(fallbacks, namedParam(inner, "txt"));
					addIfPresent(fallbacks, namedParam(inner, "pic"));
					int anchor = item.indexOf('#');
					if (anchor >= 0)
					{
						String beforeAnchor = item.substring(0, anchor).trim();
						item = fallbacks.isEmpty() ? beforeAnchor : fallbacks.remove(0);
						addIfPresent(fallbacks, beforeAnchor);
					}
					// "+{{plinkp|Trouver parchment}}" marks a companion item
					// (lock the gear at Perdu), not a wearable alternative
					if (!item.isEmpty() && !item.equalsIgnoreCase("n/a") && !item.equalsIgnoreCase("none")
						&& !item.equalsIgnoreCase("trouver parchment") && !isCompanion(v, i))
					{
						items.add(new RankedItem(item, rank, annotationAfter(v, end), fallbacks));
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
						items.set(items.size() - 1,
							new RankedItem(last.getName(), rank, merged, last.getFallbackNames()));
					}
				}
			}
			i = end;
		}
		return items;
	}

	/**
	 * A companion item rather than a wearable alternative: written attached to
	 * the plus, "{{plink|Fighter torso}}+{{plinkp|Trouver parchment}}".
	 *
	 * The plus must touch the template. Pages also use a <em>spaced</em> plus
	 * to mean "and bring this too" — Fortis Colosseum lists a whole switch
	 * loadout that way ("Osmumten's fang + Bow of faerdhinen + Sanguinesti
	 * staff") — and treating those as companions silently dropped real gear
	 * from the slot.
	 */
	private static boolean isCompanion(String s, int templateStart)
	{
		return templateStart > 0 && s.charAt(templateStart - 1) == '+';
	}

	/**
	 * Plain text between this template and the next, as a note.
	 *
	 * Roughly half the pages qualify a recommendation with a bare
	 * parenthetical — "{{plink|Slayer helmet (i)}} (on task)" — rather than an
	 * {{efn}} footnote. Without this those conditions are invisible, and the
	 * item gets recommended unconditionally.
	 *
	 * Only balanced parentheses and &lt;ref&gt; bodies count, so separators
	 * ("/", "&lt;br&gt;") and the "(or:{{plinkp|…}}" alternative-list opener
	 * yield nothing.
	 */
	static String annotationAfter(String value, int templateEnd)
	{
		int next = value.indexOf("{{", templateEnd);
		String trailing = value.substring(templateEnd, next < 0 ? value.length() : next);
		if (trailing.isEmpty())
		{
			return null;
		}

		StringBuilder note = new StringBuilder();
		Matcher ref = REF_BODY.matcher(trailing);
		while (ref.find())
		{
			append(note, stripMarkup(ref.group(1)));
		}
		Matcher paren = PARENTHETICAL.matcher(REF_BODY.matcher(trailing).replaceAll(""));
		while (paren.find())
		{
			append(note, stripMarkup(paren.group(1)));
		}
		return note.length() == 0 ? null : note.toString();
	}

	private static void append(StringBuilder note, String text)
	{
		if (text != null && !text.trim().isEmpty())
		{
			if (note.length() > 0)
			{
				note.append("; ");
			}
			note.append(text.trim());
		}
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
	private static void addIfPresent(List<String> out, String value)
	{
		if (value != null && !value.isEmpty() && !out.contains(value))
		{
			out.add(value);
		}
	}

	/** Value of a named template param ("txt", "pic"), markup stripped, or null. */
	static String namedParam(String templateBody, String param)
	{
		for (String part : splitTopLevel(templateBody, '|'))
		{
			int eq = indexOfTopLevel(part, '=');
			if (eq > 0 && part.substring(0, eq).trim().equalsIgnoreCase(param))
			{
				String value = stripMarkup(part.substring(eq + 1));
				return value.isEmpty() ? null : value;
			}
		}
		return null;
	}

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
	/**
	 * Line-based patterns (tabber labels, headings, table markup) must not
	 * see stray carriage returns. The wiki API serves LF, but a fixture
	 * checked out with CRLF — or any future caller — must parse identically.
	 */
	static String normalizeLineEndings(String s)
	{
		return s.indexOf('\r') < 0 ? s : s.replace("\r\n", "\n").replace('\r', '\n');
	}

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
