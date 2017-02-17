package es.xan.servantv3.brain;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.Events.ParrotMessageReceived;
import es.xan.servantv3.brain.nlp.Translation;
import es.xan.servantv3.brain.nlp.TranslationFacade;
import es.xan.servantv3.parrot.ParrotVerticle.Actions;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.CreateChat;
import es.xan.servantv3.parrot.ParrotVerticle.Actions.ParrotMessage;

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
	
	private static final Logger LOG = LoggerFactory.getLogger(STSVerticle.class);
	
	private JsonArray mMasters;

	public STSVerticle() {
		super(Constant.BRAIN_VERTICLE);
		
		supportedEvents(
			Events.PARRONT_AVAILABLE,
			Events.PARROT_MESSAGE_RECEIVED
		);
	}
	
	public void start() {
		super.start();
		
		this.mMasters = Vertx.currentContext().config().getJsonArray("masters");
		LOG.info("Started brainVerticle");
	}
	
	/**
	 * Activates the chat channels with the stablished users.
	 */
	public void parront_available() {
		for (Object item : mMasters.getList()) {
			JsonObject emailInfo = (JsonObject) item;

			LOG.debug("creating chat for user :" + emailInfo.getString("email"));
			publishAction(Actions.CREATE_CHAT, new CreateChat() {{
				this.user = emailInfo.getString("email");
			}});
		}
	}
	
	/**
	 * Process a incoming message performing whatever action recovered from the nlp system. 
	 * @param parrotMessage
	 */
	public void parrot_message_received(ParrotMessageReceived parrotMessage) {
		LOG.info("Received:" + parrotMessage.message + " from user:" + parrotMessage.user);
		final Translation translation = TranslationFacade.translate(parrotMessage.message);
		
		if (translation.action != null) {
			publishAction(translation.action, translation.message, response -> {
				publishAction(Actions.SEND, new ParrotMessage() {{
					this.message = translation.response.apply(response.result()).msg;
					this.user = parrotMessage.user;
				}});
			});
		}
	}
}
