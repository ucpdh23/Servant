package es.xan.servantv3.network

import es.xan.servantv3.*
import es.xan.servantv3.messages.Device
import es.xan.servantv3.messages.DeviceSecurity
import es.xan.servantv3.messages.DeviceStatus
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Home network manager. Store information about the devices connected to the local network,
 * creating events when devices come in or go out.
 * According to the changes in the devices' status, this Verticle publishes two events: Events.REM_NETWORK_DEVICES_MESSAGE and Events.REW_NETWORK_DEVICES_MESSAGE.
 * Devices status info must be provided by external sensors through the REST API defined into the controller DevicesController.   
 */
class NetworkVerticle : AbstractServantVerticle(Constant.NETWORK_VERTICLE) {
	
	companion object {
        val LOG = LoggerFactory.getLogger(NetworkVerticle::class.java.name)
		val TTL = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES); 
    }

	private var mConfiguration: JsonObject? = null
	private val storedStatus: MutableMap<String, Device> = HashMap();

	init {
		LOG.info("loading NetworkVerticle...")
		supportedActions(Actions::class.java)
		loadKnownDevides()
		LOG.info("loaded NetworkVerticle")
	}

	fun loadKnownDevides() {
		LOG.info("loading known devices...");
		App.connection.createStatement().use { statement ->
			statement.executeUpdate("create table if not exists network_device (user string, mac string, security string)")

			val currTimeStamp = Date().time
			val rs = statement.executeQuery("select * from network_device")
			while (rs.next()) {
				val security = DeviceSecurity.valueOf(rs.getString("security"))
				val device = Device(rs.getString("user"), rs.getString("mac"), DeviceStatus.UNKNOWN,  security, currTimeStamp, currTimeStamp)

				storedStatus[device.mac] = device
			}

			LOG.info("loaded [{}] devices", storedStatus.size)
		}
	}

	override fun start() {
		super.start();

		this.mConfiguration = Vertx.currentContext().config().getJsonObject("NetworkVerticle")

		vertx.setPeriodic(200000) { _ ->
			publishAction(Actions.CHECK_STATUS);
		}
	}
	

	// *********** Supported actions
	
	enum class Actions(val clazz : Class<*>? ) : Action {
		/**
		 * Updates the status of the passing device
		 */
		UPDATE_DEVICE_SECURITY(Device::class.java),
		
		/**
		 * Checks the current status the all the available devices
		 */
		CHECK_STATUS(null),
		
		/**
		 * Returns a list with the current devices status
		 */
		LIST(null);
	}
	
	
	fun update_device_security(input: Device) {
		App.connection.createStatement().use { statement ->
			statement.executeUpdate("UPDATE network_device set security = '" + input.security.name + "' where mac='" + input.mac + "'")
		}
		this.storedStatus[input.mac]?.security = input.security
		this.storedStatus[input.mac]?.status = DeviceStatus.UNKNOWN
	}
		
	fun check_status() {
		LOG.debug("checking network status...");

		val result = SSHUtils.runRemoteCommandExtended(
			this.mConfiguration?.getString("server"),
			this.mConfiguration?.getString("usr"),
			this.mConfiguration?.getString("pws"),
			"nmap -PR -sn 192.168.1.0/24")

		val networkDevices = computeDevices(result.output)

		val currTimeStamp = Date().time

		val stateChanged : MutableSet<Device> = HashSet()
		networkDevices.forEach { device ->
			val found = storedStatus[device.mac]

			if (found == null) {
				storedStatus[device.mac] = device
				stateChanged.add(device)
			} else {
				if (found.status != DeviceStatus.UP) {
					if (found.status != DeviceStatus.UNKNOWN || found.security == DeviceSecurity.INSECURE)
						stateChanged.add(found)

					found.firstTimestamp = device.firstTimestamp
					found.lastTimestamp = device.lastTimestamp
					found.status = DeviceStatus.UP

				} else {
					found.lastTimestamp = device.lastTimestamp
				}
			}
		}

		storedStatus
			.filterValues { dev -> dev.lastTimestamp < currTimeStamp - TTL}
			.forEach { _, dev ->
				if (dev.status != DeviceStatus.UNKNOWN || dev.security == DeviceSecurity.INSECURE)
					stateChanged.add(dev)

				dev.status = DeviceStatus.DOWN
			};

		for (device in stateChanged) {
			publishEvent(Events.NETWORK_DEVICES_STATUS_UPDATED, device)
		}
	}

	private fun computeDevices(commandOutput : String) : List<Device> {
		val output = ArrayList<Device>()

		var ipAddress : String? = null
		var mac : String? = null

		val currTimeStamp = Date().time
		for (line in commandOutput.lines()) {
			if (line.startsWith("Nmap done")) {

			} else if (line.startsWith("Nmap ")) {
				ipAddress = line.split(" for ")[1]
			} else if (line.startsWith("MAC Address: ")) {
				mac = line.split(" ")[2]

				if (ipAddress != null && mac != null) {
					output.add(Device(ipAddress, mac, DeviceStatus.UP, DeviceSecurity.UNKNOWN, currTimeStamp, currTimeStamp))

					ipAddress = null
					mac = null
				}
			}
		}

		return output
	}
	
	fun list(message: Message<Any>) {
		val devices = storedStatus.values.map{device -> JsonUtils.toJson(device)};
		
		val builder = MessageBuilder.createReply().apply {
			setResult(devices);
			setOk()
		}
		
		message.reply(builder.build());
	}

}