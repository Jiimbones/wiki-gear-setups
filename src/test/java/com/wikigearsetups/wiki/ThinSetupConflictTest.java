package com.wikigearsetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The thin-setup filter (W3) must not be applied before source scoring or
 * pairing. Theatre of Blood is the page that proves it: it has 5 real
 * loadouts and 9 single-item switches, and its example cascade is the format
 * the player wants. Scoring divides slots by setup count, so removing the 9
 * switches inflates the ranked average past the example score and flips the
 * page onto the wrong format.
 */
public class ThinSetupConflictTest
{
	private static String fixture(String name) throws IOException
	{
		try (InputStream in = ThinSetupConflictTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void scoringOnTheFullListKeepsTheExampleCascade() throws IOException
	{
		String wt = fixture("theatre-of-blood-strategies");
		List<GearSetup> all = new GearSetupParser().parse(wt);
		List<ExampleSetup> examples = new ExampleSetupParser().parse(wt);
		List<InventorySetup> invs = ExampleSetupParser.withoutExampleInventories(
			new InventoryParser().parse(wt), examples);

		SourceScorer.Result correct = SourceScorer.score(examples, all, invs);
		assertTrue("full list should keep ToB on its example cascade", correct.isPreferExamples());
	}

	@Test
	public void scoringOnTheFilteredListWouldHaveFlippedIt() throws IOException
	{
		String wt = fixture("theatre-of-blood-strategies");
		List<GearSetup> all = new GearSetupParser().parse(wt);
		List<ExampleSetup> examples = new ExampleSetupParser().parse(wt);
		List<InventorySetup> invs = ExampleSetupParser.withoutExampleInventories(
			new InventoryParser().parse(wt), examples);

		// this is the mistake the panel must avoid — filter, then score
		List<GearSetup> loadoutsOnly = GearSetups.partition(all).getLoadouts();
		SourceScorer.Result wrong = SourceScorer.score(examples, loadoutsOnly, invs);

		assertTrue("filtering before scoring would wrongly prefer the ranked format",
			!wrong.isPreferExamples());
	}
}
