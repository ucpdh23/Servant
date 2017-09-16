package es.xan.servantv3.outlet

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Constant
import es.xan.servantv3.Action
import es.xan.servantv3.messages.UpdateState
import es.xan.servantv3.messages.Configure
import io.vertx.core.eventbus.Message
import es.xan.servantv3.SSHUtils
import es.xan.servantv3.MessageBuilder
import io.vertx.core.logging.LoggerFactory

class OutletVerticle : AbstractServantVerticle(Constant.OUTLET_VERTICLE) {
	companion object {
        val LOG = LoggerFactory.getLogger(OutletVerticle::class.java.name) 
    }
	
	init {
		supportedActions(Actions::class.java)
	}
	
	enum class Actions(val clazz : Class<*>? ) : Action {
		SWITCHER(UpdateState::class.java),
		STATUS(null),
		SET(Configure::class.java);
	}
	
	val mHost		: String by lazy { config().getJsonObject("OutletVerticle").getString("host", "192.168.1.100") }
	val mLogin		: String by lazy { config().getJsonObject("OutletVerticle").getString("login", "ubnt") }
	val mPassword	: String by lazy { config().getJsonObject("OutletVerticle").getString("password", "ubnt") }
	
	val ON_COMMAND	= "echo \"1\" > /proc/power/relay1"
	val OFF_COMMAND	= "echo \"0\" > /proc/power/relay1"
	val STATUS_COMMAND = "grep . /proc/power/*";
	
	fun switcher(switcher : UpdateState, message: Message<Any>) {
		val reply = MessageBuilder.createReply().apply {
			try {
				when (switcher.newStatus) {
					"on" -> SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, ON_COMMAND);
					else -> SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, OFF_COMMAND);
				}
				setOk();
			} catch (e: Throwable) {
				LOG.warn("Problems trying to manage ssh remote command", e);
				setError();
				setMessage(e.localizedMessage)
			}
		}
		
		message.reply(reply.build());
	}
	
	fun status(message: Message<Any>) {
		val reply = MessageBuilder.createReply().apply {
			try {
				val response = SSHUtils.runRemoteCommandExtended(mHost, mLogin, mPassword, STATUS_COMMAND);
				setOk()
				setMessage(response.output)
			} catch (e: Throwable) {
				setError()
				setMessage(e.message)
			}
		}
		
		message.reply(reply.build())
	}

}
