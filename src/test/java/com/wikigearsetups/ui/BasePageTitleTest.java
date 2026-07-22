package com.wikigearsetups.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** The drop table lives on the boss's own page, not the setups page. */
public class BasePageTitleTest
{
	@Test
	public void strategySubpagesResolveToTheirBoss()
	{
		assertEquals("Zulrah", WikiGearSetupsPanel.basePageTitle("Zulrah/Strategies"));
		assertEquals("Tombs of Amascut", WikiGearSetupsPanel.basePageTitle("Tombs of Amascut/Strategies"));
	}

	@Test
	public void guidePagesResolveToTheirContent()
	{
		assertEquals("Theatre of Blood",
			WikiGearSetupsPanel.basePageTitle("Guide:Theatre of Blood setups"));
	}

	@Test
	public void aBasePageIsAlreadyItsOwnBase()
	{
		assertEquals("Amoxliatl", WikiGearSetupsPanel.basePageTitle("Amoxliatl"));
	}
}
