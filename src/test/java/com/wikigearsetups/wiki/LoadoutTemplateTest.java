package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {{Loadout}}: gear, a 28-slot inventory and a rune pouch in one template.
 * Zulrah/Strategies moved its inventories into it after July, and they
 * stopped importing.
 */
public class LoadoutTemplateTest
{
	private final InventoryParser parser = new InventoryParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = LoadoutTemplateTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static final String LOADOUT = "{{Loadout\n"
		+ "|loadoutname = Test (magic)\n"
		+ "|head = Ancestral hat\n"
		+ "|torso = Ancestral robe top\n"
		+ "|1=Twisted bow\n"
		+ "|2=Shark\n"
		+ "|28=Divine rune pouch\n"
		+ "|rune1 = Fire rune\n"
		+ "|rune2 = Blood rune\\1000\n"
		+ "}}";

	@Test
	public void readsTheInventoryGridAndNotTheGear()
	{
		List<InventorySetup> setups = parser.parse(LOADOUT);

		assertEquals(1, setups.size());
		List<String> slots = setups.get(0).getSlots();
		assertEquals(28, slots.size());
		assertEquals("Twisted bow", slots.get(0));
		assertEquals("Shark", slots.get(1));
		assertEquals("", slots.get(2));
		assertEquals("Divine rune pouch", slots.get(27));
		assertFalse("gear must not leak into the grid", slots.contains("Ancestral hat"));
	}

	@Test
	public void readsTheRunePouchWithoutQuantities()
	{
		assertEquals(List.of("Fire rune", "Blood rune"), parser.parse(LOADOUT).get(0).getRunes());
	}

	@Test
	public void namesItselfOnlyWhenThePageDoesNot()
	{
		assertEquals("Test (magic)", parser.parse(LOADOUT).get(0).getLabel());
		assertEquals("Magic Only", parser.parse("==Magic Only==\n" + LOADOUT).get(0).getLabel());
	}

	@Test
	public void aGearOnlyLoadoutIsNotAnInventory()
	{
		assertTrue(parser.parse("{{Loadout|loadoutname=Gear|head=Ancestral hat|torso=Ancestral robe top}}").isEmpty());
	}

	@Test
	public void aFollowingRunePouchFillsAnEmptyOne()
	{
		List<InventorySetup> setups = parser.parse("{{Loadout|1=Shark}}\n{{Rune pouch|Death rune|Blood rune}}");

		assertEquals(List.of("Death rune", "Blood rune"), setups.get(0).getRunes());
	}

	@Test
	public void zulrahReadsTheSameAsBeforeItWasConverted() throws IOException
	{
		// same page, July ({{Inventory}} + {{Rune pouch}}) and now ({{Loadout}})
		List<InventorySetup> before = parser.parse(fixture("zulrah-strategies"));
		List<InventorySetup> after = parser.parse(fixture("zulrah-strategies-loadout"));

		assertEquals(5, after.size());
		for (int i = 0; i < before.size(); i++)
		{
			assertEquals(before.get(i).getLabel(), after.get(i).getLabel());
			assertEquals(before.get(i).getLabel(), before.get(i).getRunes(), after.get(i).getRunes());
		}
		assertEquals("Twisted bow", after.get(0).getSlots().get(0));
	}

	@Test
	public void loadoutsTakePartInPairing()
	{
		String page = "{{Recommended equipment|head1={{plink|Helm A}}}}\n{{Loadout|1=Shark}}\n"
			+ "{{Recommended equipment|head1={{plink|Helm B}}}}\n{{Loadout|1=Lobster}}";

		assertEquals("EIEI", SetupPairing.templateOrder(page));
		assertArrayEquals(new int[]{0, 1}, SetupPairing.pair(page, 2, 2));
	}

	@Test
	public void zulrahPageOrderCountsEveryLoadout() throws IOException
	{
		assertEquals("EEEEIIIII", SetupPairing.templateOrder(
			GearSetupParser.normalizeLineEndings(fixture("zulrah-strategies-loadout"))));
	}
}
