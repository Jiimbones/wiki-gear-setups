package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Uses real item ids so the real ItemVariationMapping data (bundled with the
 * runelite client jar) drives variation expansion.
 */
public class ItemResolverTest
{
	// Barrows: karil's leathertop group = [4736, 4940..4944, 23632]
	private static final int KARILS_LEATHERTOP = 4736;
	private static final int KARILS_LEATHERTOP_100 = 4940;
	private static final int KARILS_LEATHERTOP_0 = 4944;
	private static final int KARILS_LEATHERTOP_LMS = 23632;

	// black mask group merges plain, charged, and imbued tiers
	private static final int BLACK_MASK = 8921;
	private static final int BLACK_MASK_10 = 8901;
	private static final int BLACK_MASK_I = 11784;
	private static final int BLACK_MASK_10_I = 11774;

	// slayer helmet group merges plain, imbued, and recolors
	private static final int SLAYER_HELMET = 11864;
	private static final int SLAYER_HELMET_I = 11865;
	private static final int BLACK_SLAYER_HELMET = 19639;
	private static final int BLACK_SLAYER_HELMET_I = 19641;

	// abyssal whip group includes recolors
	private static final int ABYSSAL_WHIP = 4151;
	private static final int VOLCANIC_ABYSSAL_WHIP = 12773;
	private static final int FROZEN_ABYSSAL_WHIP = 12774;

	private static class FakeIndex implements ItemNameIndex
	{
		private final Map<Integer, String> names;
		private final Map<String, List<Integer>> byName = new HashMap<>();

		FakeIndex(Map<Integer, String> names)
		{
			this.names = names;
			names.forEach((id, name) ->
				byName.computeIfAbsent(ItemNames.normalize(name), k -> new ArrayList<>()).add(id));
		}

		@Override
		public List<Integer> idsByName(String normalizedName)
		{
			return byName.getOrDefault(normalizedName, Collections.emptyList());
		}

		@Override
		public String nameById(int itemId)
		{
			return names.get(itemId);
		}

		@Override
		public boolean isReady()
		{
			return true;
		}
	}

	private static ItemResolver resolver(Object... idNamePairs)
	{
		Map<Integer, String> names = new HashMap<>();
		for (int i = 0; i < idNamePairs.length; i += 2)
		{
			names.put((Integer) idNamePairs[i], (String) idNamePairs[i + 1]);
		}
		return new ItemResolver(new FakeIndex(names));
	}

	@Test
	public void degradationStatesAreAccepted()
	{
		ItemResolver r = resolver(
			KARILS_LEATHERTOP, "Karil's leathertop",
			KARILS_LEATHERTOP_100, "Karil's leathertop 100",
			KARILS_LEATHERTOP_0, "Karil's leathertop 0");
		ResolvedItem item = r.resolve("Karil's leathertop").orElseThrow(AssertionError::new);
		assertEquals(KARILS_LEATHERTOP, item.getPrimaryId());
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_100));
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_0));
	}

	@Test
	public void sameNameDuplicateIsAcceptedViaVariations()
	{
		ItemResolver r = resolver(
			KARILS_LEATHERTOP, "Karil's leathertop",
			KARILS_LEATHERTOP_LMS, "Karil's leathertop");
		ResolvedItem item = r.resolve("Karil's leathertop").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP));
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_LMS));
	}

	@Test
	public void lowerTierNeverSatisfiesImbuedRecommendation()
	{
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_10, "Black mask (10)",
			BLACK_MASK_I, "Black mask (i)",
			BLACK_MASK_10_I, "Black mask (10) (i)");
		ResolvedItem item = r.resolve("Black mask (i)").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_I));
		assertTrue("charged imbued satisfies imbued", item.getAcceptableIds().contains(BLACK_MASK_10_I));
		assertFalse("plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_MASK));
		assertFalse("charged plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_MASK_10));
	}

	@Test
	public void upgradeTierSatisfiesPlainRecommendation()
	{
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_10, "Black mask (10)",
			BLACK_MASK_I, "Black mask (i)",
			BLACK_MASK_10_I, "Black mask (10) (i)");
		ResolvedItem item = r.resolve("Black mask").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_10));
		assertTrue("imbued upgrade satisfies plain", item.getAcceptableIds().contains(BLACK_MASK_I));
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_10_I));
	}

	@Test
	public void recolorsSatisfyButPlainDoesNotSatisfyImbued()
	{
		ItemResolver r = resolver(
			SLAYER_HELMET, "Slayer helmet",
			SLAYER_HELMET_I, "Slayer helmet (i)",
			BLACK_SLAYER_HELMET, "Black slayer helmet",
			BLACK_SLAYER_HELMET_I, "Black slayer helmet (i)");
		ResolvedItem item = r.resolve("Slayer helmet (i)").orElseThrow(AssertionError::new);
		assertTrue("recolored imbued satisfies imbued", item.getAcceptableIds().contains(BLACK_SLAYER_HELMET_I));
		assertFalse("plain must not satisfy imbued", item.getAcceptableIds().contains(SLAYER_HELMET));
		assertFalse("recolored plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_SLAYER_HELMET));
	}

	@Test
	public void whipRecolorsAccepted()
	{
		ItemResolver r = resolver(
			ABYSSAL_WHIP, "Abyssal whip",
			VOLCANIC_ABYSSAL_WHIP, "Volcanic abyssal whip",
			FROZEN_ABYSSAL_WHIP, "Frozen abyssal whip");
		ResolvedItem item = r.resolve("Abyssal whip").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(VOLCANIC_ABYSSAL_WHIP));
		assertTrue(item.getAcceptableIds().contains(FROZEN_ABYSSAL_WHIP));
	}

	@Test
	public void nameMatchingIsCaseAndWhitespaceInsensitive()
	{
		ItemResolver r = resolver(ABYSSAL_WHIP, "Abyssal whip");
		assertTrue(r.resolve("  abyssal   WHIP ").isPresent());
	}

	@Test
	public void unknownNameResolvesEmpty()
	{
		ItemResolver r = resolver(ABYSSAL_WHIP, "Abyssal whip");
		assertFalse(r.resolve("Sword of a thousand truths").isPresent());
	}

	@Test
	public void stripChargeDecorations()
	{
		assertEquals("black mask (i)", ItemNames.stripChargeDecorations("black mask (10) (i)"));
		assertEquals("karil's leathertop", ItemNames.stripChargeDecorations("karil's leathertop 100"));
		assertEquals("trident of the swamp (e)", ItemNames.stripChargeDecorations("trident of the swamp (e)"));
	}
}
