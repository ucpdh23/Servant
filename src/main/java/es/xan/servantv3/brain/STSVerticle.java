package es.xan.servantv3.brain;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.ParrotMessageReceived;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import io.vertx.core.eventbus.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Superior temporal sulcus.
 * 
 * This verticle handles the events from the conversational channel system and transform then into actions to be performed into the vertx event bus.
 * 
 * Further information, please see the javadoc of the nlp package.
 * 
 *  A new PERFORM Action has been included to run operations   
 * 
 * @author alopez
 * see https://en.wikipedia.org/wiki/Superior_temporal_sulcus
 */
public class STSVerticle extends AbstractServantVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(STSVerticle.class);

	private Cache<String, UserContext> mContext = CacheBuilder.newBuilder()
			.maximumSize(10000)
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();

	private Scheduler mScheduler;
	
	public enum Actions implements Action {
		HELP(null),
		PERFORM(TextMessageToTheBoss.class)
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
	
	public void perform(TextMessageToTheBoss parrotMessage) {
		LOGGER.debug("perform [{}]", parrotMessage);
		try {
			UserContext bossContext = this.mContext.get("boss", () -> new UserContext("boss"));
			final Translation translation = TranslationFacade.translate(parrotMessage.getMessage(), bossContext);

			if (translation.action != null) {
				if (translation.delayInfo == 0) {
					publishAction(translation.action, translation.message, response -> {
						LOGGER.debug("processing boss response from [{}-{}]", translation.action, translation.message);
						publishAction(HomeVerticle.Actions.NOTIFY_ALL_BOSS, new TextMessageToTheBoss(translation.response.apply(response.result()).msg));
					});
				} else {
					this.mScheduler.scheduleTask(Scheduler.in((int) translation.delayInfo, ChronoUnit.SECONDS), it -> {
						publishAction(translation.action, translation.message, response -> {
							publishAction(HomeVerticle.Actions.NOTIFY_ALL_BOSS, new TextMessageToTheBoss(translation.response.apply(response.result()).msg));
						});

						return false;
					});
				}
			}
		} catch ( ExecutionException e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Process a incoming message performing whatever action recovered from the nlp system. 
	 * @param parrotMessage
	 */
	public void parrot_message_received(final ParrotMessageReceived parrotMessage) {
		LOGGER.debug("Received [{}]", parrotMessage);

		try {
			UserContext userContext = this.mContext.get(parrotMessage.getUser(), () -> new UserContext(parrotMessage.getUser()));
			final Translation translation = TranslationFacade.translate(parrotMessage.getMessage(), userContext);

			if (translation.action != null) {
				if (translation.delayInfo == 0) {
					publishAction(translation.action, translation.message, response -> {
						LOGGER.debug("processing boss response from [{}-{}]", translation.action, translation.message);
						LOGGER.debug(" response: [{}]", response);
						LOGGER.debug(" response.result: [{}]", response.result());
						LOGGER.debug(" response.succeded: [{}]", response.succeeded());
						LOGGER.debug(" response.failed: [{}]", response.failed());

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
		} catch (ExecutionException e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}

}
