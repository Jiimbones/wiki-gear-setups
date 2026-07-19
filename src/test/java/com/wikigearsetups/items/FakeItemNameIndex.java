package com.wikigearsetups.items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Map-backed index for tests. Use real item ids so ItemVariationMapping works. */
public class FakeItemNameIndex implements ItemNameIndex
{
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<String, List<Integer>> byName = new HashMap<>();

	/** Alternating id, name pairs. */
	public static FakeItemNameIndex of(Object... idNamePairs)
	{
		FakeItemNameIndex index = new FakeItemNameIndex();
		for (int i = 0; i < idNamePairs.length; i += 2)
		{
			index.add((Integer) idNamePairs[i], (String) idNamePairs[i + 1]);
		}
		return index;
	}

	public void add(int id, String name)
	{
		names.put(id, name);
		byName.computeIfAbsent(ItemNames.normalize(name), k -> new ArrayList<>()).add(id);
	}

	@Override
	public List<Integer> idsByName(String normalizedName)
	{
		return byName.getOrDefault(normalizedName, Collections.emptyList());
	}

	@Override
	public String nameById(int itemId)
	{
		return names.get(itemId);
	}

	@Override
	public boolean isReady()
	{
		return true;
	}
}
