package es.xan.servantv3;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;

public class JsonUtils {
	private static final Gson GSON = new Gson();
	
	public static JsonObject toJson(Object item) {
		return new JsonObject(GSON.toJson(item));
	}
	
	public static <T> T toBean(String json, Class<T> valueType) {
		return GSON.fromJson(json, valueType);
	}

}
