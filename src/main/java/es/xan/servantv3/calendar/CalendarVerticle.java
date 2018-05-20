package es.xan.servantv3.calendar;

import static es.xan.servantv3.Scheduler.at;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Scheduler;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CalendarVerticle extends AbstractServantVerticle {
	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarVerticle.class);
	
	private String calendar;
	private File secretFile;
	
	private Map<String, UUID> watcher = new HashMap<>();

	private Scheduler mScheduler;
	
	public CalendarVerticle() {
		super(Constant.CALENDAR_VERTICLE);
		
		supportedActions(Actions.values());
	}
	
	public void start() {
		super.start();
		
		JsonObject config = vertx.getOrCreateContext().config().getJsonObject("CalendarVerticle");
		calendar = config.getString("calendar");
		secretFile = new File(config.getString("secret"));

		vertx.setPeriodic(TimeUnit.MINUTES.toMillis(15), id -> {publishAction(Actions.CHECK_CALENDAR);});
		
		this.mScheduler = new Scheduler(getVertx());

	}
	
	public enum Actions implements Action {
		CHECK_CALENDAR(null)
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
	
	public void check_calendar(Message<Object> message) {
		try {
			List<Notification> nextNotifications = GCalendarUtils.nextNotifications(secretFile, calendar);
			
			nextNotifications.forEach(notif -> {
				if (!watcher.containsKey(notif.id)) {
					UUID scheduleTask = mScheduler.scheduleTask(
							at(notif.date),
							(UUID id) -> {
								Notification notification = GCalendarUtils.getNotification(notif.id, secretFile, calendar);
								
								if (notification != null) {
									publishAction(HomeVerticle.Actions.NOTIFY_BOSS, new TextMessageToTheBoss(notification.text));
								}
								
								watcher.remove(notif.id);
								return false; });
					
					watcher.put(notif.id, scheduleTask);
				}
			});
			
		} catch (IOException | GeneralSecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	

}
