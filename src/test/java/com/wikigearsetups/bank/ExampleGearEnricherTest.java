package com.wikigearsetups.bank;

import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExampleGearEnricherTest
{
	private static GearSetup single(EquipSlot slot, String item)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(slot, Collections.singletonList(new RankedItem(item, 1, null)));
		return new GearSetup("Example", slots);
	}

	private static GearSetup ranked(EquipSlot slot, String... items)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		java.util.ArrayList<RankedItem> list = new java.util.ArrayList<>();
		for (int i = 0; i < items.length; i++)
		{
			list.add(new RankedItem(items[i], i + 1, null));
		}
		slots.put(slot, list);
		return new GearSetup("Ranked", slots);
	}

	@Test
	public void anchorStaysFirstAndLadderFollows()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Rune crossbow");
		GearSetup ladder = ranked(EquipSlot.WEAPON, "Twisted bow", "Bow of faerdhinen", "Rune crossbow", "Magic shortbow");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(ladder));
		List<RankedItem> weapon = enriched.getSlots().get(EquipSlot.WEAPON);

		assertEquals("Rune crossbow", weapon.get(0).getName());
		assertEquals(Arrays.asList("Twisted bow", "Bow of faerdhinen", "Magic shortbow"),
			Arrays.asList(weapon.get(1).getName(), weapon.get(2).getName(), weapon.get(3).getName()));
	}

	@Test
	public void anchorAbsentFromAllLaddersStaysAlone()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Dragon sword");
		GearSetup ladder = ranked(EquipSlot.WEAPON, "Twisted bow", "Rune crossbow");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(ladder));

		assertEquals(1, enriched.getSlots().get(EquipSlot.WEAPON).size());
	}

	@Test
	public void ladderMatchIsSlotScoped()
	{
		// the anchor appears in a HEAD ladder — the WEAPON slot must not use it
		GearSetup example = single(EquipSlot.WEAPON, "Slayer helmet");
		GearSetup wrongSlot = ranked(EquipSlot.HEAD, "Slayer helmet", "Neitiznot faceguard");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(wrongSlot));

		assertEquals(1, enriched.getSlots().get(EquipSlot.WEAPON).size());
	}

	@Test
	public void noRankedSetupsReturnsExampleUnchanged()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Rune crossbow");
		assertSame(example, ExampleGearEnricher.enrich(example, Collections.emptyList()));
	}
}
