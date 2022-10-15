package es.xan.servantv3.webservice

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Constant
import io.vertx.ext.web.Router
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.BridgeEventType
import io.vertx.core.Vertx
import io.vertx.ext.web.handler.BodyHandler

class WebServerVerticle : AbstractServantVerticle(Constant.WEBSERVER_VERTICLE) {
	
	companion object {
        val LOG = LoggerFactory.getLogger(WebServerVerticle::class.java.name) 
    }
	
	val mPort by lazy { config().getJsonObject("WebServerVerticle").getInteger("port", 8080) }
	
	override fun start() {
		super.start();
		
		var router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		
		webSocketConfiguration(router);

		router = DashboardController(router, this).create();
		router = TemperatureController(router, this).create();
		router = DevicesController(router, this).create();
		router = MainController(router, this).create();
		router = SecurityController(router, this).create();

		vertx.createHttpServer().requestHandler(router::accept).listen(mPort);
		
		LOG.info("Started web server listening in port [{}]", mPort);
		
	}
	
	fun webSocketConfiguration(router : Router) {
		val options = BridgeOptions().apply {
    			addOutboundPermitted(PermittedOptions().setAddressRegex(Constant.EVENT))
    			addInboundPermitted(PermittedOptions().setAddressRegex(".*"));
		}
	
		router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options, { 
	         if (it.type().equals(BridgeEventType.SOCKET_CREATED)) {
	            LOG.info("A socket was created");
	         }
	         it.complete(true);
		}));
	}
	
}