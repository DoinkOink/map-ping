package com.doinkoink;

import lombok.Getter;
import net.runelite.client.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Getter
public enum MapPinImage {
	DEFAULT("Default", "/default_pin.png"),
	CUSTOM_IMAGE("Custom Image");

	private final String name;
	private BufferedImage mapPinImage;

	MapPinImage(String name)
	{
		this.name = name;
		this.mapPinImage = null;
	}

	MapPinImage(String name, String icon)
	{
		this.name = name;
		this.mapPinImage = ImageUtil.loadImageResource(MapPinPlugin.class, icon);
	}

	@Override
	public String toString()
	{
		return name;
	}

	public void setImage(File fImage) throws IOException
	{
		BufferedImage image;
		synchronized (ImageIO.class)
		{
			image = ImageIO.read(fImage);
		}
		this.mapPinImage = image;
	}
}
