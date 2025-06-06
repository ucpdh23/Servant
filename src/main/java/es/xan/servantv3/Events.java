package es.xan.servantv3;

import es.xan.servantv3.messages.*;
import es.xan.servantv3.modes.NightModeVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;

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
	
	NETWORK_DEVICES_STATUS_UPDATED(Device.class),

	PERSON_AT_HOME(Person.class),
	PERSON_LEAVE_HOME(Person.class),
	
	BOILER_SWITCHED(NewStatus.class),
	LAMP_SWITCHED(NewStatus.class),
	
	LAUNDRY_OFF(Power.class),

	NEW_FILE_STORED(VideoMessage.class),

	DOOR_STATUS_CHANGED(NewStatus.class),
	OCCUPANCY_CHANGED(NewStatus.class),
	WATER_LEAK_STATUS_CHANGED(NewStatus.class),
	VIDEO_RECORDED(null),

	NEW_VERSION_AVAILABLE(VersionInfo.class),

	REMOTE_CONTROL(NewStatus.class),

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
