package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.brain.UserContext;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OperationFacade {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OperationFacade.class);
	
	public static Operation translate(String text, UserContext context) {
		Operation translation = new Operation();
		
		fillTimeInformation(text, translation);
		fillMessageAndAddress(text, translation, context);
		
		return translation;
	}

	private static void fillMessageAndAddress(String text, Operation translation, UserContext context) {
		for (Rules option : Rules.values()) {
			if (option.mPredicate.test(Pair.of(text, context))) {
				translation.action = option.mAddress;
				context.thisMessage = text;
				translation.message = option.mFunction.apply(tokenizer(text), context);
				translation.response = option.mResponse;
				
				return;
			}
		}
	}
	
	private static String[] tokenizer(String text) {
		return text.split(" ");
	}

	private static void fillTimeInformation(String message, Operation translation) {
		long delay = TimeFactory.findTimeAndTransform(message);
		if (delay > 0) {
			LOGGER.info("Setting delay of [{}]", delay);
			translation.delayInfo = delay;
		} else {
			LOGGER.debug("no time delay info from message [{}]", message);
		}
	}
	

	private static void addEveryDayInfo(Operation translation) {
		translation.everyDay = true;
	}
	
}
