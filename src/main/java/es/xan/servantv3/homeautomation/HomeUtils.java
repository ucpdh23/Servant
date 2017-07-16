package es.xan.servantv3.homeautomation;

import java.util.List;

import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.messages.Person;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class HomeUtils {
	
	@SuppressWarnings("unchecked")
	public static String toString(Message<Object> msg) {
		JsonObject result = (JsonObject) msg.body();
		JsonArray persons = result.getJsonArray("result");
		List<JsonObject> items = persons.getList();
			
		StringBuilder builder = new StringBuilder();
		for (JsonObject item : items) {
			Person person = JsonUtils.toBean(item.encode(), Person.class);
			builder.append(person.getName()).append(" ");
			
			builder.append(person.getInHome()? "at home" : "outside");
			builder.append("\n");
		}
		
		return builder.toString();
	}

}
