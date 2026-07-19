package com.wikigearsetups.wiki;

import javax.annotation.Nullable;
import lombok.Value;

/**
 * One recommended item for a slot. Items with the same rank are same-tier
 * alternatives, listed in the wiki's order of appearance (best first).
 */
@Value
public class RankedItem
{
	String name;
	int rank;
	@Nullable
	String note;
}
