package com.wikigearsetups.layout;

public enum LayoutStyle
{
	EQUIPMENT_SCREEN("Equipment screen"),
	COMPACT("Compact rows");

	private final String label;

	LayoutStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
