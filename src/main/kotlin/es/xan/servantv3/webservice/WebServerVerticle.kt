package es.xan.servantv3.webservice

//import io.vertx.ext.web.handler.sockjs.BridgeOptions
//import io.vertx.ext.web.handler.sockjs.PermittedOptions
//import io.vertx.ext.web.handler.sockjs.BridgeEventType
import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.github.AzureDevOpsVerticle
import es.xan.servantv3.github.AzureDevOpsVerticle.Actions
import es.xan.servantv3.github.AzureDevOpsVerticle.Companion
import es.xan.servantv3.messages.MCPMessage
import io.vertx.core.eventbus.Message
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.slf4j.LoggerFactory

class WebServerVerticle : AbstractServantVerticle(Constant.WEBSERVER_VERTICLE) {
	
	companion object {
        val LOG = LoggerFactory.getLogger(WebServerVerticle::class.java.name) 
    }
	
	val mPort by lazy { config().getJsonObject("WebServerVerticle").getInteger("port", 8080) }
	var sseController : SSEController? = null;

	init {
		AzureDevOpsVerticle.LOG.info("loading WebServerVerticle...")
		supportedActions(Actions::class.java)
		AzureDevOpsVerticle.LOG.info("loaded WebServerVerticle")
	}
	
	override fun start() {
		super.start();
		
		var router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		
		webSocketConfiguration(router);

		this.sseController = SSEController(router, this)
		router = this.sseController?.create();
		router = DashboardController(router, this).create();
		router = TemperatureController(router, this).create();
		router = DevicesController(router, this).create();
		router = MainController(router, this).create();
		router = SecurityController(router, this).create();

		vertx.createHttpServer().requestHandler(router).listen(mPort);
		
		LOG.info("Started web server listening in port [{}]", mPort);

		this.supportedActions(Actions::class.java)
		
	}

	enum class Actions(val clazz : Class<*>? ) : Action {
		PUBLIC_SSE_EVENT(MCPMessage::class.java),
		CLOSE_SSE(null)
		;
	}

	fun close_sse(msg: Message<Any>) {
		LOG.debug("close_sse ")
	}

	fun public_sse_event(message : MCPMessage, msg: Message<Any>) {
		LOG.debug("public_sse_event {}", message)
		this.sseController?.publishEvent(message.message)
	}
	
	fun webSocketConfiguration(router : Router) {
	/*
		val options = BridgeOptions().apply {
    			addOutboundPermitted(PermittedOptions().setAddressRegex(Constant.EVENT))
    			addInboundPermitted(PermittedOptions().setAddressRegex(".*"));
		}*/
		
		val sockJSHandler = SockJSHandler.create(vertx);
		val options = SockJSBridgeOptions();
		// mount the bridge on the router
	
		router.route("/eventbus/*")
			.subRouter(sockJSHandler.bridge(options));
		/*
		.handler(SockJSHandler.create(vertx).bridge(options, { 
	         if (it.type().equals(BridgeEventType.SOCKET_CREATED)) {
	            LOG.info("A socket was created");
	         }
	         it.complete(true);
		}));*/
	}
	
}