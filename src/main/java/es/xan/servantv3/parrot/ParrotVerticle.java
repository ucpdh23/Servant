package es.xan.servantv3.parrot;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.Events.ParrotMessageReceived;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.CreateChat;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.ParrotMessage;


public class ParrotVerticle extends AbstractServantVerticle implements CommunicationListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(ParrotVerticle.class);
	
	public ParrotVerticle() {
		super(Constant.PARROT_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private GTalkService channel;
	
	private static final int WAITING_TIME = /*30 **/ 1000;

	/**
	 * Supported actions for this {@link Verticle}
	 */
	public enum Actions implements Action {
		/**
		 * Creates a communication line between this bot and an user
		 */
		CREATE_CHAT(CreateChat.class),
		/**
		 * Sends a message to an user
		 */
		SEND(ParrotMessage.class); 
		
		public static class CreateChat { public String user; }
		public static class ParrotMessage { public String user, message; }

		private Class<?> mMessageClass;
		
		Actions(Class<?> messageClass) {
			this.mMessageClass = messageClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return mMessageClass;
		}
	}

	public void create_chat(CreateChat createChat) {
		try {
			channel.createChat(createChat.user);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void send(ParrotMessage message) {
		channel.send(message.user, message.message);
	}
	
	
	public void start() {
		super.start();
		
		channel = new GTalkService(Vertx.currentContext().config().getJsonObject("ParrotVerticle"));
		channel.setCommunicationListener(this);
				
		vertx.setTimer(WAITING_TIME, t -> {
			channel.start();
			publishEvent(Events.PARRONT_AVAILABLE);
		});
		
		LOG.info("Started ParrotVerticle");
	}
	
	@Override
	public void onMessage(String sender, String message) {
		final ParrotMessageReceived bean = (ParrotMessageReceived) Events.PARROT_MESSAGE_RECEIVED.createBean();
		bean.user = sender;
		bean.message = message;
		
		publishEvent(Events.PARROT_MESSAGE_RECEIVED, bean);
	}

}
