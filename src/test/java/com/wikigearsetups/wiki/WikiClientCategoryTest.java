package com.wikigearsetups.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiClientCategoryTest
{
	private static JsonObject json(String s)
	{
		return new Gson().fromJson(s, JsonObject.class);
	}

	@Test
	public void categoryMembersAreExtracted()
	{
		Set<String> pets = WikiClient.extractCategoryMembers(json("{\"query\":{\"categorymembers\":["
			+ "{\"ns\":0,\"title\":\"Pet snakeling\"},"
			+ "{\"ns\":0,\"title\":\"Olmlet\"},"
			+ "{\"ns\":0,\"title\":\"Lil' zik\"}]}}"));

		assertEquals(3, pets.size());
		assertTrue("names follow no pattern, so the list is the only signal",
			pets.contains("Pet snakeling") && pets.contains("Olmlet") && pets.contains("Lil' zik"));
	}

	@Test
	public void missingOrEmptyResponsesYieldNoMembers()
	{
		assertTrue(WikiClient.extractCategoryMembers(json("{}")).isEmpty());
		assertTrue(WikiClient.extractCategoryMembers(json("{\"query\":{}}")).isEmpty());
	}
}
