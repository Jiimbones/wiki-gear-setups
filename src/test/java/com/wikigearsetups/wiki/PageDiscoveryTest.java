package com.wikigearsetups.wiki;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageDiscoveryTest
{
	@Test
	public void basePageProbesGuideThenStrategies()
	{
		assertEquals(Arrays.asList("Guide:Zulrah setups", "Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah"));
	}

	@Test
	public void explicitSubpageComesAfterGuideWithoutDuplicates()
	{
		assertEquals(Arrays.asList("Guide:Zulrah setups", "Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah/Strategies"));
	}

	@Test
	public void nonStrategySubpageIsKeptAheadOfStrategies()
	{
		assertEquals(
			Arrays.asList("Guide:Money making guide setups", "Money making guide/Killing Zulrah",
				"Money making guide/Strategies", "Money making guide"),
			PageDiscovery.candidatesFor("Money making guide/Killing Zulrah"));
	}

	@Test
	public void guidePagesDoNotProbeGuideAgain()
	{
		assertEquals(
			Arrays.asList("Guide:Theatre of Blood setups/Strategies", "Guide:Theatre of Blood setups"),
			PageDiscovery.candidatesFor("Guide:Theatre of Blood setups"));
	}
}
