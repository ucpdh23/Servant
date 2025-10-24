package es.xan.servantv3.webservice

import es.xan.servantv3.mcp.MCPVerticle
import es.xan.servantv3.messages.MCPMessage
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import org.slf4j.LoggerFactory
import org.thymeleaf.util.StringUtils
import java.util.*


class SSEController constructor(val router: Router, val publisher : WebServerVerticle) {

    var writer: HttpServerResponse? = null;

    companion object {
        val LOGGER = LoggerFactory.getLogger(SSEController::class.java.name)
    }

    val sseEndpoint = "/sse"
    val messageEndpoint = "/message"

    val handler : Router.() -> Unit = {
        LOGGER.info("Creating SSE endpoints...")
        get(sseEndpoint).handler { context ->
            LOGGER.debug("invoking SEE get method...")
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

            /*
            // Mantener conexión viva con "ping" cada X segundos
            publisher.vertx.setPeriodic(5000) {
                if (!response.closed()) {
                    response.write("event: ping\n")
                    response.write("data: keep-alive\n\n")
                }
            }
             */
        }

        post(messageEndpoint).handler { context ->
            LOGGER.debug("invoking SEE post method...")

            val request = context.request()
            val response = context.response()

            val sessionId = request.getParam("sessionId")
            LOGGER.debug("sessionId {}", sessionId)

            val body = context.body().asJsonObject()
            LOGGER.debug("body {}", body)

            publisher.publishAction(MCPVerticle.Actions.HANDLE_MESSAGE, MCPMessage(sessionId, body)) { response ->
                LOGGER.debug("response {}", response)
                if (response.failed())
                    LOGGER.info("{}", response.cause());
            }

            response.setStatusCode(200)
            response.end()
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
        sendEvent("message", message.toString())
    }

}