package com.wikigearsetups.wiki;

import java.util.Locale;

public enum EquipSlot
{
	HEAD("head"),
	CAPE("cape"),
	NECK("neck"),
	AMMO("ammo"),
	WEAPON("weapon"),
	BODY("body"),
	SHIELD("shield"),
	LEGS("legs"),
	HANDS("hands"),
	FEET("feet"),
	RING("ring"),
	SPECIAL("special");

	private final String wikiParam;

	EquipSlot(String wikiParam)
	{
		this.wikiParam = wikiParam;
	}

	public static EquipSlot fromWikiParam(String param)
	{
		String p = param.toLowerCase(Locale.ROOT);
		for (EquipSlot slot : values())
		{
			if (slot.wikiParam.equals(p))
			{
				return slot;
			}
		}
		return null;
	}
}
