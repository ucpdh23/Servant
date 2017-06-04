package es.xan.servantv3.capabilities.autoconfigurable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import es.xan.servantv3.AbstractServantVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AutoConfigurableUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoConfigurableUtils.class);
	
	public static boolean setup(AbstractServantVerticle verticle, Configure conf) {
		final Map<String, Field> verticleFields = Arrays.
				stream(verticle.getClass().getDeclaredFields()).
				collect(Collectors.toMap(f -> f.getName(), f -> f));
		
		final Field field = verticleFields.get(conf.field.toLowerCase());
		field.setAccessible(true);
		
		try {
			field.set(verticle, conf.value);
			return true;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			LOGGER.warn(e.getMessage(), e);
			
			return false;
		}
	}
}
