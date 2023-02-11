package es.xan.servantv3.temperature;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import es.xan.servantv3.AbstractMongoVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.messages.Query;
import es.xan.servantv3.messages.Room;
import es.xan.servantv3.messages.Temperature;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Registered and returns information about the temperature.
 * <p>
 * Emits:<br>
 * {@link Events#TEMPERATURE_RECEIVED} when a new value is stored<br>
 * {@link Events#NO_TEMPERATURE_INFO} When more than one hour has passed since the last sample<br>
 * <p>
 * Actions:<br>
 * {@link Actions#QUERY} to ask for a stored value<br>
 * {@link Actions#LAST_VALUES} to ask for the current temperature (last samples)<br>
 * {@link Actions#SAVE} to store a sample<br>
 * 
 * 
 * @author alopez
 *
 */
public class TemperatureVerticle extends AbstractMongoVerticle<Temperature> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureVerticle.class);

	private static final String TEMPERATURES_COLLECTION = "temperatures";
	
	private static final long MIN_TIME_INTERVAL = 45000L; // 45 seg
	private static final int MAX_TIME_INTERVAL = 60 * 60 * 1000; // 1 hour

	public TemperatureVerticle() {
		super(TEMPERATURES_COLLECTION, Constant.TEMPERATURE_VERTICLE);
		
		supportedActions(Actions.values());
	}
	
	public enum Actions implements Action {
		SAVE(Temperature.class),
		QUERY(Query.class),
		LAST_VALUES(null);
		


		private Class<?> mBeanClass;
		
		private Actions (Class<?> beanClass) {
			this.mBeanClass = beanClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return this.mBeanClass;
		}
	}

	private Map<String,Long> mTimers = new HashMap<>();
	
	protected BiConsumer<Temperature, String> onSaved() {
		return (temperature, id) -> {
			String room = temperature.getRoom();
			Long timerId = this.mTimers.get(room);
			if (timerId != null)
				this.vertx.cancelTimer(timerId);
			
			Long newTimerId = this.vertx.setTimer(MAX_TIME_INTERVAL, createTimerForRoom(room));
			this.mTimers.put(room, newTimerId);
			
			this.publishEvent(Events.TEMPERATURE_RECEIVED, temperature);
		};
	}
	
	private Map<String,Long> mLastTimestamp = new HashMap<>();
	
	@Override
	public void start() {
		super.start();
	}
	
	protected boolean saveFilter(Temperature item) {
		final Long lastTimestamp = mLastTimestamp.getOrDefault(item.getRoom(), 0L);

		if (item.getTimestamp() - lastTimestamp < MIN_TIME_INTERVAL) {
			return false;
		} else {
			mLastTimestamp.put(item.getRoom(), item.getTimestamp());
			return true;
		}
	}
	
	private Handler<Long> createTimerForRoom(String room) {
		return (Long event) -> {
			publishEvent(Events.NO_TEMPERATURE_INFO,  new Room(room));
		};
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void last_values(Message<Object> msg) {
		JsonArray array = new JsonArray();
		
		Calendar aWeekAgo = Calendar.getInstance();
		aWeekAgo.add(Calendar.DAY_OF_MONTH, -7);
		
		array.add(new JsonObject("{ \"$match\": { \"timestamp\" : { \"$gt\" : " + aWeekAgo.getTimeInMillis() + "}}}"));
		array.add(new JsonObject("{ \"$group\" : { \"_id\" :\"$room\", \"maxTimestamp\" : { \"$max\" : \"$timestamp\"}}}"));
		
		JsonObject command = new JsonObject()
		  .put("aggregate", TEMPERATURES_COLLECTION)
		  .put("pipeline", array)
		  .put("cursor", new JsonObject());
		
		LOGGER.debug("performing command on mongo [{}]", command);
		mongoClient.runCommand("aggregate", command, res -> {
			if (res.succeeded()) {
				LOGGER.debug(res.result().toString());
				List<JsonObject> items = res.result().getJsonObject("cursor").getJsonArray("firstBatch").getList();
				
				List<Promise> promises = new ArrayList<>();
				List<Future> futures = new ArrayList<>();
				for (JsonObject item : items) {
					String room = item.getString("_id");
					Long maxTimestamp = item.getLong("maxTimestamp");
					
					JsonObject query = new JsonObject().
							put("room", room).
							put("timestamp", maxTimestamp);
					
					//Future<JsonObject> future = Future.future();
					Promise<JsonObject> promise = Promise.promise();
					promises.add(promise);
					futures.add(promise.future());
					mongoClient.findOne(TEMPERATURES_COLLECTION, query, null,  result -> {
						  if (result.succeeded()) {
							  promise.complete(result.result());
						  } else {
							  promise.fail(result.cause());
						  }
					});
					
				}
				
				CompositeFuture.all(futures).onComplete(result -> {
					if (result.failed()) {
						ReplyBuilder builder = MessageBuilder.createReply();
						builder.setError();
						builder.setMessage(result.cause().getLocalizedMessage());
						msg.reply(builder.build());
					} else {
						ReplyBuilder builder = MessageBuilder.createReply();
						LOGGER.debug("replying [{}]", result.result().list());
						
						builder.setResult(result.result().list()); 
						msg.reply(builder.build());
					}
				});
			} else {
				LOGGER.warn("something weird happends when calling database [{}]", res);
				
				ReplyBuilder builder = MessageBuilder.createReply();
				builder.setError();

				msg.reply(builder.build());
			}
		});
	}
} 