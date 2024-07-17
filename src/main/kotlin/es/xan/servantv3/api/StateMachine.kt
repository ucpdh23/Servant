package es.xan.servantv3.api

import es.xan.servantv3.*
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.timer

/**
 * I love state machines!
 *
 * And as I love state machines I always try to transform complex functional workflows into simple state machines.
 * For this reason, I have created these interface and two abstract clases. 
 *
 * Implementing a StateMachine is very easy. Just create a enum implementing the State interface and instantiate a
 * StateMachine class with the first state and the reference to the context object.
 * The enum represents the states of the state machine, and includes the transactions from one state to another.
 * 
 * In order to move to one state to another, just call the process method. This will evaluate the predicates of the transitions
 * of the current state until one returns true. This will be the transition to proceed determining the next state of the state machine. 
 * 
 */
interface State<V> {
	val trans : Array<out Transition<V, out State<V>>>
}

class Transition<V, S : State<V>>(val predicate : (JsonObject) -> Boolean, val operation: (V, JsonObject) -> S)

class EventTransition<V, S : State<V>>(val predicate : (Events, JsonObject) -> Boolean, val operation: (V) -> S)

data class AgentInput(val operation : Object, val entity : JsonObject) {
	fun <V> entityAs(clazz: Class<V>) : V {
		return JsonUtils.toBean(entity.encode(), clazz)
	}
}

class ServantContext<V>(val v: AbstractServantVerticle, val a: Agent<V>) {

	fun publishAction(action: Action, item : Any?) {
		v.publishAction(action, item)
	}

	fun timed(state : AgentState<V>, period : Long) : AgentState<V> {
		a.setupTimeout(period);
		return state;
	}
}

fun interface AgentState<V> {
	fun trans(v : ServantContext<V>) : Array<AgentTransition<V, AgentState<V>>>

	fun timeout() : AgentState<V> {
		return this
	}

	fun entering(servantContext: ServantContext<V>) {
	}

	fun exiting(servantContext: ServantContext<V>) {
	}
}

class AgentTransition<V, S : AgentState<V>>(val predicate : (context: AgentContext, input : V) -> Boolean, val operation: (context: AgentContext, input : V)  -> S)

interface AgentContext {
}

class Agent<V>(firstState: AgentState<V>, val verticle : AbstractServantVerticle, val context: AgentContext) {

	private var timeoutTimer: Timer? = null
	private val servantContext = ServantContext(verticle, this)

	companion object {
		val LOG = LoggerFactory.getLogger(Agent::class.java.name)
	}

	private var currentState = firstState


	private var timeout : Boolean = false

	fun process(input : V) : AgentState<V> {
		return _process(input, false)
	}

	fun _process(input : V?, timedout : Boolean) : AgentState<V> {
		synchronized(this) {
			val lastState = currentState

			if (timedout) {
				currentState = currentState.timeout()
			} else if (input != null) {
				val newState = currentState.trans(servantContext)
					.firstOrNull { tran -> tran.predicate.invoke(context, input) }
					?.run { operation.invoke(context, input) }

				if (newState == null) {
					currentState
				} else {
					timeoutTimer?.cancel()

					if (currentState != newState) {
						currentState.exiting(servantContext)
						newState.entering(servantContext)
					}

					LOG.info("Moving to [{}]", newState)

					currentState = newState
				}
			}
		}

		return currentState
	}

	fun setupTimeout(period: Long) {
		this.timeoutTimer = timer("timeout", period = period) {
			_process(null, true);
		}
	}

}

class StateMachine<V>(firstState: State<V>, val instance: V) {
	companion object {
        val LOG = LoggerFactory.getLogger(StateMachine::class.java.name)
    }
	
	var currentState = firstState
	
	fun process(data : JsonObject) : State<V> {
		val lastState = currentState
		
		currentState = currentState.trans
				.firstOrNull { tran -> tran.predicate.invoke(data) }
				?.run   { operation.invoke(instance, data) } ?: currentState;
		
		LOG.debug("Moving from state [{}] to [{}]", lastState, currentState)
		
		return currentState
	}
	
	fun getCurrentState() : String {
		if (currentState.javaClass.isEnum) {
			return (currentState as Enum<*>).name
		} else {
			return currentState.toString()
		}
	}
	
}