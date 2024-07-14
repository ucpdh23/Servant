package es.xan.servantv3.sensors

import es.xan.servantv3.*
import es.xan.servantv3.messages.Sensor
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.collections.HashMap

class SensorVerticle : AbstractServantVerticle(Constant.SENSOR_VERTICLE) {
	companion object {
        val LOG = LoggerFactory.getLogger(SensorVerticle::class.java.name)
    }
	
	init {
		supportedActions(Actions::class.java)
	}
	
	enum class Actions(val clazz : Class<*>? ) : Action {
		RESET_SENSOR(Sensor::class.java)
		;
	}
	
	val mHost		: String by lazy { config().getJsonObject("SensorVerticle").getString("server", "localhost") }
	val mLogin		: String by lazy { config().getJsonObject("SensorVerticle").getString("usr", "guest") }
	val mPassword	: String by lazy { config().getJsonObject("SensorVerticle").getString("pws", "guest") }
	val mSensors	: Map<String, String> by lazy {
		HashMap<String,String>().apply {
			config().getJsonObject("SensorVerticle").getJsonArray("items").forEach{ it ->
				val item = it as JsonObject
				val name = item.getString("name").replaceFirstChar { it.lowercase(Locale.getDefault()) }
				val command = item.getString("command")
				LOG.info("putting [$name]->[$command]")
				put(name, command)
			}
		}
	};

	/**
	 * Deprecated
	 */
	fun reset_sensor(sensor : Sensor , message : Message<Any> ) {
		LOG.info("Asking to reset sensor [{}]", sensor.sensor);
		
		val item = sensor.sensor.replaceFirstChar { it.lowercase(Locale.getDefault()) };
		
		val sensorsToReset = when (item) {
			"all" -> this.mSensors.keys;
			in mSensors.keys -> setOf(item);
			else -> {
				val builder = MessageBuilder.createReply()
				builder.setError();
				builder.setMessage("Options [$mSensors.keys]")
				message.reply(builder.build())
				
				return;
			}
		}
		
		val resetedWithError = sensorsToReset.filter { it ->
			val command = mSensors.get(it);
			try {
				SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, command);
				false
			} catch (e: Throwable) {
				LOG.warn("Problems trying to manage ssh remote command [$command]", e);
				true
			}
		}
		
		val reply = MessageBuilder.createReply().apply {
			when {
				resetedWithError.isEmpty() -> setOk()
				else -> {
					setError()
					setMessage("Problems reseting items [$resetedWithError]")
				}
			}
		}
		
		message.reply(reply.build());
	}

}