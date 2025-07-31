package es.xan.servantv3;

import com.google.common.base.Strings;
import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.calendar.CalendarVerticle;
import es.xan.servantv3.folder.FolderVerticle;
import es.xan.servantv3.github.AzureDevOpsVerticle;
import es.xan.servantv3.github.GithubVerticle;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.knowledge.KnowledgeVerticle;
import es.xan.servantv3.lamp.LampVerticle;
import es.xan.servantv3.laundry.LaundryVerticle;
import es.xan.servantv3.mcp.MCPVerticle;
import es.xan.servantv3.modes.NightModeVerticle;
import es.xan.servantv3.modes.SecurityModeVerticle;
import es.xan.servantv3.mqtt.MqttVerticle;
import es.xan.servantv3.neo4j.Neo4jVerticle;
import es.xan.servantv3.network.NetworkVerticle;
import es.xan.servantv3.openia.OpenIAVerticle;
import es.xan.servantv3.outlet.OutletVerticle;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.road.RoadVerticle;
import es.xan.servantv3.scrumleader.ScrumLeaderVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.shoppinglist.ShoppingListVerticle;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import es.xan.servantv3.webservice.WebServerVerticle;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.sql.Connection;
import java.sql.DriverManager;

public class App extends AbstractVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

	TcpEventBusBridge bridge;
	public static Connection connection;
	private JsonObject mConfiguration;

	@Override
	public void start() {
		LOGGER.info("Starting servant app");

		this.mConfiguration = Vertx.currentContext().config().getJsonObject("App");

		initializeLogBridges();
		initializeLocalDatabase();
		initializeVerticles();
		initializeEventBusBridge();
		
		LOGGER.info("Started :)");
	}

	private void initializeLocalDatabase() {
		try {
			Class.forName("org.sqlite.JDBC");
			App.connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
		} catch (Exception e) {
			LOGGER.error("Problems loading sqlite driver", e);
			System.exit(1);
		}
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

		final String mode = this.mConfiguration.getString("mode");

		if (Strings.isNullOrEmpty(mode) || "complete".equals(mode.toLowerCase())) {
			vertx.deployVerticle(WebServerVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(MCPVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(ScrumLeaderVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(ParrotVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(STSVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(MqttVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(ShoppingListVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(SecurityModeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(NightModeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(TemperatureVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(RoadVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(NetworkVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(HomeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(ThermostatVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(SensorVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(OutletVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(LaundryVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(LampVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(CalendarVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(WhiteboardVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(FolderVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(KnowledgeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(OpenIAVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(Neo4jVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(GithubVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(AzureDevOpsVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		} else if ("security".equals(mode.toLowerCase())) {
			vertx.deployVerticle(WebServerVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(ParrotVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(STSVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(MqttVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(SecurityModeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(NightModeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(HomeVerticle.class.getName(), new DeploymentOptions().setConfig(config));
			vertx.deployVerticle(FolderVerticle.class.getName(), new DeploymentOptions().setConfig(config));
		}


	}

	private void initializeLogBridges() {
		// Optionally remove existing handlers attached to j.u.l root logger
		 SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

		 // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
		 // the initialization phase of your application
		 SLF4JBridgeHandler.install();		
	}
}
