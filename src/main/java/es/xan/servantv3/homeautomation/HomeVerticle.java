package es.xan.servantv3.homeautomation;

import static es.xan.servantv3.Scheduler.at;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.MessageUtils;
import es.xan.servantv3.Scheduler;
import es.xan.servantv3.messages.*;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.temperature.TemperatureUtils;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
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

	protected Cache<String, String> memory = CacheBuilder.newBuilder()
			.expireAfterAccess(2, TimeUnit.MINUTES)
			.expireAfterWrite(2, TimeUnit.MINUTES)
			.build();
	
	public HomeVerticle() {
		super(Constant.HOME_VERTICLE);
		
		supportedActions(
			Actions.values());
		
		supportedEvents(
			Events.PARRONT_AVAILABLE,
			Events.NO_TEMPERATURE_INFO,  
			Events.NEW_NETWORK_DEVICES_MESSAGE,
			Events.REM_NETWORK_DEVICES_MESSAGE,
			Events.LAUNDRY_OFF,
			Events._EVENT_);
	}
	
	public enum Actions implements Action {
		GET_HOME_STATUS(null),
		NOTIFY_BOSS(TextMessageToTheBoss.class),
		NOTIFY_ALL_BOSS(TextMessageToTheBoss.class),
		REPORT_TEMPERATURE(null),
		MANAGE_VIDEO(Recorded.class),
		RECORD_VIDEO(null),
		SHUTDOWN_SECURITY(null)
		;
		
		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return beanClass;
		}
	}

	public void shutdown_security(final Message<Object> msg) {
		this.publishRawAction("SHUTDOWN_SECURITY");
	}

	public void record_video(final Message<Object> msg) {
		try {
			Boolean waitingVideo = Boolean.parseBoolean(memory.get("WAITING_VIDEO", () -> "false"));
			LOGGER.info("WaitingVideo [{}]", waitingVideo);

			if (!waitingVideo) {
				LOGGER.info("publishing event");

				this.publishRawAction("RECORD_VIDEO", new Recording(10, "CODE"));
				LOGGER.debug("Waiting video");
				memory.put("WAITING_VIDEO", "true");
			} else {
				LOGGER.info("cannot publish RECORD_VIDEO action");
			}
		} catch (ExecutionException e) {
			LOGGER.warn(e);
		}

		MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
		builderOn.setOk();
		msg.reply(builderOn.build());
	}

	public void manage_video(Recorded recorded) {
		LOGGER.debug("recorded [{}-{}-{}]", recorded.getFilepath(), new File(recorded.getFilepath()).exists(), recorded.getCode());
		memory.put("WAITING_VIDEO", "false");

		this.mMasters.forEach( master -> {
			VideoMessage message = new VideoMessage(master, "", recorded.getFilepath());
			publishAction(ParrotVerticle.Actions.SEND_VIDEO, message);
		});
	}

	public void _event_(Event event) {
		LOGGER.debug("proceesing event");

		LOGGER.debug("event. [{}-{}]", event.getName(), event.getStatus());
		if ("door".equals(event.getName()) && event.getStatus().startsWith("BUTTON")) {
			LOGGER.info("processing door...");
			try {
				Boolean waitingVideo = Boolean.parseBoolean(memory.get("WAITING_VIDEO", () -> "false"));
				LOGGER.info("WaitingVideo [{}]", waitingVideo);

				if (!waitingVideo) {
					LOGGER.info("publishing event");

					this.publishRawAction("RECORD_VIDEO", new Recording(10, "CODE"));
					LOGGER.debug("Waiting video");
					memory.put("WAITING_VIDEO", "true");
				} else {
					LOGGER.info("cannot publish RECORD_VIDEO action");
				}
			} catch (ExecutionException e) {
				LOGGER.warn(e);
			}
		} else if ("door".equals(event.getName()) && event.getStatus().startsWith("temp")) {
			String data = event.getStatus().split("=")[1];
			String s_securityTemp = data.substring(0, data.indexOf("'"));
			LOGGER.debug("Computed temperature [{}]", s_securityTemp);

			Float temperature = Float.parseFloat(s_securityTemp);
			if (temperature > 45) {
				for (String master : this.mMasters) {
					publishAction(ParrotVerticle.Actions.SEND, new TextMessage(master, "Temperature de rasp de seguridad muy alta " + temperature));
				}
			}
		} else {
			LOGGER.debug("unsupported event");
		}
	}

	public void get_home_status(Message<Object> message) {
		ReplyBuilder builder = MessageBuilder.createReply();
		
		List<JsonObject> persons = this.mPopulation.values().stream().map(person -> { return JsonUtils.toJson(person);}).collect(Collectors.toList());
		builder.setResult(persons);
		
		message.reply(builder.build());
	}
	
	public void report_temperature(Message<Object> message) {
		publishAction(TemperatureVerticle.Actions.QUERY, TemperatureUtils.buildMinTempQuery("bedRoom", 10 * 3600 * 1000), response -> {
			for (String master : this.mMasters) {
				publishAction(ParrotVerticle.Actions.SEND, new TextMessage(master, TemperatureUtils.toString(response.result())));
			}
		});
	}
	
	private Map<String, Person> mPopulation = new HashMap<>();
	private List<String> mMasters;
	private String mBoss;
	private Scheduler mScheduler;
	private UUID mScheduledTask;
	private UUID mScheduledStartBoilerTask;
	private UUID mScheduledStopBoilerTask;
	
	private static final boolean OUTSIDE_HOME = false;
	private static final boolean INSIDE_HOME = true;
	
	
	@SuppressWarnings("unchecked")
	public void start() {
		super.start();
		
		final JsonArray homeConfig = vertx.getOrCreateContext().config().getJsonArray("HomeVerticle");
		this.mPopulation = loadPopulation(homeConfig.getList());

		JsonArray masters = vertx.getOrCreateContext().config().getJsonArray("masters");
		this.mMasters = loadMasters(masters.getList());
		this.mBoss = this.mMasters.get(0);
		
		this.mScheduler = new Scheduler(getVertx());
		this.mScheduledTask = mScheduler.scheduleTask(at(LocalTime.of(8,0)), (UUID id) -> { publishAction(Actions.REPORT_TEMPERATURE);  return true; });
//		this.mScheduledStartBoilerTask = mScheduler.scheduleTask(at(LocalTime.of(15,0)), (UUID id) -> { publishAction(ThermostatVerticle.Actions.SWITCH_BOILER, new UpdateState("on"), response -> {
//			for (String master : this.mMasters) {
//				publishAction(ParrotVerticle.Actions.SEND, new TextMessage(master, "Boiler switched on " + response.result().body().toString()));
//			}
//		});  return true; });
//		this.mScheduledStopBoilerTask = mScheduler.scheduleTask(at(LocalTime.of(22,0)), (UUID id) -> { publishAction(ThermostatVerticle.Actions.SWITCH_BOILER, new UpdateState("off"), response -> {
//			for (String master : this.mMasters) {
//				publishAction(ParrotVerticle.Actions.SEND, new TextMessage(master, "Boiler switched off " + response.result().body().toString()));
//			}
//		});  return true; });
		
		LOGGER.info("Started HomeVerticle");
	}

	public void no_temperature_info(Room room) {
		notify_boss(new TextMessageToTheBoss("no temperature info since 1 hour for room " + room.getName()));
		publishAction(SensorVerticle.Actions.RESET_SENSOR, new Sensor(room.getName()),
				msg -> {if (MessageUtils.isOk(msg)) {
						notify_boss(new TextMessageToTheBoss("Sensor reseted"));
					} else {
						notify_boss(new TextMessageToTheBoss("Cannot reset sensor"));
					}});
	}
	
	
	public void notify_boss(TextMessageToTheBoss content) {
		TextMessage message = new TextMessage(this.mBoss, content.getMessage());
		publishAction(ParrotVerticle.Actions.SEND, message);
	}
	
	public void notify_all_boss(TextMessageToTheBoss content) {
		this.mMasters.forEach( master -> {
			TextMessage message = new TextMessage(master, content.getMessage());
			publishAction(ParrotVerticle.Actions.SEND, message);
		});
	}

	public void new_network_devices_message(Device device) {
		Person person = mPopulation.get(device.getMac());
		
		if (person == null) return;
		
		if (!person.getInHome()) {
			updatePerson(person, INSIDE_HOME);
		}
	}

	public void rem_network_devices_message(Device device) {
		Person person = mPopulation.get(device.getMac());
		
		if (person == null) return;
		
		if (person.getInHome()) {
			updatePerson(person, OUTSIDE_HOME);
		}
	}
	
	public void laundry_off(Power power) {
		this.mMasters.forEach( master -> publishAction(ParrotVerticle.Actions.SEND, new TextMessage(master, "My laundry just finished: " + power)));
	}
	
	/**
	 * Activates the chat channels with the established users.
	 */
	public void parront_available() {
		for (String user : mMasters) {
			LOGGER.debug("creating chat for user [{}]", user);
			publishAction(es.xan.servantv3.parrot.ParrotVerticle.Actions.CREATE_CHAT, new OpenChat(user));;
		}
	}
		
	private void updatePerson(Person person, boolean atHome) {
		person.setInHome(atHome);
		
		if (atHome) {
			publishEvent(Events.PERSON_AT_HOME, person);
		} else {
			publishEvent(Events.PERSON_LEAVE_HOME, person);
		}
	}


	private List<String> loadMasters(List<JsonObject> list) {
		final List<String> result = new ArrayList<>();
		for (JsonObject item : list) {
			result.add(item.getString("email"));
		}
		
		return result;
	}


	private Map<String, Person> loadPopulation(List<JsonObject> l_devices) {
		final Map<String,Person> result = new HashMap<>();
		l_devices.stream().forEach(it -> {
			result.put(
					it.getString("mac"),
					new Person(
						it.getString("name"),
						false));
		});
		
		return result;
	}


}
