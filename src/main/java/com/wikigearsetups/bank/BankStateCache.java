package com.wikigearsetups.bank;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.wikigearsetups.WikiGearSetupsConfig;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;

/**
 * The bank is only readable while its widget is open, so ownership checks
 * run against a snapshot: captured whenever the bank container changes and
 * persisted per RuneScape profile (so alts and leagues don't cross-pollute).
 * Inventory and equipment are always readable and are merged in live.
 */
@Slf4j
@Singleton
public class BankStateCache
{
	static final String SNAPSHOT_KEY = "bankSnapshot";
	private static final Type SNAPSHOT_TYPE = new TypeToken<Map<Integer, Integer>>()
	{
	}.getType();

	private final Client client;
	private final ConfigManager configManager;
	private final Gson gson;

	private volatile Map<Integer, Integer> bank = Collections.emptyMap();
	private volatile boolean snapshotLoaded;

	@Inject
	public BankStateCache(Client client, ConfigManager configManager, Gson gson)
	{
		this.client = client;
		this.configManager = configManager;
		this.gson = gson;
	}

	/** Call with the bank container whenever it changes (bank must be open). */
	public void updateFromContainer(ItemContainer container)
	{
		Map<Integer, Integer> snapshot = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				snapshot.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		bank = snapshot;
		snapshotLoaded = true;
		configManager.setRSProfileConfiguration(WikiGearSetupsConfig.GROUP, SNAPSHOT_KEY, serialize(gson, snapshot));
		log.debug("Bank snapshot updated: {} distinct items", snapshot.size());
	}

	/** Loads the persisted snapshot for the current RS profile, if any. */
	public void loadSnapshot()
	{
		String json = configManager.getRSProfileConfiguration(WikiGearSetupsConfig.GROUP, SNAPSHOT_KEY);
		Map<Integer, Integer> snapshot = deserialize(gson, json);
		if (snapshot != null)
		{
			bank = snapshot;
			snapshotLoaded = true;
			log.debug("Bank snapshot loaded: {} distinct items", snapshot.size());
		}
		else
		{
			bank = Collections.emptyMap();
			snapshotLoaded = false;
		}
	}

	/** False until the player has opened their bank once (this profile). */
	public boolean hasSnapshot()
	{
		return snapshotLoaded;
	}

	public Map<Integer, Integer> getBankItems()
	{
		return Collections.unmodifiableMap(bank);
	}

	/**
	 * All item ids the player owns: bank snapshot plus live inventory and
	 * equipment. Must be called on the client thread (reads item containers).
	 *
	 * Group Ironman shared group storage is deliberately not included — only
	 * the player's own bank, inventory and worn gear count as owned.
	 */
	public Set<Integer> ownedItemIds()
	{
		Set<Integer> owned = new HashSet<>(bank.keySet());
		addContainer(owned, client.getItemContainer(InventoryID.INV));
		addContainer(owned, client.getItemContainer(InventoryID.WORN));
		return owned;
	}

	private static void addContainer(Set<Integer> owned, ItemContainer container)
	{
		if (container == null)
		{
			return;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() >= 0 && item.getQuantity() > 0)
			{
				owned.add(item.getId());
			}
		}
	}

	static String serialize(Gson gson, Map<Integer, Integer> snapshot)
	{
		return gson.toJson(snapshot, SNAPSHOT_TYPE);
	}

	static Map<Integer, Integer> deserialize(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, SNAPSHOT_TYPE);
		}
		catch (JsonSyntaxException e)
		{
			log.warn("Discarding corrupt bank snapshot", e);
			return null;
		}
	}
}
