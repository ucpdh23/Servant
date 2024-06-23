package es.xan.servantv3.webservice

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException


class MainController constructor(override val router: Router, var publisher: WebServerVerticle) : Controller({

	val log = LoggerFactory.getLogger(MainController::class.java.name)

	route().handler(StaticHandler.create())

	route().last().handler { it.fail(404) }

	route().failureHandler { errorContext ->
		val e: Throwable? = errorContext.failure()
		if (e != null) {
			log.error(e.message, e)
		}
		val code = when (e) {
			is FileNotFoundException -> HttpResponseStatus.NOT_FOUND.code()
			is NullPointerException -> HttpResponseStatus.NOT_FOUND.code()
			is SecurityException -> HttpResponseStatus.UNAUTHORIZED.code()
			else ->
				if (errorContext.statusCode() > 0) {
					errorContext.statusCode()
				} else {
					500
				}
		}

		val result = mapOf(
				"success" to false,
				"message" to errorContext?.failure()?.message
		)

		errorContext.response().apply {
			setStatusCode(code)
			end(Json.encodePrettily(result))
		}
	}
})