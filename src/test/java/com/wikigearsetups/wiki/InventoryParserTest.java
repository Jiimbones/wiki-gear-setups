package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryParserTest
{
	private final InventoryParser parser = new InventoryParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = InventoryParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void zulrahInventoriesParseWithTabberLabels() throws IOException
	{
		List<InventorySetup> setups = parser.parse(fixture("zulrah-strategies"));
		assertTrue("expected multiple inventory setups", setups.size() >= 2);
		assertEquals("Max Magic & Ranged", setups.get(0).getLabel());
		assertEquals("Void Mage and Ranged", setups.get(1).getLabel());
	}

	@Test
	public void slotsPreserveWikiOrder() throws IOException
	{
		InventorySetup first = parser.parse(fixture("zulrah-strategies")).get(0);
		assertEquals(28, first.getSlots().size());
		assertEquals("Twisted bow", first.getSlots().get(0));
		assertEquals("Toxic blowpipe", first.getSlots().get(8));
		assertEquals("Divine rune pouch", first.getSlots().get(27));
	}

	@Test
	public void placeholderTemplatesBecomeTheirName() throws IOException
	{
		// slot 11 is {{Cheap food}}
		InventorySetup first = parser.parse(fixture("zulrah-strategies")).get(0);
		assertEquals("Cheap food", first.getSlots().get(10));
	}

	@Test
	public void pageWithoutInventoriesParsesEmpty() throws IOException
	{
		assertTrue(parser.parse(fixture("dragon-slayer-ii")).isEmpty());
	}

	@Test
	public void headingLabelUsedOutsideTabber()
	{
		List<InventorySetup> setups = parser.parse(
			"==Suggested inventory==\n{{Inventory|1=Shark|2=Shark}}");
		assertEquals(1, setups.size());
		assertEquals("Suggested inventory", setups.get(0).getLabel());
		assertEquals("Shark", setups.get(0).getSlots().get(0));
	}

	@Test
	public void emptyInventoryTemplateIsDropped()
	{
		assertTrue(parser.parse("{{Inventory|align=right}}").isEmpty());
	}
}
