package es.xan.servantv3;


/**
 * Events are messages published into the event bus with some interesting information recollected by a verticle.
 * 
 * Events are defined into the {@link Events} class.
 * 
 * An Event may include a payload with some additional data.
 * 
 * Any verticle may handle an event in order to perform an internal operation.
 * 
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
