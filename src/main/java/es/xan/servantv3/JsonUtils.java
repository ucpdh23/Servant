package es.xan.servantv3;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.setVisibility(mapper.getSerializationConfig()
				.getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		
	}
	
	public static String toJson(Object item) {
		try {
			return mapper.writeValueAsString(item);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static <T> T toBean(String json, Class<T> valueType) {
		try {
			return mapper.readValue(json, valueType);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
