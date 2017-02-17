package es.xan.servantv3;


/**
 * An action represents any operation requested to a verticle.
 * Any Verticle may contain an enum with all the supported actions.
 * 
 * If you want to perform an action just send a message into the eventbus by calling the method sendAction passing the value of the enum which represents the action
 * and a payload with additional information if needed.
 * 
 * The action may return a reply with some data about the action.
 * 
 * @author alopez
 *
 */
public interface Action {
	/**
	 * The name is used to identify the name of the method in charge of resolve the response 
	 * @return
	 */
	String name();
	
	/**
	 * The class of the payload.
	 * @return
	 */
	Class<?> getPayloadClass();
}
