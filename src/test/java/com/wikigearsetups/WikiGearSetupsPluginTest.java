package com.wikigearsetups;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WikiGearSetupsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WikiGearSetupsPlugin.class);
		RuneLite.main(args);
	}
}
