package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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

	private static final Pattern TRAILING_PAREN = Pattern.compile("\\s*\\([^)]*\\)$");
	private static final Pattern LEADING_QUANTITY = Pattern.compile("^\\d+\\s*[x×]\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_QUANTITY = Pattern.compile("\\s*[x×]\\s*\\d+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_NUMBER = Pattern.compile("\\s+\\d+$");

	/**
	 * Empty when the name is unknown — callers should check
	 * {@code index.isReady()} to distinguish "unknown" from "index not built yet".
	 *
	 * Wiki names diverge from in-game names in recurring ways, each tried in
	 * order until one resolves: quantity markers ("2 x Shark"), potion names
	 * without a dose ("Stamina potion" — the item is "Stamina potion(4)"),
	 * page disambiguators ("Teleport to house (tablet)"), plurals ("Sharks"),
	 * and generic names with no item of their own ("Imbued god cape").
	 */
	public Optional<ResolvedItem> resolve(String wikiName)
	{
		for (String candidate : candidateNames(wikiName))
		{
			Optional<ResolvedItem> resolved = resolveExact(candidate);
			if (!resolved.isPresent())
			{
				resolved = resolveGeneric(candidate);
			}
			if (resolved.isPresent())
			{
				return resolved;
			}
		}
		return Optional.empty();
	}

	private static List<String> candidateNames(String wikiName)
	{
		Set<String> names = new LinkedHashSet<>();
		String raw = wikiName.trim();
		names.add(raw);
		String base = LEADING_QUANTITY.matcher(raw).replaceFirst("");
		base = TRAILING_QUANTITY.matcher(base).replaceFirst("").trim();
		names.add(base);
		if (!base.endsWith(")"))
		{
			names.add(base + "(4)");
		}
		String noParen = TRAILING_PAREN.matcher(base).replaceFirst("");
		if (!noParen.isEmpty())
		{
			names.add(noParen);
		}
		// a bare trailing count with no "x" ("Coins 10000" -> "Coins"). Tried
		// after the exact name, so genuinely numbered items ("Ardougne cloak
		// 4") still resolve as themselves first.
		String noNumber = TRAILING_NUMBER.matcher(base).replaceFirst("");
		if (!noNumber.isEmpty() && !noNumber.equals(base))
		{
			names.add(noNumber);
		}
		// storage sacks are named by their closed form; the wiki writes the
		// open state ("Open herb sack" -> "Herb sack")
		if (base.regionMatches(true, 0, "Open ", 0, 5))
		{
			names.add(base.substring(5));
		}
		if (base.length() > 3 && base.endsWith("s"))
		{
			String singular = base.substring(0, base.length() - 1);
			names.add(singular);
			names.add(singular + "(4)");
		}
		names.remove("");
		return new ArrayList<>(names);
	}

	/** Generic wiki names ("Imbued god cape") accept any of their concrete items. */
	private Optional<ResolvedItem> resolveGeneric(String name)
	{
		List<String> concrete = ItemAliases.genericFor(name);
		if (concrete.isEmpty())
		{
			return Optional.empty();
		}
		Integer primary = null;
		Set<Integer> acceptable = new LinkedHashSet<>();
		for (String c : concrete)
		{
			Optional<ResolvedItem> resolved = resolveExact(c);
			if (resolved.isPresent())
			{
				if (primary == null)
				{
					primary = resolved.get().getPrimaryId();
				}
				acceptable.addAll(resolved.get().getAcceptableIds());
			}
		}
		return primary == null
			? Optional.empty()
			: Optional.of(new ResolvedItem(name, primary, acceptable));
	}

	private Optional<ResolvedItem> resolveExact(String wikiName)
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

		// trouver-locked "(l)" variants aren't always in the variation group
		acceptable.addAll(index.idsByName(ItemNames.normalize(wiki + " (l)")));

		// cosmetic ornament and recolour variants are the same item at the same
		// tier — an ornament kit ("Amulet of fury (or)") and an Echo recolour
		// ("Echo virtus mask"). ItemVariationMapping doesn't always group them,
		// so accept them by name. Both markers are unambiguous, so this never
		// crosses into a different item. (Overloaded dye prefixes like "Blood"
		// or "Ice" are handled per-item in ItemAliases instead.)
		acceptable.addAll(index.idsByName(ItemNames.normalize(wiki + " (or)")));
		acceptable.addAll(index.idsByName(ItemNames.normalize("Echo " + wiki)));

		// distinct upgrade items in other variation groups (Divine rune pouch
		// for "Rune pouch") also satisfy the recommendation
		for (String upgrade : ItemAliases.upgradesFor(wiki))
		{
			resolveExact(upgrade).ifPresent(r -> acceptable.addAll(r.getAcceptableIds()));
		}

		// same-tier interchangeables (the three imbued god capes). Looked up
		// by name rather than resolved, since the relation is bidirectional
		// and resolving would recurse forever
		for (String equivalent : ItemAliases.equivalentsFor(wiki))
		{
			acceptable.addAll(index.idsByName(ItemNames.normalize(equivalent)));
		}

		// a divine potion satisfies its base potion (never the reverse)
		if (wiki.contains("potion") && !wiki.startsWith("divine "))
		{
			resolveExact("divine " + wiki).ifPresent(r -> acceptable.addAll(r.getAcceptableIds()));
		}

		return Optional.of(new ResolvedItem(wikiName, primary.get(0), acceptable));
	}
}
