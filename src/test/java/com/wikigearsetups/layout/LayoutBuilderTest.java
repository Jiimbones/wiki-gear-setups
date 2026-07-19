package com.wikigearsetups.layout;

import com.wikigearsetups.bank.InventoryPick;
import com.wikigearsetups.bank.SetupResolution;
import com.wikigearsetups.bank.SlotPick;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.plugins.banktags.tabs.Layout;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LayoutBuilderTest
{
	private static SlotPick pick(EquipSlot slot, int itemId)
	{
		return new SlotPick(slot, new RankedItem("x", 1, null), itemId, true, true);
	}

	@Test
	public void compactPacksInSlotOrderRegardlessOfInputOrder()
	{
		List<SlotPick> picks = Arrays.asList(
			pick(EquipSlot.WEAPON, 300),
			pick(EquipSlot.HEAD, 100),
			pick(EquipSlot.NECK, 200));

		Layout layout = LayoutBuilder.build("test", picks, LayoutStyle.COMPACT);

		assertEquals(100, layout.getItemAtPos(0)); // head
		assertEquals(200, layout.getItemAtPos(1)); // neck
		assertEquals(300, layout.getItemAtPos(2)); // weapon
		assertEquals("test", layout.getTag());
	}

	@Test
	public void equipmentScreenMirrorsWornInterface()
	{
		List<SlotPick> picks = Arrays.asList(
			pick(EquipSlot.HEAD, 100),
			pick(EquipSlot.CAPE, 200),
			pick(EquipSlot.NECK, 300),
			pick(EquipSlot.WEAPON, 400),
			pick(EquipSlot.BODY, 500),
			pick(EquipSlot.SHIELD, 600),
			pick(EquipSlot.LEGS, 700),
			pick(EquipSlot.HANDS, 800),
			pick(EquipSlot.FEET, 900),
			pick(EquipSlot.RING, 1000));

		Layout layout = LayoutBuilder.build("t", picks, LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(100, layout.getItemAtPos(1));   // head: row 0, centre-left
		assertEquals(200, layout.getItemAtPos(8));   // cape: row 1 col 0
		assertEquals(300, layout.getItemAtPos(9));   // neck: row 1 col 1
		assertEquals(400, layout.getItemAtPos(16));  // weapon: row 2 col 0
		assertEquals(500, layout.getItemAtPos(17));  // body: row 2 col 1
		assertEquals(600, layout.getItemAtPos(18));  // shield: row 2 col 2
		assertEquals(700, layout.getItemAtPos(25));  // legs: row 3 col 1
		assertEquals(800, layout.getItemAtPos(32));  // hands: row 4 col 0
		assertEquals(900, layout.getItemAtPos(33));  // feet: row 4 col 1
		assertEquals(1000, layout.getItemAtPos(34)); // ring: row 4 col 2

		// columns 4-7 stay free for future inventory placement
		for (int row = 0; row < 5; row++)
		{
			for (int col = 4; col < 8; col++)
			{
				assertEquals(-1, layout.getItemAtPos(row * 8 + col));
			}
		}
	}

	@Test
	public void extrasFlowBelowEquipmentScreenLayout()
	{
		List<SlotPick> picks = Collections.singletonList(pick(EquipSlot.WEAPON, 400));

		Layout layout = LayoutBuilder.build("t", Collections.singletonList(picks),
			Collections.emptyList(), Collections.emptyList(),
			Arrays.asList(50, 60), LayoutStyle.EQUIPMENT_SCREEN);

		// one gear block occupies rows 0-4, row 5 stays blank, extras start row 6
		assertEquals(50, layout.getItemAtPos(48));
		assertEquals(60, layout.getItemAtPos(49));
		for (int pos = 40; pos < 48; pos++)
		{
			assertEquals(-1, layout.getItemAtPos(pos));
		}
	}

	@Test
	public void extrasSkipItemsAlreadyPlaced()
	{
		List<SlotPick> picks = Collections.singletonList(pick(EquipSlot.WEAPON, 400));

		Layout layout = LayoutBuilder.build("t", Collections.singletonList(picks),
			Collections.emptyList(), Collections.emptyList(),
			Arrays.asList(400, 60), LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(60, layout.getItemAtPos(48));
		assertEquals(-1, layout.getItemAtPos(49));
	}

	@Test
	public void deterministicAcrossShuffledInput()
	{
		List<SlotPick> picks = new ArrayList<>(Arrays.asList(
			pick(EquipSlot.HEAD, 1),
			pick(EquipSlot.CAPE, 2),
			pick(EquipSlot.WEAPON, 3),
			pick(EquipSlot.FEET, 4),
			pick(EquipSlot.RING, 5)));

		int[] first = LayoutBuilder.build("t", picks, LayoutStyle.EQUIPMENT_SCREEN).getLayout();
		Collections.reverse(picks);
		int[] second = LayoutBuilder.build("t", picks, LayoutStyle.EQUIPMENT_SCREEN).getLayout();

		assertEquals(Arrays.toString(first), Arrays.toString(second));
	}

	@Test
	public void weaponPickBecomesTabIcon()
	{
		SetupResolution r = resolution(
			pick(EquipSlot.HEAD, 100),
			pick(EquipSlot.WEAPON, 300));
		assertEquals(300, BankTagImporter.iconFor(Collections.singletonList(r)));
	}

	@Test
	public void firstPickIsIconWhenNoWeapon()
	{
		SetupResolution r = resolution(pick(EquipSlot.HEAD, 100));
		assertEquals(100, BankTagImporter.iconFor(Collections.singletonList(r)));
	}

	@Test
	public void combinedStylesStackAsBlocksAndDedupeSharedGear()
	{
		List<SlotPick> magic = Arrays.asList(
			pick(EquipSlot.HEAD, 100),
			pick(EquipSlot.FEET, 900));  // same boots both styles
		List<SlotPick> ranged = Arrays.asList(
			pick(EquipSlot.HEAD, 101),
			pick(EquipSlot.FEET, 900));

		Layout layout = LayoutBuilder.build("t", Arrays.asList(magic, ranged),
			Collections.emptyList(), Collections.emptyList(), LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(100, layout.getItemAtPos(1));       // block 0 head
		assertEquals(900, layout.getItemAtPos(33));      // block 0 feet
		assertEquals(101, layout.getItemAtPos(41));      // block 1 head (rows offset by 5)
		assertEquals(-1, layout.getItemAtPos(73));       // block 1 feet: deduped, empty
	}

	@Test
	public void inventoryFillsRightHalfInWikiOrder()
	{
		List<InventoryPick> inventory = Arrays.asList(
			new InventoryPick(0, 500, true),   // row 0 col 4
			new InventoryPick(3, 501, true),   // row 0 col 7
			new InventoryPick(4, 502, true),   // row 1 col 4
			new InventoryPick(27, 503, true)); // row 6 col 7

		Layout layout = LayoutBuilder.build("t",
			Collections.singletonList(Collections.singletonList(pick(EquipSlot.HEAD, 100))),
			inventory, Collections.emptyList(), LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(500, layout.getItemAtPos(4));
		assertEquals(501, layout.getItemAtPos(7));
		assertEquals(502, layout.getItemAtPos(12));
		assertEquals(503, layout.getItemAtPos(55));
	}

	@Test
	public void inventoryItemAlreadyPlacedAsGearIsSkipped()
	{
		Layout layout = LayoutBuilder.build("t",
			Collections.singletonList(Collections.singletonList(pick(EquipSlot.WEAPON, 300))),
			Collections.singletonList(new InventoryPick(0, 300, true)), Collections.emptyList(),
			LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(300, layout.getItemAtPos(16)); // weapon slot
		assertEquals(-1, layout.getItemAtPos(4));   // inventory spot stays empty
	}

	@Test
	public void inventoryDuplicatesWithinInventoryAreKept()
	{
		List<InventoryPick> inventory = Arrays.asList(
			new InventoryPick(0, 500, true),
			new InventoryPick(1, 500, true));

		Layout layout = LayoutBuilder.build("t", Collections.emptyList(),
			inventory, Collections.emptyList(), LayoutStyle.EQUIPMENT_SCREEN);

		assertEquals(500, layout.getItemAtPos(4));
		assertEquals(500, layout.getItemAtPos(5));
	}

	@Test
	public void runesSitDirectlyUnderTheLastEquipmentBlock()
	{
		List<InventoryPick> runes = Arrays.asList(
			new InventoryPick(0, 554, true),
			new InventoryPick(1, 565, true),
			new InventoryPick(2, 564, true));

		Layout layout = LayoutBuilder.build("t",
			Collections.singletonList(Collections.singletonList(pick(EquipSlot.HEAD, 100))),
			Collections.emptyList(), runes, LayoutStyle.EQUIPMENT_SCREEN);

		// single gear block occupies rows 0-4; runes on row 5, cols 0-2
		assertEquals(554, layout.getItemAtPos(40));
		assertEquals(565, layout.getItemAtPos(41));
		assertEquals(564, layout.getItemAtPos(42));
	}

	@Test
	public void runesFollowStackedBlocksInCombinedImports()
	{
		List<InventoryPick> runes = Collections.singletonList(new InventoryPick(0, 554, true));

		Layout layout = LayoutBuilder.build("t",
			Arrays.asList(
				Collections.singletonList(pick(EquipSlot.HEAD, 100)),
				Collections.singletonList(pick(EquipSlot.HEAD, 101))),
			Collections.emptyList(), runes, LayoutStyle.EQUIPMENT_SCREEN);

		// two blocks occupy rows 0-9; runes on row 10
		assertEquals(554, layout.getItemAtPos(80));
	}

	private static SetupResolution resolution(SlotPick... picks)
	{
		GearSetup setup = new GearSetup("Test", Collections.emptyMap());
		return new SetupResolution(setup, Arrays.asList(picks),
			Collections.emptyMap(), Collections.emptyList());
	}
}
