package com.wikigearsetups.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StyleTokensTest
{
	@Test
	public void mageMatchesMagic()
	{
		assertTrue(StyleTokens.stylesOverlap("Mage", "Magic switches"));
	}

	@Test
	public void rangedVariantsMatch()
	{
		assertTrue(StyleTokens.stylesOverlap("Phase 2 Ranged", "Melee and ranged"));
		assertTrue(StyleTokens.stylesOverlap("Range", "Ranging inventory"));
	}

	@Test
	public void differentStylesDoNotMatch()
	{
		assertFalse(StyleTokens.stylesOverlap("Melee", "Magic switches"));
	}

	@Test
	public void labelsWithoutStyleTokensDoNotMatch()
	{
		assertFalse(StyleTokens.stylesOverlap("Entry-level", "Inventory"));
	}
}
