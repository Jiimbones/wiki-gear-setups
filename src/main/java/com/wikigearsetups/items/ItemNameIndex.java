package com.wikigearsetups.items;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Bidirectional item name/id lookup. Backed by a scan of client item
 * definitions at runtime; faked in tests.
 */
public interface ItemNameIndex
{
	/** Item ids whose in-game name matches (after {@link ItemNames#normalize}). Empty if none or not ready. */
	List<Integer> idsByName(String normalizedName);

	@Nullable
	String nameById(int itemId);

	boolean isReady();
}
