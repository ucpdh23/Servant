package es.xan.servantv3.webservice

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



class TemperatureController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	
	val log = LoggerFactory.getLogger(TemperatureController::class.java.name)
	
	get("/temperature/:room/:value").handler {
		publisher.publishAction(TemperatureVerticle.Actions.SAVE, Temperature(
			it.request().params().get("room"),
			it.request().params().get("value").toFloat(),
			Date().getTime()));
			
		it.response().end("ok");
	};
	
	get("/listTemperature").handler { context ->
		publisher.publishAction(TemperatureVerticle.Actions.LAST_VALUES, { output ->
			context.response().setChunked(true);
			context.response().end(output.result().body().toString());
		});
	}; 

})