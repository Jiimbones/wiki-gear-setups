package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses the "example setups" idiom raid pages share: a wikitable whose
 * header cells label complete loadout columns, with parallel rows of
 * {{Equipment}}, {{Inventory}} and {{Rune pouch}} templates — CoX/ToA use
 * one table of tier columns, the ToB guide one table of role columns per
 * mode/tier section. Section headings above each table become the setup's
 * path so the UI can offer them as cascading choices.
 *
 * Table markup is only meaningful at template depth 0 — an {{Equipment}}
 * body is full of lines starting with '|' that are template params, not
 * cells.
 */
@Slf4j
public class ExampleSetupParser
{
	private static final Pattern HEADING = Pattern.compile("^(={2,6})\\s*(.+?)\\s*\\1\\s*$");

	private static final Map<String, EquipSlot> EQUIPMENT_PARAMS = new HashMap<>();

	static
	{
		EQUIPMENT_PARAMS.put("head", EquipSlot.HEAD);
		EQUIPMENT_PARAMS.put("cape", EquipSlot.CAPE);
		EQUIPMENT_PARAMS.put("neck", EquipSlot.NECK);
		EQUIPMENT_PARAMS.put("ammo", EquipSlot.AMMO);
		EQUIPMENT_PARAMS.put("weapon", EquipSlot.WEAPON);
		EQUIPMENT_PARAMS.put("torso", EquipSlot.BODY);
		EQUIPMENT_PARAMS.put("body", EquipSlot.BODY);
		EQUIPMENT_PARAMS.put("shield", EquipSlot.SHIELD);
		EQUIPMENT_PARAMS.put("legs", EquipSlot.LEGS);
		EQUIPMENT_PARAMS.put("gloves", EquipSlot.HANDS);
		EQUIPMENT_PARAMS.put("hands", EquipSlot.HANDS);
		EQUIPMENT_PARAMS.put("boots", EquipSlot.FEET);
		EQUIPMENT_PARAMS.put("feet", EquipSlot.FEET);
		EQUIPMENT_PARAMS.put("ring", EquipSlot.RING);
	}

	private final InventoryParser inventoryParser = new InventoryParser();

	public List<ExampleSetup> parse(String rawWikitext)
	{
		List<ExampleSetup> setups = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return setups;
		}
		String[] lines = GearSetupParser.normalizeLineEndings(rawWikitext).split("\n", -1);
		String h2 = null;
		String h3 = null;

