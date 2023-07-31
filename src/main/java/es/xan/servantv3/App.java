package es.xan.servantv3;

import es.xan.servantv3.folder.FolderVerticle;
import es.xan.servantv3.shoppinglist.ShoppingListVerticle;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import org.slf4j.bridge.SLF4JBridgeHandler;

import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.calendar.CalendarVerticle;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.lamp.LampVerticle;
import es.xan.servantv3.laundry.LaundryVerticle;
import es.xan.servantv3.network.NetworkVerticle;
import es.xan.servantv3.outlet.OutletVerticle;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.mqtt.MqttVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import es.xan.servantv3.webservice.WebServerVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class App extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	TcpEventBusBridge bridge;

	@Override
	public void start() {
		LOGGER.info("Starting servant app");
		
		initializeLogBridges();
		initializeVerticles();
		initializeEventBusBridge();
		
		LOGGER.info("Started :)");
	}

	private void initializeEventBusBridge() {
		bridge = TcpEventBusBridge.create(
				vertx,
				new BridgeOptions()
						.addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
						.addOutboundPermitted(new PermittedOptions().setAddressRegex(".*")));

		LOGGER.info("Opening eventbus port 7000...");
		bridge.listen(7000, res -> {
			if (res.succeeded()) {
				// succeed...
				LOGGER.info("Starting eventbus connection...");
			} else {
				// fail...
				LOGGER.info("Failing eventbus connection...");
			}
		});
	}

	private void initializeVerticles() {
		JsonObject config = Vertx.currentContext().config();
		
		vertx.deployVerticle(WebServerVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(TemperatureVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(ParrotVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(STSVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(NetworkVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(HomeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(ThermostatVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(SensorVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(OutletVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(LaundryVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(LampVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(CalendarVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(WhiteboardVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(ShoppingListVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(MqttVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(FolderVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		
	}

	private void initializeLogBridges() {
		// Optionally remove existing handlers attached to j.u.l root logger
		 SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

		 // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
		 // the initialization phase of your application
		 SLF4JBridgeHandler.install();		
	}
}
