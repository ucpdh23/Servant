package es.xan.servantv3.api

import io.vertx.core.json.JsonObject
import io.vertx.core.AbstractVerticle
import es.xan.servantv3.Events

/**
 * I love state machines!
 *
 * And as I love state machines I always try to transform complex functional workflows into simple state machines.
 * For this reason, I have created these interface and two abstract clases. 
 *
 * TODO
 */
interface State {}

class Transition<V : AbstractVerticle, S : State>(val predicate : (JsonObject) -> Boolean, val operation: (V) -> S)

class EventTransition<V : AbstractVerticle, S : State>(val predicate : (Events, JsonObject) -> Boolean, val operation: (V) -> S)