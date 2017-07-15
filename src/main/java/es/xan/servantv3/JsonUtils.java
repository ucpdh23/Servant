package es.xan.servantv3;

import com.google.gson.Gson;

public class JsonUtils {
	private static final Gson GSON = new Gson();

	public static String toJson(Object item) {
		return GSON.toJson(item);
	}
	
	public static <T> T toBean(String json, Class<T> valueType) {
		return GSON.fromJson(json, valueType);
	}

}
