package com.wikigearsetups.layout;

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
		assertEquals(300, BankTagImporter.iconFor(r));
	}

	@Test
	public void firstPickIsIconWhenNoWeapon()
	{
		SetupResolution r = resolution(pick(EquipSlot.HEAD, 100));
		assertEquals(100, BankTagImporter.iconFor(r));
	}

	private static SetupResolution resolution(SlotPick... picks)
	{
		GearSetup setup = new GearSetup("Test", Collections.emptyMap());
		return new SetupResolution(setup, Arrays.asList(picks),
			Collections.emptyMap(), Collections.emptyList());
	}
}
