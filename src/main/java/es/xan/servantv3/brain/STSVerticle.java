package es.xan.servantv3.brain;

import java.time.temporal.ChronoUnit;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.Scheduler;
import es.xan.servantv3.brain.nlp.Rules;
import es.xan.servantv3.brain.nlp.Translation;
import es.xan.servantv3.brain.nlp.TranslationFacade;
import es.xan.servantv3.messages.ParrotMessageReceived;
import es.xan.servantv3.messages.TextMessage;
import io.vertx.core.eventbus.Message;
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
	
	
	private Scheduler mScheduler;
	
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
			Events.PARROT_MESSAGE_RECEIVED
		);
	}
	
	public void start() {
		super.start();
		
		this.mScheduler = new Scheduler(getVertx());
		LOGGER.info("Started brainVerticle");
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
		LOGGER.debug("Received [{}]", parrotMessage);
		
		final Translation translation = TranslationFacade.translate(parrotMessage.getMessage());
		
		if (translation.action != null) {
			if (translation.delayInfo == 0) {
				publishAction(translation.action, translation.message, response -> {
					publishAction(es.xan.servantv3.parrot.ParrotVerticle.Actions.SEND, new TextMessage(parrotMessage.getUser(), translation.response.apply(response.result()).msg));
				});
			} else {
				this.mScheduler.scheduleTask(Scheduler.in((int) translation.delayInfo, ChronoUnit.SECONDS), it -> {
					publishAction(translation.action, translation.message, response -> {
						publishAction(es.xan.servantv3.parrot.ParrotVerticle.Actions.SEND, new TextMessage(parrotMessage.getUser(), translation.response.apply(response.result()).msg));
					});
					
					return false;
				});
			}
		}
	}
}
