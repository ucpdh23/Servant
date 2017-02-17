package es.xan.servantv3.brain.nlp;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;

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
		JsonObject response = (JsonObject) msg.body();
		if (Constant.REPLY_OK.equals(response.getString("status"))) {
			return "Your wish is my command";
		} else {
			return "Sorry, but something weird happens";
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
