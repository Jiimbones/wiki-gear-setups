package com.wikigearsetups.ui;

import com.wikigearsetups.layout.TagNames;
import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Several pages give two setups the same {@code |style=} — Royal Titans has
 * "Melee" twice. Identical entries are unpickable in the dropdown, and they
 * slug to the same tag, so importing the second would silently replace the
 * first tab.
 */
public class SetupLabelsTest
{
	private static GearSetup setup(String style)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.HEAD, Collections.singletonList(new RankedItem("Torva full helm", 1, null)));
		return new GearSetup(style, slots);
	}

	@Test
	public void repeatedStyleLabelsAreNumbered()
	{
		List<GearSetup> out = WikiGearSetupsPanel.disambiguate(
			Arrays.asList(setup("Melee"), setup("Ranged"), setup("Melee"), setup("Melee")));

		assertEquals("Melee", out.get(0).getStyleLabel());
		assertEquals("Ranged", out.get(1).getStyleLabel());
		assertEquals("Melee (2)", out.get(2).getStyleLabel());
		assertEquals("Melee (3)", out.get(3).getStyleLabel());
	}

	@Test
	public void distinctLabelsAreUntouched()
	{
		List<GearSetup> in = Arrays.asList(setup("Melee"), setup("Magic"), setup("Ranged"));
		List<GearSetup> out = WikiGearSetupsPanel.disambiguate(in);

		for (int i = 0; i < in.size(); i++)
		{
			assertEquals(in.get(i).getStyleLabel(), out.get(i).getStyleLabel());
		}
	}

	@Test
	public void slotsSurviveRelabelling()
	{
		List<GearSetup> out = WikiGearSetupsPanel.disambiguate(
			Arrays.asList(setup("Melee"), setup("Melee")));
		assertEquals(setup("Melee").getSlots(), out.get(1).getSlots());
	}

	/** The point of the numbering: two tabs, not one overwritten twice. */
	@Test
	public void relabelledSetupsProduceDistinctTagNames()
	{
		List<GearSetup> out = WikiGearSetupsPanel.disambiguate(
			Arrays.asList(setup("Melee"), setup("Melee"), setup("Ranged"), setup("Ranged")));

		Set<String> tags = new HashSet<>();
		List<String> built = new ArrayList<>();
		for (GearSetup s : out)
		{
			String tag = TagNames.build("Royal Titans/Strategies", s.getStyleLabel());
			built.add(tag);
			tags.add(tag);
		}
		assertEquals("every setup needs its own tag: " + built, out.size(), tags.size());
		assertNotEquals(built.get(0), built.get(1));
	}

	/** Inventory labels repeat on real pages too (Corporeal Beast). */
	@Test
	public void repeatedPlainLabelsAreNumbered()
	{
		assertEquals(Arrays.asList("Best in slot", "Quick-Pool", "Best in slot (2)"),
			WikiGearSetupsPanel.uniquify(
				Arrays.asList("Best in slot", "Quick-Pool", "Best in slot")));
	}

	@Test
	public void uniquifyLeavesDistinctLabelsAlone()
	{
		List<String> labels = Arrays.asList("Melee", "Magic", "Ranged");
		assertEquals(labels, WikiGearSetupsPanel.uniquify(labels));
	}
}
