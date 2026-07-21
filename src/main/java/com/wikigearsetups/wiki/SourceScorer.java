package com.wikigearsetups.wiki;

import java.util.List;
import lombok.Value;

/**
 * Decides which of a page's two formats to offer first: the complete example
 * loadout columns, or the ranked recommendation tables. Pages differ — some
 * keep meticulous tabber setups and a token example table, others the
 * reverse — so the default is scored per page rather than fixed.
 *
 * Scoring is deliberately parse-time only: no item index, no game state, no
 * network. A default that changed depending on whether the player was logged
 * in would be impossible to explain, and an unexplainable default is worse
 * than an occasionally suboptimal one. It measures completeness, which is
 * observable; it does not try to judge editorial accuracy, which isn't.
 */
public final class SourceScorer
{
	/** Below this the two sources are close enough that neither is worth labelling. */
	private static final double DECISIVE_MARGIN = 2.0;

	private static final double INVENTORY_BONUS = 3.0;
	private static final double RUNES_BONUS = 1.0;
	private static final double DEPTH_BONUS = 2.0;
	private static final double DEPTH_THRESHOLD = 1.5;

	@Value
	public static class Result
	{
		double exampleScore;
		double rankedScore;
		boolean preferExamples;

		/** True when the winner leads by enough to be worth telling the user about. */
		public boolean isDecisive()
		{
			return Math.abs(exampleScore - rankedScore) >= DECISIVE_MARGIN;
		}
	}

	private SourceScorer()
	{
	}

	/**
	 * {@code rankedInventories} must already have example-owned inventories
	 * removed (see {@link ExampleSetupParser#withoutExampleInventories}),
	 * otherwise both sources score the inventory bonus on every raid page and
	 * the signal is worthless.
	 */
	public static Result score(List<ExampleSetup> examples, List<GearSetup> ranked,
		List<InventorySetup> rankedInventories)
	{
		double exampleScore = scoreExamples(examples);
		double rankedScore = scoreRanked(ranked, rankedInventories);
		// ties favour examples: complete concrete loadouts are the more
		// intuitive starting point, and enrichment gives them ranked depth anyway
		return new Result(exampleScore, rankedScore, exampleScore >= rankedScore);
	}

	private static double scoreExamples(List<ExampleSetup> examples)
	{
		if (examples.isEmpty())
		{
			return 0;
		}
		double slots = 0;
		int withInventory = 0;
		boolean runes = false;
		for (ExampleSetup example : examples)
		{
			slots += example.getGear().getSlots().size();
			InventorySetup inventory = example.getInventory();
			if (inventory != null)
			{
				withInventory++;
				runes |= !inventory.getRunes().isEmpty();
			}
		}
		double score = slots / examples.size();
		if (withInventory * 2 >= examples.size())
		{
			score += INVENTORY_BONUS;
		}
		return runes ? score + RUNES_BONUS : score;
	}

	private static double scoreRanked(List<GearSetup> ranked, List<InventorySetup> rankedInventories)
	{
		if (ranked.isEmpty())
		{
			return 0;
		}
		double slots = 0;
		double alternatives = 0;
		for (GearSetup setup : ranked)
		{
			slots += setup.getSlots().size();
			for (List<RankedItem> slot : setup.getSlots().values())
			{
				alternatives += slot.size();
			}
		}
		double score = slots / ranked.size();
		if (!rankedInventories.isEmpty())
		{
			score += INVENTORY_BONUS;
			for (InventorySetup inventory : rankedInventories)
			{
				if (!inventory.getRunes().isEmpty())
				{
					score += RUNES_BONUS;
					break;
				}
			}
		}
		// several alternatives per slot is real adaptivity the examples only
		// inherit second-hand through enrichment
		if (slots > 0 && alternatives / slots > DEPTH_THRESHOLD)
		{
			score += DEPTH_BONUS;
		}
		return score;
	}
}
