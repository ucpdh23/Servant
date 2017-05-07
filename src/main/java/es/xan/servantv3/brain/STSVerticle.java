package es.xan.servantv3.brain;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.Events.ParrotMessageReceived;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.brain.nlp.Rules;
import es.xan.servantv3.brain.nlp.Translation;
import es.xan.servantv3.brain.nlp.TranslationFacade;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.CreateChat;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.ParrotMessage;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Superior temporal sulcus.
 * 
 * This verticle handles the events from the conversational channel system and transform then into actions to be performed into the vertx event bus.
 * 
 * Further information, please see the javadoc of the nlp package. 
 * 
 * @author alopez
 * @see https://en.wikipedia.org/wiki/Superior_temporal_sulcus
 */
public class STSVerticle extends AbstractServantVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(STSVerticle.class);
	
	private JsonArray mMasters;
	
	public enum Actions implements Action {
		HELP(null),
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
	
	public STSVerticle() {
		super(Constant.STS_VERTICLE);
		
		supportedActions(Actions.values());
		
		supportedEvents(
			Events.PARRONT_AVAILABLE,
			Events.PARROT_MESSAGE_RECEIVED
		);
	}
	
	public void start() {
		super.start();
		
		this.mMasters = Vertx.currentContext().config().getJsonArray("masters");
		LOGGER.info("Started brainVerticle");
	}
	
	/**
	 * Activates the chat channels with the stablished users.
	 */
	public void parront_available() {
		for (Object item : mMasters.getList()) {
			JsonObject emailInfo = (JsonObject) item;

			LOGGER.debug("creating chat for user [{}]", emailInfo.getString("email"));
			publishAction(es.xan.servantv3.parrot.ParrotVerticle.Actions.CREATE_CHAT, new CreateChat() {{
				this.user = emailInfo.getString("email");
			}});
		}
	}
	
	public void help(final Message<Object> msg) {
		ReplyBuilder builder = MessageBuilder.createReply();
		
		StringBuilder response = new StringBuilder();
		for (Rules rule : Rules.values()) {
			response.append("command:").append(rule.name().toLowerCase()).append("\n");
			response.append("           ").append(rule.getHelpMessage()).append("\n\n");
		}
		
		builder.setOk();
		builder.setMessage(response.toString());
		
		msg.reply(builder.build());
	}
	
	/**
	 * Process a incoming message performing whatever action recovered from the nlp system. 
	 * @param parrotMessage
	 */
	public void parrot_message_received(ParrotMessageReceived parrotMessage) {
		LOGGER.debug("Received [{}] from user [{}]", parrotMessage.message, parrotMessage.user);
		
		final Translation translation = TranslationFacade.translate(parrotMessage.message);
		
		if (translation.action != null) {
			publishAction(translation.action, translation.message, response -> {
				publishAction(es.xan.servantv3.parrot.ParrotVerticle.Actions.SEND, new ParrotMessage() {{
					this.message = translation.response.apply(response.result()).msg;
					this.user = parrotMessage.user;
				}});
			});
		}
	}
}
