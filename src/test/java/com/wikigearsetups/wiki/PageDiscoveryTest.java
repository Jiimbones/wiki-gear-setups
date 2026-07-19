package com.wikigearsetups.wiki;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageDiscoveryTest
{
	@Test
	public void basePageProbesStrategiesFirst()
	{
		assertEquals(Arrays.asList("Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah"));
	}

	@Test
	public void explicitSubpageIsTriedFirstWithoutDuplicates()
	{
		assertEquals(Arrays.asList("Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah/Strategies"));
	}

	@Test
	public void nonStrategySubpageKeepsItselfFirst()
	{
		assertEquals(
			Arrays.asList("Money making guide/Killing Zulrah", "Money making guide/Strategies", "Money making guide"),
			PageDiscovery.candidatesFor("Money making guide/Killing Zulrah"));
	}
}
