package es.xan.servantv3;

import com.google.gson.Gson;

import com.google.gson.reflect.TypeToken;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Type;
import java.util.List;

public class JsonUtils {
	private static final Gson GSON = new Gson();
	
	public static JsonObject toJson(Object item) {
		if (item instanceof JsonObject)
			return (JsonObject) item;
		else
			return new JsonObject(GSON.toJson(item));
	}

	public static <T> T toBean(String json, Class<T> valueType) {
		return GSON.fromJson(json, valueType);
	}

	public static <T> List<T> toListBean(String json, Class<T> valueType) {
		Type listType = TypeToken.getParameterized(List.class, valueType).getType();
		return GSON.fromJson(json, listType);
	}

}
