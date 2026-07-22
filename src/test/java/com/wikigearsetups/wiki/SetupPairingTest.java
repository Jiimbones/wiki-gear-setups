package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pages routinely head every inventory "==Inventory==", so their labels can't
 * tell them apart — but their order can. These pin when that order is trusted
 * and, just as importantly, when it isn't.
 */
public class SetupPairingTest
{
	private static String fixture(String name) throws IOException
	{
		try (InputStream in = SetupPairingTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void interleavedPagesPairEachSetupWithTheInventoryBelowIt() throws IOException
	{
		String wikitext = fixture("general-graardor-strategies");
		List<GearSetup> setups = new GearSetupParser().parse(wikitext);
		List<InventorySetup> inventories = new InventoryParser().parse(wikitext);
		assertEquals(8, setups.size());
		assertEquals(8, inventories.size());

		assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7},
			SetupPairing.pair(wikitext, setups.size(), inventories.size()));
	}

	@Test
	public void interleavedOrderIsRecognised()
	{
		assertTrue(SetupPairing.isInterleaved("EIEIEI"));
		assertTrue(SetupPairing.isInterleaved("EI"));
	}

	@Test
	public void blockedOrderIsRecognised()
	{
		assertTrue(SetupPairing.isBlocked("EEEIII"));
		assertTrue(SetupPairing.isBlocked("EII"));
	}

	@Test
	public void raggedOrderIsNeither()
	{
		// Araxxor: E E I E I — no way to say which setup the first owns
		assertEquals(0, SetupPairing.pair("{{Recommended equipment|head1=a}}"
			+ "{{Recommended equipment|head1=b}}{{Inventory|1=Shark}}"
			+ "{{Recommended equipment|head1=c}}{{Inventory|1=Shark}}", 3, 2).length);
	}

	/** An unequal blocked page can't say which setup the spare belongs to. */
	@Test
	public void blockedOrderWithUnequalCountsIsNotPaired()
	{
		String wikitext = "{{Recommended equipment|head1=a}}{{Recommended equipment|head1=b}}"
			+ "{{Inventory|1=Shark}}";
		assertEquals(0, SetupPairing.pair(wikitext, 2, 1).length);
	}

	@Test
	public void blockedOrderWithEqualCountsPairsByIndex()
	{
		String wikitext = "{{Recommended equipment|head1=a}}{{Recommended equipment|head1=b}}"
			+ "{{Inventory|1=Shark}}{{Inventory|1=Tuna}}";
		assertArrayEquals(new int[]{0, 1}, SetupPairing.pair(wikitext, 2, 2));
	}

	/**
	 * A template the parsers dropped (empty, malformed) shifts every index
	 * after it, so a count that doesn't match the models disables pairing
	 * rather than mispairing.
	 */
	@Test
	public void countMismatchWithTheParsedModelsDisablesPairing()
	{
		String wikitext = "{{Recommended equipment|head1=a}}{{Inventory|1=Shark}}";
		assertEquals(0, SetupPairing.pair(wikitext, 2, 1).length);
	}

	@Test
	public void emptyInputsPairNothing()
	{
		assertEquals(0, SetupPairing.pair(null, 1, 1).length);
		assertEquals(0, SetupPairing.pair("{{Inventory|1=Shark}}", 0, 1).length);
	}

	@Test
	public void crlfInputPairsIdentically() throws IOException
	{
		String lf = fixture("general-graardor-strategies");
		assertArrayEquals(SetupPairing.pair(lf, 8, 8),
			SetupPairing.pair(lf.replace("\n", "\r\n"), 8, 8));
	}
}
