package es.xan.servantv3.parrot;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.messages.*;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;


public class ParrotVerticle extends AbstractServantVerticle implements CommunicationListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ParrotVerticle.class);
	
	public ParrotVerticle() {
		super(Constant.PARROT_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private TelegramService channel;
	
	private static final int WAITING_TIME = 1000; // 1 seg

	/**
	 * Supported actions for this {@link Verticle}
	 */
	public enum Actions implements Action {
		/**
		 * Creates a communication line between this bot and an user
		 */
		CREATE_CHAT(OpenChat.class),
		/**
		 * Sends a message to an user
		 */
		SEND(TextMessage.class),
		SEND_VIDEO(VideoMessage.class),
		;
		
		private Class<?> mMessageClass;
		
		Actions(Class<?> messageClass) {
			this.mMessageClass = messageClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return mMessageClass;
		}
	}

	public void create_chat(OpenChat createChat) {
		try {
//			channel.createChat(createChat.getUser());
			channel.send(createChat.getUser(), "Greetings! Sir");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void send(TextMessage message) {
		channel.send(message.getUser(), message.getMessage());
	}

	public void send_video(VideoMessage message) {
		channel.sendVideo(message.getUser(), message.getMessage(), new File(message.getFilepath()));
	}
	
	
	public void start() {
		super.start();
		
		channel = TelegramService.build(Vertx.currentContext().config().getJsonObject("ParrotVerticle"));
		channel.setCommunicationListener(this);
		
		vertx.setTimer(WAITING_TIME, t -> {
			publishEvent(Events.PARRONT_AVAILABLE);
		});
		
		
		LOGGER.info("Started ParrotVerticle");
	}
	
	@Override
	public void onMessage(String sender, String message) {
		publishEvent(Events.PARROT_MESSAGE_RECEIVED, new ParrotMessageReceived(sender, message));
	}

	@Override
	public void onFile(String sender, String content) {
		String[] items = content.split("#");
		String caption = items[1];
		String filepath = items[2];
		publishEvent(Events.PARROT_FILE_RECEIVED, new VideoMessage(sender, caption, filepath));
	}

}
