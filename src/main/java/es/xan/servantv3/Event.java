package es.xan.servantv3;

/**
 * Events are messages published into the event bus with some interesting information recollected by a verticle.
 * <p>
 * All events of this application are defined into the {@link Events} class.
 * <p>
 * An Event may include a payload with some additional data.
 * <p>
 * <h3>Handling events</h3>
 * Verticles may handle events in order to perform an internal operation.
 *  * In order to handle events:<br>
 * <b>1.-</b> Include the following line into the Verticle's constructor: <code>supportedEvents(Events...);</code><br>
 * &nbsp;This call must include the list of events that this Verticle wants to receive<br>
 * <b>2.-</b> Implement a method with the same name of the event with the following signature<br>
 * <code>public void event_name_in_lowercase(Payload)<code><br>
 * <p>
 * Notes:<br>
 * <ul>
 * <li>An event is handled by all the verticles registed to  it</li>
 * </ul>
 * <p>
 * <h3>Event Publication</h3>
 * In order to publish an event from a Verticle just include the following line: <code>publishEvent(eventName, payload);</code><br>
 * &nbsp;eventName is the item of the {@link Events} enum<br>
 * &nbsp;payload is the payload related to this event<br>
 * <p>
 * @author alopez
 *
 */
public interface Event {
	/**
	 * Class of the payload
	 * @return
	 */
	Class<?> getPayloadClass();
}
