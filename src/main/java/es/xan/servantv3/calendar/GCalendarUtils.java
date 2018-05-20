package es.xan.servantv3.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.common.collect.Lists;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GCalendarUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(GCalendarUtils.class);
	
	private static final String APPLICATION_NAME = "My Project";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    
    /**
     * Notifications en the next 5 hours
     * @param secret
     * @param calendar
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public static List<Notification> nextNotifications(File secret, String calendar) throws IOException, GeneralSecurityException {
    	return getNotificationsInWindow(secret, calendar, 0, TimeUnit.HOURS.toMillis(5));
    }
    	
    
    private static List<Notification> getNotificationsInWindow(File secret, String calendar, long startOffset, long endOffset) throws IOException, GeneralSecurityException {
    	
    	
    	Calendar service = null;
    	try (FileInputStream input = new FileInputStream(secret)) {
    		GoogleCredential credential = GoogleCredential.fromStream(input).createScoped(SCOPES);
    		
    		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    		service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
    				.setApplicationName(APPLICATION_NAME)
    				.build();
    	}
    	
        // Build a new authorized API client service.

        // List the next 10 events from the primary calendar.
        DateTime minTime = new DateTime(System.currentTimeMillis() + startOffset);
        DateTime maxTime = new DateTime(System.currentTimeMillis() + endOffset); //TimeUnit.HOURS.toMillis(5));
        
        CalendarList list = service.calendarList().list().execute();
        LOGGER.debug(list.toPrettyString());
        
        Events events = service.events().list(calendar)
                .setMaxResults(10)
                .setTimeMin(minTime)
                .setTimeMax(maxTime)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        
        List<Event> items = events.getItems();
        if (items.isEmpty()) {
            LOGGER.debug("No upcoming events found.");
            
            return Collections.emptyList();
        } else {
            LOGGER.debug("Upcoming events");
            
            List<Notification> notifications = Lists.newArrayList();
            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                if (start == null) {
                    start = event.getStart().getDate();
                }
                LOGGER.debug("{} - {} ({})", event.getId(), event.getSummary(), start);
                
                Notification notification = new Notification();
                notification.id = event.getId();
                notification.text = event.getSummary();
                notification.date = Instant.ofEpochMilli(start.getValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
                
                if (startOffset == 0 && notification.date.isBefore(LocalDateTime.now())) {
                	LOGGER.debug("rejected notification [{}], [{}] is before than now", notification.text, notification.date);
                } else {
                	notifications.add(notification);
                }
            }
            
            return notifications;
        }
    }
    
    public static Notification getNotification(String id, File secret, String calendar) {
		try {
			List<Notification> notificationsInWindow = getNotificationsInWindow(secret, calendar, -TimeUnit.MINUTES.toMillis(2), TimeUnit.MINUTES.toMillis(2));
			
			for (Notification notification : notificationsInWindow) {
				if (notification.id.equals(id)) {
					return notification;
				}
			}
		} catch (IOException | GeneralSecurityException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		LOGGER.warn("Not found notification with id [{}]", id);
    	return null;
    }

}
