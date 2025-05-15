package es.xan.servantv3.webservice

import es.xan.servantv3.mcp.MCPVerticle
import es.xan.servantv3.messages.MCPMessage
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory
import java.util.*


class SSEController constructor(val router: Router, val publisher : WebServerVerticle) {

    var writer: HttpServerResponse? = null;

    companion object {
        val LOGGER = LoggerFactory.getLogger(SSEController::class.java.name)
    }

    val sseEndpoint = "/sse"
    val messageEndpoint = "/message"

    val handler : Router.() -> Unit = {
        get(sseEndpoint).handler { context ->
            val request = context.request()
            val response = context.response()
            response.setChunked(true)

            val accept = request.getHeader("Accept")
            if (accept != null && !accept.contains("text/event-stream")) {
                response.setStatusCode(406)
                response.end()
                return@handler
            }

            val sessionId = UUID.randomUUID().toString();
            LOGGER.debug("Creating Session", sessionId)
            publisher.publishAction(MCPVerticle.Actions.CREATE_SESSION, MCPMessage(sessionId, JsonObject()))

            response.setStatusCode(200)
            response.putHeader("Content-Type", "text/event-stream")
            response.putHeader("Cache-Control", "no-cache")
            response.putHeader("Connection", "keep-alive")
            response.putHeader("Access-Control-Allow-Origin", "*")

            // Here I would like to assign the _response variable in SSEControler with the context.response()
            this@SSEController.writer = response;

            this@SSEController.sendEvent("endpoint", messageEndpoint + "?sessionId=" + sessionId)
        }

        post(messageEndpoint).handler { context ->
            val request = context.request()
            val sessionId = request.getParam("sessionId")
            LOGGER.debug("sessionId {}", sessionId)

            val body = context.body().asJsonObject()
            LOGGER.debug("body {}", body)

            publisher.publishAction(MCPVerticle.Actions.HANDLE_MESSAGE, MCPMessage(sessionId, body))
        }
    };

    fun sendEvent(eventType: String, event: String) {
        this.writer?.write("event: " + eventType + "\n")
        this.writer?.write("data: " + event + "\n\n")
    }

    fun create() : Router {
        this.router.apply(this.handler)

        return this.router;
    }

    fun publishEvent(message : JsonObject) {
        LOGGER.debug("publish Event {}", message);
        sendEvent("", message.toString())
    }

}