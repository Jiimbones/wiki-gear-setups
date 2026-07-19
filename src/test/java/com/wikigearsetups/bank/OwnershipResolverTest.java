package com.wikigearsetups.bank;

import com.wikigearsetups.WikiGearSetupsConfig.MissingGearBehavior;
import com.wikigearsetups.items.FakeItemNameIndex;
import com.wikigearsetups.items.ItemResolver;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OwnershipResolverTest
{
	// real ids — karil's leathertop variation group
	private static final int KARILS_LEATHERTOP = 4736;
	private static final int KARILS_LEATHERTOP_100 = 4940;
	// real ids with no variation group entanglement needed
	private static final int OCCULT_NECKLACE = 12002;
	private static final int AMULET_OF_FURY = 6585;

	private final FakeItemNameIndex index = FakeItemNameIndex.of(
		KARILS_LEATHERTOP, "Karil's leathertop",
		KARILS_LEATHERTOP_100, "Karil's leathertop 100",
		OCCULT_NECKLACE, "Occult necklace",
		AMULET_OF_FURY, "Amulet of fury");

	private final OwnershipResolver resolver = new OwnershipResolver(new ItemResolver(index));

	private static GearSetup setup(EquipSlot slot, RankedItem... items)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(slot, Arrays.asList(items));
		return new GearSetup("Test", slots);
	}

	private static Set<Integer> owned(Integer... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	@Test
	public void topRecommendationPickedWhenOwned()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(OCCULT_NECKLACE, AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		assertEquals(1, r.getPicks().size());
		SlotPick pick = r.getPicks().get(0);
		assertEquals(OCCULT_NECKLACE, pick.getItemId());
		assertTrue(pick.isOwned());
		assertTrue(pick.isTopRecommendation());
	}

	@Test
	public void fallsBackToLowerRankedAlternative()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		SlotPick pick = r.getPicks().get(0);
		assertEquals(AMULET_OF_FURY, pick.getItemId());
		assertTrue(pick.isOwned());
		assertFalse(pick.isTopRecommendation());
	}

	@Test
	public void ownedVariantIdIsPlacedNotTheBaseId()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Karil's leathertop", 1, null));
		SetupResolution r = resolver.resolve(s, owned(KARILS_LEATHERTOP_100), MissingGearBehavior.OMIT, false);

		assertEquals(KARILS_LEATHERTOP_100, r.getPicks().get(0).getItemId());
	}

	@Test
	public void exactMatchBeatsOwnedVariant()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Karil's leathertop", 1, null));
		SetupResolution r = resolver.resolve(s,
			owned(KARILS_LEATHERTOP, KARILS_LEATHERTOP_100), MissingGearBehavior.OMIT, false);

		assertEquals(KARILS_LEATHERTOP, r.getPicks().get(0).getItemId());
	}

	@Test
	public void missingSlotOmittedByDefault()
	{
		GearSetup s = setup(EquipSlot.NECK, new RankedItem("Occult necklace", 1, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.OMIT, false);

		assertTrue(r.getPicks().isEmpty());
		assertEquals(Collections.singletonList(EquipSlot.NECK), r.getEmptySlots());
	}

	@Test
	public void missingSlotShowsGoalItemWhenConfigured()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertTrue(r.getEmptySlots().isEmpty());
		SlotPick pick = r.getPicks().get(0);
		assertEquals("goal item is the best recommendation", OCCULT_NECKLACE, pick.getItemId());
		assertFalse(pick.isOwned());
	}

	@Test
	public void unresolvedNameIsRecordedAndSkipped()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Necklace of made-up power", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		assertEquals(Collections.singletonList("Necklace of made-up power"),
			r.getUnresolvedNames().get(EquipSlot.NECK));
		assertEquals(AMULET_OF_FURY, r.getPicks().get(0).getItemId());
	}

	@Test
	public void allNamesUnresolvedLeavesSlotEmptyEvenWithGoalConfig()
	{
		GearSetup s = setup(EquipSlot.NECK, new RankedItem("Necklace of made-up power", 1, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertTrue(r.getPicks().isEmpty());
		assertEquals(Collections.singletonList(EquipSlot.NECK), r.getEmptySlots());
	}

	private static final int SLAYER_HELMET_I = 11865;
	private static final int ANCESTRAL_HAT = 21018;

	private OwnershipResolver slayerResolver()
	{
		return new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			SLAYER_HELMET_I, "Slayer helmet (i)",
			ANCESTRAL_HAT, "Ancestral hat")));
	}

	private static GearSetup slayerSetup()
	{
		return setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "If on a Zulrah slayer boss task"),
			new RankedItem("Ancestral hat", 1, null));
	}

	@Test
	public void slayerConditionalSkippedWhenOffTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			owned(SLAYER_HELMET_I, ANCESTRAL_HAT), MissingGearBehavior.OMIT, false);

		assertEquals(ANCESTRAL_HAT, r.getPicks().get(0).getItemId());
	}

	@Test
	public void slayerConditionalPickedWhenOnTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			owned(SLAYER_HELMET_I, ANCESTRAL_HAT), MissingGearBehavior.OMIT, true);

		assertEquals(SLAYER_HELMET_I, r.getPicks().get(0).getItemId());
	}

	@Test
	public void slayerConditionalNeverBecomesGoalItemOffTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertEquals("goal falls to the unconditional alternative",
			ANCESTRAL_HAT, r.getPicks().get(0).getItemId());
	}

	@Test
	public void informationalNotesAreNotTreatedAsSlayerConditional()
	{
		assertFalse(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Serpentine helm", 1, "Negates the need to bring anti-venom potions")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "If on a slayer task")));
	}
}
