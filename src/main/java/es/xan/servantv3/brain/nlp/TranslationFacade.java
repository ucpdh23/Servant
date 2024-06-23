package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.brain.UserContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranslationFacade {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TranslationFacade.class);
	
	public static Translation translate(String text, UserContext context) {
		Translation translation = new Translation();
		
		fillTimeInformation(text, translation);
		fillMessageAndAddress(text, translation, context);
		
		return translation;
	}

	private static void fillMessageAndAddress(String text, Translation translation, UserContext context) {
		for (Rules option : Rules.values()) {
			if (option.mPredicate.test(Pair.of(text, context))) {
				LOGGER.debug("Applying rule [{}]", option);
				
				translation.action = option.mAddress;
				translation.message = option.mFunction.apply(tokenizer(text), context);
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
