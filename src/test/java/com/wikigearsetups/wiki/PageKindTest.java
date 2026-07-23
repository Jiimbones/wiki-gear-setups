package com.wikigearsetups.wiki;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageKindTest
{
	@Test
	public void slayerTaskPagesAreRecognised()
	{
		assertEquals(PageKind.SLAYER_TASK, PageKind.of("Slayer task/Abyssal demons"));
		assertEquals(PageKind.SLAYER_TASK, PageKind.of("Slayer task/Gargoyles"));
	}

	@Test
	public void guidePagesAreRecognised()
	{
		assertEquals(PageKind.GUIDE, PageKind.of("Guide:Theatre of Blood setups"));
		assertEquals(PageKind.GUIDE, PageKind.of("Ultimate Ironman Guide/Equipment"));
		assertEquals(PageKind.GUIDE, PageKind.of("Ultimate Ironman Guide/Slayer"));
	}

	@Test
	public void bossStrategyPagesHaveNoKind()
	{
		assertEquals(PageKind.NONE, PageKind.of("Zulrah/Strategies"));
		assertEquals(PageKind.NONE, PageKind.of("General Graardor/Strategies"));
	}

	/** Combat Achievement pages follow no convention — deliberately unlabelled. */
	@Test
	public void combatAchievementPagesStayUnlabelled()
	{
		assertEquals(PageKind.NONE, PageKind.of("Morytania Only"));
		assertEquals(PageKind.NONE, PageKind.of("Budget Cutter"));
		assertEquals(PageKind.NONE, PageKind.of("Fight Caves Speed-Runner"));
	}

	@Test
	public void nullTitleIsNone()
	{
		assertEquals(PageKind.NONE, PageKind.of(null));
	}
}
