package com.wikigearsetups.bank;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BankStateCacheTest
{
	private final Gson gson = new Gson();

	@Test
	public void snapshotRoundTrips()
	{
		Map<Integer, Integer> snapshot = new HashMap<>();
		snapshot.put(4151, 1);
		snapshot.put(995, 1_234_567);

		String json = BankStateCache.serialize(gson, snapshot);
		assertEquals(snapshot, BankStateCache.deserialize(gson, json));
	}

	@Test
	public void missingOrCorruptSnapshotYieldsNull()
	{
		assertNull(BankStateCache.deserialize(gson, null));
		assertNull(BankStateCache.deserialize(gson, ""));
		assertNull(BankStateCache.deserialize(gson, "{not json"));
	}
}
