package com.wikigearsetups.items;

import java.util.Set;
import lombok.Value;

/**
 * A wiki item name resolved to game item ids. primaryId is the exact-name
 * match used for display; acceptableIds is every variant that satisfies the
 * recommendation (charges, degradation states, recolors, upgrades).
 */
@Value
public class ResolvedItem
{
	String wikiName;
	int primaryId;
	Set<Integer> acceptableIds;
}
