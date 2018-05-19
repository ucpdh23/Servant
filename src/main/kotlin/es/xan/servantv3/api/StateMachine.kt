package es.xan.servantv3.api

import io.vertx.core.json.JsonObject
import io.vertx.core.AbstractVerticle
import es.xan.servantv3.Events


interface State {}

class Transition<V : AbstractVerticle, S : State>(val predicate : (JsonObject) -> Boolean, val operation: (V) -> S)

class EventTransition<V : AbstractVerticle, S : State>(val predicate : (Events, JsonObject) -> Boolean, val operation: (V) -> S)