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
import es.xan.servantv3.network.NetworkVerticle
import es.xan.servantv3.messages.Device
import es.xan.servantv3.messages.DeviceStatus


/**
 * Any external sensor must use this endpoint in order to provide information about devices network status
 */
class DevicesController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	
	/**
	 * Updates the status of a device with the provided info
	 * allowed values for the argument status are UP and DOWN
	 */
	get("/devices/:mac/:status").handler {
		publisher.publishAction(NetworkVerticle.Actions.UPDATE_DEVICE_STATUS,
			Device("",
					it.request().params().get("mac"),
					DeviceStatus.valueOf(it.request().params().get("status")),
					Date().getTime()));
			
		it.response().end("ok");
	};
	
	get("/devices").handler {
		publisher.publishAction(NetworkVerticle.Actions.LIST,
			{reply -> it.response().end(reply.result().body().toString())});
		
	};

})