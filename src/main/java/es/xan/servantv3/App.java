package es.xan.servantv3;

import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.network.NetworkVerticle;
import es.xan.servantv3.parrot.ParrotVerticle;
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

	@Override
	public void start() {
		LOGGER.info("Starting servant app");
		
		JsonObject config = Vertx.currentContext().config();
		
		vertx.deployVerticle(WebServerVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(TemperatureVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(ParrotVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(STSVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(NetworkVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(HomeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		vertx.deployVerticle(ThermostatVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		
		LOGGER.info("Started :)");
	}
}
