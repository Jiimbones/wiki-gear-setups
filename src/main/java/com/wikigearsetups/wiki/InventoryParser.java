package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

	/** Tabber pane openers: "<tabber>\nLabel=", "|-|\nLabel=", or "|-|Label=". */
	private static final Pattern PANE_LABEL =
		Pattern.compile("(?m)^(?:<tabber>[ \\t]*\\n?[ \\t]*|\\|-\\|[ \\t]*\\n?[ \\t]*)([^=\\n|{}]+?)[ \\t]*=");
	private static final Pattern HEADING = Pattern.compile("(?m)^={2,5}\\s*(.+?)\\s*={2,5}\\s*$");

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

	public List<InventorySetup> parse(String wikitext)
	{
		List<Pending> pending = new ArrayList<>();
		if (wikitext == null || wikitext.isEmpty())
		{
			return Collections.emptyList();
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
		String stripped = stripMarkup(value);
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
				return templateName(value.substring(t + 2, end - 2));
			}
		}
		return "";
	}

	/** Nearest preceding tabber pane label or heading, else "Inventory N". */
	static String labelFor(String wikitext, int templateStart, int index)
	{
		String before = wikitext.substring(0, templateStart);
		int bestPos = -1;
		String best = null;

		Matcher pane = PANE_LABEL.matcher(before);
		while (pane.find())
		{
			bestPos = pane.start();
			best = pane.group(1).trim();
		}
		Matcher heading = HEADING.matcher(before);
		while (heading.find())
		{
			if (heading.start() > bestPos)
			{
				bestPos = heading.start();
				best = stripMarkup(heading.group(1));
			}
		}
		return best != null && !best.isEmpty() ? best : "Inventory " + index;
	}
}
