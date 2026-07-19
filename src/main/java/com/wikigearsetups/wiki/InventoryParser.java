package com.wikigearsetups.wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

import static com.wikigearsetups.wiki.GearSetupParser.findTemplateEnd;
import static com.wikigearsetups.wiki.GearSetupParser.splitTopLevel;
import static com.wikigearsetups.wiki.GearSetupParser.stripMarkup;
import static com.wikigearsetups.wiki.GearSetupParser.templateName;

/**
 * Extracts {{Inventory}} templates (28 numbered slot params). Labels come
 * from the nearest preceding tabber pane label or section heading, since the
 * template has no style param of its own.
 */
@Slf4j
public class InventoryParser
{
	private static final String TEMPLATE_NAME = "inventory";
	private static final int SLOTS = 28;

	/** Tabber pane openers: "<tabber>\nLabel=" or "|-|\nLabel=". */
	private static final Pattern PANE_LABEL = Pattern.compile("(?m)^(?:<tabber>|\\|-\\|)\\s*\\n([^=\\n|{}]+?)\\s*=");
	private static final Pattern HEADING = Pattern.compile("(?m)^={2,5}\\s*(.+?)\\s*={2,5}\\s*$");

	public List<InventorySetup> parse(String wikitext)
	{
		List<InventorySetup> setups = new ArrayList<>();
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
				InventorySetup setup = parseInventory(body, labelFor(wikitext, i, setups.size() + 1));
				if (setup != null)
				{
					setups.add(setup);
				}
				i = end;
			}
			else
			{
				i += 2;
			}
		}
		return setups;
	}

	private InventorySetup parseInventory(String templateBody, String label)
	{
		List<String> slots = new ArrayList<>(SLOTS);
		for (int s = 0; s < SLOTS; s++)
		{
			slots.add("");
		}

		int filled = 0;
		for (String part : splitTopLevel(templateBody, '|'))
		{
			int eq = GearSetupParser.indexOfTopLevel(part, '=');
			if (eq < 0)
			{
				continue;
			}
			String name = part.substring(0, eq).trim();
			String value = part.substring(eq + 1);
			int slot;
			try
			{
				slot = Integer.parseInt(name);
			}
			catch (NumberFormatException e)
			{
				continue; // align=, etc.
			}
			if (slot < 1 || slot > SLOTS)
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

		return filled == 0 ? null : new InventorySetup(label, slots);
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
