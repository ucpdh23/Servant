package es.xan.servantv3;

import es.xan.servantv3.homeautomation.HomeVerticle.Person;
import es.xan.servantv3.network.RouterPageManager.Device;
import es.xan.servantv3.temperature.TemperatureVerticle.Actions.Temperature;

/**
 * List of events 
 * @author alopez
 *
 */
public enum Events implements Event {
	PARROT_MESSAGE_RECEIVED(ParrotMessageReceived.class),
	PARRONT_AVAILABLE(null),
	
	TEMPERATURE_RECEIVED(Temperature.class),
	NO_TEMPERATURE_INFO(Room.class),
	
	NEW_NETWORK_DEVICES_MESSAGE(Device.class),
	REM_NETWORK_DEVICES_MESSAGE(Device.class),
	
	PERSON_AT_HOME(Person.class),
	PERSON_LEAVE_HOME(Person.class),
	
	BOILER_SWITCHED(NewStatus.class)
	;
	
	public static class NewStatus { public String status; }
	public static class ParrotMessageReceived { public String user, message; }
	public static class Room { public String room; }
	
	private Class<?> mBeanClass;

	private Events(Class<?> beanClass) {
		this.mBeanClass = beanClass;
	}

	@Override
	public Class<?> getPayloadClass() {
		return this.mBeanClass;
	}
	
	public Object createBean() {
		if (this.mBeanClass != null) {
			try {
				return this.mBeanClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			return null;
		}
	}
}
