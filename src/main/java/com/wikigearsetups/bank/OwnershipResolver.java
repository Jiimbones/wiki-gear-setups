package com.wikigearsetups.bank;

import com.wikigearsetups.WikiGearSetupsConfig.MissingGearBehavior;
import com.wikigearsetups.items.ItemResolver;
import com.wikigearsetups.items.ResolvedItem;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Walks each slot's ranked alternatives (best first) and picks the first
 * recommendation the player can satisfy, recording the specific owned
 * variant id. Pure logic — callers supply the owned-id set (see
 * {@link BankStateCache#ownedItemIds()}).
 */
@Singleton
public class OwnershipResolver
{
	private final ItemResolver itemResolver;

	@Inject
	public OwnershipResolver(ItemResolver itemResolver)
	{
		this.itemResolver = itemResolver;
	}

	public SetupResolution resolve(GearSetup setup, Set<Integer> ownedIds, MissingGearBehavior missingBehavior,
		boolean assumeSlayerTask)
	{
		List<SlotPick> picks = new ArrayList<>();
		Map<EquipSlot, List<String>> unresolved = new EnumMap<>(EquipSlot.class);
		List<EquipSlot> emptySlots = new ArrayList<>();

		for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
		{
			EquipSlot slot = entry.getKey();
			List<RankedItem> alternatives = entry.getValue();

			SlotPick pick = null;
			RankedItem bestResolvable = null;
			ResolvedItem bestResolved = null;

			for (int i = 0; i < alternatives.size(); i++)
			{
				RankedItem candidate = alternatives.get(i);
				if (!assumeSlayerTask && isSlayerTaskConditional(candidate))
				{
					continue;
				}
				Optional<ResolvedItem> resolved = itemResolver.resolve(candidate.getName());
				if (!resolved.isPresent())
				{
					unresolved.computeIfAbsent(slot, k -> new ArrayList<>()).add(candidate.getName());
					continue;
				}
				if (bestResolvable == null)
				{
					bestResolvable = candidate;
					bestResolved = resolved.get();
				}

				Integer ownedId = firstOwned(resolved.get(), ownedIds);
				if (ownedId != null)
				{
					pick = new SlotPick(slot, candidate, ownedId, true, i == 0);
					break;
				}
			}

			if (pick != null)
			{
				picks.add(pick);
			}
			else if (missingBehavior == MissingGearBehavior.SHOW_BEST_UNOWNED && bestResolvable != null)
			{
				picks.add(new SlotPick(slot, bestResolvable, bestResolved.getPrimaryId(), false, true));
			}
			else
			{
				emptySlots.add(slot);
			}
		}

		return new SetupResolution(setup, picks, unresolved, emptySlots);
	}

	/**
	 * Wiki footnotes flag items that are only best-in-slot while on a slayer
	 * task (e.g. "If on a Zulrah slayer boss task" on the slayer helmet).
	 * When the user isn't assuming a task, such items are skipped entirely —
	 * they must satisfy neither ownership nor the goal-item fallback.
	 */
	static boolean isSlayerTaskConditional(RankedItem item)
	{
		String note = item.getNote();
		if (note == null)
		{
			return false;
		}
		String n = note.toLowerCase(java.util.Locale.ROOT);
		return n.contains("slayer") && n.contains("task");
	}

	/**
	 * The specific owned variant to place in the layout. acceptableIds
	 * iterates insertion order with the primary (exact-name) id first, so an
	 * owned exact match beats an owned variant.
	 */
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