		for (int i = 0; i < lines.length; i++)
		{
			String line = lines[i];
			Matcher heading = HEADING.matcher(line);
			if (heading.matches())
			{
				String text = GearSetupParser.stripMarkup(heading.group(2));
				if (heading.group(1).length() == 2)
				{
					h2 = text;
					h3 = null;
				}
				else
				{
					h3 = text;
				}
				continue;
			}
			if (line.startsWith("{|"))
			{
				List<String> path = new ArrayList<>();
				if (h2 != null)
				{
					path.add(h2);
				}
				if (h3 != null)
				{
					path.add(h3);
				}
				i = parseTable(lines, i + 1, path, setups);
			}
		}
		return setups;
	}

	/** Consumes table lines from {@code start}; returns the index of its closing line. */
	private int parseTable(String[] lines, int start, List<String> path, List<ExampleSetup> out)
	{
		List<String> headers = new ArrayList<>();
		List<List<StringBuilder>> rows = new ArrayList<>();
		StringBuilder current = null;
		int templateDepth = 0;
		int i = start;

		for (; i < lines.length; i++)
		{
			String line = lines[i];
			if (templateDepth > 0)
			{
				current.append('\n').append(line);
				templateDepth += depthDelta(line);
				continue;
			}
			if (line.startsWith("|}"))
			{
				break;
			}
			if (line.startsWith("|+"))
			{
				continue;
			}
			if (line.startsWith("|-"))
			{
				rows.add(new ArrayList<>());
				current = null;
				continue;
			}
			if (line.startsWith("!"))
			{
				for (String part : line.substring(1).split("!!"))
				{
					headers.add(part.trim());
				}
				current = null;
				continue;
			}
			if (line.startsWith("|"))
			{
				if (rows.isEmpty())
				{
					rows.add(new ArrayList<>());
				}
				List<StringBuilder> row = rows.get(rows.size() - 1);
				for (String part : line.substring(1).split("\\|\\|"))
				{
					current = new StringBuilder(part);
					row.add(current);
				}
				// only the last cell on the line can leave a template open
				templateDepth = Math.max(0, depthDelta(current.toString()));
				continue;
			}
			// continuation: multi-line header labels or free text in a cell
			if (!headers.isEmpty() && rows.isEmpty() && !headers.get(headers.size() - 1).isEmpty()
				&& !line.trim().isEmpty())
			{
				headers.set(headers.size() - 1, headers.get(headers.size() - 1) + " " + line.trim());
			}
			else if (current != null)
			{
				// a template may open here ("|" cell marker on its own line,
				// "{{Equipment" on the next) — track depth or its params get
				// misread as table cells
				current.append('\n').append(line);
				templateDepth = Math.max(0, templateDepth + depthDelta(line));
			}
		}

		emitColumns(headers, rows, path, out);
		return i;
	}

	private void emitColumns(List<String> headers, List<List<StringBuilder>> rows,
		List<String> path, List<ExampleSetup> out)
	{
		for (int col = 0; col < headers.size(); col++)
		{
			String label = GearSetupParser.stripMarkup(headers.get(col)).replaceAll("\\s+", " ").trim();
			if (label.isEmpty())
			{
				continue;
			}
			StringBuilder columnText = new StringBuilder();
			for (List<StringBuilder> row : rows)
			{
				if (col < row.size())
				{
					columnText.append(row.get(col)).append('\n');
				}
			}
			try
			{
				GearSetup gear = parseEquipment(columnText.toString(), label);
				if (gear == null)
				{
					continue;
				}
				List<InventorySetup> inventories = inventoryParser.parse(columnText.toString());
				InventorySetup inventory = inventories.isEmpty() ? null : inventories.get(0);
				out.add(new ExampleSetup(new ArrayList<>(path), label, gear, inventory));
			}
			catch (Exception e)
			{
				log.warn("Skipping malformed example setup column '{}'", label, e);
			}
		}
	}

	/** The first {{Equipment}} template in the text, or null. */
	private static GearSetup parseEquipment(String text, String label)
	{
		int i = 0;
		while ((i = text.indexOf("{{", i)) >= 0)
		{
			int end = GearSetupParser.findTemplateEnd(text, i);
			if (end < 0)
			{
				return null;
			}
			String inner = text.substring(i + 2, end - 2);
			if (!GearSetupParser.templateName(inner).equalsIgnoreCase("equipment"))
			{
				i = end;
				continue;
			}

			Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
			for (String part : GearSetupParser.splitTopLevel(inner, '|'))
			{
				int eq = GearSetupParser.indexOfTopLevel(part, '=');
				if (eq < 0)
				{
					continue;
				}
				EquipSlot slot = EQUIPMENT_PARAMS.get(part.substring(0, eq).trim().toLowerCase(Locale.ROOT));
				if (slot == null)
				{
					continue;
				}
				String item = GearSetupParser.stripMarkup(part.substring(eq + 1));
				if (!item.isEmpty() && !item.equalsIgnoreCase("none") && !item.equalsIgnoreCase("n/a"))
				{
					List<RankedItem> single = new ArrayList<>();
					single.add(new RankedItem(item, 1, null));
					slots.put(slot, single);
				}
			}
			return slots.isEmpty() ? null : new GearSetup(label, slots);
		}
		return null;
	}

	private static int depthDelta(String line)
	{
		int depth = 0;
		for (int i = 0; i < line.length() - 1; i++)
		{
			if (line.charAt(i) == '{' && line.charAt(i + 1) == '{')
			{
				depth++;
				i++;
			}
			else if (line.charAt(i) == '}' && line.charAt(i + 1) == '}')
			{
				depth--;
				i++;
			}
		}
		return depth;
	}
}
