package com.wikigearsetups.wiki;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Value;

/**
 * One recommended item for a slot. Items with the same rank are same-tier
 * alternatives, listed in the wiki's order of appearance (best first).
 *
 * {@code fallbackNames} carries the other names a {{plink}} offered — its
 * display text and its picture — for when the linked target is a wiki page
 * rather than an item ("God capes#Imbuing" with txt "Imbued god cape").
 * They are only consulted if the primary name doesn't resolve, so a normal
 * link with a shortened display text is unaffected.
 */
@Value
public class RankedItem
{
	String name;
	int rank;
	@Nullable
	String note;
	List<String> fallbackNames;

	public RankedItem(String name, int rank, @Nullable String note)
	{
		this(name, rank, note, Collections.emptyList());
	}

	public RankedItem(String name, int rank, @Nullable String note, List<String> fallbackNames)
	{
		this.name = name;
		this.rank = rank;
		this.note = note;
		this.fallbackNames = fallbackNames;
	}
}
