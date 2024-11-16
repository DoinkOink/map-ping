package com.doinkoink;

import com.doinkoink.messages.MapPin;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
	name = "Party World Map Ping"
)
public class MapPinPlugin extends Plugin
{
	private static final File CUSTOM_IMAGE_FILE = new File(RuneLite.RUNELITE_DIR, "custom_pin.png");

	@Inject
	private Client _client;

	@Inject
	private MapPinConfig _config;

	@Inject
	private WSClient _wsClient;

	@Inject
	private PartyService _partyService;

	@Inject
	private WorldMapOverlay _worldMapOverlay;

	@Inject
	private WorldMapPointManager _worldMapPointManager;

	public HashMap<String, WorldMapPoint> markers = new HashMap<>();

	private static MapPinImage _selectedPin;
	private static int _pinSize;

	@Override
	protected void startUp() throws Exception
	{
		_wsClient.registerMessage(MapPin.class);

		_pinSize = _config.mapPinSize();
		updateMapPinImage();
	}

	@Override
	protected void shutDown() throws Exception
	{
		_wsClient.unregisterMessage(MapPin.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if(event.getGroup().equals("mapPin"))
		{
			if (event.getKey().equals("pinStyle"))
			{
				updateMapPinImage();
			}
			if (event.getKey().equals("pinSize"))
			{
				_pinSize = _config.mapPinSize();
			}
		}
	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event) {
		Point _mousePosOnMenuOpened = _client.getMouseCanvasPosition();

		final Widget _map = _client.getWidget(ComponentID.WORLD_MAP_MAPVIEW);

		if (_map == null) {
			return;
		}

		WorldPoint target = calculateMapPoint(_mousePosOnMenuOpened);

		if (_map.getBounds().contains(_client.getMouseCanvasPosition().getX(), _client.getMouseCanvasPosition().getY())) {
			final MenuEntry[] _entries = event.getMenuEntries();

			_client.createMenuEntry(0)
				.setOption("Send")
				.setTarget("<col=ff9040>Pin</col>")
				.onClick(e -> {
					final MapPin _pin = new MapPin(target, _client.getLocalPlayer().getName(), _config.mapPinColor());

					if (_partyService.isInParty()) {
						_partyService.send(_pin);
					} else {
						setTarget(_pin);
					}
				}
			);

			for(MenuEntry entry : _entries) {
				if (entry.getTarget().contains("Pin")) {
					String _pinOwner = entry.getTarget().split("'")[0].split(">")[1];

					_client.createMenuEntry(0)
						.setOption("Remove")
						.setTarget(entry.getTarget())
						.onClick(e -> {
							if (markers.containsKey(_pinOwner)) {
								_worldMapPointManager.removeIf(x -> x == markers.get(_pinOwner));
								markers.put(_pinOwner, null);
							}
						}
					);
				}
			}
		}
	}

	@Subscribe
	public void onMapPin(MapPin _mapPin) {
		setTarget(_mapPin);
	}

	private void setTarget(MapPin _pin) {
		if (!markers.containsKey(_pin.getMember())) {
			markers.put(_pin.getMember(), null);
		}

		WorldMapPoint _marker = markers.get(_pin.getMember());

		WorldMapPoint _finalMarker = _marker;
		_worldMapPointManager.removeIf(x -> x == _finalMarker);

		_marker = new WorldMapPoint(_pin.getMapPoint(), changeColor(_selectedPin.getMapPinImage(), _pin.getPinColor()));
		_marker.setImagePoint(new Point(_pinSize / 2, _pinSize));
		_marker.setName(_pin.getMember() + "'s Pin");
		_marker.setTarget(_marker.getWorldPoint());
		_marker.setJumpOnClick(true);
		_marker.setSnapToEdge(true);

		_worldMapPointManager.add(_marker);
		markers.put(_pin.getMember(), _marker);
	}

	public static BufferedImage changeColor(BufferedImage image, Color replacement_color) {
		BufferedImage _dimg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D _graphics2D = _dimg.createGraphics();
		_graphics2D.setComposite(AlphaComposite.Src);
		_graphics2D.drawImage(image, null, 0, 0);//w  w w  .ja  v a 2  s . co m
		_graphics2D.dispose();
		for (int i = 0; i < _dimg.getHeight(); i++) {
			for (int j = 0; j < _dimg.getWidth(); j++) {
				int argb = _dimg.getRGB(j, i);
				int alpha = (argb >> 24) & 0xff;
				if (alpha > 0) {
					Color col = new Color(replacement_color.getRed(), replacement_color.getGreen(), replacement_color.getBlue(), alpha);
					_dimg.setRGB(j, i, col.getRGB());
				}
			}
		}
		return resize(_dimg);
	}

	public static BufferedImage resize(BufferedImage _img)
	{
		Image _temp = _img.getScaledInstance(_pinSize, _pinSize, Image.SCALE_SMOOTH);
		BufferedImage _dimg = new BufferedImage(_pinSize, _pinSize, BufferedImage.TYPE_INT_ARGB);

		Graphics2D  _graphics2D = _dimg.createGraphics();
		_graphics2D.drawImage(_temp, 0, 0, null);
		_graphics2D.dispose();

		return _dimg;
	}

	private WorldPoint calculateMapPoint(Point point) {
		WorldMap _worldMap = _client.getWorldMap();
		float _zoom = _worldMap.getWorldMapZoom();
		final WorldPoint _mapPoint = new WorldPoint(_worldMap.getWorldMapPosition().getX(), _worldMap.getWorldMapPosition().getY(), 0);
		final Point _middle = _worldMapOverlay.mapWorldPointToGraphicsPoint(_mapPoint);

		final int dx = (int) ((point.getX() - _middle.getX()) / _zoom);
		final int dy = (int) ((-(point.getY() - _middle.getY())) / _zoom);

		return _mapPoint.dx(dx).dy(dy);
	}

	private void updateMapPinImage()
	{
		_selectedPin = _config.selectedPin();

		if(_selectedPin == MapPinImage.CUSTOM_IMAGE)
		{
			if(CUSTOM_IMAGE_FILE.exists())
			{
				try
				{
					_selectedPin.setImage(CUSTOM_IMAGE_FILE);
				}
				catch (Exception e)
				{
					log.error("Error setting custom pin", e);
					_selectedPin = MapPinImage.DEFAULT;
				}
			}
			else
			{
				_selectedPin = MapPinImage.DEFAULT;
			}
		}
	}

	@Provides
	MapPinConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MapPinConfig.class);
	}
}
