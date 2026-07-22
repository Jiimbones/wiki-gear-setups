package com.wikigearsetups.items;

import java.util.List;
import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Names the wiki uses that no item carries. Each of these was found failing
 * to resolve on a real page, so the point is that the generic name reaches
 * the concrete items — not which one wins, which is ownership's job.
 */
public class ItemAliasesTest
{
	@Test
	public void blessedDhideAcceptsEveryGodsVersion()
	{
		List<String> body = ItemAliases.genericFor("Blessed d'hide body");
		assertTrue(body.contains("Guthix d'hide body"));
		assertTrue(body.contains("Armadyl d'hide body"));
		assertEquals("all six gods", 6, body.size());
	}

	/** Pages write the same slot half a dozen ways; all of them must land. */
	@Test
	public void everySpellingOfAGodDhideSlotResolvesToTheSameItems()
	{
		List<String> canonical = ItemAliases.genericFor("Blessed d'hide body");
		assertEquals(canonical, ItemAliases.genericFor("Blessed body"));
		assertEquals(canonical, ItemAliases.genericFor("Blessed dragonhide body"));

		List<String> bracers = ItemAliases.genericFor("Blessed bracers");
		assertEquals(bracers, ItemAliases.genericFor("Blessed vambraces"));
		assertFalse(bracers.isEmpty());
	}

	/** "{{plink|Blessed_bracers}}" — a URL-style link must still normalize. */
	@Test
	public void underscoresInLinksNormalizeToSpaces()
	{
		assertEquals(ItemNames.normalize("Blessed bracers"), ItemNames.normalize("Blessed_bracers"));
		assertFalse(ItemAliases.genericFor("Blessed_bracers").isEmpty());
	}

	@Test
	public void blessingsCoverTheSixGods()
	{
		assertEquals(6, ItemAliases.genericFor("Blessing").size());
		assertTrue(ItemAliases.genericFor("Blessing").contains("Holy blessing"));
	}

	/** Diary rewards are numbered in game; best tier first so it's preferred. */
	@Test
	public void diaryRewardsListTheirTiersHighestFirst()
	{
		assertEquals(java.util.Arrays.asList(
			"Ardougne cloak 4", "Ardougne cloak 3", "Ardougne cloak 2", "Ardougne cloak 1"),
			ItemAliases.genericFor("Ardougne cloak"));
		assertEquals("Explorer's ring 4", ItemAliases.genericFor("Explorer's ring").get(0));
		assertEquals("Rada's blessing 4", ItemAliases.genericFor("Rada's blessing").get(0));
	}

	@Test
	public void wintertodtWarmClothingHasConcreteCandidates()
	{
		List<String> warm = ItemAliases.genericFor("Warm clothing");
		assertTrue(warm.contains("Bruma torch"));
		assertTrue(warm.contains("Warm gloves"));
	}

	@Test
	public void unknownNamesStayUnknown()
	{
		assertTrue(ItemAliases.genericFor("Twisted bow").isEmpty());
	}

	/** Generic names only apply when the exact name failed, so nothing regresses. */
	@Test
	public void aRealItemNameStillResolvesExactly()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			11826, "Armadyl helmet", 10370, "Guthix d'hide body");
		ItemResolver resolver = new ItemResolver(index);

		Optional<ResolvedItem> exact = resolver.resolve("Armadyl helmet");
		assertTrue(exact.isPresent());
		assertEquals(11826, exact.get().getPrimaryId());
	}

	@Test
	public void aGenericNameResolvesThroughToAConcreteItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(10370, "Guthix d'hide body");
		ItemResolver resolver = new ItemResolver(index);

		Optional<ResolvedItem> resolved = resolver.resolve("Blessed d'hide body");
		assertTrue("generic name should reach the concrete item", resolved.isPresent());
		assertTrue(resolved.get().getAcceptableIds().contains(10370));
	}
}
