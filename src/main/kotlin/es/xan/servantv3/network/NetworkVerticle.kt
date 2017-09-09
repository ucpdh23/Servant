package es.xan.servantv3.network

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Constant
import es.xan.servantv3.Action
import es.xan.servantv3.messages.UpdateState
import es.xan.servantv3.messages.Configure
import io.vertx.core.eventbus.Message
import es.xan.servantv3.SSHUtils
import es.xan.servantv3.MessageBuilder
import io.vertx.core.logging.LoggerFactory
import es.xan.servantv3.messages.Device
import es.xan.servantv3.messages.DeviceStatus
import java.util.Date
import java.util.concurrent.TimeUnit
import es.xan.servantv3.Events
import es.xan.servantv3.MessageBuilder.ReplyBuilder
import es.xan.servantv3.JsonUtils


/**
 * Home network manager. Store information about the devices connected to the network,
 * creating events when devices come in or out.
 * According to the changes in the devices' status, this Verticle publishes two events: Events.REM_NETWORK_DEVICES_MESSAGE and Events.REW_NETWORK_DEVICES_MESSAGE.
 * Devices status info must be provided by external sensors through the REST API defined into the controller DevicesController.   
 */
class NetworkVerticle : AbstractServantVerticle(Constant.NETWORK_VERTICLE) {
	
	companion object {
        val LOG = LoggerFactory.getLogger(NetworkVerticle::class.java.name)
		val TTL = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES); 
    }
	
	init {
		supportedActions(Actions::class.java)
	}
	
	override fun start() {
		super.start();
		
		vertx.setPeriodic(300000, { id -> 
			publishAction(Actions.CHECK_STATUS);
		});
	}
	
	// *********** Verticle state info
	
	var storedStatus: MutableMap<String, Device> = HashMap();
	
	
	// *********** Supported actions
	
	enum class Actions(val clazz : Class<*>? ) : Action {
		/**
		 * Updates the status of the passing device
		 */
		UPDATE_DEVICE_STATUS(Device::class.java),
		
		/**
		 * Checks the current status the all the available devices
		 */
		CHECK_STATUS(null),
		
		/**
		 * Returns a list with the current devices status
		 */
		LIST(null);
	}
	
	
	fun update_device_status(input: Device) {
		if (input.status == DeviceStatus.DOWN) input.status = DeviceStatus.QUARANTINE;
		
		val stored = storedStatus.getOrPut(input.mac)
				{Device(input.user, input.mac, DeviceStatus.NEW, 0)}
		
		updateStatus(stored, input.timestamp, input.status)
	}
		
	fun check_status() {
		LOG.debug("check network status");
		
		val currTimeStamp = Date().getTime()
		
		storedStatus
				.filterValues { dev -> dev.status == DeviceStatus.QUARANTINE && dev.timestamp < currTimeStamp - TTL}
				.forEach { _, dev -> updateStatus(dev, currTimeStamp, DeviceStatus.DOWN)};
	}
	
	fun list(message: Message<Any>) {
		val devices = storedStatus.values.map{device -> JsonUtils.toJson(device)};
		
		val builder = MessageBuilder.createReply().apply {
			setResult(devices);
			setOk()
		}
		
		message.reply(builder.build());
	}
	
	
	
	// ********* Private methos
	
	private fun updateStatus(dev : Device, timestamp : Long, status : DeviceStatus) {
		if (dev.status == status) return;
		
		val step = DeviceWorkflow.resolve(dev.status, status);
		if (step == null) return;
		
		dev.timestamp = timestamp;
		dev.status = status;
		
		if (step.event != null) {
			LOG.debug("publishing event [{}] for device [{}]", step.event, dev.mac)
			publishEvent(step.event, dev);
		}
	}
	
	
	public enum class DeviceWorkflow(val from: DeviceStatus, val to: DeviceStatus, val event: Events? = null) {
		NEW_UP(DeviceStatus.NEW, DeviceStatus.UP, Events.NEW_NETWORK_DEVICES_MESSAGE),
		NEW_QUARANTINE(DeviceStatus.NEW, DeviceStatus.QUARANTINE),
		UP_QUARANTINE(DeviceStatus.UP, DeviceStatus.QUARANTINE),
		QUARANTINE_UP(DeviceStatus.QUARANTINE, DeviceStatus.UP),
		QUARANTINE_DOWN(DeviceStatus.QUARANTINE, DeviceStatus.DOWN, Events.REM_NETWORK_DEVICES_MESSAGE),
		DOWN_UP(DeviceStatus.DOWN, DeviceStatus.UP, Events.NEW_NETWORK_DEVICES_MESSAGE);
		
		companion object {
			fun resolve(curr: DeviceStatus, new: DeviceStatus) = DeviceWorkflow.values().firstOrNull { it.from == curr && it.to == new };
		}
	}

}