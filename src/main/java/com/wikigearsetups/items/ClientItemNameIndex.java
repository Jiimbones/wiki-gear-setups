package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;

/**
 * Builds a name/id index by scanning every item definition, chunked across
 * client frames to avoid a startup stall. Noted items and placeholders are
 * excluded. Immutable maps are published via volatile fields once complete.
 */
@Slf4j
@Singleton
public class ClientItemNameIndex implements ItemNameIndex
{
	private static final int CHUNK_SIZE = 4000;

	private final Client client;
	private final ClientThread clientThread;

	private volatile Map<String, List<Integer>> byName = Collections.emptyMap();
	private volatile Map<Integer, String> byId = Collections.emptyMap();
	private volatile boolean ready;
	private volatile boolean building;

	@Inject
	public ClientItemNameIndex(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	/** Kicks off the scan if not already built or in progress. Safe to call repeatedly. */
	public void ensureBuilt()
	{
		if (ready || building)
		{
			return;
		}
		building = true;
		long start = System.currentTimeMillis();
		Map<String, List<Integer>> names = new HashMap<>();
		Map<Integer, String> ids = new HashMap<>();
		clientThread.invokeLater(() -> buildChunk(0, names, ids, start));
	}

	private void buildChunk(int from, Map<String, List<Integer>> names, Map<Integer, String> ids, long startMs)
	{
		int count = client.getItemCount();
		int to = Math.min(from + CHUNK_SIZE, count);
		for (int id = from; id < to; id++)
		{
			ItemComposition def = client.getItemDefinition(id);
			if (def == null)
			{
				continue;
			}
			String name = def.getName();
			if (name == null || name.equals("null"))
			{
				continue;
			}
			if (def.getNote() != -1 || def.getPlaceholderTemplateId() != -1)
			{
				continue;
			}
			ids.put(id, name);
			names.computeIfAbsent(ItemNames.normalize(name), k -> new ArrayList<>()).add(id);
		}

		if (to < count)
		{
			clientThread.invokeLater(() -> buildChunk(to, names, ids, startMs));
		}
		else
		{
			byName = names;
			byId = ids;
			ready = true;
			building = false;
			log.debug("Item name index built: {} names, {} items in {}ms",
				names.size(), ids.size(), System.currentTimeMillis() - startMs);
		}
	}

	@Override
	public List<Integer> idsByName(String normalizedName)
	{
		List<Integer> ids = byName.get(normalizedName);
		return ids == null ? Collections.emptyList() : ids;
	}

	@Override
	public String nameById(int itemId)
	{
		return byId.get(itemId);
	}

	@Override
	public boolean isReady()
	{
		return ready;
	}
}
