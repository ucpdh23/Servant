package es.xan.servantv3.eventsprocessor;

import es.xan.servantv3.AbstractMongoVerticle;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.messages.EventWrapper;
import es.xan.servantv3.messages.Power;
import es.xan.servantv3.messages.Temperature;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.function.BiConsumer;

public class EventsprocessorVerticle extends AbstractMongoVerticle<EventWrapper> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventsprocessorVerticle.class);

    private static final String EVENTS_COLLECTION = "events";


    public EventsprocessorVerticle() {
        super(EVENTS_COLLECTION, Constant.EVENTSPROCESSOR_VERTICLE);

        supportedEvents(Events.TEMPERATURE_RECEIVED, Events.LAUNDRY_OFF);
    }

    public void temperature_received(Temperature temperature) {
        save(new EventWrapper(Events.TEMPERATURE_RECEIVED.name(), temperature), null);
    }

    public void laundry_off(Power power) {
        save(new EventWrapper(Events.LAUNDRY_OFF.name(), power), null);
    }

    @Override
    protected BiConsumer<EventWrapper, String> onSaved() {
        return null;
    }
}
