package com.wikigearsetups.wiki;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Nothing on a fixture page goes unread. Both of 1.0.1's parser bugs were a
 * template feature the parsers skipped without a word — the two-handed
 * column and {{Loadout}} — so a new one has to fail here, loudly, the moment
 * a fixture contains it. The corpus check (tools.CorpusCheck) runs the same
 * test against the live wiki.
 */
public class TemplateCoverageTest
{
	private static List<Path> fixtures() throws IOException, URISyntaxException
	{
		Path dir = Paths.get(TemplateCoverageTest.class.getResource("/fixtures").toURI());
		try (Stream<Path> files = Files.list(dir))
		{
			return files.filter(f -> f.toString().endsWith(".wikitext")).sorted().collect(Collectors.toList());
		}
	}

	@Test
	public void everyFixtureIsFullyRead() throws IOException, URISyntaxException
	{
		List<Path> fixtures = fixtures();
		assertFalse(fixtures.isEmpty());
		for (Path fixture : fixtures)
		{
			String text = new String(Files.readAllBytes(fixture), StandardCharsets.UTF_8);
			assertEquals(fixture.getFileName() + ": gear parameters the parser skips",
				Set.of(), GearSetupParser.unrecognisedParams(text));
			assertEquals(fixture.getFileName() + ": inventory-shaped templates the parser skips",
				Set.of(), InventoryParser.unrecognisedInventoryTemplates(text));
		}
	}

	@Test
	public void anUnknownSlotIsReported()
	{
		assertEquals(Set.of("quiver1"), GearSetupParser.unrecognisedParams(
			"{{Recommended equipment|style=Ranged|head1={{plink|Masori mask (f)}}"
				+ "|quiver1={{plink|Dizana's quiver}}|hidecol=ammo}}"));
	}

	@Test
	public void layoutOptionsAndEverySlotAreNotReported()
	{
		// "hidecol5 = yes" hides rank column 5 — numbered, but still layout
		assertTrue(GearSetupParser.unrecognisedParams("{{Recommended equipment|style=x|bucket=no|hidecol=ammo"
			+ "|hidecol5=yes|align=left|head1=a|cape1=a|neck1=a|ammo1=a|weapon1=a|2h1=a|body1=a|shield1=a"
			+ "|legs1=a|hands1=a|feet1=a|ring1=a|special1=a}}").isEmpty());
	}

	@Test
	public void aSlotWithNoRankIsSkippedJustAsTheWikiSkipsIt()
	{
		// Thermonuclear smoke devil/Strategies writes "|weapon = {{plink|Ghrazi
		// rapier}}". The template only reads weapon1-5, so the page never shows
		// the rapier — importing it would put on the tab what the wiki doesn't.
		String page = "{{Recommended equipment|weapon1={{plink|Abyssal whip}}|weapon={{plink|Ghrazi rapier}}}}";

		assertEquals(List.of("Abyssal whip"), new GearSetupParser().parse(page).get(0)
			.getSlots().get(EquipSlot.WEAPON).stream().map(RankedItem::getName).collect(Collectors.toList()));
		assertTrue(GearSetupParser.unrecognisedParams(page).isEmpty());
		assertEquals(Set.of("weapon"), GearSetupParser.unrankedSlotParams(page));
	}

	@Test
	public void anUnknownInventoryShapedTemplateIsReported()
	{
		StringBuilder slots = new StringBuilder();
		for (int i = 1; i <= 12; i++)
		{
			slots.append('|').append(i).append("=Shark");
		}

		assertEquals(Set.of("Bag"), InventoryParser.unrecognisedInventoryTemplates("{{Bag" + slots + "}}"));
		assertTrue(InventoryParser.unrecognisedInventoryTemplates("{{Inventory" + slots + "}}").isEmpty());
		assertTrue(InventoryParser.unrecognisedInventoryTemplates("{{Loadout" + slots + "}}").isEmpty());
	}
}
