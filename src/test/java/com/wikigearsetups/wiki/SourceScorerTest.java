package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SourceScorerTest
{
	private final GearSetupParser gearParser = new GearSetupParser();
	private final InventoryParser inventoryParser = new InventoryParser();
	private final ExampleSetupParser exampleParser = new ExampleSetupParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = SourceScorerTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private SourceScorer.Result scoreFixture(String name) throws IOException
	{
		String wikitext = fixture(name);
		List<ExampleSetup> examples = exampleParser.parse(wikitext);
		List<GearSetup> ranked = gearParser.parse(wikitext);
		List<InventorySetup> inventories = ExampleSetupParser.withoutExampleInventories(
			inventoryParser.parse(wikitext), examples);
		return SourceScorer.score(examples, ranked, inventories);
	}

	@Test
	public void raidPagesPreferTheirExampleColumns() throws IOException
	{
		for (String page : Arrays.asList("toa-strategies", "tob-setups-guide", "cox-strategies"))
		{
			SourceScorer.Result r = scoreFixture(page);
			assertTrue(page + " should prefer examples (examples=" + r.getExampleScore()
				+ " ranked=" + r.getRankedScore() + ")", r.isPreferExamples());
		}
	}

	@Test
	public void tabberPagesFallBackToRankedSetups() throws IOException
	{
		// no example tables at all — the selector stays hidden and the classic
		// path must win on score too
		for (String page : Arrays.asList("zulrah-strategies", "vorkath-strategies", "gotr-strategies"))
		{
			SourceScorer.Result r = scoreFixture(page);
			assertEquals(page + " should score no examples", 0.0, r.getExampleScore(), 0.001);
			assertTrue(page + " should score ranked setups", r.getRankedScore() > 0);
		}
	}

	@Test
	public void sparseExampleTableLosesToCompleteRankedSetups()
	{
		// the whole point of scoring: a token example column must not beat
		// meticulous ranked tables
		Map<EquipSlot, List<RankedItem>> thin = new EnumMap<>(EquipSlot.class);
		thin.put(EquipSlot.WEAPON, Collections.singletonList(new RankedItem("Dragon sword", 1, null)));
		thin.put(EquipSlot.HEAD, Collections.singletonList(new RankedItem("Helm of neitiznot", 1, null)));
		ExampleSetup sparse = new ExampleSetup(Collections.emptyList(), "Example",
			new GearSetup("Example", thin), null);

		Map<EquipSlot, List<RankedItem>> full = new EnumMap<>(EquipSlot.class);
		for (EquipSlot slot : EquipSlot.values())
		{
			full.put(slot, Arrays.asList(
				new RankedItem("A", 1, null), new RankedItem("B", 2, null), new RankedItem("C", 3, null)));
		}
		List<InventorySetup> inventories = Collections.singletonList(
			new InventorySetup("Inv", Collections.nCopies(28, "Shark"), Arrays.asList("Fire rune")));

		SourceScorer.Result r = SourceScorer.score(Collections.singletonList(sparse),
			Collections.singletonList(new GearSetup("Melee", full)), inventories);

		assertFalse("complete ranked tables should win", r.isPreferExamples());
		assertTrue("and decisively", r.isDecisive());
	}

	@Test
	public void closeScoresAreNotLabelledDecisive()
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.WEAPON, Collections.singletonList(new RankedItem("Dragon sword", 1, null)));
		ExampleSetup example = new ExampleSetup(Collections.emptyList(), "Example",
			new GearSetup("Example", slots), null);

		SourceScorer.Result r = SourceScorer.score(Collections.singletonList(example),
			Collections.singletonList(new GearSetup("Melee", slots)), Collections.emptyList());

		assertEquals(r.getExampleScore(), r.getRankedScore(), 0.001);
		assertFalse("identical scores must not claim one is more complete", r.isDecisive());
	}

	@Test
	public void exampleOwnedInventoriesAreNotOfferedToRankedSetups() throws IOException
	{
		String wikitext = fixture("toa-strategies");
		List<ExampleSetup> examples = exampleParser.parse(wikitext);
		List<InventorySetup> all = inventoryParser.parse(wikitext);
		List<InventorySetup> remaining = ExampleSetupParser.withoutExampleInventories(all, examples);

		int paired = 0;
		for (ExampleSetup example : examples)
		{
			if (example.getInventory() != null)
			{
				paired++;
			}
		}
		assertTrue("fixture should have paired inventories to filter", paired > 0);
		assertTrue("filtered list must be smaller", remaining.size() < all.size());

		List<List<String>> remainingSlots = new ArrayList<>();
		for (InventorySetup inventory : remaining)
		{
			remainingSlots.add(inventory.getSlots());
		}
		for (ExampleSetup example : examples)
		{
			if (example.getInventory() != null)
			{
				assertFalse("example inventory still offered separately",
					remainingSlots.contains(example.getInventory().getSlots()));
			}
		}
	}
}
