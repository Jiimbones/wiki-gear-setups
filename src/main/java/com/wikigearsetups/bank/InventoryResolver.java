package com.wikigearsetups.bank;

import com.wikigearsetups.WikiGearSetupsConfig.MissingGearBehavior;
import com.wikigearsetups.items.ItemResolver;
import com.wikigearsetups.items.ResolvedItem;
import com.wikigearsetups.wiki.InventorySetup;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves an inventory setup's 28 slots (and its rune pouch) against owned
 * items. Inventory has no ranked alternatives — a slot's item is either
 * owned (place the owned variant), missing (placed only under
 * SHOW_BEST_UNOWNED), or unrecognized (placeholder templates like
 * "Cheap food", reported not placed).
 */
@Singleton
public class InventoryResolver
{
	private final ItemResolver itemResolver;

	@Inject
	public InventoryResolver(ItemResolver itemResolver)
	{
		this.itemResolver = itemResolver;
	}

	public InventoryResolution resolve(InventorySetup setup, Set<Integer> ownedIds, MissingGearBehavior missingBehavior)
	{
		List<InventoryPick> picks = new ArrayList<>();
		List<InventoryPick> runePicks = new ArrayList<>();
		Set<String> unresolved = new LinkedHashSet<>();
		int[] missing = {0};

		List<String> slots = setup.getSlots();
		for (int i = 0; i < slots.size(); i++)
		{
			resolveOne(slots.get(i), i, ownedIds, missingBehavior, picks, unresolved, missing);
		}
		List<String> runes = setup.getRunes();
		for (int i = 0; i < runes.size(); i++)
		{
			resolveOne(runes.get(i), i, ownedIds, missingBehavior, runePicks, unresolved, missing);
		}

		return new InventoryResolution(setup, picks, runePicks, new ArrayList<>(unresolved), missing[0]);
	}

	private void resolveOne(String name, int slot, Set<Integer> ownedIds, MissingGearBehavior missingBehavior,
		List<InventoryPick> out, Set<String> unresolved, int[] missing)
	{
		if (name.isEmpty())
		{
			return;
		}
		Optional<ResolvedItem> resolved = itemResolver.resolve(name);
		if (!resolved.isPresent())
		{
			unresolved.add(name);
			return;
		}

		Integer ownedId = firstOwned(resolved.get(), ownedIds);
		if (ownedId != null)
		{
			out.add(new InventoryPick(slot, ownedId, true));
		}
		else
		{
			missing[0]++;
			if (missingBehavior == MissingGearBehavior.SHOW_BEST_UNOWNED)
			{
				out.add(new InventoryPick(slot, resolved.get().getPrimaryId(), false));
			}
		}
	}

	private static Integer firstOwned(ResolvedItem resolved, Set<Integer> ownedIds)
	{
		for (int id : resolved.getAcceptableIds())
		{
			if (ownedIds.contains(id))
			{
				return id;
			}
		}
		return null;
	}
}
