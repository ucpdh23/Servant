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
		if (message.toLowerCase().contains(" at ")) {
			addtimingInfo(message, translation);
			if (message.toLowerCase().contains("every day")) {
				addEveryDayInfo(translation);
			}
		} else if (message.toLowerCase().contains(" in ")) {
			addtimingInfoIn(message, translation);
		}
	}
	
	protected static void addtimingInfoIn(String message, Translation translation) {
		final int indexOf = message.indexOf(" in ");
		
		final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
		final Matcher matcher = compile.matcher(message);
		
		if (matcher.find(indexOf)) {
			final String str_min = matcher.group(1);
			final String str_sec = matcher.group(2);
			
			final int min = Integer.parseInt(str_min);
			final int sec = Integer.parseInt(str_sec);
				        
	        translation.delayInfo = min * 60 + sec;
		}
	}
	
	protected static void addtimingInfo(String message, Translation translation) {
		final int indexOf = message.indexOf(" at ");
		
		final Pattern compile = Pattern.compile("(\\d+):(\\d+)");
		final Matcher matcher = compile.matcher(message);
		
		if (matcher.find(indexOf)) {
			final String str_hour = matcher.group(1);
			final String str_min = matcher.group(2);
			
			final int hour = Integer.parseInt(str_hour);
			final int min = Integer.parseInt(str_min);
			
			final LocalDateTime localNow = LocalDateTime.now();
			final ZoneId currentZone = ZoneId.of("Europe/Madrid");
			final ZonedDateTime zonedNow = ZonedDateTime.of(localNow, currentZone);
			ZonedDateTime zonedNextScheduled = zonedNow.withHour(hour).withMinute(min).withSecond(0);
	        if(zonedNow.compareTo(zonedNextScheduled) > 0)
	        	zonedNextScheduled = zonedNextScheduled.plusDays(1);

	        final Duration duration = Duration.between(zonedNow, zonedNextScheduled);
	        final long initialDelay = duration.getSeconds();
	        
	        translation.delayInfo = initialDelay;
		}
	}
	
	private static void addEveryDayInfo(Translation translation) {
		translation.everyDay = true;
	}
	
}
