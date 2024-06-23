package es.xan.servantv3.webservice

import es.xan.servantv3.messages.Temperature
import es.xan.servantv3.temperature.TemperatureVerticle
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory
import java.util.*


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