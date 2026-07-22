package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

import static com.wikigearsetups.wiki.GearSetupParser.findTemplateEnd;
import static com.wikigearsetups.wiki.GearSetupParser.indexOfTopLevel;
import static com.wikigearsetups.wiki.GearSetupParser.splitTopLevel;
import static com.wikigearsetups.wiki.GearSetupParser.stripMarkup;
import static com.wikigearsetups.wiki.GearSetupParser.templateName;

/**
 * Extracts {{Inventory}} templates and their trailing {{Rune pouch}}.
 *
 * Slots may be numbered ("|1=Twisted bow", Zulrah style) or positional
 * ("|Giant pouch |Large pouch |...", GotR style) — positional params count
 * empty cells too, preserving grid positions. Labels come from the nearest
 * preceding tabber pane label or section heading, since the template has no
 * style param of its own. A {{Rune pouch}} attaches to the inventory that
 * precedes it.
 */
@Slf4j
public class InventoryParser
{
	private static final int SLOTS = 28;
	private static final int RUNE_SLOTS = 4;

	/**
	 * Joins the candidates of a choice template into one slot value. No item
	 * name contains it, so splitting back out is unambiguous.
	 */
	public static final String CHOICE_SEPARATOR = " / ";

	/**
	 * Tabber pane openers: "<tabber>Label=", "|-|Label=", or the label on a
	 * later line — blank lines between the separator and the label are common
	 * and must not break the match.
	 */
	private static final Pattern PANE_LABEL =
		Pattern.compile("(?m)^(?:<tabber>|\\|-\\|)[ \\t]*(?:\\n[ \\t]*)*([^=\\n|{}]+?)[ \\t]*=");
	private static final Pattern HEADING = Pattern.compile("(?m)^={2,5}\\s*(.+?)\\s*={2,5}\\s*$");

	/**
	 * Headings that describe what the template <em>is</em> rather than which
	 * setup it belongs to. Pages routinely put "==Inventory==" immediately
	 * above every {{Inventory}}, which would otherwise give every inventory
	 * on the page the same label and make the dropdown unusable.
	 */
	private static final Set<String> GENERIC_LABELS = new HashSet<>(Arrays.asList(
		"inventory", "inventories", "equipment", "gear", "setup", "setups",
		"loadout", "loadouts", "items", "recommended equipment"));

	private static final class Pending
	{
		final String label;
		final List<String> slots;
		List<String> runes = Collections.emptyList();

		Pending(String label, List<String> slots)
		{
			this.label = label;
			this.slots = slots;
		}
	}

