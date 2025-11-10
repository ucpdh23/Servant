package es.xan.servantv3.brain.a2a;

import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import io.a2a.A2A;
import io.a2a.spec.Message;

import java.util.List;
import java.util.Optional;

public class EventConverter {
    public static Optional<Message> convertEventsToA2AMessage(InvocationContext invocationContext) {
        List<Event> events = invocationContext.session().events();

        List<Event> reversed =  events.reversed();

        for (Event event : reversed) {
            return Optional.of(A2A.toUserMessage(event.stringifyContent()));
        }

        return Optional.of(A2A.toUserMessage(""));
    }
}
