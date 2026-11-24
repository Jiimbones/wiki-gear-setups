package com.wikigearsetups.wiki;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * An equipment slot, and every name the wiki uses for it.
 *
 * {{Recommended equipment}} has a separate two-handed column, "2h1"-"2h5"
 * (since its December 2022 rework). A two-hander still occupies the weapon
 * slot, so "2h" is a weapon name here; whether a shield can sit beside it is
 * decided from the item's own stats when the setup resolves, not from which
 * column the wiki filed it under. "torso", "gloves" and "boots" are the names
 * {{Loadout}} uses, and some pages carry them on the recommendation template
 * too.
 */
public enum EquipSlot
{
	HEAD("head"),
	CAPE("cape"),
	NECK("neck"),
	AMMO("ammo"),
	WEAPON("weapon", "2h"),
	BODY("body", "torso"),
	SHIELD("shield"),
	LEGS("legs"),
	HANDS("hands", "gloves"),
	FEET("feet", "boots"),
	RING("ring"),
	SPECIAL("special");

	private final List<String> wikiParams;

	EquipSlot(String... wikiParams)
	{
		this.wikiParams = Arrays.asList(wikiParams);
	}

	public static EquipSlot fromWikiParam(String param)
	{
		String p = param.toLowerCase(Locale.ROOT);
		for (EquipSlot slot : values())
		{
			if (slot.wikiParams.contains(p))
			{
				return slot;
			}
		}
		return null;
	}
}
