package es.xan.servantv3.api

import io.vertx.core.json.JsonObject
import io.vertx.core.AbstractVerticle
import es.xan.servantv3.Events
import org.slf4j.LoggerFactory

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