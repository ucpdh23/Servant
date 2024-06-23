package es.xan.servantv3.laundry

import es.xan.servantv3.*
import es.xan.servantv3.api.State
import es.xan.servantv3.api.StateMachine
import es.xan.servantv3.api.Transition
import es.xan.servantv3.messages.Power
import es.xan.servantv3.outlet.OutletVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

/**
 * This verticle checks the current state of the outlet in order to determine whether the laundry has finished.
 * The secuence of steps is:
 *   1.- Every 60 seconds, this verticle publish the STATUS action of the outlet, and the result is send to the stream method.
 *   2.- The stream  method updates the state machine (currState field)
 *   3.- When the state is STOPPED, a event of LAUNDRY_OFF is published, this event is readed by the homeautomation module in order to notify
 */
class LaundryVerticle : AbstractServantVerticle(Constant.LAUNDRY_VERTICLE) {
	companion object {
		val ACTIVEPWR1 = """active_pwr1:(\d+.\d+)\s*\n""".toRegex()
		val SUMPWR1 = """energy_sum1:(\d+.\d+)\s*\n""".toRegex()
		
        val LOG = LoggerFactory.getLogger(LaundryVerticle::class.java.name)
		
        fun findActivePwr1(text : String) = ACTIVEPWR1.find(text)?.groups?.get(1)?.value?:"0.0"
		fun findSumPwr1(text : String) = SUMPWR1.find(text)?.groups?.get(1)?.value?:"0.0"
		
		
		fun isWorking(x : JsonObject) = findActivePwr1(x.getString("message")).toFloat() > 2.0F
		
		fun sumPower1(x : JsonObject) = findSumPwr1(x.getString("message")).toFloat()
		
    }
	
	fun notifyWasStoped(x : JsonObject) {
		val newPower = sumPower1(x);
		
		//https://api.esios.ree.es/archives/70/download_json?locale=es
		
		publishEvent(Events.LAUNDRY_OFF, Power(newPower - power));
		
		power = 0F
	} 
	
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
			setMessage(machineState.getCurrentState())
		}
		
		message.reply(reply.build())
	}
	
	override fun start() {
		super.start();
		
		vertx.setPeriodic(45000, { _ -> 
			source()
		});
	}

	val machineState = StateMachine(States.STOPPED, this);
	
	enum class States(override vararg val trans : Transition<LaundryVerticle, out State<LaundryVerticle>>) : State<LaundryVerticle> {
		STOPPED(
			Transition({x -> isWorking(x)}, { v, b -> v.storePower(b); States.WORKING})),
		WORKING(
			Transition({x -> !isWorking(x)},{ v, b -> v.storeTempPower(b); States.FIRST_CONFIRMATION})),
		FIRST_CONFIRMATION(
			Transition({x -> isWorking(x)}, { _,_ -> States.WORKING}),
			Transition({x -> !isWorking(x)},{ v, b -> if (v.storedTempAsChanged(b)) States.WORKING else States.SECOND_CONFIRMATION})
			),
		SECOND_CONFIRMATION(
			Transition({x -> isWorking(x)}, { _,_ -> States.WORKING}),
			Transition({x -> !isWorking(x)},{ v, b -> if (v.storedTempAsChanged(b)) States.WORKING else States.THIRD_CONFIRMATION})
			),
		THIRD_CONFIRMATION(
			Transition({x -> isWorking(x)}, { _,_ -> States.WORKING}),
			Transition({x -> !isWorking(x)},{ v, b -> v.notifyWasStoped(b); States.STOPPED})
			),
		;
	}
	
	var power : Float = 0F;
	
	fun storePower(x : JsonObject) {
		power = sumPower1(x);
	}
	
	var tmpPower : Float = 0F
	
	fun storeTempPower(x : JsonObject) {
		tmpPower = sumPower1(x)
	}
	
	fun storedTempAsChanged(x : JsonObject): Boolean {
		val currTmpPower = sumPower1(x)
		
		return !currTmpPower.equals(tmpPower);
	}
	
	fun source() {
		publishAction(OutletVerticle.Actions.STATUS, {e -> stream(e.result())})
	}
	
	fun stream(message : Message<Any>) {
		val body = message.body() as JsonObject;
		
		machineState.process(body)
	}
	
}