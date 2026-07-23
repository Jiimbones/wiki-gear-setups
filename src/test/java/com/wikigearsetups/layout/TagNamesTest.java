package com.wikigearsetups.layout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TagNamesTest
{
	@Test
	public void combinesPageAndStyle()
	{
		assertEquals("zulrah-magic", TagNames.build("Zulrah/Strategies", "Magic"));
	}

	@Test
	public void punctuationBecomesSingleDashes()
	{
		assertEquals("zulrah-magic-diary-kill", TagNames.build("Zulrah/Strategies", "Magic (Diary kill)"));
	}

	@Test
	public void apostrophesAreDropped()
	{
		assertEquals("kreearra-ranged", TagNames.build("Kree'arra/Strategies", "Ranged"));
	}

	@Test
	public void plainPageTitleWorks()
	{
		assertEquals("vorkath-setup-1", TagNames.build("Vorkath", "Setup 1"));
	}

	@Test
	public void longNamesAreCapped()
	{
		String tag = TagNames.build("Some Extremely Long Boss Name Nobody Would Believe", "A Very Detailed Style Label Indeed");
		assertTrue(tag.length() <= 50);
	}

	@Test
	public void deterministic()
	{
		assertEquals(
			TagNames.build("Chambers of Xeric/Strategies", "Magic"),
			TagNames.build("Chambers of Xeric/Strategies", "Magic"));
	}

	@Test
	public void prefixIsPrependedAsASlug()
	{
		assertEquals("wgs-zulrah-magic", TagNames.withPrefix("wgs", "zulrah-magic"));
		assertEquals("wgs-zulrah-magic", TagNames.withPrefix("WGS!", "zulrah-magic"));
	}

	/** No prefix configured is the installation default — a pure no-op. */
	@Test
	public void emptyPrefixLeavesTheTagUnchanged()
	{
		assertEquals("zulrah-magic", TagNames.withPrefix("", "zulrah-magic"));
		assertEquals("zulrah-magic", TagNames.withPrefix(null, "zulrah-magic"));
		assertEquals("", TagNames.withPrefix("wgs", ""));
	}
}
