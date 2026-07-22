package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two kinds of wiki-name knowledge the variation mapping can't express:
 *
 * Generic names — the wiki writes "Imbued god cape" but no item carries that
 * name; any of the concrete god capes satisfies it.
 *
 * Upgrades — distinct items (separate variation groups, often differently
 * named) where owning the better or equivalent one satisfies a
 * recommendation for the lesser: a Divine rune pouch IS a rune pouch, an
 * Uncharged trident IS a trident of the seas. Chains resolve transitively
 * and must stay acyclic.
 */
public final class ItemAliases
{
	private static final Map<String, List<String>> GENERIC = new HashMap<>();
	private static final Map<String, List<String>> UPGRADES = new HashMap<>();
	private static final Map<String, List<String>> EQUIVALENT = new HashMap<>();

	private static final String[] GODS = {
		"Guthix", "Saradomin", "Zamorak", "Ancient", "Armadyl", "Bandos",
	};

	/** Diary rewards exist as tiers 1-4; the wiki writes the bare family name. */
	private static final int DIARY_TIERS = 4;

	static
	{
		// interchangeable within their tier: the wiki names one god cape but
		// any of them does the same job, and pages disagree on which to name
		equivalent("Imbued saradomin cape", "Imbued guthix cape", "Imbued zamorak cape",
			"Imbued saradomin max cape", "Imbued guthix max cape", "Imbued zamorak max cape");
		equivalent("Saradomin cape", "Guthix cape", "Zamorak cape");

		generic("God cape", "Saradomin cape", "Guthix cape", "Zamorak cape");
		generic("Imbued god cape",
			"Imbued saradomin cape", "Imbued guthix cape", "Imbued zamorak cape",
			"Imbued saradomin max cape", "Imbued guthix max cape", "Imbued zamorak max cape");
		generic("God book",
			"Holy book", "Book of balance", "Unholy book",
			"Book of law", "Book of war", "Book of darkness");

		// God ("blessed") d'hide: the wiki names the set, the game names the
		// god. Every piece is identical in stats, so any of them will do —
		// and pages write the same slot half a dozen different ways.
		godDhide("d'hide body", "Blessed body", "Blessed d'hide body",
			"Blessed dragonhide body", "Blessed dragonhide chestplate");
		godDhide("chaps", "Blessed chaps", "Blessed d'hide chaps",
			"Blessed dragonhide chaps", "Blessed dragonhide legs");
		godDhide("d'hide boots", "Blessed boots", "Blessed d'hide boots",
			"Blessed dragonhide boots");
		godDhide("coif", "Blessed coif", "Blessed d'hide coif", "Blessed dragonhide coif");
		godDhide("bracers", "Blessed bracers", "Blessed vambraces",
			"Blessed d'hide vambraces", "Blessed dragonhide vambraces");
		godDhide("d'hide shield", "Blessed shield", "Blessed d'hide shield",
			"Blessed dragonhide shield");
		godDhide("halo", "Halo");

		generic("Blessing", "Holy blessing", "Unholy blessing", "Peaceful blessing",
			"War blessing", "Honourable blessing", "Ancient blessing");

		// Diary rewards: the wiki names the family, the game numbers the tier.
		// Highest first so the ownership walk lands on the best one held.
		diaryTiers("Ardougne cloak");
		diaryTiers("Explorer's ring");
		diaryTiers("Rada's blessing");
		diaryTiers("Karamja gloves");
		diaryTiers("Desert amulet");
		diaryTiers("Morytania legs");
		diaryTiers("Fremennik sea boots");
		diaryTiers("Falador shield");
		diaryTiers("Varrock armour");

		// Wintertodt asks for "warm clothing" without naming anything. Best
		// first: the torch also lights braziers, so it earns the top spot.
		generic("Warm clothing",
			"Bruma torch", "Warm gloves", "Infernal cape", "Fire cape",
			"Clue hunter garb", "Clue hunter trousers", "Clue hunter gloves",
			"Clue hunter boots", "Clue hunter cloak",
			"Santa hat", "Woolly hat", "Woolly scarf", "Bomber jacket", "Bomber cap",
			"Yak-hide armour (top)", "Yak-hide armour (legs)",
			"Polar camo top", "Polar camo legs");

		upgrade("Rune pouch", "Rune pouch (l)", "Divine rune pouch", "Divine rune pouch (l)");
		upgrade("Divine rune pouch", "Divine rune pouch (l)");
		upgrade("Saradomin cape", "Imbued saradomin cape");
		upgrade("Guthix cape", "Imbued guthix cape");
		upgrade("Zamorak cape", "Imbued zamorak cape");
		upgrade("Toxic blowpipe", "Toxic blowpipe (empty)", "Blazing blowpipe", "Blazing blowpipe (empty)");
		upgrade("Blazing blowpipe", "Blazing blowpipe (empty)");
		// Dizana's quiver outclasses the assemblers
		upgrade("Ava's accumulator", "Ava's assembler");
		upgrade("Ava's assembler",
			"Masori assembler", "Dizana's quiver (uncharged)", "Dizana's quiver",
			"Blessed dizana's quiver", "Dizana's max cape");
		upgrade("Masori assembler", "Masori assembler max cape");
		upgrade("Dizana's quiver", "Blessed dizana's quiver", "Dizana's max cape");
		// charged weapons whose uncharged state carries a different name
		upgrade("Trident of the seas", "Uncharged trident", "Trident of the seas (full)");
		upgrade("Trident of the seas (e)", "Uncharged trident (e)", "Trident of the seas (full) (e)");
		upgrade("Trident of the swamp", "Uncharged toxic trident");
		upgrade("Trident of the swamp (e)", "Uncharged toxic trident (e)");
		upgrade("Toxic staff of the dead", "Toxic staff (uncharged)");
		// the slayer helmet is an assembled black mask
		upgrade("Black mask", "Slayer helmet");
		upgrade("Black mask (i)", "Slayer helmet (i)");
		upgrade("Void knight top", "Elite void top");
		upgrade("Void knight robe", "Elite void robe");
		upgrade("Salve amulet", "Salve amulet (e)", "Salve amulet (i)", "Salve amulet (ei)");
		upgrade("Salve amulet (e)", "Salve amulet (ei)");
		upgrade("Salve amulet (i)", "Salve amulet (ei)");
		// (r)/(i)/(ri) suffixes defeat the startsWith rule — "(ri)" doesn't
		// extend "(i)" textually even though it's the same ring, imbued+recoil
		upgrade("Ring of suffering", "Ring of suffering (r)", "Ring of suffering (i)", "Ring of suffering (ri)");
		upgrade("Ring of suffering (i)", "Ring of suffering (ri)");
		upgrade("Ring of suffering (r)", "Ring of suffering (ri)");
	}

