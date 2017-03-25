package es.xan.servantv3;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import es.xan.servantv3.MessageBuilder.ActionBuilder;
import es.xan.servantv3.MessageBuilder.EventBuilder;

public class AbstractServantVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServantVerticle.class);
	
	private final String mVerticleName;
	private final Map<String, Pair<Action, Method>> mActionMap;
	private final Map<String, Pair<Event, Method>> mEventMap;
	
	private final Map<String, Method> mMethodMap;
	
	static {
		Json.mapper.setVisibility(Json.mapper.getSerializationConfig()
				.getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE)
				);
	}
	
	private static class Pair<A,B> {
		A left;
		B right;
		
		public Pair(A left, B right) {
			this.left = left;
			this.right = right;
		}
	}
	
	protected AbstractServantVerticle(String verticleName) {
		this(verticleName, null, null);
	}

	
	protected AbstractServantVerticle(String verticleName, Class<? extends Enum<?>> actions) {
		this(verticleName, actions, null);
	}

	protected AbstractServantVerticle(String verticleName, Class<? extends Enum<?>> actions, Class<? extends Enum<?>> events) {
		LOGGER.info("Processing verticle [{}]", verticleName);
		this.mVerticleName = verticleName;
		
		this.mMethodMap = createMap(this.getClass().getMethods());
		
		this.mActionMap = new HashMap<>();
		if (actions != null) {
			for (Enum<?> item : actions.getEnumConstants()) {
				mActionMap.put(item.name(), new Pair<Action, Method>((Action) item, this.mMethodMap.get(item.name().toLowerCase())));
			}
		}
		
		this.mEventMap = new HashMap<>();
		if (events != null) {
			for (Enum<?> item : events.getEnumConstants()) {
				mEventMap.put(item.name(), new Pair<Event, Method>((Event) item, this.mMethodMap.get(item.name().toLowerCase())));
			}
		}
		
	}

	private Map<String, Method> createMap(Method[] declaredMethods) {
		final Map<String, Method> map = new HashMap<>();
		
		for (final Method method : declaredMethods) {
			final String currentMethodClass = method.getDeclaringClass().getCanonicalName();
			if (currentMethodClass.equals(Object.class.getCanonicalName())) continue;
			if (currentMethodClass.equals(AbstractVerticle.class.getCanonicalName())) continue;
			if (currentMethodClass.equals(AbstractServantVerticle.class.getCanonicalName())) continue;
			
			LOGGER.debug("method from: [{}]", method.getDeclaringClass().getCanonicalName());
			LOGGER.debug("Mapping method [{}]", method.getName());
			map.put(method.getName(), method);
		}
		
		return map;
	}

	public void start() {
		final EventBus eb = vertx.eventBus();
		if (!this.mActionMap.isEmpty())
			eb.consumer(mVerticleName).handler(event -> processAction(event));
		
		if (!this.mEventMap.isEmpty())
			eb.consumer(Constant.EVENT).handler(event -> processEvent(event));
	}
	
	private void processEvent(Message<Object> message) {
		JsonObject body = (JsonObject) message.body();
		String action = body.getString("action");
		LOGGER.debug("Processing action [{}]", action);
		
		if (!mEventMap.containsKey(action)) {
			LOGGER.debug("Not processed event [{}] in verticle [{}]", action, this.mVerticleName);
			return;
		}
		
		Event event = mEventMap.get(action).left;
		
		try {
			int count = mEventMap.get(action).right.getParameterCount();
			LOGGER.trace("trying to execute method [{}] with [{}] parameters", mEventMap.get(action).right.getName(), count);
			
			if (count == 0) {
				mEventMap.get(action).right.invoke(this);
				
			} else {
				Class<?> beanClass = event.getPayloadClass();
				JsonObject entity = body.getJsonObject("bean");
				Object newInstance = Json.decodeValue(entity.encode(), beanClass);
				
				if (count == 1) {
					mEventMap.get(action).right.invoke(this, newInstance);
					
				} else if (count == 2) {
					mEventMap.get(action).right.invoke(this, newInstance, message);
				}
			}
		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}
	
	private void processAction(Message<Object> message) {
		final JsonObject json = (JsonObject) message.body();
		final String actionName = json.getString("action");
		
		LOGGER.debug("Action [{}]", actionName);
		
		Pair<Action, Method> actionPair = mActionMap.get(actionName);
		if (actionPair == null) {
			LOGGER.warn("Cannot perform action [{}] in verticle [{}]", actionName, mVerticleName);
			return;
		}
		
		final Action action = actionPair.left;
		final Method method = actionPair.right;
		
		LOGGER.debug("mapped [{}-{}]", action, method.getName());
		
		try {
			final int parameterCount = method.getParameterCount();
			if (parameterCount == 0) {
				method.invoke(this);
				
			} else {
				if (parameterCount == 1) {
					final Class<?> beanClass = action.getPayloadClass();

					Object parameter = message;
					if (beanClass != null) {
						final JsonObject entity = json.getJsonObject("bean");
						parameter = Json.decodeValue(entity.encode(), beanClass);
					}
					
					method.invoke(this, parameter);
				
				} else if (parameterCount == 2) {
					final Class<?> beanClass = action.getPayloadClass();
					final JsonObject entity = json.getJsonObject("bean");
					final Object newInstance = Json.decodeValue(entity.encode(), beanClass);

					method.invoke(this, newInstance, message);
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Action: [{}]", action, e);
		}
		
	}

	protected void publishEvent(Events event) {
		EventBuilder eventBuilder = MessageBuilder.createEvent();
		eventBuilder.setAction(event.name());
		vertx.eventBus().publish(Constant.EVENT, eventBuilder.build());
	}
	
	protected void publishEvent(Events event, Object item) {
		EventBuilder eventBuilder = MessageBuilder.createEvent();
		eventBuilder.setAction(event.name());
		eventBuilder.setBean(new JsonObject(Json.encode(item)));
		vertx.eventBus().publish(Constant.EVENT, eventBuilder.build());
	}
	
	protected void publishAction(Action send) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.name());
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), builder.build());
	}
	
	protected void publishAction(Action send, Object item) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.name());
		builder.setBean(new JsonObject(Json.encode(item)));
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), builder.build());
	}
	
	protected void publishAction(Action send, Handler<AsyncResult<Message<Object>>> replyHandler) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.name());
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), builder.build(), replyHandler);
	}
	

	protected void publishAction(Action send, Object item, Handler<AsyncResult<Message<Object>>> replyHandler) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.name());
		if (item != null)
			builder.setBean(new JsonObject(Json.encode(item)));
		
		JsonObject object = builder.build();
		
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), object, replyHandler);
	}

	private static String resolveVerticleName(String canonicalName) {
		Pattern pattern = Pattern.compile("\\.([^\\.]*)Verticle\\.");
		Matcher matcher = pattern.matcher(canonicalName);
		
		if (matcher.find()) {
			return matcher.group(1).toLowerCase()+".verticle";
		}
		
		throw new RuntimeException("Not found pattern of 'Verticle' into canonicalName:" + canonicalName);
	}

	protected void supportedEvents(Events...events) {
		for (Events item : events)
			mEventMap.put(item.name(), new Pair<Event, Method>((Event) item, this.mMethodMap.get(item.name().toLowerCase())));
	}
	
	protected void supportedActions(Action...actions) {
		for (Action item : actions) {
			mActionMap.put(item.name(), new Pair<Action, Method>((Action) item, this.mMethodMap.get(item.name().toLowerCase())));
		}
	}
}
