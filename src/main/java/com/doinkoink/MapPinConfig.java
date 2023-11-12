package com.doinkoink;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.Color;

@ConfigGroup("mapPin")
public interface MapPinConfig extends Config
{
	@Alpha
	@ConfigItem(
		keyName = "mapPinColor",
		name = "Map Pin Color",
		description = "Sets the color of your Map Pins.",
		position = 0
	)
	default Color mapPinColor()
	{
		return new Color(220, 138, 0);
	}
}
