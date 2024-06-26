package es.xan.servantv3.webservice

import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory
import java.io.File


/**
 * Any external sensor must use this endpoint in order to provide information about devices network status
 */
class DashboardController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	val log = LoggerFactory.getLogger(DashboardController::class.java.name)
	
	/**
	 * send File
	 */
	get("/dashboard").handler { context ->
		val input: File = File("dashboard.html")
		log.warn("processing input [{}]", input.absolutePath)

		val response: HttpServerResponse = context.response()
		response.sendFile(input.absolutePath)
	}

	get("/dashboard_bmp").handler { context ->
		val input: File = File("dashboard.bmp")
		log.warn("processing input [{}]", input.absolutePath)

		val response: HttpServerResponse = context.response()
		response.sendFile(input.absolutePath)
	}
	

})