package es.xan.servantv3.homeautomation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.Events.Room;
import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.MessageUtils;
import es.xan.servantv3.homeautomation.HomeVerticle.Actions.BossMessage;
import es.xan.servantv3.network.RouterPageManager.Device;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.ParrotMessage;
import es.xan.servantv3.sensors.SensorVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Home automation verticle.
 * 
 * @author alopez
 *
 */
public class HomeVerticle extends AbstractServantVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeVerticle.class);
	
	public HomeVerticle() {
		super(Constant.HOME_VERTICLE);
		
		supportedActions(
			Actions.values());
		
		supportedEvents(
			Events.NO_TEMPERATURE_INFO,  
			Events.NEW_NETWORK_DEVICES_MESSAGE,
			Events.REM_NETWORK_DEVICES_MESSAGE);
	}
	
	public static class Person {
		String name;
		boolean inHome;
	}
	
	public enum Actions implements Action {
		GET_HOME_STATUS(null),
		NOTIFY_BOSS(Message.class)
		;
		
		public static class BossMessage { public String text; }

		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return beanClass;
		}
		
	}
	
	public void get_home_status(Message<Object> message) {
		ReplyBuilder builder = MessageBuilder.createReply();
		
		List<JsonObject> persons = this.mPopulation.values().stream().map(person -> { return new JsonObject(JsonUtils.toJson(person));}).collect(Collectors.toList());
		builder.setResult(persons);
		
		message.reply(builder.build());
	}
	
	private Map<String, Person> mPopulation = new HashMap<>();
	private String mBoss;
	
	private static final boolean OUTSIDE_HOME = false;
	private static final boolean INSIDE_HOME = true;
	
	
	@SuppressWarnings("unchecked")
	public void start() {
		super.start();
		
		final JsonArray homeConfig = vertx.getOrCreateContext().config().getJsonArray("HomeVerticle");
		this.mPopulation = loadPopulation(homeConfig.getList());
		
		final JsonArray mastersConfig = vertx.getOrCreateContext().config().getJsonArray("masters");
		this.mBoss = loadBoss(mastersConfig.getList());
		
		LOGGER.info("Started HomeVerticle");
	}
	
	public void no_temperature_info(Room room) {
		notify_boss(new BossMessage() {{ this.text = "no temperature info since 1 hour for room " + room.room; }});
		publishAction(SensorVerticle.Actions.RESET_SENSOR, new SensorVerticle.Actions.Sensor() {{ this.sensor = room.room;}},
				msg -> {if (MessageUtils.isOk(msg)) {
						notify_boss(new BossMessage() {{ this.text = "Sensor reseted"; }});
					} else {
						notify_boss(new BossMessage() {{ this.text = "Cannot reset sensor"; }});
					}});
	}
	
	
	public void notify_boss(BossMessage content) {
		ParrotMessage message = new ParrotMessage();
		message.user = this.mBoss;
		message.message = content.text;
		publishAction(ParrotVerticle.Actions.SEND, message);
		
	}


	public void new_network_devices_message(Device device) {
		Person person = mPopulation.get(device.mac);
		
		if (person == null) return;
		
		if (!person.inHome) {
			updatePerson(person, INSIDE_HOME);
		}
	}

	public void rem_network_devices_message(Device device) {
		Person person = mPopulation.get(device.mac);
		
		if (person == null) return;
		
		if (person.inHome) {
			updatePerson(person, OUTSIDE_HOME);
		}
	}
		
	private void updatePerson(Person person, boolean atHome) {
		person.inHome = atHome;
		if (atHome) {
			publishEvent(Events.PERSON_AT_HOME, person);
		} else {
			publishEvent(Events.PERSON_LEAVE_HOME, person);
		}
	}


	private String loadBoss(List<JsonObject> list) {
		JsonObject emailInfo = list.get(0);
		return emailInfo.getString("email");
	}

	private Map<String, Person> loadPopulation(List<JsonObject> l_devices) {
		final Map<String,Person> result = new HashMap<>();
		l_devices.stream().forEach(it -> {
			result.put(
					it.getString("mac"),
					new Person() {{
						this.name = it.getString("name");
						this.inHome = false;
			}});
		});
		
		return result;
	}


}
