package es.xan.servantv3;

import es.xan.servantv3.messages.*;

/**
 * List of events 
 * @author alopez
 *
 */
public enum Events implements Event {
	PARROT_MESSAGE_RECEIVED(ParrotMessageReceived.class),
	PARROT_FILE_RECEIVED(VideoMessage.class),
	PARRONT_AVAILABLE(null),
	
	TEMPERATURE_RECEIVED(Temperature.class),
	NO_TEMPERATURE_INFO(Room.class),
	
	NEW_NETWORK_DEVICES_MESSAGE(Device.class),
	REM_NETWORK_DEVICES_MESSAGE(Device.class),
	
	PERSON_AT_HOME(Person.class),
	PERSON_LEAVE_HOME(Person.class),
	
	BOILER_SWITCHED(NewStatus.class),
	LAMP_SWITCHED(NewStatus.class),
	
	LAUNDRY_OFF(Power.class),

	NEW_FILE_STORED(VideoMessage.class),

	DOOR_STATUS_CHANGED(NewStatus.class),

	_EVENT_(es.xan.servantv3.messages.Event.class),

	
	;
	
	
	private Class<?> mBeanClass;

	private Events(Class<?> beanClass) {
		this.mBeanClass = beanClass;
	}

	@Override
	public Class<?> getPayloadClass() {
		return this.mBeanClass;
	}
	

}
