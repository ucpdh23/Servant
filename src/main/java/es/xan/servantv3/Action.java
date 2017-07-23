package es.xan.servantv3;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
