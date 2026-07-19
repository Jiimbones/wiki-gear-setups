package com.wikigearsetups.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiClientTest
{
	private final Gson gson = new Gson();

	@Test
	public void extractsWikitextFromParseResponse()
	{
		JsonObject json = gson.fromJson(
			"{\"parse\":{\"title\":\"Zulrah/Strategies\",\"pageid\":123,\"wikitext\":\"{{Recommended equipment}}\"}}",
			JsonObject.class);
		assertEquals("{{Recommended equipment}}", WikiClient.extractWikitext(json));
	}

	@Test(expected = IllegalStateException.class)
	public void missingWikitextThrows()
	{
		WikiClient.extractWikitext(gson.fromJson("{\"parse\":{}}", JsonObject.class));
	}

	@Test
	public void extractsSearchResults()
	{
		JsonObject json = gson.fromJson(
			"{\"query\":{\"search\":["
				+ "{\"title\":\"Zulrah\",\"pageid\":1},"
				+ "{\"title\":\"Zulrah/Strategies\",\"pageid\":2}]}}",
			JsonObject.class);
		List<SearchResult> results = WikiClient.extractSearchResults(json);
		assertEquals(2, results.size());
		assertEquals("Zulrah/Strategies", results.get(1).getTitle());
		assertEquals(2, results.get(1).getPageId());
	}

	@Test
	public void emptySearchResponseYieldsEmptyList()
	{
		assertTrue(WikiClient.extractSearchResults(gson.fromJson("{}", JsonObject.class)).isEmpty());
	}
}
