package com.wikigearsetups.ui;

import com.wikigearsetups.wiki.GearSetup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the core tri-brid loadout from a page's many setups: the first
 * setup listed for each combat style (the wiki orders general loadouts
 * before situational per-room ones). Purely label-driven, so any current
 * or future raid works without a hardcoded list — a page whose labels
 * carry no style words simply yields nothing and the option isn't offered.
 */
final class RaidLoadout
{
	private RaidLoadout()
	{
	}

	static List<GearSetup> bestPerStyle(List<GearSetup> setups)
	{
		Map<String, GearSetup> byStyle = new LinkedHashMap<>();
		for (GearSetup setup : setups)
		{
			for (String token : StyleTokens.tokens(setup.getStyleLabel()))
			{
				byStyle.putIfAbsent(token, setup);
			}
		}
		// a combined label ("Melee and ranged") can win two tokens — keep it once
		List<GearSetup> loadout = new ArrayList<>();
		for (GearSetup setup : byStyle.values())
		{
			if (!loadout.contains(setup))
			{
				loadout.add(setup);
			}
		}
		return loadout;
	}
}
