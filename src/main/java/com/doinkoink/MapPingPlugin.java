package com.doinkoink;

import com.doinkoink.messages.MapPin;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.HashMap;

@Slf4j
@PluginDescriptor(
	name = "Party World Map Ping"
)
public class MapPingPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MapPingConfig config;

	@Inject
	private PartyService partyService;

	@Inject
	private WorldMapOverlay worldMapOverlay;

	@Inject
	private WorldMapPointManager worldMapPointManager;

	public HashMap<String, WorldMapPoint> markers = new HashMap<>();

	private static final BufferedImage MAP_PIN = ImageUtil.getResourceStreamFromClass(MapPingPlugin.class, "/map_pin.png");

	private Point mousePosOnMenuOpened;

	@Override
	protected void startUp() throws Exception
	{

	}

	@Override
	protected void shutDown() throws Exception
	{

	}

	@Subscribe
	public void onMenuOpened(final MenuOpened event) {
		mousePosOnMenuOpened = client.getMouseCanvasPosition();

		final Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);

		if (map == null) {
			return;
		}

		if (map.getBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY())) {
			final MenuEntry[] entries = event.getMenuEntries();
			client.createMenuEntry(0)
				.setOption("Send")
				.setTarget("Ping")
				.onClick(e -> {
					WorldPoint target = calculateMapPoint(mousePosOnMenuOpened);
					final MapPin pin = new MapPin(target, client.getLocalPlayer().getName());

					setTarget(pin);
					partyService.send(pin);
				}
			);
		}
	}

	@Subscribe
	public void onMapPin(MapPin mapPin) {
		System.out.println("Recieved pin from: " + mapPin.getMember());
	}

	private void setTarget(MapPin pin) {
		//this.target = target;
		//pathUpdateScheduled = true;
		if (!markers.containsKey(pin.getMember())) {
			markers.put(pin.getMember(), null);
		}

		WorldMapPoint marker = markers.get(pin.getMember());

		WorldMapPoint finalMarker = marker;
		worldMapPointManager.removeIf(x -> x == finalMarker);

		marker = new WorldMapPoint(pin.getMapPoint(), MAP_PIN);
		marker.setImagePoint(new Point(25, 50));
		marker.setName(pin.getMember() + "'s Pin");
		marker.setTarget(marker.getWorldPoint());
		marker.setJumpOnClick(true);
		marker.setSnapToEdge(true);

		worldMapPointManager.add(marker);
	}

	private WorldPoint calculateMapPoint(Point point) {
		float zoom = client.getRenderOverview().getWorldMapZoom();
		RenderOverview renderOverview = client.getRenderOverview();
		final WorldPoint mapPoint = new WorldPoint(renderOverview.getWorldMapPosition().getX(), renderOverview.getWorldMapPosition().getY(), 0);
		final Point middle = worldMapOverlay.mapWorldPointToGraphicsPoint(mapPoint);

		final int dx = (int) ((point.getX() - middle.getX()) / zoom);
		final int dy = (int) ((-(point.getY() - middle.getY())) / zoom);

		return mapPoint.dx(dx).dy(dy);
	}

	@Provides
	MapPingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MapPingConfig.class);
	}
}
