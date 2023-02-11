package es.xan.servantv3.webservice

import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.Recorded
import es.xan.servantv3.messages.Recording
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.Router


class SecurityController constructor(override val router: Router, var publisher : WebServerVerticle) : Controller({
	
	val log = LoggerFactory.getLogger(SecurityController::class.java.name)
	
	post("/security/video/:code").handler { context ->
		log.debug("video " + context.request().params().get("code"))

		val uploads: List<FileUpload> = context.fileUploads()

		uploads.forEach {
			var info = Recorded(it.uploadedFileName(), context.request().params().get("code"))
			publisher.publishAction(HomeVerticle.Actions.MANAGE_VIDEO, info)
		}

		context.response().end("ok");
	};

})