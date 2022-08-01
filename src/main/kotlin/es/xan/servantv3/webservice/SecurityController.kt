package es.xan.servantv3.webservice

import es.xan.servantv3.Events
import es.xan.servantv3.messages.Event
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.core.logging.LoggerFactory
import java.io.FileNotFoundException
import java.lang.NullPointerException
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.core.json.Json
import es.xan.servantv3.temperature.TemperatureVerticle
import es.xan.servantv3.messages.Temperature
import java.util.Date



class SecurityController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	
	val log = LoggerFactory.getLogger(SecurityController::class.java.name)
	
	get("/security/door/:status").handler {
		publisher.publishEvent(Events._EVENT_, Event(
			"door",
			it.request().params().get("status").toString(),
			Date().getTime()));
			
		it.response().end("ok");
	};
	
	post("/security/video").handler { context ->
	};

})