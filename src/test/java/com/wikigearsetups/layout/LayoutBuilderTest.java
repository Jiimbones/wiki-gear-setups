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
	public void itemsArePackedInSlotOrderRegardlessOfInputOrder()
	{
		List<SlotPick> picks = Arrays.asList(
			pick(EquipSlot.WEAPON, 300),
			pick(EquipSlot.HEAD, 100),
			pick(EquipSlot.NECK, 200));

		Layout layout = LayoutBuilder.build("test", picks);

		assertEquals(100, layout.getItemAtPos(0)); // head
		assertEquals(200, layout.getItemAtPos(1)); // neck
		assertEquals(300, layout.getItemAtPos(2)); // weapon
		assertEquals("test", layout.getTag());
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

		int[] first = LayoutBuilder.build("t", picks).getLayout();
		Collections.reverse(picks);
		int[] second = LayoutBuilder.build("t", picks).getLayout();

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
