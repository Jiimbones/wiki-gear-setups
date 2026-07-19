package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GearSetupParserTest
{
	private final GearSetupParser parser = new GearSetupParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = GearSetupParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void zulrahParsesAllTabberSetups() throws IOException
	{
		List<GearSetup> setups = parser.parse(fixture("zulrah-strategies"));
		assertEquals(4, setups.size());
		assertEquals("Magic", setups.get(0).getStyleLabel());
		assertEquals("Ranged", setups.get(1).getStyleLabel());
		assertEquals("Magic (Diary kill)", setups.get(3).getStyleLabel());
	}

	@Test
	public void zulrahMagicSlotContents() throws IOException
	{
		GearSetup magic = parser.parse(fixture("zulrah-strategies")).get(0);

		List<RankedItem> neck = magic.getSlots().get(EquipSlot.NECK);
		assertEquals("Occult necklace", neck.get(0).getName());
		assertEquals(1, neck.get(0).getRank());

		// head1 = Slayer helmet (i) {{efn|...}} / Ancestral hat
		List<RankedItem> head = magic.getSlots().get(EquipSlot.HEAD);
		assertEquals("Slayer helmet (i)", head.get(0).getName());
		assertEquals("If on a Zulrah slayer boss task", head.get(0).getNote());
		assertEquals("Ancestral hat", head.get(1).getName());
		assertEquals(1, head.get(1).getRank());
		assertNull(head.get(1).getNote());

		// ranks are ascending
		int prev = 0;
		for (RankedItem item : head)
		{
			assertTrue(item.getRank() >= prev);
			prev = item.getRank();
		}
	}

	@Test
	public void efnBackReferenceWithoutTextLeavesNoteNull() throws IOException
	{
		// magic weapon4: {{plink|Smoke battlestaff}}{{efn|name=fire}}
		GearSetup magic = parser.parse(fixture("zulrah-strategies")).get(0);
		RankedItem smokeStaff = magic.getSlots().get(EquipSlot.WEAPON).stream()
			.filter(i -> i.getName().equals("Smoke battlestaff"))
			.findFirst()
			.orElseThrow(AssertionError::new);
		assertNull(smokeStaff.getNote());
	}

	@Test
	public void noteMarkupIsStripped() throws IOException
	{
		// ranged head2: {{efn|With full [[crystal armour]] and [[bow of faerdhinen]]|name=crystal}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		RankedItem crystalHelm = ranged.getSlots().get(EquipSlot.HEAD).stream()
			.filter(i -> i.getName().equals("Crystal helm"))
			.findFirst()
			.orElseThrow(AssertionError::new);
		assertEquals("With full crystal armour and bow of faerdhinen", crystalHelm.getNote());
	}

	@Test
	public void plinkPicParameterIsNotTheItemName() throws IOException
	{
		// ranged head4: {{plink|Blessed coif|pic=Ancient coif}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		assertTrue(ranged.getSlots().get(EquipSlot.HEAD).stream()
			.anyMatch(i -> i.getName().equals("Blessed coif")));
	}

	@Test
	public void orBrSeparatorYieldsBothItems() throws IOException
	{
		// ranged head1 = {{plink|Slayer helmet (i)}} or <br> {{plink|Masori mask (f)}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		List<RankedItem> head = ranged.getSlots().get(EquipSlot.HEAD);
		assertEquals("Slayer helmet (i)", head.get(0).getName());
		assertEquals("Masori mask (f)", head.get(1).getName());
	}

	@Test
	public void vorkathParsesTwoSetups() throws IOException
	{
		assertEquals(2, parser.parse(fixture("vorkath-strategies")).size());
	}

	@Test
	public void coxParsesAllFifteenSetups() throws IOException
	{
		assertEquals(15, parser.parse(fixture("cox-strategies")).size());
	}

	@Test
	public void alchemicalHydraParsesTwoSetups() throws IOException
	{
		assertEquals(2, parser.parse(fixture("alchemical-hydra-strategies")).size());
	}

	@Test
	public void questPageWithoutTemplateParsesEmpty() throws IOException
	{
		assertTrue(parser.parse(fixture("dragon-slayer-ii")).isEmpty());
	}

	@Test
	public void emptyAndNullInput()
	{
		assertTrue(parser.parse(null).isEmpty());
		assertTrue(parser.parse("").isEmpty());
	}

	@Test
	public void unclosedTemplateDoesNotCrash()
	{
		assertTrue(parser.parse("{{Recommended equipment|head1 = {{plink|Foo}").isEmpty());
	}

	@Test
	public void missingStyleFallsBackToSetupIndex()
	{
		List<GearSetup> setups = parser.parse(
			"{{Recommended equipment|head1 = {{plink|Bronze med helm}}}}");
		assertEquals(1, setups.size());
		assertEquals("Setup 1", setups.get(0).getStyleLabel());
	}

	@Test
	public void unknownSlotAndNaAreIgnored()
	{
		List<GearSetup> setups = parser.parse(
			"{{Recommended equipment"
				+ "|style = Melee"
				+ "|foo1 = {{plink|Not a slot}}"
				+ "|head1 = {{plink|N/A}}"
				+ "|weapon1 = {{plink|Abyssal whip}}"
				+ "}}");
		assertEquals(1, setups.size());
		GearSetup s = setups.get(0);
		assertNull(s.getSlots().get(EquipSlot.HEAD));
		assertEquals(1, s.getSlots().size());
		assertEquals("Abyssal whip", s.getSlots().get(EquipSlot.WEAPON).get(0).getName());
	}

	@Test
	public void templateWithNoItemsIsDropped()
	{
		assertTrue(parser.parse("{{Recommended equipment|style = Magic}}").isEmpty());
	}

	@Test
	public void splitTopLevelRespectsNesting()
	{
		List<String> parts = GearSetupParser.splitTopLevel(
			"a|{{x|y}}|[[p|q]]|b", '|');
		assertEquals(4, parts.size());
		assertEquals("{{x|y}}", parts.get(1));
		assertEquals("[[p|q]]", parts.get(2));
	}
}
