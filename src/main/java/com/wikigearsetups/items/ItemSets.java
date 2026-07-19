package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Items whose recommendation is only valid as part of a set (or alongside a
 * specific weapon). Two gating modes:
 *
 * always — the piece is pointless without its set (void; crystal armour
 * without a crystal bow), so the requirement applies to every
 * recommendation of it.
 *
 * note-flagged — the piece is reasonable standalone gear (barrows, moon
 * sets, obsidian, justiciar) and is gated only when the wiki footnote on
 * that particular recommendation says "full/complete ... set". The same
 * item can appear both gated (rank 2, with note) and ungated (rank 3,
 * standalone) on one page — Amoxliatl's Dual macuahuitl does exactly this.
 */
public final class ItemSets
{
	public static final class Requirement
	{
		private final boolean always;
		private final List<List<String>> otherPieces;
		private final List<String> weapons;

		private Requirement(boolean always, List<List<String>> otherPieces, List<String> weapons)
		{
			this.always = always;
			this.otherPieces = otherPieces;
			this.weapons = weapons;
		}

		public boolean isAlways()
		{
			return always;
		}

		/** Any-of groups, one per other slot in the set; all groups must be owned. */
		public List<List<String>> getOtherPieces()
		{
			return otherPieces;
		}

		/** Non-empty means one of these weapons must also be owned. */
		public List<String> getWeapons()
		{
			return weapons;
		}
	}

	private static final Map<String, Requirement> BY_PIECE = new HashMap<>();

	static
	{
		armourSet(true, Collections.emptyList(),
			group("Void melee helm", "Void ranger helm", "Void mage helm"),
			group("Void knight top", "Elite void top"),
			group("Void knight robe", "Elite void robe"),
			group("Void knight gloves"));

		// crystal armour only shines behind a crystal bow — per-piece, no set needed
		List<String> crystalBows = Arrays.asList(
			"Bow of faerdhinen", "Bow of faerdhinen (c)", "Bow of faerdhinen (inactive)", "Crystal bow");
		for (String piece : new String[]{"Crystal helm", "Crystal body", "Crystal legs"})
		{
			BY_PIECE.put(ItemNames.normalize(piece),
				new Requirement(true, Collections.emptyList(), crystalBows));
		}

		armourSet(false, Collections.emptyList(),
			group("Justiciar faceguard"), group("Justiciar chestguard"), group("Justiciar legguards"));
		armourSet(false, Collections.emptyList(),
			group("Obsidian helmet"), group("Obsidian platebody"), group("Obsidian platelegs"));

		barrows("Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff");
		barrows("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
		barrows("Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear");
		barrows("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow");
		barrows("Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers");
		barrows("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");

		moonSet("Blood moon helm", "Blood moon chestplate", "Blood moon tassets", "Dual macuahuitl");
		moonSet("Blue moon helm", "Blue moon chestplate", "Blue moon tassets", "Blue moon spear");
		moonSet("Eclipse moon helm", "Eclipse moon chestplate", "Eclipse moon tassets", "Eclipse atlatl");
	}

	private static List<String> group(String... names)
	{
		return Arrays.asList(names);
	}

	private static void barrows(String... pieces)
	{
		List<List<String>> groups = new ArrayList<>();
		for (String piece : pieces)
		{
			groups.add(group(piece));
		}
		armourSet(false, Collections.emptyList(), groups.toArray(new List[0]));
	}

	private static void moonSet(String helm, String chest, String legs, String weapon)
	{
		armourSet(false, Collections.emptyList(),
			group(helm), group(chest), group(legs), group(weapon));
	}

	/** Registers each member of each group with a requirement of all OTHER groups. */
	@SafeVarargs
	private static void armourSet(boolean always, List<String> weapons, List<String>... pieceGroups)
	{
		for (int i = 0; i < pieceGroups.length; i++)
		{
			List<List<String>> others = new ArrayList<>();
			for (int j = 0; j < pieceGroups.length; j++)
			{
				if (j != i)
				{
					others.add(pieceGroups[j]);
				}
			}
			Requirement requirement = new Requirement(always, others, weapons);
			for (String piece : pieceGroups[i])
			{
				BY_PIECE.put(ItemNames.normalize(piece), requirement);
			}
		}
	}

	private ItemSets()
	{
	}

	/** The set requirement covering this item, or null if it isn't a set piece. */
	public static Requirement requirementFor(String name)
	{
		return BY_PIECE.get(ItemNames.normalize(name));
	}

	/** True when a wiki footnote marks the recommendation as full-set-only. */
	public static boolean setConditionalNote(String note)
	{
		if (note == null)
		{
			return false;
		}
		String n = note.toLowerCase(Locale.ROOT);
		return n.contains("set") && (n.contains("full") || n.contains("complete"));
	}
}
