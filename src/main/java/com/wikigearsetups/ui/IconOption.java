package com.wikigearsetups.ui;

/** A pickable tab icon: an item id with the name shown on hover. */
class IconOption
{
	final int itemId;
	final String name;

	IconOption(int itemId, String name)
	{
		this.itemId = itemId;
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof IconOption && ((IconOption) o).itemId == itemId;
	}

	@Override
	public int hashCode()
	{
		return itemId;
	}
}
