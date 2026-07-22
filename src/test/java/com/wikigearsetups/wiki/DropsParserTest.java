package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DropsParserTest
{
	private final DropsParser parser = new DropsParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = DropsParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void zulrahUniquesAndPetAreParsed() throws IOException
	{
		List<String> drops = parser.parse(fixture("zulrah-page"));

		assertTrue("uniques: " + drops, drops.contains("Tanzanite fang"));
		assertTrue(drops.contains("Magic fang"));
		assertTrue(drops.contains("Serpentine visage"));
		assertTrue("the pet lives in the tertiary section", drops.contains("Pet snakeling"));
	}

	@Test
	public void bulkDropTableSectionsAreIgnored() throws IOException
	{
		List<String> drops = parser.parse(fixture("zulrah-page"));

		// runes, herbs and seeds make up most of the table and would drown
		// the icon picker
		assertFalse(drops.contains("Death rune"));
		assertFalse(drops.contains("Grimy ranarr weed"));
		assertTrue("the list stays short enough to pick from: " + drops.size(), drops.size() <= 12);
	}

	@Test
	public void namedParamsAndNestedTemplatesDoNotBreakTheRow()
	{
		// the pet row carries a nested {{NamedRef}} inside raritynotes
		List<String> drops = parser.parse("===Tertiary===\n"
			+ "{{DropsLine|name=Pet snakeling|quantity=1|rarity=1/4000"
			+ "|raritynotes={{NamedRef|Confirmed droprates}}|gemw=No}}\n");

		assertEquals(1, drops.size());
		assertEquals("Pet snakeling", drops.get(0));
	}

	@Test
	public void pagesWithoutADropTableYieldNothing() throws IOException
	{
		assertTrue(parser.parse(fixture("zulrah-strategies")).isEmpty());
	}
}
