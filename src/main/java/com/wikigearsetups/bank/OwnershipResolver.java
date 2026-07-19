package com.wikigearsetups.bank;

import com.wikigearsetups.WikiGearSetupsConfig.MissingGearBehavior;
import com.wikigearsetups.items.ItemResolver;
import com.wikigearsetups.items.ItemSets;
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
import java.util.function.IntPredicate;
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
			// tracked ignoring the set gate, so a slot whose every alternative
			// needs an unowned set still shows the top piece as a goal
			RankedItem bestAnyResolvable = null;
			ResolvedItem bestAnyResolved = null;

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
				if (bestAnyResolvable == null)
				{
					bestAnyResolvable = candidate;
					bestAnyResolved = resolved.get();
				}
				if (!setSatisfied(candidate, ownedIds))
				{
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

			if (bestResolvable == null)
			{
				bestResolvable = bestAnyResolvable;
				bestResolved = bestAnyResolved;
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
	 * Set pieces (void, blood moon, crystal armour…) are only recommended
	 * because of their set bonus — "always" pieces unconditionally, others
	 * only when the wiki footnote flags the recommendation as full-set. When
	 * the player can't complete the set (or lacks the paired weapon), the
	 * piece is skipped so the next best alternative wins instead.
	 */
	private boolean setSatisfied(RankedItem item, Set<Integer> ownedIds)
	{
		ItemSets.Requirement requirement = ItemSets.requirementFor(item.getName());
		if (requirement == null)
		{
			return true;
		}
		if (!requirement.isAlways() && !ItemSets.setConditionalNote(item.getNote()))
		{
			return true;
		}
		for (List<String> group : requirement.getOtherPieces())
		{
			if (!anyOwned(group, ownedIds))
			{
				return false;
			}
		}
		return requirement.getWeapons().isEmpty() || anyOwned(requirement.getWeapons(), ownedIds);
	}

	private boolean anyOwned(List<String> names, Set<Integer> ownedIds)
	{
		for (String name : names)
		{
			Optional<ResolvedItem> resolved = itemResolver.resolve(name);
			if (resolved.isPresent() && firstOwned(resolved.get(), ownedIds) != null)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * A two-handed weapon pick makes a shield pick contradictory — the wiki
	 * lists shields for the one-handed alternatives. Returns the picks with
	 * the shield removed, or the original list when nothing conflicts.
	 */
	public static List<SlotPick> dropShieldForTwoHanded(List<SlotPick> picks, IntPredicate isTwoHanded)
	{
		boolean twoHanded = false;
		for (SlotPick pick : picks)
		{
			if (pick.getSlot() == EquipSlot.WEAPON && isTwoHanded.test(pick.getItemId()))
			{
				twoHanded = true;
				break;
			}
		}
		if (!twoHanded)
		{
			return picks;
		}
		List<SlotPick> filtered = new ArrayList<>(picks.size());
		for (SlotPick pick : picks)
		{
			if (pick.getSlot() != EquipSlot.SHIELD)
			{
				filtered.add(pick);
			}
		}
		return filtered;
	}

	/**
	 * Owned items from the setup's ranked alternatives that were NOT picked —
	 * switches and spec weapons the player has banked. Wiki-table order,
	 * deduplicated, excluding {@code excludeIds} (the already-placed picks).
	 */
	public List<Integer> ownedAlternatives(GearSetup setup, Set<Integer> ownedIds,
		boolean assumeSlayerTask, Set<Integer> excludeIds)
	{
		List<Integer> extras = new ArrayList<>();
		for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
		{
			for (RankedItem candidate : entry.getValue())
			{
				if (!assumeSlayerTask && isSlayerTaskConditional(candidate))
				{
					continue;
				}
				if (!setSatisfied(candidate, ownedIds))
				{
					continue;
				}
				Optional<ResolvedItem> resolved = itemResolver.resolve(candidate.getName());
				if (!resolved.isPresent())
				{
					continue;
				}
				Integer ownedId = firstOwned(resolved.get(), ownedIds);
				if (ownedId != null && !excludeIds.contains(ownedId) && !extras.contains(ownedId))
				{
					extras.add(ownedId);
				}
			}
		}
		return extras;
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
