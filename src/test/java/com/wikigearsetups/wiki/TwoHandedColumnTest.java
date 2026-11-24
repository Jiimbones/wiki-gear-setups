package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {{Recommended equipment}}'s two-handed column ("2h1"-"2h5"). It went
 * unread because the slot name starts with a digit, which left setups like
 * Doom of Mokhaiotl's Ranged and Kree'arra's "Tbow / Bowfa" with no weapon.
 */
public class TwoHandedColumnTest
{
	private final GearSetupParser parser = new GearSetupParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = TwoHandedColumnTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static List<String> names(List<RankedItem> items)
	{
		return items.stream().map(RankedItem::getName).collect(Collectors.toList());
	}

	private static GearSetup style(List<GearSetup> setups, String label)
	{
		return setups.stream().filter(s -> s.getStyleLabel().equals(label)).findFirst()
			.orElseThrow(() -> new AssertionError("no setup styled '" + label + "'"));
	}

	@Test
	public void twoHandedColumnFillsTheWeaponSlot()
	{
		GearSetup setup = parser.parse("{{Recommended equipment|style=Ranged"
			+ "|2h1={{plink|Twisted bow}}|2h2={{plink|Bow of faerdhinen}}}}").get(0);

		List<RankedItem> weapons = setup.getSlots().get(EquipSlot.WEAPON);
		assertEquals(List.of("Twisted bow", "Bow of faerdhinen"), names(weapons));
		assertEquals(1, weapons.get(0).getRank());
		assertEquals(2, weapons.get(1).getRank());
	}

	@Test
	public void oneAndTwoHandedAtTheSameRankAreBothKeptInPageOrder()
	{
		// before, the later "1" replaced the earlier one at the same rank
		GearSetup setup = parser.parse("{{Recommended equipment|style=Larvae"
			+ "|weapon1={{plink|Darklight}}|shield1={{plink|Eye of Ayak}}"
			+ "|2h1={{plink|Scorching bow}}|weapon2={{plink|Burning claws}}}}").get(0);

		assertEquals(List.of("Darklight", "Scorching bow", "Burning claws"),
			names(setup.getSlots().get(EquipSlot.WEAPON)));
		assertEquals(List.of("Eye of Ayak"), names(setup.getSlots().get(EquipSlot.SHIELD)));
	}

	@Test
	public void pageOrderDecidesTiesEitherWay()
	{
		GearSetup setup = parser.parse("{{Recommended equipment"
			+ "|2h1={{plink|Twisted bow}}|weapon1={{plink|Toxic blowpipe}}}}").get(0);

		assertEquals(List.of("Twisted bow", "Toxic blowpipe"), names(setup.getSlots().get(EquipSlot.WEAPON)));
	}

	@Test
	public void ranksPastNineStillParse()
	{
		GearSetup setup = parser.parse("{{Recommended equipment|weapon12={{plink|Abyssal whip}}}}").get(0);

		RankedItem whip = setup.getSlots().get(EquipSlot.WEAPON).get(0);
		assertEquals("Abyssal whip", whip.getName());
		assertEquals(12, whip.getRank());
	}

	@Test
	public void loadoutStyleSlotNamesAreAccepted()
	{
		GearSetup setup = parser.parse("{{Recommended equipment"
			+ "|torso1={{plink|Bandos chestplate}}|gloves1={{plink|Barrows gloves}}"
			+ "|boots1={{plink|Primordial boots}}}}").get(0);

		assertEquals("Bandos chestplate", setup.getSlots().get(EquipSlot.BODY).get(0).getName());
		assertEquals("Barrows gloves", setup.getSlots().get(EquipSlot.HANDS).get(0).getName());
		assertEquals("Primordial boots", setup.getSlots().get(EquipSlot.FEET).get(0).getName());
	}

	@Test
	public void doomOfMokhaiotlSetupsAllHaveWeapons() throws IOException
	{
		List<GearSetup> setups = parser.parse(fixture("doom-of-mokhaiotl-strategies"));
		for (GearSetup setup : setups)
		{
			assertTrue(setup.getStyleLabel() + " has no weapon",
				setup.getSlots().containsKey(EquipSlot.WEAPON));
		}

		List<String> ranged = names(style(setups, "Ranged").getSlots().get(EquipSlot.WEAPON));
		assertEquals("Twisted bow", ranged.get(0));
		assertTrue(ranged.contains("Scorching bow"));
		assertTrue(ranged.contains("Ascension crossbows"));

		assertTrue(names(style(setups, "Melee punish").getSlots().get(EquipSlot.WEAPON))
			.contains("Noxious halberd"));
	}

	@Test
	public void kreearraSetupsAllHaveWeapons() throws IOException
	{
		List<GearSetup> setups = parser.parse(fixture("kreearra-strategies"));
		for (GearSetup setup : setups)
		{
			assertTrue(setup.getStyleLabel() + " has no weapon",
				setup.getSlots().containsKey(EquipSlot.WEAPON));
		}

		assertEquals("Tumeken's shadow",
			style(setups, "Magic").getSlots().get(EquipSlot.WEAPON).get(0).getName());
		assertEquals(List.of("Twisted bow", "Bow of Faerdhinen"),
			names(style(setups, "Tbow / Bowfa").getSlots().get(EquipSlot.WEAPON)));
	}
}
