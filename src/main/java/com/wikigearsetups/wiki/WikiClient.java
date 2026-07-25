package com.wikigearsetups.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
	// wiki policy wants a way to reach the maintainer; the repo's issue tracker
	// serves that without putting a personal address in every outbound request
	static final String USER_AGENT =
		"wiki-gear-setups RuneLite plugin (https://github.com/Jiimbones/wiki-gear-setups)";
	private static final Duration CACHE_TTL = Duration.ofMinutes(15);

	private final OkHttpClient http;
	private final Gson gson;
	private final ConcurrentMap<String, CachedPage> pageCache = new ConcurrentHashMap<>();
	private volatile CompletableFuture<Set<String>> petNames;

	@Inject
	public WikiClient(OkHttpClient http, Gson gson)
	{
		// the injected client has no call timeout; a hung wiki should fail
		// fast enough for the panel to show something useful
		this.http = http.newBuilder().callTimeout(Duration.ofSeconds(15)).build();
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
		}).handle((wikitext, err) ->
		{
			// wiki down: a stale cached copy beats an error
			if (err != null)
			{
				CachedPage stale = pageCache.get(title);
				if (stale != null)
				{
					log.debug("Serving stale wiki page '{}' after fetch failure", title, err);
					return stale.getWikitext();
				}
				throw new java.util.concurrent.CompletionException(err);
			}
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

	/**
	 * Every pet the wiki knows about, so a boss's pet can be told apart from
	 * the jars and clue scrolls it shares a "Tertiary" section with (their
	 * names follow no pattern — Olmlet, Lil' zik, Vorki). Fetched at most
	 * once per session; failures resolve empty rather than propagating,
	 * since this only decides a default icon.
	 */
	public CompletableFuture<Set<String>> petNames()
	{
		CompletableFuture<Set<String>> cached = petNames;
		if (cached != null)
		{
			return cached;
		}
		synchronized (this)
		{
			if (petNames == null)
			{
				HttpUrl url = API.newBuilder()
					.addQueryParameter("action", "query")
					.addQueryParameter("list", "categorymembers")
					.addQueryParameter("cmtitle", "Category:Pets")
					.addQueryParameter("cmlimit", "500")
					.addQueryParameter("format", "json")
					.addQueryParameter("formatversion", "2")
					.build();
				petNames = fetchJson(url)
					.thenApply(WikiClient::extractCategoryMembers)
					.exceptionally(e ->
					{
						log.debug("Pet category lookup failed", e);
						return Collections.emptySet();
					});
			}
			return petNames;
		}
	}

	static Set<String> extractCategoryMembers(JsonObject json)
	{
		Set<String> names = new LinkedHashSet<>();
		JsonObject query = json.getAsJsonObject("query");
		if (query == null)
		{
			return names;
		}
		JsonArray members = query.getAsJsonArray("categorymembers");
		if (members == null)
		{
			return names;
		}
		for (JsonElement el : members)
		{
			JsonElement title = el.getAsJsonObject().get("title");
			if (title != null)
			{
				names.add(title.getAsString());
			}
		}
		return names;
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
