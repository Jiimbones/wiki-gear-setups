package com.wikigearsetups.bank;

import com.wikigearsetups.WikiGearSetupsConfig.MissingGearBehavior;
import com.wikigearsetups.items.FakeItemNameIndex;
import com.wikigearsetups.items.ItemResolver;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.InventorySetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventoryResolverTest
{
	private static final int SHARK = 385;
	private static final int KARAMBWAN = 3144;

	private final InventoryResolver resolver = new InventoryResolver(new ItemResolver(
		FakeItemNameIndex.of(SHARK, "Shark", KARAMBWAN, "Cooked karambwan")));

	private static InventorySetup setup(String... names)
	{
		List<String> slots = new ArrayList<>(Arrays.asList(names));
		while (slots.size() < 28)
		{
			slots.add("");
		}
		return new InventorySetup("Test", slots, Collections.emptyList());
	}

	@Test
	public void ownedItemsArePlacedAtTheirWikiSlots()
	{
		InventoryResolution r = resolver.resolve(setup("Shark", "", "Cooked karambwan"),
			new HashSet<>(Arrays.asList(SHARK, KARAMBWAN)), MissingGearBehavior.OMIT);

		assertEquals(2, r.getPicks().size());
		assertEquals(0, r.getPicks().get(0).getSlot());
		assertEquals(SHARK, r.getPicks().get(0).getItemId());
		assertEquals(2, r.getPicks().get(1).getSlot());
		assertEquals(KARAMBWAN, r.getPicks().get(1).getItemId());
	}

	@Test
	public void duplicateSlotsAreAllKept()
	{
		InventoryResolution r = resolver.resolve(setup("Shark", "Shark", "Shark"),
			Collections.singleton(SHARK), MissingGearBehavior.OMIT);
		assertEquals(3, r.getPicks().size());
	}

	@Test
	public void missingItemsOmittedByDefaultButCounted()
	{
		InventoryResolution r = resolver.resolve(setup("Shark"),
			Collections.emptySet(), MissingGearBehavior.OMIT);
		assertTrue(r.getPicks().isEmpty());
		assertEquals(1, r.getMissingCount());
	}

	@Test
	public void missingItemsPlacedUnderGoalConfig()
	{
		InventoryResolution r = resolver.resolve(setup("Shark"),
			Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED);
		assertEquals(1, r.getPicks().size());
		assertFalse(r.getPicks().get(0).isOwned());
	}

	@Test
	public void cheapFoodPlaceholderBecomesOwnedFood()
	{
		InventoryResolution r = resolver.resolve(setup("Cheap food", "Shark", "Cheap food"),
			Collections.singleton(SHARK), MissingGearBehavior.OMIT);
		// all three slots fill with the shark the player owns
		assertEquals(3, r.getPicks().size());
		assertEquals(SHARK, r.getPicks().get(0).getItemId());
		assertTrue(r.getUnresolvedNames().isEmpty());
	}

	@Test
	public void cheapFoodWithNothingOwnedBecomesGoalUnderShowConfig()
	{
		InventoryResolution r = resolver.resolve(setup("Cheap food"),
			Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED);
		assertEquals(1, r.getPicks().size());
		assertFalse(r.getPicks().get(0).isOwned());
		assertEquals(SHARK, r.getPicks().get(0).getItemId());
	}

	@Test
	public void unknownPlaceholdersStillLandInUnresolved()
	{
		InventoryResolution r = resolver.resolve(setup("Fancy nonexistent widget"),
			Collections.singleton(SHARK), MissingGearBehavior.OMIT);
		assertEquals(Collections.singletonList("Fancy nonexistent widget"), r.getUnresolvedNames());
		assertTrue(r.getPicks().isEmpty());
	}

	@Test
	public void unownedFoodSubstitutesBestOwnedFromItsClass()
	{
		int monkfish = 7946;
		InventoryResolver r = new InventoryResolver(new ItemResolver(
			FakeItemNameIndex.of(SHARK, "Shark", monkfish, "Monkfish")));

		InventoryResolution res = r.resolve(setup("Shark"),
			Collections.singleton(monkfish), MissingGearBehavior.OMIT);

		assertEquals(1, res.getPicks().size());
		assertEquals(monkfish, res.getPicks().get(0).getItemId());
		assertTrue(res.getPicks().get(0).isOwned());
		assertEquals(0, res.getMissingCount());
	}

	@Test
	public void unownedSwitchItemFallsBackThroughGearLadder()
	{
		int blowpipe = 12926;
		int rcb = 9185;
		InventoryResolver r = new InventoryResolver(new ItemResolver(
			FakeItemNameIndex.of(blowpipe, "Toxic blowpipe", rcb, "Rune crossbow")));

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.WEAPON, Arrays.asList(
			new RankedItem("Toxic blowpipe", 1, null),
			new RankedItem("Rune crossbow", 2, null)));
		GearSetup ladder = new GearSetup("Ranged", slots);

		InventoryResolution res = r.resolve(setup("Toxic blowpipe"),
			Collections.singleton(rcb), MissingGearBehavior.OMIT,
			Collections.singletonList(ladder));

		assertEquals(rcb, res.getPicks().get(0).getItemId());
		assertTrue(res.getPicks().get(0).isOwned());
	}

	@Test
	public void nothingOwnedInClassStillGoesMissing()
	{
		InventoryResolution r = resolver.resolve(setup("Shark"),
			Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED);
		assertEquals(1, r.getMissingCount());
		assertFalse(r.getPicks().get(0).isOwned());
		assertEquals(SHARK, r.getPicks().get(0).getItemId());
	}

	@Test
	public void missingItemsAreNamed()
	{
		InventoryResolution r = resolver.resolve(setup("Shark", "Cooked karambwan", "Shark"),
			Collections.singleton(KARAMBWAN), MissingGearBehavior.SHOW_BEST_UNOWNED);

		assertEquals(Collections.singletonList("Shark"), r.getMissingNames());
		assertEquals(2, r.getMissingCount());
	}

	@Test
	public void runesResolveIntoRunePicks()
	{
		int fireRune = 554;
		InventoryResolver r = new InventoryResolver(new ItemResolver(
			FakeItemNameIndex.of(SHARK, "Shark", fireRune, "Fire rune")));
		InventorySetup s = new InventorySetup("Test",
			Collections.nCopies(28, ""), Arrays.asList("Fire rune"));

		InventoryResolution res = r.resolve(s, Collections.singleton(fireRune), MissingGearBehavior.OMIT);
		assertEquals(1, res.getRunePicks().size());
		assertEquals(fireRune, res.getRunePicks().get(0).getItemId());
		assertEquals(0, res.getRunePicks().get(0).getSlot());
	}
}
