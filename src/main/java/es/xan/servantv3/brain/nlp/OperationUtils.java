package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.MessageUtils;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OperationUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationUtils.class);
	
	public static boolean isEveryDay(Operation item) {
		return item.everyDay;
	}

	public static boolean isScheduled(Operation item) {
		return item.delayInfo > 0;
	}

	public static boolean isForwarding(Operation item) {
		return item.forwarding;
	}
	
	public static long getSchedule(Operation item) {
		return item.delayInfo;
	}

	public static String result(Message<Object> msg) {
		if (msg == null) {
			LOGGER.debug("no message provided for result");
			return "Sorry, but something extremally weird happened";
		}

		if (MessageUtils.isOk(msg)) {
			if (MessageUtils.hasResult(msg)) {
				return MessageUtils.getResult(msg);
			} else {
				return "Without message";
			}
		}

		return "Response is not ok";
	}
	
	public static String forwarding(Message<Object> msg) {
		if (msg == null) {
			LOGGER.debug("no message provided for forwarding");
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
	
	public static Reply reply(final es.xan.servantv3.Action pAction, String pMsg) {
		return new Reply() {{
			this.action = pAction;
			this.msg = pMsg;
		}};
	}
	
	public static class Reply {
		public es.xan.servantv3.Action action;
		public String msg;
	}
}
