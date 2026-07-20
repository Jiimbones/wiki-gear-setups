package com.wikigearsetups.ui;

import com.wikigearsetups.wiki.EquipSlot;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.GearSetupParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RaidLoadoutTest
{
	private static GearSetup setup(String label)
	{
		return new GearSetup(label, new EnumMap<>(EquipSlot.class));
	}

	@Test
	public void firstSetupPerStyleWinsInPageOrder()
	{
		GearSetup melee = setup("Melee");
		GearSetup ranged = setup("Ranged");
		GearSetup magic = setup("Magic");
		List<GearSetup> raid = RaidLoadout.bestPerStyle(Arrays.asList(
			melee, ranged, magic,
			setup("Melee (Maiden)"), setup("Ranged (budget)"), setup("Magic (Verzik)")));

		assertEquals(Arrays.asList(melee, ranged, magic), raid);
	}

	@Test
	public void combinedStyleLabelIsKeptOnce()
	{
		GearSetup hybrid = setup("Melee and ranged");
		GearSetup magic = setup("Magic");
		List<GearSetup> raid = RaidLoadout.bestPerStyle(Arrays.asList(hybrid, magic));

		assertEquals(Arrays.asList(hybrid, magic), raid);
	}

	@Test
	public void styleLessLabelsYieldNothing()
	{
		assertTrue(RaidLoadout.bestPerStyle(Arrays.asList(
			setup("Entry-level"), setup("Advanced"))).isEmpty());
	}

	@Test
	public void singleStylePageYieldsOne()
	{
		assertEquals(1, RaidLoadout.bestPerStyle(Collections.singletonList(setup("Melee"))).size());
	}

	@Test
	public void coxFixtureYieldsOneSetupPerCombatStyle() throws IOException
	{
		List<GearSetup> setups;
		try (InputStream in = RaidLoadoutTest.class.getResourceAsStream("/fixtures/cox-strategies.wikitext"))
		{
			assertNotNull("missing cox fixture", in);
			setups = new GearSetupParser().parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}

		List<GearSetup> raid = RaidLoadout.bestPerStyle(setups);
		assertTrue("expected a multi-style raid loadout, got " + raid.size(), raid.size() >= 2);
		assertTrue("loadout must be smaller than the full setup list", raid.size() < setups.size());
		// one setup per style — no style token appears twice
		HashSet<String> seen = new HashSet<>();
		for (GearSetup s : raid)
		{
			for (String token : StyleTokens.tokens(s.getStyleLabel()))
			{
				assertTrue("style " + token + " appears twice", seen.add(token));
			}
		}
	}
}
