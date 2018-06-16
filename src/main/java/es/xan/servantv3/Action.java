package es.xan.servantv3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import es.xan.servantv3.parrot.ParrotVerticle;
import io.vertx.core.eventbus.Message;


/**
 * An action represents any operation requested to a verticle.
 * Any Verticle may contain an enum with all the supported actions.
 * <p>
 * If you want to perform an action just send a message into the eventbus by calling the method sendAction passing the value of the enum which represents the action
 * and a payload with additional information if needed.
 * <p>
 * The action may return a reply with some data about the action.
 * <p>
 * <h3>Conventions</h3>
 * In order to declare actions in a verticle...<br>
 * <b>1.-</b> Include a nested enumeration inside the Verticle. This enum must be called <i>Actions</i> and must implements the {@link Action} interface.<br>
 * <b>2.-</b> In order to register the actions, include the following line into the Verticle constructor: <code>supportedActions(Actions.values());</code><br>
 * <b>2.-</b> Actions must be declared in uppercase characters.<br>
 * <b>3.-</b> If an action required arguments, the element in this enumerator may present one parameter: the class of the payload.<br>
 * <b>4.-</b> Each action must be implemented inside a public method of the verticle.<br>
 * <b>5.-</b> This method must follow the following signature:<br>
 * &nbsp<code>public void action_name_in_lowercase(Message?, Payload?)</code><br>
 * &nbsp&nbsp&nbspMessage: The vertx message. This argument is not mandatory.<br>
 * &nbsp&nbsp&nbspPayload: Argument to the action. This argument is only required if a payload class is declared into the Actions enum item.<br>
 * <b>6.-</b> In order to return information, the action must use the {@link Message#reply(Object)} method.<br>
 * <p>
 * <h3>Calling an Action</h3>
 * In order to call an action from a Verticle just call the method {@link AbstractServantVerticle#publishAction(Action, Object, io.vertx.core.Handler)}.<br>
 * Arguments:<br>
 * &nbsp&nbsp&nbspAction: The enum item of the determined Verticle. For instance {@link ParrotVerticle}.Actions.SEND<br>
 * &nbsp&nbsp&nbspObject: Payload. Instance of the payload class declared in the enum.<br> 
 * &nbsp&nbsp&nbspHandler: Callback of the action.<br>
 * This method is overloaded in order to support actions without payload.<br>
 * 
 * <h3>Notes</h3>
 * <ul>
 * <li>Actions are executed into the main thread, makes sense to modify this behaviour and run then into a separate thread. 
 * </ul>
 * 
 * 
 * @author alopez
 *
 */
public interface Action {
	/**
	 * The name is used to identify the name of the method in charge of resolve the response 
	 * @return
	 */
	default String getName() {
		if (this.getClass().isEnum()) {
			return ((Enum<?>) this).name();
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * The class of the payload.
	 * @return
	 */
	default Class<?> getPayloadClass() {
		if (this.getClass().isEnum()) {
			try {
				Method declaredMethod = this.getClass().getDeclaredMethod("getClazz");
					return (Class<?>) declaredMethod.invoke(this);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new UnsupportedOperationException(e);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new UnsupportedOperationException(e);
			}
		} else {
			throw new UnsupportedOperationException();
		}
		
	}
	
}