	public List<InventorySetup> parse(String rawWikitext)
	{
		List<Pending> pending = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return Collections.emptyList();
		}
		String wikitext = GearSetupParser.normalizeLineEndings(rawWikitext);

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
			String name = templateName(body);
			if (name.equalsIgnoreCase("inventory"))
			{
				List<String> slots = parseSlots(body, SLOTS);
				if (slots != null)
				{
					pending.add(new Pending(labelFor(wikitext, i, pending.size() + 1), slots));
				}
				i = end;
			}
			else if (name.equalsIgnoreCase("rune pouch"))
			{
				List<String> runes = parseSlots(body, RUNE_SLOTS);
				if (runes != null && !pending.isEmpty())
				{
					Pending last = pending.get(pending.size() - 1);
					if (last.runes.isEmpty())
					{
						last.runes = compact(runes);
					}
				}
				i = end;
			}
			else
			{
				i += 2;
			}
		}

		List<InventorySetup> setups = new ArrayList<>(pending.size());
		for (Pending p : pending)
		{
			setups.add(new InventorySetup(p.label, p.slots, p.runes));
		}
		return setups;
	}

	/**
	 * Numbered and positional params both fill slots; positional params count
	 * empty cells so grid positions survive. Returns null when nothing filled.
	 */
	private static List<String> parseSlots(String templateBody, int maxSlots)
	{
		List<String> slots = new ArrayList<>(maxSlots);
		for (int s = 0; s < maxSlots; s++)
		{
			slots.add("");
		}

		List<String> parts = splitTopLevel(templateBody, '|');
		int positional = 0;
		int filled = 0;
		for (int p = 1; p < parts.size(); p++)
		{
			String part = parts.get(p);
			int eq = indexOfTopLevel(part, '=');
			int slot;
			String value;
			if (eq < 0)
			{
				slot = ++positional;
				value = part;
			}
			else
			{
				String name = part.substring(0, eq).trim();
				try
				{
					slot = Integer.parseInt(name);
				}
				catch (NumberFormatException e)
				{
					continue; // align=, buttons=, etc.
				}
				value = part.substring(eq + 1);
			}
			if (slot < 1 || slot > maxSlots)
			{
				continue;
			}

			String item = cleanSlotValue(value);
			if (!item.isEmpty())
			{
				slots.set(slot - 1, item);
				filled++;
			}
		}
		return filled == 0 ? null : slots;
	}

	/** Drops empty cells, preserving order — rune lists have no grid meaning. */
	private static List<String> compact(List<String> slots)
	{
		List<String> out = new ArrayList<>();
		for (String s : slots)
		{
			if (!s.isEmpty())
			{
				out.add(s);
			}
		}
		return out;
	}

	/**
	 * Plain names pass through; a value that is only a placeholder template
	 * ({{Cheap food}}) yields that template's name so it surfaces as
	 * unrecognized downstream instead of vanishing silently.
	 */
	static String cleanSlotValue(String value)
	{
		String stripped = stripQuantity(stripMarkup(value));
		if (!stripped.isEmpty())
		{
			return stripped;
		}
		int t = value.indexOf("{{");
		if (t >= 0)
		{
			int end = findTemplateEnd(value, t);
			if (end > 0)
			{
				String body = value.substring(t + 2, end - 2);
				String choices = choiceList(body);
				return choices.isEmpty() ? templateName(body) : choices;
			}
		}
		return "";
	}

	/**
	 * Some templates carry their own candidates rather than naming one item:
	 * {{MinPrice|format=item|link=n|Manta ray|Tuna potato|Anglerfish|Dark
	 * crab}} means "whichever of these is cheapest". Their unnamed parameters
	 * are the candidate list, emitted joined so the resolver can treat it the
	 * same way as a hand-listed placeholder.
	 *
	 * Empty unless there are at least two candidates — a single unnamed
	 * parameter is far more likely to be an argument than a choice.
	 */
	static String choiceList(String templateBody)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		List<String> choices = new ArrayList<>();
		for (int p = 1; p < parts.size(); p++)
		{
			if (indexOfTopLevel(parts.get(p), '=') >= 0)
			{
				continue; // format=, link=, align= …
			}
			String choice = stripMarkup(parts.get(p));
			if (!choice.isEmpty())
			{
				choices.add(choice);
			}
		}
		return choices.size() < 2 ? "" : String.join(CHOICE_SEPARATOR, choices);
	}

	/**
	 * The template writes a stack size after a backslash ("Blood rune\16000",
	 * overwhelmingly in {{Rune pouch}}). No item name contains a backslash,
	 * so anything from the first one on is quantity, not name.
	 */
	static String stripQuantity(String value)
	{
		int slash = value.indexOf('\\');
		return slash < 0 ? value : value.substring(0, slash).trim();
	}

	/**
	 * The nearest preceding tabber pane label or heading that actually says
	 * which setup this is, else "Inventory N".
	 *
	 * "Nearest" alone is not enough: a page that writes "==Inventory==" above
	 * every template would label all of them "Inventory". Generic headings
	 * are therefore stepped over in favour of the enclosing pane label
	 * ("Melee", "Magic") further back, and only used if nothing better exists.
	 */
	static String labelFor(String wikitext, int templateStart, int index)
	{
		String before = wikitext.substring(0, templateStart);
		NavigableMap<Integer, String> candidates = new TreeMap<>();

		Matcher pane = PANE_LABEL.matcher(before);
		while (pane.find())
		{
			candidates.put(pane.start(), pane.group(1).trim());
		}
		Matcher heading = HEADING.matcher(before);
		while (heading.find())
		{
			candidates.put(heading.start(), stripMarkup(heading.group(1)));
		}

		String nearest = null;
		for (String label : candidates.descendingMap().values())
		{
			if (label.isEmpty())
			{
				continue;
			}
			if (nearest == null)
			{
				nearest = label;
			}
			if (!GENERIC_LABELS.contains(label.toLowerCase(Locale.ROOT)))
			{
				return label;
			}
		}
		return nearest != null ? nearest : "Inventory " + index;
	}
}
