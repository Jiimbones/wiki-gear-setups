package com.wikigearsetups.wiki;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Works out which {{Inventory}} belongs to which {{Recommended equipment}}
 * from their order in the page.
 *
 * Document position is a far stronger signal than the labels are: pages
 * routinely head every inventory "==Inventory==", so half of them carry no
 * distinguishing label at all, while their order is unambiguous. Two shapes
 * cover the pages that do this —
 *
 * <pre>
 *   interleaved   E I E I E I    each setup is followed by its own inventory
 *   blocked       E E E I I I    all the setups, then all the inventories
 * </pre>
 *
 * — and both mean setup <i>i</i> owns inventory <i>i</i>. Anything else
 * (ragged orders like E E I E I) is genuinely ambiguous and gets no pairing,
 * leaving label matching to do its best.
 *
 * The template counts found here must match the number of setups the parsers
 * actually kept, otherwise the indices refer to different things — a template
 * that parsed to nothing is dropped from the model but still appears in the
 * page. A mismatch disables pairing rather than guessing.
 */
@Slf4j
public final class SetupPairing
{
	/** No inventory paired to any setup. */
	public static final int[] NONE = new int[0];

	private SetupPairing()
	{
	}

	/**
	 * Index of the inventory belonging to each setup, or -1 where unknown.
	 * Empty when the page's order carries no usable pairing.
	 */
	public static int[] pair(String rawWikitext, int setupCount, int inventoryCount)
	{
		if (rawWikitext == null || setupCount == 0 || inventoryCount == 0)
		{
			return NONE;
		}

		String order = templateOrder(GearSetupParser.normalizeLineEndings(rawWikitext));
		int setups = count(order, 'E');
		int inventories = count(order, 'I');
		if (setups != setupCount || inventories != inventoryCount)
		{
			// a template we skipped (empty, malformed) would shift every index
			log.debug("pairing skipped: page has {}E/{}I, parsers kept {}/{}",
				setups, inventories, setupCount, inventoryCount);
			return NONE;
		}

		// Interleaved order pairs by adjacency, so a spare template at the end
		// is harmless. Blocked order pairs by index alone, and with unequal
		// counts there is no way to tell which setup the spare belongs to.
		boolean pairable = isInterleaved(order)
			|| (isBlocked(order) && setupCount == inventoryCount);
		if (!pairable)
		{
			return NONE;
		}

		int[] pairing = new int[setupCount];
		Arrays.fill(pairing, -1);
		for (int i = 0; i < setupCount && i < inventoryCount; i++)
		{
			pairing[i] = i;
		}
		return pairing;
	}

	/** The page's equipment and inventory templates as a string like "EIEIEI". */
	static String templateOrder(String wikitext)
	{
		StringBuilder order = new StringBuilder();
		int i = 0;
		while ((i = wikitext.indexOf("{{", i)) >= 0)
		{
			int end = GearSetupParser.findTemplateEnd(wikitext, i);
			if (end < 0)
			{
				i += 2;
				continue;
			}
			String name = GearSetupParser.templateName(wikitext.substring(i + 2, end - 2));
			if (name.equalsIgnoreCase("recommended equipment"))
			{
				order.append('E');
				i = end;
			}
			else if (name.equalsIgnoreCase("inventory"))
			{
				order.append('I');
				i = end;
			}
			else
			{
				// step inside: raid tables nest {{Equipment}} and {{Inventory}}
				// within wikitable rows, and those still count
				i += 2;
			}
		}
		return order.toString();
	}

	/** "EIEIEI" — every setup immediately followed by its inventory. */
	static boolean isInterleaved(String order)
	{
		if (order.length() % 2 != 0)
		{
			return false;
		}
		for (int i = 0; i < order.length(); i += 2)
		{
			if (order.charAt(i) != 'E' || order.charAt(i + 1) != 'I')
			{
				return false;
			}
		}
		return order.length() > 0;
	}

	/** "EEEIII" — every setup before every inventory. */
	static boolean isBlocked(String order)
	{
		int firstInventory = order.indexOf('I');
		return firstInventory > 0
			&& order.indexOf('E', firstInventory) < 0
			&& order.lastIndexOf('I') == order.length() - 1;
	}

	private static int count(String s, char c)
	{
		int n = 0;
		for (int i = 0; i < s.length(); i++)
		{
			if (s.charAt(i) == c)
			{
				n++;
			}
		}
		return n;
	}
}
