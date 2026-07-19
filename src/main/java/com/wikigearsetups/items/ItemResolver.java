package com.wikigearsetups.items;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemVariationMapping;

/**
 * Resolves a wiki item name to the set of item ids that satisfy the
 * recommendation.
 *
 * Variation groups (ItemVariationMapping) merge cosmetic recolors, charge
 * states AND upgrade tiers — e.g. plain and imbued black masks share one
 * group. Expansion is therefore filtered by name so a lesser tier never
 * satisfies a higher-tier recommendation:
 *  - equal after stripping charge decorations (charges/degradation), or
 *  - candidate starts with the wiki name (suffixed upgrades, e.g. "(e)"), or
 *  - candidate ends with the wiki name (recolor prefixes, e.g. "Black slayer helmet (i)").
 */
@Singleton
public class ItemResolver
{
	private final ItemNameIndex index;

	@Inject
	public ItemResolver(ItemNameIndex index)
	{
		this.index = index;
	}

	/**
	 * Empty when the name is unknown — callers should check
	 * {@code index.isReady()} to distinguish "unknown" from "index not built yet".
	 */
	public Optional<ResolvedItem> resolve(String wikiName)
	{
		String wiki = ItemNames.normalize(wikiName);
		List<Integer> primary = index.idsByName(wiki);
		if (primary.isEmpty())
		{
			return Optional.empty();
		}

		Set<Integer> acceptable = new LinkedHashSet<>(primary);
		String wikiStripped = ItemNames.stripChargeDecorations(wiki);

		for (int id : primary)
		{
			for (int variant : ItemVariationMapping.getVariations(ItemVariationMapping.map(id)))
			{
				String name = index.nameById(variant);
				if (name == null)
				{
					continue;
				}
				String candidate = ItemNames.normalize(name);
				if (ItemNames.stripChargeDecorations(candidate).equals(wikiStripped)
					|| candidate.startsWith(wiki)
					|| candidate.endsWith(wiki))
				{
					acceptable.add(variant);
				}
			}
		}

		return Optional.of(new ResolvedItem(wikiName, primary.get(0), acceptable));
	}
}
