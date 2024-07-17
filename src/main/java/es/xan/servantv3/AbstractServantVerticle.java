package es.xan.servantv3;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.xan.servantv3.MessageBuilder.ActionBuilder;
import es.xan.servantv3.MessageBuilder.EventBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.api.*;
import es.xan.servantv3.security.SecurityVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all the verticles of this application.
 * 
 * In order to register some actions or handle events, call the {@link AbstractServantVerticle#supportedActions(Action...)} and {@link AbstractServantVerticle#supportedEvents(Events...)}
 * in the constructor of the child class.
 * 
 * 
 * 
 * 
 * @author alopez
 *
 */
public class AbstractServantVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServantVerticle.class);
	
	private static final Pattern VERTICLE_NAME_PATTERN = Pattern.compile("\\.([^\\.]*)Verticle\\d?\\.");
	
	private final String mVerticleName;
	private final Map<String, Pair<Action, Method>> mActionMap;
	private final Map<String, Pair<Event, Method>> mEventMap;
	
	private final Map<String, Method> mMethodMap;

	private Agent<AgentInput> agent;

	public void registerStateMachine(AgentState<AgentInput> state) {
		LOGGER.info("registeringStateMachine with initial State [{}]", state);

		AgentContext context = new AgentContext(){};
		this.agent = new Agent<>(state, this, context);
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
		LOGGER.info("Processing verticle [{}]", verticleName);
		this.mVerticleName = verticleName;
		
		this.mMethodMap = createMap(this.getClass().getMethods());
		
		this.mActionMap = new HashMap<>();
		this.mEventMap = new HashMap<>();
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

	/**
	 * {@inheritDoc}
	 *  
	 *  this method registers this verticle into the vertx's event bus in order to consume actions and events.
	 */
	@Override
	public void start() {
		final EventBus eb = vertx.eventBus();
		if (!this.mActionMap.isEmpty())
			eb.consumer(mVerticleName).handler(event -> processAction(event));
		
		if (!this.mEventMap.isEmpty())
			eb.consumer(Constant.EVENT).handler(event -> processEvent(event));
	}
	
	/**
	 * Process the incoming event, determining whether it must be consumed.
	 * The event name is searched into the internal events maps of this class, if found, calls the handler
	 * @param message
	 */
	private void processEvent(Message<Object> message) {
		JsonObject body = (JsonObject) message.body();
		String eventName = body.getString("action");
		LOGGER.debug("Processing event [{}]", eventName);
		
		if (!mEventMap.containsKey(eventName)) {
			LOGGER.debug("mEventMap:" + mEventMap.keySet());
			LOGGER.debug("Not processed event [{}] in verticle [{}]", eventName, this.mVerticleName);
			return;
		}
		
		Event event = mEventMap.get(eventName).left;

		if (this.agent != null) {
			JsonObject entity = body.containsKey("bean")? body.getJsonObject("bean") : null;
			processInStateMachine(event, entity);
		}  else {
			processEventAsMethodExecution(event, mEventMap.get(eventName), body, message);
		}
	}

	private void processInStateMachine(Object operation, @Nullable JsonObject entity) {
		LOGGER.info("processInStateMachine [{}-{}]", operation, entity.toString());
		AgentInput input = new AgentInput(operation, entity);
		this.agent.process(input);
	}

	private void processEventAsMethodExecution(Event event, Pair<Event, Method> info, JsonObject body, Message<Object> message) {
		try {
			int count = info.right.getParameterCount();
			LOGGER.trace("trying to execute method [{}] with [{}] parameters", info.right.getName(), count);

			if (count == 0) {
				info.right.invoke(this);

			} else {
				Class<?> beanClass = event.getPayloadClass();
				JsonObject entity = body.getJsonObject("bean");
				Object newInstance = JsonUtils.toBean(entity.encode(), beanClass);

				if (count == 1) {
					info.right.invoke(this, newInstance);

					//Automatic Reply
					ReplyBuilder builder = MessageBuilder.createReply();
					message.reply(builder.build());
				} else if (count == 2) {
					info.right.invoke(this, newInstance, message);
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

		if (this.agent != null) {
			JsonObject entity = json.containsKey("bean")? json.getJsonObject("bean") : null;
			processInStateMachine(action, entity);
		} else {
			LOGGER.debug("mapped [{}-{}]", action, method.getName());

			// Actions are executed as very hard code.
			vertx.executeBlocking(future -> {
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
								parameter = beanClass.equals(JsonObject.class)?
										entity : JsonUtils.toBean(entity.encode(), beanClass);
							}

							method.invoke(this, parameter);

						} else if (parameterCount == 2) {
							final Class<?> beanClass = action.getPayloadClass();
							final JsonObject entity = json.getJsonObject("bean");
							final Object newInstance = beanClass.equals(JsonObject.class)?
									entity : JsonUtils.toBean(entity.encode(), beanClass);

							method.invoke(this, newInstance, message);
						}
					}
				} catch (Exception e) {
					LOGGER.warn("Action: [{}]", action, e);
				}
			}, res -> {
				LOGGER.trace("[{}]", res);
			});
		}

	}

	protected void publishEvent(Events event) {
		LOGGER.debug("publish event [{}]", event.name());
		EventBuilder eventBuilder = MessageBuilder.createEvent();
		eventBuilder.setAction(event.name());
		vertx.eventBus().publish(Constant.EVENT, eventBuilder.build());
	}
	
	public void publishEvent(Events event, Object item) {
		EventBuilder eventBuilder = MessageBuilder.createEvent();
		eventBuilder.setAction(event.name());
		eventBuilder.setBean(JsonUtils.toJson(item));
		LOGGER.debug("publishing event [{}]", event);

		vertx.eventBus().publish(Constant.EVENT, eventBuilder.build());
	}

	protected void publishRawAction(String raw_action) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(raw_action);

		vertx.eventBus().send(raw_action, builder.build());
	}

	protected void publishRawAction(String raw_action, Object item) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(raw_action);
		builder.setBean(JsonUtils.toJson(item));

		vertx.eventBus().send(raw_action, builder.build());
	}
	
	protected void publishAction(Action send) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.getName());
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), builder.build());
	}
	
	public void publishAction(Action send, Object item) {
		LOGGER.debug("publish action [{}]", send);
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.getName());
		builder.setBean(JsonUtils.toJson(item));
		vertx.eventBus().send(resolveVerticleName(send.getClass().getCanonicalName()), builder.build());
	}
	
	public void publishAction(Action send, Handler<AsyncResult<Message<Object>>> replyHandler) {
		LOGGER.debug("publish action [{}]", send);
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.getName());
		vertx.eventBus().request(resolveVerticleName(send.getClass().getCanonicalName()), builder.build(), replyHandler);
	}
	

	public void publishAction(Action send, Object item, Handler<AsyncResult<Message<Object>>> replyHandler) {
		ActionBuilder builder = MessageBuilder.createAction();
		builder.setAction(send.getName());
		if (item != null)
			builder.setBean(JsonUtils.toJson(item));
		
		JsonObject object = builder.build();
		
		vertx.eventBus().request(resolveVerticleName(send.getClass().getCanonicalName()), object, replyHandler);
	}

	private static String resolveVerticleName(String canonicalName) {
		final Matcher matcher = VERTICLE_NAME_PATTERN.matcher(canonicalName);
		
		if (matcher.find()) {
			final String verticleName = matcher.group(1).toLowerCase() + ".verticle";
			LOGGER.debug("verticleName [{}]", verticleName);
			
			return verticleName;
		}
		
		throw new RuntimeException("Not found pattern of 'Verticle' into canonicalName: " + canonicalName);
	}

	protected void supportedEvents(Events...events) {
		for (Events item : events) {
			LOGGER.debug("adding event [{}->{}]", item.name(), this.mMethodMap.get(item.name().toLowerCase()));
			mEventMap.put(item.name(), new Pair<Event, Method>((Event) item, this.mMethodMap.get(item.name().toLowerCase())));
		}
	}
	
	protected <T extends Enum<T> & Action> void supportedActions(Class<T> actions) {
		supportedActions(actions.getEnumConstants());
	}
	
	protected void supportedActions(Action...actions) {
		if (actions.length > 0) {
			final String actionsVerticleName = resolveVerticleName(actions[0].getClass().getCanonicalName());
			
			if (actionsVerticleName.equals(mVerticleName)) {
				LOGGER.info("adding support for actions of verticleName [" + actionsVerticleName + "]", actionsVerticleName);
			} else {
				LOGGER.error("actionsVerticleName [{}] is not equals to verticle provided name [{}]", actionsVerticleName, mVerticleName);
				throw new RuntimeException("verticleName " + mVerticleName + " is not valid");
			}
		} else {
			LOGGER.warn("No actions to support");
		}
		
		for (Action item : actions) {
			mActionMap.put(item.getName(), new Pair<Action, Method>((Action) item, this.mMethodMap.get(item.getName().toLowerCase())));
		}

		LOGGER.info("Done");
	}
}
