package es.xan.servantv3.webservice;

import java.util.Date;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Constant;
import es.xan.servantv3.messages.Temperature;
import es.xan.servantv3.messages.UpdateState;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;



public class WebServerVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebServerVerticle.class);
	
	private JsonObject mConfiguration;
	
	public WebServerVerticle() {
		super(Constant.WEBSERVER_VERTICLE);
	}
	
	public void start() {
		LOGGER.debug("Starting Web Server...");
		
		mConfiguration = Vertx.currentContext().config().getJsonObject("WebServerVerticle");

		final int port = mConfiguration.getInteger("port");
		
		Router router = buildRouter();

	    vertx.createHttpServer().requestHandler(router::accept).listen(port);
		
		LOGGER.info("Started web server listening in port [{}]", port);
	}

	private Router buildRouter() {
		Router router = Router.router(vertx);

		router.route("/eventbus/*").handler(eventBusHandler());
//		router.mountSubRouter("/api", apiRouter());
		
		router.get("/temperature/:room/:value").handler(context -> {
			this.publishAction(TemperatureVerticle.Actions.SAVE, new Temperature(
				context.request().params().get("room"),
				Float.parseFloat(context.request().params().get("value")),
				new Date().getTime()));
			
			context.response().end("ok");
		});
	
		router.get("/listTemperature").handler(context -> {
			this.publishAction(TemperatureVerticle.Actions.LAST_VALUES, output -> {
				context.response().setChunked(true);
				context.response().write(output.result().body().toString()).end();
			});
		});
	
		router.get("/boiler/:operation").handler(context -> {
			this.publishAction(ThermostatVerticle.Actions.SWITCH_BOILER, new UpdateState("on"));
			
			context.response().end("ok");
		});
	
	    router.route().handler(StaticHandler.create());

	    return router;
	}


	private SockJSHandler eventBusHandler() {
	    BridgeOptions options = new BridgeOptions()
	    			.addOutboundPermitted(new PermittedOptions().setAddressRegex(Constant.EVENT))
	    			.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"));
	    return SockJSHandler.create(vertx).bridge(options, event -> {
	         if (event.type().equals(BridgeEventType.SOCKET_CREATED)) {
	            LOGGER.info("A socket was created");
	        }
	        event.complete(true);
	    });
	}
}
