package es.xan.servantv3;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MessageUtils {
	public static boolean isOk(AsyncResult<Message<Object>> msg) {
		return isOk(msg.result());
	}
	
	public static boolean isOk(Message<Object> msg) {
		if (msg == null) return false;
		
		final JsonObject response = (JsonObject) msg.body();
		return Constant.REPLY_OK.equals(response.getString("status"));
	}
	
	public static boolean hasMessage(Message<Object> msg) {
		final JsonObject response = (JsonObject) msg.body();
		return response.fieldNames().contains("message");
	}
	
	public static String getMessage(Message<Object> msg) {
		final JsonObject response = (JsonObject) msg.body();
		return response.getString("message");
	}

}
