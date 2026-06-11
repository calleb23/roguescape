package com.pluginideahub.roguescape;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RogueScapePluginTestClient
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RogueScapePlugin.class);
		RuneLite.main(args);
	}
}
