package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.Action;
import es.xan.servantv3.MessageUtils;
import io.vertx.core.eventbus.Message;

public class TranslationUtils {
	
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
		if (msg == null) return "Sorry, but something extremally weird happened";
		
		if (MessageUtils.isOk(msg)) {
			return "Your wish is my command";
		} else {
			final String message = MessageUtils.getErrorMessage(msg);
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
