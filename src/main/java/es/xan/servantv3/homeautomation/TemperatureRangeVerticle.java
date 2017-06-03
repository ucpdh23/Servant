package es.xan.servantv3.homeautomation;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.lang3.Range;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.Events.TemperatureOOR;
import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.homeautomation.TemperatureRangeVerticle.Actions.RangeDetails;
import es.xan.servantv3.homeautomation.TemperatureRangeVerticle.Actions.SwitchButton;
import es.xan.servantv3.temperature.TemperatureVerticle.Actions.Temperature;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TemperatureRangeVerticle  extends AbstractServantVerticle {
	private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureRangeVerticle.class);
	
	public TemperatureRangeVerticle() {
		super(Constant.HOME_VERTICLE);
		
		supportedActions(
			Actions.values());
		
		supportedEvents(
			Events.TEMPERATURE_RECEIVED
		);
	}
	
	private Map<String, MonitoredRoomInfo> mMonitoredRooms;
	
	public enum Actions implements Action {
		SWITCH_BUTTON(SwitchButton.class),
		UPDATE_RANGE(RangeDetails.class)
		;
		
		public static class SwitchButton { public Boolean on; }
		public static class RangeDetails { public String room; public int startHour, endHour; public float minTemp, maxTemp; }

		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return beanClass;
		}
	}
	
	public void start() {
		super.start();

		loadConfiguration(vertx.getOrCreateContext().config().getJsonObject("TemperatureRangeVerticle"));
		
		LOGGER.info("Started NightModeVerticle");
	}
	
	private void loadConfiguration(JsonObject config) {
		this.mMonitoredRooms = new HashMap<String, MonitoredRoomInfo>();
		JsonArray rooms = config.getJsonArray("rooms");
		for (int i=0; i < rooms.getList().size(); i++) {
			final JsonObject roomDetails = (JsonObject) rooms.getList().get(i);
			
			RangeDetails bean = JsonUtils.toBean(roomDetails.encodePrettily(), RangeDetails.class);
			update_range(bean);
		}
	}
	
	public void update_range(RangeDetails details) {
		MonitoredRoomInfo monitoredRoomInfo = this.mMonitoredRooms.get(details.room);
		if (monitoredRoomInfo == null) {
			monitoredRoomInfo = new MonitoredRoomInfo(details.room);
			this.mMonitoredRooms.put(details.room, monitoredRoomInfo);
		}
		
		monitoredRoomInfo.appendRange(details.startHour, details.endHour, details.minTemp, details.maxTemp);
	}
	
	private Boolean mEnabled = Boolean.FALSE;
	
	public void switch_button(SwitchButton button) {
		this.mEnabled = button.on;
	}
	
	public void temperature_received(Temperature temp) {
		if (this.mEnabled) {
			MonitoredRoomInfo roomInfo = this.mMonitoredRooms.get(temp.room);
			if (roomInfo == null) return;
			
			Range<Float> temperatures = roomInfo.resolveTemperatureRange(LocalTime.now().getHour());
			if (temperatures == null) return;
			
			if (!temperatures.contains(temp.temperature)) {
				publishEvent(Events.TEMPERATURE_OUT_OF_RANGE, new TemperatureOOR() {{
						this.rangeMax = temperatures.getMaximum();
						this.rangeMin = temperatures.getMinimum();
						this.temperature = temp;
						this.toHot = (temperatures.isBefore(temperature.temperature));
				}});
			}
		}
	}
	
	private static class MonitoredRoomInfo {
		private String room;
		NavigableMap<Integer, Range<Float>> map = new TreeMap<Integer, Range<Float>>();

		MonitoredRoomInfo(String room) {
			this.room = room;
		}
		
		Range<Float> resolveTemperatureRange(int hour) {
			return map.get(hour);
		}

		Range<Float> appendRange(int startHour, int endHour, float minTemperature, float maxTemperature) {
			final Range<Float> tempRange = Range.between(minTemperature, maxTemperature);
			
			for (int i = 0; i < (endHour - startHour) % 24; i++) {
				this.map.put((i + startHour) % 24, tempRange);
			}
			
			return tempRange;
		}
		
		
	}
	
}
