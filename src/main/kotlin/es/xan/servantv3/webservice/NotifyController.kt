package es.xan.servantv3.webservice

import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.TextMessageToTheBoss
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory


class NotifyController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	
	val log = LoggerFactory.getLogger(NotifyController::class.java.name)
	
	post("/notify/boss").handler { context ->
		try {
			val body: JsonObject = context.bodyAsJson
			val text = body.getString("text")
			
			if (text == null || text.isEmpty()) {
				context.response().apply {
					setStatusCode(400)
					end("""{"success": false, "message": "El campo 'text' es requerido"}""")
				}
				return@handler
			}
			
			log.debug("Sending notification to boss: {}", text)
			
			val message = TextMessageToTheBoss(text)
			publisher.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, message)
			
			context.response().apply {
				setStatusCode(200)
				end("""{"success": true, "message": "Notificación enviada"}""")
			}
		} catch (e: Exception) {
			log.error("Error processing notification request", e)
			context.response().apply {
				setStatusCode(500)
				end("""{"success": false, "message": "Error interno del servidor"}""")
			}
		}
	}

})
