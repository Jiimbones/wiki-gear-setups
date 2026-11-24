package com.wikigearsetups.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wikigearsetups.wiki.ExampleSetupParser;
import com.wikigearsetups.wiki.GearSetup;
import com.wikigearsetups.wiki.GearSetupParser;
import com.wikigearsetups.wiki.InventoryParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the plugin's real parsers over every live wiki page that uses
 * {{Recommended equipment}}, and reports what they would get wrong.
 *
 * Fixtures freeze a handful of pages; the wiki keeps moving. Both of
 * 1.0.1's parser bugs — the two-handed column and {{Loadout}} — were
 * invisible to the test suite and found by exactly this kind of sweep, so
 * run it before each release:
 *
 * <pre>./gradlew corpusCheck</pre>
 *
 * Exits non-zero if a page crashes a parser or carries a gear parameter or
 * inventory-shaped template the parsers skip. Listed for review only, since
 * they are the page's problem rather than ours: pages that parse fewer
 * setups than they have templates (an empty or misused template), and slot
 * names missing their rank, which the wiki doesn't display either.
 *
 * Covers the pages the plugin can open: search reaches the main namespace,
 * and PageDiscovery also tries "Guide:&lt;name&gt; setups". The rest of the
 * Guide namespace is player-written guides, past Leagues among them, that
 * the plugin never shows.
 *
 * Needs the network, so it lives here rather than in the test suite. Polite
 * to the wiki: a descriptive User-Agent, 50 pages per request, a pause
 * between requests.
 */
public final class CorpusCheck
{
	private static final String API = "https://oldschool.runescape.wiki/api.php";
	private static final String USER_AGENT =
		"wiki-gear-setups corpus check (https://github.com/Jiimbones/wiki-gear-setups)";
	private static final Pattern TEMPLATE_CALL = Pattern.compile("\\{\\{\\s*[Rr]ecommended[ _]equipment\\b");
	private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	private static final int BATCH = 50;
	private static final int GUIDE_NAMESPACE = 3002;

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
	private final Gson gson = new Gson();

	private CorpusCheck()
	{
	}

	public static void main(String[] args) throws Exception
	{
		System.exit(new CorpusCheck().run());
	}

	private int run() throws IOException, InterruptedException
	{
		List<String> titles = pagesUsingTemplate();
		Map<String, String> pages = fetchWikitext(titles);
		System.out.printf("Checking %d pages that use {{Recommended equipment}}%n%n", pages.size());

		Map<String, String> crashed = new TreeMap<>();
		List<String> nothingParsed = new ArrayList<>();
		List<String> partial = new ArrayList<>();
		Map<String, Set<String>> unknownParams = new TreeMap<>();
		Map<String, Set<String>> unknownInventories = new TreeMap<>();
		Map<String, Set<String>> unranked = new TreeMap<>();
		int setups = 0;
		int inventories = 0;
		int examples = 0;

		for (Map.Entry<String, String> page : pages.entrySet())
		{
			String title = page.getKey();
			String text = page.getValue();
			int templates = count(TEMPLATE_CALL, HTML_COMMENT.matcher(text).replaceAll(""));
			try
			{
				List<GearSetup> parsed = new GearSetupParser().parse(text);
				setups += parsed.size();
				inventories += new InventoryParser().parse(text).size();
				examples += new ExampleSetupParser().parse(text).size();

				if (templates > 0 && parsed.isEmpty())
				{
					nothingParsed.add(title);
				}
				else if (parsed.size() < templates)
				{
					partial.add(String.format("%s (%d of %d)", title, parsed.size(), templates));
				}
				for (String param : GearSetupParser.unrecognisedParams(text))
				{
					unknownParams.computeIfAbsent(param, k -> new TreeSet<>()).add(title);
				}
				for (String template : InventoryParser.unrecognisedInventoryTemplates(text))
				{
					unknownInventories.computeIfAbsent(template, k -> new TreeSet<>()).add(title);
				}
				for (String param : GearSetupParser.unrankedSlotParams(text))
				{
					unranked.computeIfAbsent(title, k -> new TreeSet<>()).add(param);
				}
			}
			catch (RuntimeException e)
			{
				crashed.put(title, e.toString());
			}
		}

		System.out.printf("Parsed %d setups, %d inventories, %d example loadouts%n%n", setups, inventories, examples);
		report("Pages that crash a parser", crashed.entrySet(), e -> e.getKey() + ": " + e.getValue());
		report("Gear parameters the parser skips", unknownParams.entrySet(), e -> e.getKey() + " on " + e.getValue());
		report("Inventory-shaped templates the parser skips", unknownInventories.entrySet(),
			e -> e.getKey() + " on " + e.getValue());
		report("Template present but no setup parsed", nothingParsed, t -> t);
		report("Fewer setups than templates (review; usually an empty or misused template)", partial, t -> t);
		report("Slot names with no rank (the wiki doesn't show these either; fix them on the wiki)",
			unranked.entrySet(), e -> e.getKey() + ": " + e.getValue());

		boolean failed = !crashed.isEmpty() || !unknownParams.isEmpty() || !unknownInventories.isEmpty()
			|| !nothingParsed.isEmpty();
		System.out.println(failed ? "FAILED: the parsers miss something on the live wiki" : "OK: nothing on the live wiki goes unread");
		return failed ? 1 : 0;
	}

