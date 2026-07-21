package com.wikigearsetups.ui;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InventoryMatcherTest
{
	@Test
	public void exactLabelBeatsAMerelyOverlappingOne()
	{
		// the Zulrah case: "Ranged" gear must not settle for the combined
		// "Max Magic & Ranged" inventory listed before it
		assertEquals(1, InventoryMatcher.bestMatch("Ranged",
			Arrays.asList("Max Magic & Ranged", "Ranged")));
	}

	@Test
	public void exactMatchIsCaseAndSpacingInsensitive()
	{
		assertEquals(0, InventoryMatcher.bestMatch("Magic Only",
			Arrays.asList("  magic   only ", "Melee Only")));
	}

	@Test
	public void styleTokensMatchWhenNoExactLabelExists()
	{
		assertEquals(1, InventoryMatcher.bestMatch("Mage",
			Arrays.asList("Melee switches", "Magic switches")));
	}

	@Test
	public void containmentIsTheLastResort()
	{
		assertEquals(0, InventoryMatcher.bestMatch("Entry-level",
			Collections.singletonList("Entry-level inventory")));
	}

	@Test
	public void nothingSuitableReturnsNoMatch()
	{
		assertEquals(-1, InventoryMatcher.bestMatch("Melee",
			Arrays.asList("Magic switches", "Ranged switches")));
	}
}