	private static void generic(String name, String... concrete)
	{
		GENERIC.put(ItemNames.normalize(name), Arrays.asList(concrete));
	}

	/**
	 * Registers every way the pages write one god-armour slot against the six
	 * concrete gods' pieces ("Blessed d'hide body" and "Blessed body" both
	 * accept "Guthix d'hide body" and its five siblings).
	 */
	private static void godDhide(String piece, String... wikiNames)
	{
		List<String> concrete = new ArrayList<>(GODS.length);
		for (String god : GODS)
		{
			concrete.add(god + " " + piece);
		}
		for (String wikiName : wikiNames)
		{
			GENERIC.put(ItemNames.normalize(wikiName), concrete);
		}
	}

	private static void diaryTiers(String family)
	{
		List<String> tiers = new ArrayList<>(DIARY_TIERS);
		for (int tier = DIARY_TIERS; tier >= 1; tier--)
		{
			tiers.add(family + " " + tier);
		}
		GENERIC.put(ItemNames.normalize(family), tiers);
	}

	private static void upgrade(String name, String... betterItems)
	{
		UPGRADES.put(ItemNames.normalize(name), Arrays.asList(betterItems));
	}

	/** Registers a mutually interchangeable group: each member accepts the others. */
	private static void equivalent(String... members)
	{
		for (String member : members)
		{
			List<String> others = new ArrayList<>(members.length - 1);
			for (String other : members)
			{
				if (!other.equals(member))
				{
					others.add(other);
				}
			}
			EQUIVALENT.put(ItemNames.normalize(member), others);
		}
	}

	private ItemAliases()
	{
	}

	/** Concrete item names a generic wiki name stands for, or empty. */
	public static List<String> genericFor(String name)
	{
		List<String> concrete = GENERIC.get(ItemNames.normalize(name));
		return concrete == null ? Collections.emptyList() : concrete;
	}

	/** Additional item names that also satisfy a recommendation for {@code name}, or empty. */
	public static List<String> upgradesFor(String normalizedName)
	{
		List<String> upgrades = UPGRADES.get(normalizedName);
		return upgrades == null ? Collections.emptyList() : upgrades;
	}

	/** Same-tier interchangeable items for {@code name}, or empty. */
	public static List<String> equivalentsFor(String normalizedName)
	{
		List<String> equivalents = EQUIVALENT.get(normalizedName);
		return equivalents == null ? Collections.emptyList() : equivalents;
	}
}
