package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.Action;
import es.xan.servantv3.MessageUtils;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TranslationUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(TranslationUtils.class);
	
	public static boolean isEveryDay(Translation item) {
		return item.everyDay;
	}

	public static boolean isScheduled(Translation item) {
		return item.delayInfo > 0;
	}

	public static boolean isForwarding(Translation item) {
		return item.forwarding;
	}
	
	public static long getSchedule(Translation item) {
		return item.delayInfo;
	}
	
	public static String forwarding(Message<Object> msg) {
		if (msg == null) {
			LOGGER.debug("no message provided");
			return "Sorry, but something extremally weird happened";
		}
		
		if (MessageUtils.isOk(msg)) {
			if (MessageUtils.hasMessage(msg)) {
				return MessageUtils.getMessage(msg);
			} else {
				return "Your wish is my command";
			}
		} else {
			final String message = MessageUtils.getMessage(msg);
			return "Sorry, something weird happens. " + ((message != null)? message : "");
		}
		
	}
	
	public static Reply reply(final Action pAction, String pMsg) {
		return new Reply() {{
			this.action = pAction;
			this.msg = pMsg;
		}};
	}
	
	public static class Reply {
		public Action action;
		public String msg;
	}
}
