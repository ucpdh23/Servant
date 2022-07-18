package es.xan.servantv3.brain.nlp;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class TranslationFacade {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TranslationFacade.class);
	
	public static Translation translate(String text) {
		Translation translation = new Translation();
		
		fillTimeInformation(text, translation);
		fillMessageAndAddress(text, translation);
		
		return translation;
	}

	private static void fillMessageAndAddress(String text, Translation translation) {
		for (Rules option : Rules.values()) {
			if (option.mPredicate.test(text)) {
				LOGGER.debug("Applying rule [{}]", option);
				
				translation.action = option.mAddress;
				translation.message = option.mFunction.apply(tokenizer(text));
				translation.response = option.mResponse;
				
				return;
			}
		}
	}
	
	private static String[] tokenizer(String text) {
		return text.split(" ");
	}

	private static void fillTimeInformation(String message, Translation translation) {
		long delay = TimeFactory.findTimeAndTransform(message);
		if (delay > 0) {
			LOGGER.info("Setting delay of [{}]", delay);
			translation.delayInfo = delay;
		} else {
			LOGGER.debug("no time delay info from message [{}]", message);
		}
	}
	

	private static void addEveryDayInfo(Translation translation) {
		translation.everyDay = true;
	}
	
}
