package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

import static com.wikigearsetups.wiki.GearSetupParser.findTemplateEnd;
import static com.wikigearsetups.wiki.GearSetupParser.indexOfTopLevel;
import static com.wikigearsetups.wiki.GearSetupParser.namedParam;
import static com.wikigearsetups.wiki.GearSetupParser.splitTopLevel;
import static com.wikigearsetups.wiki.GearSetupParser.stripMarkup;
import static com.wikigearsetups.wiki.GearSetupParser.templateName;

/**
 * Extracts inventories: {{Inventory}} with its trailing {{Rune pouch}}, and
 * {{Loadout}}, which carries both in one template.
 *
 * Slots may be numbered ("|1=Twisted bow", Zulrah style) or positional
 * ("|Giant pouch |Large pouch |...", GotR style) — positional params count
 * empty cells too, preserving grid positions. Labels come from the nearest
 * preceding tabber pane label or section heading, since {{Inventory}} has no
 * style param of its own. A {{Rune pouch}} attaches to the inventory that
 * precedes it.
 *
 * {{Loadout}} numbers its 28 slots the same way and holds its runes in
 * "rune1"-"rune4". Its gear fields are not read: the {{Equipment}} panels
 * that sat beside these inventories before were never read either, and the
 * page's {{Recommended equipment}} is where its gear comes from.
 */
@Slf4j
public class InventoryParser
{
	private static final int SLOTS = 28;
	private static final int RUNE_SLOTS = 4;
	/**
	 * Numbered slots at which an unread template looks like an inventory. A
	 * real inventory fills far more than this; ordinary templates take a
	 * handful of numbered params at most.
	 */
	private static final int INVENTORY_SHAPED = 10;

	/**
	 * Joins the candidates of a choice template into one slot value. No item
	 * name contains it, so splitting back out is unambiguous.
	 */
	public static final String CHOICE_SEPARATOR = " / ";

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
			else if (name.equalsIgnoreCase("loadout"))
			{
				// named gear params ("head=") aren't numbers, so only the
				// 28 inventory slots fill the grid
				List<String> slots = parseSlots(body, SLOTS);
				if (slots != null)
				{
					Pending loadout = new Pending(loadoutLabel(wikitext, i, body, pending.size() + 1), slots);
					loadout.runes = loadoutRunes(body);
					pending.add(loadout);
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
		if (stripped.equalsIgnoreCase("placeholder"))
		{
			// a deliberately empty grid cell (Maniacal monkeys write "|16=Placeholder")
			return "";
		}
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
	 * Strips the two markers the template appends to a name: a stack size
	 * after a backslash ("Blood rune\16000", overwhelmingly in {{Rune pouch}})
	 * and a ";n" noted flag ("Iron ore;n" on restock inventories). Values
	 * carry both ("Shark\200;n"). No item name contains either character, so
	 * everything from the first one on is a marker, not part of the name.
	 */
	static String stripQuantity(String value)
	{
		int cut = value.length();
		int slash = value.indexOf('\\');
		if (slash >= 0)
		{
			cut = slash;
		}
		int semi = value.indexOf(';');
		if (semi >= 0 && semi < cut)
		{
			cut = semi;
		}
		return cut == value.length() ? value : value.substring(0, cut).trim();
	}

	/** The pane/heading that distinguishes this inventory, else "Inventory N". */
	static String labelFor(String wikitext, int templateStart, int index)
	{
		String label = SectionLabels.precedingLabel(wikitext, templateStart);
		return label != null ? label : "Inventory " + index;
	}

	/**
	 * A loadout names itself, but the page's own pane or heading label wins
	 * when there is one. Zulrah's loadouts sit under the same tab labels its
	 * {{Inventory}} templates did ("Magic Only", "Ranged Only"), so the
	 * dropdown reads exactly as it did before the page was converted;
	 * "loadoutname" fills in only where the page gives no label of its own.
	 */
	static String loadoutLabel(String wikitext, int templateStart, String body, int index)
	{
		String label = SectionLabels.precedingLabel(wikitext, templateStart);
		if (label != null)
		{
			return label;
		}
		String name = namedParam(body, "loadoutname");
		return name != null ? name : "Inventory " + index;
	}

	/** The loadout's rune pouch, "rune1"-"rune4", in pouch order. */
	private static List<String> loadoutRunes(String body)
	{
		List<String> runes = new ArrayList<>(RUNE_SLOTS);
		for (int r = 0; r < RUNE_SLOTS; r++)
		{
			runes.add("");
		}
		for (String part : splitTopLevel(body, '|'))
		{
			int eq = indexOfTopLevel(part, '=');
			if (eq < 0)
			{
				continue;
			}
			String name = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			if (name.length() == 5 && name.startsWith("rune")
				&& name.charAt(4) >= '1' && name.charAt(4) <= '0' + RUNE_SLOTS)
			{
				runes.set(name.charAt(4) - '1', cleanSlotValue(part.substring(eq + 1)));
			}
		}
		return compact(runes);
	}

	/** Whether a template holds an inventory this parser reads. */
	public static boolean isInventoryTemplate(String templateName)
	{
		return templateName.equalsIgnoreCase("inventory") || templateName.equalsIgnoreCase("loadout");
	}

	/**
	 * Templates on the page laid out like an inventory — ten or more numbered
	 * slots — that this parser does not read. {{Loadout}} arrived exactly this
	 * way: Zulrah's inventories moved into it and silently stopped importing.
	 * Empty means every inventory on the page is accounted for.
	 */
	public static Set<String> unrecognisedInventoryTemplates(String rawWikitext)
	{
		Set<String> unread = new TreeSet<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return unread;
		}
		String wikitext = GearSetupParser.normalizeLineEndings(rawWikitext);
		int i = 0;
		while ((i = wikitext.indexOf("{{", i)) >= 0)
		{
			int end = findTemplateEnd(wikitext, i);
			if (end > 0)
			{
				String body = wikitext.substring(i + 2, end - 2);
				String name = templateName(body);
				if (!isInventoryTemplate(name) && !name.equalsIgnoreCase("rune pouch")
					&& numberedParams(body) >= INVENTORY_SHAPED)
				{
					unread.add(name);
				}
			}
			// always step inside: inventories nest in tables and tabbers
			i += 2;
		}
		return unread;
	}

	private static int numberedParams(String body)
	{
		int count = 0;
		List<String> parts = splitTopLevel(body, '|');
		for (int p = 1; p < parts.size(); p++)
		{
			int eq = indexOfTopLevel(parts.get(p), '=');
			if (eq > 0 && parts.get(p).substring(0, eq).trim().matches("[0-9]+"))
			{
				count++;
			}
		}
		return count;
	}
}
