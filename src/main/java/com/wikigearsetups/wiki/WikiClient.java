package com.wikigearsetups.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Async OSRS Wiki MediaWiki API client. All requests go through OkHttp's
 * dispatcher (never the client thread) and carry a descriptive User-Agent as
 * required by wiki API policy. Page wikitext is cached in-memory.
 */
@Slf4j
@Singleton
public class WikiClient
{
	static final HttpUrl API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	static final String USER_AGENT = "wiki-gear-setups RuneLite plugin (https://github.com/Jiimbones/wiki-gear-setups)";
	private static final Duration CACHE_TTL = Duration.ofMinutes(15);

	private final OkHttpClient http;
	private final Gson gson;
	private final ConcurrentMap<String, CachedPage> pageCache = new ConcurrentHashMap<>();

	@Inject
	public WikiClient(OkHttpClient http, Gson gson)
	{
		this.http = http;
		this.gson = gson;
	}

	public CompletableFuture<String> getPageWikitext(String title)
	{
		CachedPage cached = pageCache.get(title);
		if (cached != null && cached.getFetchedAt().plus(CACHE_TTL).isAfter(Instant.now()))
		{
			return CompletableFuture.completedFuture(cached.getWikitext());
		}

		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", title)
			.addQueryParameter("prop", "wikitext")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.addQueryParameter("redirects", "1")
			.build();

		return fetchJson(url).thenApply(json ->
		{
			String wikitext = extractWikitext(json);
			pageCache.put(title, new CachedPage(wikitext, Instant.now()));
			return wikitext;
		});
	}

	/**
	 * Searches only pages that actually contain a gear table, via the
	 * CirrusSearch hastemplate keyword — status-effect and lore pages (e.g.
	 * "Venom" for a zulrah search) never show up.
	 */
	public CompletableFuture<List<SearchResult>> search(String query)
	{
		HttpUrl url = API.newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("list", "search")
			.addQueryParameter("srsearch", query + " hastemplate:\"Recommended equipment\"")
			.addQueryParameter("srlimit", "10")
			.addQueryParameter("format", "json")
			.addQueryParameter("formatversion", "2")
			.build();

		return fetchJson(url).thenApply(WikiClient::extractSearchResults);
	}

	private CompletableFuture<JsonObject> fetchJson(HttpUrl url)
	{
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();

		CompletableFuture<JsonObject> future = new CompletableFuture<>();
		http.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						future.completeExceptionally(new IOException("Wiki API HTTP " + response.code()));
						return;
					}
					JsonObject json = gson.fromJson(body.charStream(), JsonObject.class);
					if (json == null)
					{
						future.completeExceptionally(new IOException("Empty wiki API response"));
						return;
					}
					if (json.has("error"))
					{
						JsonObject error = json.getAsJsonObject("error");
						JsonElement info = error.get("info");
						future.completeExceptionally(new IOException(
							"Wiki API error: " + (info != null ? info.getAsString() : error.toString())));
						return;
					}
					future.complete(json);
				}
				catch (Exception e)
				{
					future.completeExceptionally(e);
				}
			}
		});
		return future;
	}

	static String extractWikitext(JsonObject json)
	{
		JsonObject parse = json.getAsJsonObject("parse");
		if (parse == null || !parse.has("wikitext"))
		{
			throw new IllegalStateException("Wiki API response missing parse.wikitext");
		}
		return parse.get("wikitext").getAsString();
	}

	static List<SearchResult> extractSearchResults(JsonObject json)
	{
		List<SearchResult> results = new ArrayList<>();
		JsonObject query = json.getAsJsonObject("query");
		if (query == null)
		{
			return results;
		}
		JsonArray search = query.getAsJsonArray("search");
		if (search == null)
		{
			return results;
		}
		for (JsonElement el : search)
		{
			JsonObject o = el.getAsJsonObject();
			results.add(new SearchResult(o.get("title").getAsString(), o.get("pageid").getAsInt()));
		}
		return results;
	}

	@Value
	private static class CachedPage
	{
		String wikitext;
		Instant fetchedAt;
	}
}