	private static <T> void report(String heading, Iterable<T> items, java.util.function.Function<T, String> line)
	{
		List<String> lines = new ArrayList<>();
		items.forEach(item -> lines.add(line.apply(item)));
		System.out.printf("%s: %s%n", heading, lines.isEmpty() ? "none" : lines.size());
		lines.forEach(l -> System.out.println("  " + l));
		System.out.println();
	}

	private static int count(Pattern pattern, String text)
	{
		int n = 0;
		Matcher m = pattern.matcher(text);
		while (m.find())
		{
			n++;
		}
		return n;
	}

	/** Pages transcluding the template that the plugin can open (see class comment). */
	private List<String> pagesUsingTemplate() throws IOException, InterruptedException
	{
		List<String> titles = new ArrayList<>();
		String cont = null;
		do
		{
			Map<String, String> params = new LinkedHashMap<>();
			params.put("action", "query");
			params.put("list", "embeddedin");
			params.put("eititle", "Template:Recommended equipment");
			params.put("eilimit", "500");
			params.put("format", "json");
			params.put("formatversion", "2");
			if (cont != null)
			{
				params.put("eicontinue", cont);
			}
			JsonObject res = post(params);
			for (JsonElement e : res.getAsJsonObject("query").getAsJsonArray("embeddedin"))
			{
				int ns = e.getAsJsonObject().get("ns").getAsInt();
				String title = e.getAsJsonObject().get("title").getAsString();
				if (ns == 0 || (ns == GUIDE_NAMESPACE && title.endsWith(" setups")))
				{
					titles.add(title);
				}
			}
			JsonObject next = res.getAsJsonObject("continue");
			cont = next == null ? null : next.get("eicontinue").getAsString();
		}
		while (cont != null);
		return titles;
	}

	private Map<String, String> fetchWikitext(List<String> titles) throws IOException, InterruptedException
	{
		Map<String, String> pages = new TreeMap<>();
		for (int i = 0; i < titles.size(); i += BATCH)
		{
			Map<String, String> params = new LinkedHashMap<>();
			params.put("action", "query");
			params.put("prop", "revisions");
			params.put("rvprop", "content");
			params.put("rvslots", "main");
			params.put("format", "json");
			params.put("formatversion", "2");
			params.put("titles", String.join("|", titles.subList(i, Math.min(i + BATCH, titles.size()))));
			JsonArray results = post(params).getAsJsonObject("query").getAsJsonArray("pages");
			for (JsonElement e : results)
			{
				JsonObject p = e.getAsJsonObject();
				JsonArray revisions = p.getAsJsonArray("revisions");
				if (revisions != null && revisions.size() > 0)
				{
					pages.put(p.get("title").getAsString(), revisions.get(0).getAsJsonObject()
						.getAsJsonObject("slots").getAsJsonObject("main").get("content").getAsString());
				}
			}
			Thread.sleep(500);
		}
		return pages;
	}

	private JsonObject post(Map<String, String> params) throws IOException, InterruptedException
	{
		StringBuilder form = new StringBuilder();
		for (Map.Entry<String, String> p : params.entrySet())
		{
			if (form.length() > 0)
			{
				form.append('&');
			}
			form.append(URLEncoder.encode(p.getKey(), StandardCharsets.UTF_8)).append('=')
				.append(URLEncoder.encode(p.getValue(), StandardCharsets.UTF_8));
		}
		HttpRequest request = HttpRequest.newBuilder(URI.create(API))
			.header("User-Agent", USER_AGENT)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.timeout(Duration.ofSeconds(60))
			.POST(HttpRequest.BodyPublishers.ofString(form.toString()))
			.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200)
		{
			throw new IOException("wiki API returned " + response.statusCode());
		}
		return gson.fromJson(response.body(), JsonObject.class);
	}
}
