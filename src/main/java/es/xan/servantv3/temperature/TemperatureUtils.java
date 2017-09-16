package es.xan.servantv3.temperature;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.messages.Temperature;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class TemperatureUtils {
	
	public static String toString(Message<Object> msg) {
		JsonObject result = (JsonObject) msg.body();
		JsonArray temperatures = result.getJsonArray("result");
		@SuppressWarnings("unchecked")
		List<JsonObject> items = temperatures.getList();
			
		StringBuilder builder = new StringBuilder();
		for (JsonObject item : items) {
			Temperature temperature = JsonUtils.toBean(item.encode(), Temperature.class);
			builder.append("room:").append(temperature.getRoom()).append(" ");
			builder.append("temperature:").append(temperature.getTemperature()).append(" ");
			builder.append("at ").append(localTimezome(new Date(temperature.getTimestamp())));
			builder.append("\n");
		}
		
		return builder.toString();
	}
	
	private static String localTimezome(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getDefault());
		return sdf.format(date);
	}
	
	public static JsonObject buildMinTempQuery(final String room, long interval) {
		return MessageBuilder.createQuery()
				.filtering(filter -> {
					if (room != null)
						filter.put("room", room);
					filter.put("timestamp", new JsonObject(new HashMap<String,Object>() {{this.put("$gte", new Date().getTime() - interval);}}));
					})
				.sorting(sort -> {
					sort.put("temperature", 1);
					})
				.fielding(fields -> {
					fields.put("temperature", 1);
					fields.put("room", 1);
					fields.put("timestamp", 1);
					fields.put("_id", 0);
					})
				.limit(1)
				.build();
	}
}
