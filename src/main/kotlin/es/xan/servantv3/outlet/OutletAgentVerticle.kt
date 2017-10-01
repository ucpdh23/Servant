package es.xan.servantv3.laundry

import es.xan.servantv3.Constant

import es.xan.servantv3.AbstractServantVerticle
import io.vertx.core.logging.LoggerFactory
import es.xan.servantv3.Events
import es.xan.servantv3.messages.Temperature
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.TextMessageToTheBoss
import es.xan.servantv3.Action
import es.xan.servantv3.MessageBuilder
import es.xan.servantv3.outlet.OutletVerticle

class LaundryVerticle : AbstractServantVerticle(Constant.LAUNDRY_VERTICLE) {
	companion object {
		val ACTIVEPWR1 = """active_pwr1:(\d+.\d+)\s*\n""".toRegex()
		
        val LOG = LoggerFactory.getLogger(LaundryVerticle::class.java.name)
		
        fun findActivePwr1(text : String) = ACTIVEPWR1.find(text)?.groups?.get(1)?.value?:"0.0"
		
		fun isWorking(x : JsonObject) = findActivePwr1(x.getString("message")) !== "0.0"
		
    }
	
	fun notifyWasStoped() = publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("laundry is off"));
	
	init {
		supportedActions(Actions::class.java)
	}
	
	enum class Actions(val clazz : Class<*>? ) : Action {
		/**
		 * Checks the current status the all the available devices
		 */
		CHECK_STATUS(null),
		;
	}
	
	fun check_status(message : Message<Any>) {
		val reply = MessageBuilder.createReply().apply {
			setOk()
			setMessage(currState.name)
		}
		
		message.reply(reply.build())
	}
	
	override fun start() {
		super.start();
		
		vertx.setPeriodic(300000, { _ -> 
			source()
		});
	}
	
	var currState : States = States.STOPPED;
	
	enum class States(vararg val trans : Transition) {
		STOPPED(
			Transition({x -> isWorking(x)}, {States.WORKING})),
		WORKING(
			Transition({x -> !isWorking(x)}, {States.MAYBE_HAS_STOPED})),
		MAYBE_HAS_STOPED(
			Transition({x -> isWorking(x)}, {States.WORKING}),
			Transition({x -> !isWorking(x)},{v -> v.notifyWasStoped(); States.STOPPED})
			),
		;
		
	}
	
	class Transition(val predicate : (JsonObject) -> Boolean, val operation: (LaundryVerticle) -> States)
	
	fun source() {
		publishAction(OutletVerticle.Actions.STATUS, {e -> stream(e.result())})
	}
	
	fun stream(message : Message<Any>) {
		val body = message.body() as JsonObject;
		
		LOG.debug("current state [{}]", currState)
		
		for (tran : Transition in currState.trans) {
			if (tran.predicate.invoke(body)) {
				this.currState = tran.operation.invoke(this);
				break;
			}
		}
		
		LOG.debug("new state [{}]", currState)
	}
	
}