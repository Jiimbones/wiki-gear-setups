package com.wikigearsetups.items;

import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Locks in that a recommendation for a base item accepts the player's owned
 * variant of it. Most of these ride RuneLite's {@link
 * net.runelite.client.game.ItemVariationMapping} — the ids below are real
 * members of the same variation group, and the resolver's startsWith/endsWith
 * name filter keeps the same-base variant. The Echo recolours do NOT share a
 * variation group, so they exercise the explicit prefix rule in ItemResolver.
 *
 * These matter because a throwaway audit once flagged all of them as "gaps"
 * — but that ran on a fake index with no variation data. With the real ids a
 * live client uses, they resolve. This test pins that, so the mapping-backed
 * behaviour can't silently regress.
 */
public class VariantAcceptanceTest
{
	private static Set<Integer> accepted(ItemNameIndex index, String wikiName)
	{
		return new ItemResolver(index).resolve(wikiName).get().getAcceptableIds();
	}

	/** Imbued rings — real variation-group members (base id / imbued id). */
	@Test
	public void imbuedRingsSatisfyTheBaseRing()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			6737, "Berserker ring", 11773, "Berserker ring (i)",
			6733, "Archers ring", 11771, "Archers ring (i)",
			6731, "Seers ring", 11770, "Seers ring (i)");

		assertTrue(accepted(index, "Berserker ring").contains(11773));
		assertTrue(accepted(index, "Archers ring").contains(11771));
		assertTrue(accepted(index, "Seers ring").contains(11770));
	}

	@Test
	public void enchantedAndImbuedWeaponsSatisfyTheBase()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			4170, "Slayer's staff", 21255, "Slayer's staff (e)",
			861, "Magic shortbow", 12788, "Magic shortbow (i)");

		assertTrue("Slayer's staff (e)", accepted(index, "Slayer's staff").contains(21255));
		assertTrue("Magic shortbow (i)", accepted(index, "Magic shortbow").contains(12788));
	}

	/** Tier safety: the imbued ring is better, so plain must not satisfy it. */
	@Test
	public void plainRingDoesNotSatisfyAnImbuedRecommendation()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			6737, "Berserker ring", 11773, "Berserker ring (i)");
		assertTrue(!accepted(index, "Berserker ring (i)").contains(6737));
	}

	/** Echo recolours share no variation group, so the explicit rule carries them. */
	@Test
	public void echoRecolourSatisfiesTheBaseWithoutAVariationGroup()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			26241, "Virtus mask", 28001, "Echo virtus mask",
			4708, "Ahrim's hood", 28002, "Echo ahrim's hood");

		assertTrue(accepted(index, "Virtus mask").contains(28001));
		assertTrue(accepted(index, "Ahrim's hood").contains(28002));
	}
}
