package es.xan.servantv3.brain.nlp;


import es.xan.servantv3.Action;
import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.brain.UserContext;
import es.xan.servantv3.brain.nlp.OperationUtils.Reply;
import es.xan.servantv3.github.AzureDevOpsVerticle;
import es.xan.servantv3.github.GithubVerticle;
import es.xan.servantv3.homeautomation.HomeUtils;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.lamp.LampVerticle;
import es.xan.servantv3.laundry.LaundryVerticle;
import es.xan.servantv3.messages.*;
import es.xan.servantv3.modes.SecurityModeVerticle;
import es.xan.servantv3.outlet.OutletVerticle;
import es.xan.servantv3.productivity.ProductivityVerticle;
import es.xan.servantv3.road.RoadVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.shoppinglist.ShoppingListVerticle;
import es.xan.servantv3.temperature.TemperatureUtils;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.AutomaticMode;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.core.eventbus.Message;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static es.xan.servantv3.brain.nlp.RuleUtils.*;
import static es.xan.servantv3.brain.nlp.OperationUtils.reply;

/**
 * Rules to transform NLM into Actions for the vertx event bus.
 * 
 * Any rule contains the following information:
 * <ul>
 * 	<li>Action to be performed</li>
 *  <li>predicate for the incoming message</li>
 *  <li>Functor to create a bean with the information recoverd from the NLM</li>
 *  <li>Actions to perform once the action is processed by the event bus</li>
 * </ul>
 * 
 * @author alopez
 *
 */
public enum Rules {
	HELP(STSVerticle.Actions.HELP,
			isContextFree()
				.and(messageIs("help||ayuda")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Information about all the available commands"
	),
	TRACK_TRAVEL(RoadVerticle.Actions.START_MONITORING,
			isContextFree()
					.and(messageContains("https://maps.app.goo.gl")),
			(tokens, userContext) -> {return new TextMessage(userContext.getUser(), concatStrings(tokens));},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Start tracking a journey"
	),
	NOTIFY_BOSS(HomeVerticle.Actions.NOTIFY_ALL_BOSS,
			isContextFree()
					.and(messageStartsWith("notify")),
			(tokens, userContext) -> {return new TextMessageToTheBoss(concatStrings(tokens));},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Send a Instant Message (or notification) to the user."
	),
	HOME_DOOR_OPEN(HomeVerticle.Actions.DOOR_OPEN,
			isContextFree()
					.and(messageContains("testing")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"testing door"
	),
	UNTRACK_TRAVEL(RoadVerticle.Actions.STOP_MONITORING,
			isContextFree()
				.and(messageContains("untrack")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"untrack"
	),
	SHUTDOWN_SECURITY(HomeVerticle.Actions.SHUTDOWN_SECURITY,
			isContextFree()
					.and(messageContains("apagar"))
					.and(messageContains("security")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"apagar security"
	),
	START_SHOPPING_LIST(ShoppingListVerticle.Actions.START_LIST,
			isContextFree()
					.and(messageContains("comenzar"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention("Shopping"); return null; },
			msg -> { return reply( null, OperationUtils.forwarding(msg));},
			"Ex. comenzar lista"
	),
	REMOVE_ITEM_FROM_SHOPPING_LIST(ShoppingListVerticle.Actions.REMOVE_FROM_LIST,
			isContextFree()
					.and(messageContains("eliminar||quitar||quita||elimina"))
					.and(messageContains("elemento||producto"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {return new TextMessage(userContext.getUser(), findNumber(tokens));},
			msg -> { return reply( null, OperationUtils.forwarding(msg));},
			"Ex. eliminar elemento [number] de lista"
	),
	CONTINUE_SHOPPING_LIST(ShoppingListVerticle.Actions.CONTINUE_LIST,
			isContextFree()
					.and(messageContains("continuar"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention("Shopping"); return null; },
			msg -> { return reply( null, OperationUtils.forwarding(msg));},
			"Ex. continuar lista"
	),
	END_SHOPPING_LIST(ShoppingListVerticle.Actions.END_LIST,
			isContext("Shopping")
				.and(messageContains("finalizar"))
				.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention(""); return null; },
			msg -> { return reply( null, OperationUtils.forwarding(msg));},
			"Ex. finalizar lista"
	),
	SHOW_SHOPPING(ShoppingListVerticle.Actions.GET_LIST,
			messageContains("muestra", "mostrar")
			.and(messageContains("lista")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Load the list of items for the grocery. Ex. Muestra lista"
	),
	ADD_TO_SHOPPING_LIST(ShoppingListVerticle.Actions.SAVE_ITEM,
			isContext("Shopping"),
			(tokens, userContext) -> {return new TextMessage(userContext.getUser(), concatStrings(tokens));},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Add a new item into the shopping list for the grocery. Ex. Item"
	),

	HN_TODAYS_ITEMS(ProductivityVerticle.Actions.RESOLVE_TODAY_ITEMS,
			isContextFree()
					.and(messageStartsWith("hacker news")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.result(msg));},
			"Ex. hacker news"
	),

	PRINT_ACTION(WhiteboardVerticle.Actions.PRINT,
			isContextFree()
				.and(messageStartsWith("imprimir")),
			(tokens, userContext) -> {return new TextMessage("dummy", concatStrings(tokens));},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. imprimir hola"
	),

	LAUNDRY_STATUS(LaundryVerticle.Actions.CHECK_STATUS,
			isContextFree()
				.and(messageContains("laundry||lavadora"))
				.and(messageContains("status||estado")),
			(tokens, userContext) -> {return null;},
		msg -> { return reply(null, OperationUtils.forwarding(msg));},
		"Ex. laundry status"
		),
	OUTLET_STATUS(OutletVerticle.Actions.STATUS,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("status||estado")),
			(tokens, userContext) -> {return null;},
		msg -> { return reply(null, OperationUtils.forwarding(msg));},
		"Ex. outlet status"
		),
	REPELENTE_ON(LampVerticle.Actions.SWITCH_CHILDRENROOM_OUTLET,
			isContextFree()
					.and(messageContains("repelente"))
					.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. repelente on"
	),
	REPELENTE_OFF(LampVerticle.Actions.SWITCH_CHILDRENROOM_OUTLET,
			isContextFree()
					.and(messageContains("repelente"))
					.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. repelente on"
	),
	OUTLET_ON(OutletVerticle.Actions.SWITCHER,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
		msg -> { return reply(null, OperationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_OFF(OutletVerticle.Actions.SWITCHER,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
		msg -> { return reply(null, OperationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_SET(OutletVerticle.Actions.SET,
			isContextFree()
				.and(messageContains("outlet"))
				.and(messageContains("field")),
			(tokens, userContext) -> {return new Configure(nextTokenTo("field").apply(tokens), nextTokenTo("value").apply(tokens));},
		msg -> { return reply(null, OperationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	
	BOILER_AUTOMATIC_ON(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("on||encender||activar||conectar"))
				.and(messageContains("automático||automatic||automatico")),
			(tokens, userContext) -> {return new AutomaticMode() {{ this.enabled = true; }};},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. automatic boiler on"
			),

	BOILER_AUTOMATIC_OFF(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("off||apagar||desactivar||desconectar"))
				.and(messageContains("automático||automatic||automatico")),
			(tokens, userContext) -> {return new AutomaticMode() {{ this.enabled = false; }};},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. automatic boiler off"
			),

	
	BOILER_ON(ThermostatVerticle.Actions.SWITCH_BOILER,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. boiler on"
			),
			
	BOILER_OFF(ThermostatVerticle.Actions.SWITCH_BOILER,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. boiler off"
			),
	
	LAMP_ON(LampVerticle.Actions.SWITCH_LIVINGROOM_LAMP,
			isContextFree()
				.and(messageContains("lamp||lampara||lámpara"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. lamp on"
			),
			
	LAMP_OFF(LampVerticle.Actions.SWITCH_LIVINGROOM_LAMP,
			isContextFree()
				.and(messageContains("lamp||lampara||lámpara"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. lamp off"
			),

	
	TEMPERATURE_QUERY(TemperatureVerticle.Actions.QUERY,
			isContextFree()
				.and(messageContains("temperatura||temperature"))
				.and(messageContains("minimun||mínima||minima")),
			(tokens, userContext) -> {
					String room = null;
					if (contains("livingroom").test(tokens)) 
						room = "livingRoom";
					else if (contains("outside").test(tokens))
						room = "outside";
					else if (contains("bedroom").test(tokens))
						room = "bedRoom";
					
					return TemperatureUtils.buildMinTempQuery(room, 1000 * 3600 * 24);},
			msg -> { return reply(null, TemperatureUtils.toString(msg));},
			"Ex. temperature minumun"
			),
	
	TEMPERATURE(TemperatureVerticle.Actions.LAST_VALUES,
			isContextFree()
				.and(messageContains("temperatura||temperature")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, TemperatureUtils.toString(msg));},
			"Returns the temperature from the different sensors at home (internal and external sensors). The response contains the timestamp and temperature for each location. Shortcut 'temperature'"
			),
	TEMPERATURE_AGGREGATION(TemperatureVerticle.Actions.AGGREGATION,
			isContextFree()
					.and(messageContains("temperatura||temperature"))
					.and(messageContains("aggregation")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, TemperatureUtils.toString(msg));},
			"Returns a data aggregation from the temperature datamodel. This tool can response questions about the average or max or min temperature for a defined range of time. This tools requires a valid JsonArray object to query the internal mongodb database. The data model is composed by room (String) suported values [inside,outside], temperature (Float) temperature in celcius degree, timestamp (Long): Timestamp in epoch format"
		),
	PHONE_CALL(HomeVerticle.Actions.PHONE_CALL,
			isContextFree()
					.and(messageContains("telefono||teléfono")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, "Done!");},
			"Ex. telefono"
			),
	NEXT_EVENTS(WhiteboardVerticle.Actions.RESOLVE_CALENDAR_EVENTS,
			isContextFree()
					.and(messageContains("events"))
					.and(messageContains("google")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Returns the list of the next events in the calendar. This list contains information of events for the next few days. Ex. events google"
			),
	HOME(HomeVerticle.Actions.GET_HOME_STATUS,
			isContextFree()
				.and(messageContains("home||casa")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, HomeUtils.toString(msg));},
			"Ex. home"
			),
	DEVICE_UPDATE_DANGERIOUS(HomeVerticle.Actions.PROCESS_DEVICE_SECURITY,
			isContextFree()
					.and(messageStartsWith("source: is device ")),
			(tokens, userContext) -> {return new TextMessage("dummy", userContext.thisMessage);},
			msg -> {return reply(null, OperationUtils.forwarding(msg));},
			"Mark device status"
			),
	RESET_SENSOR(SensorVerticle.Actions.RESET_SENSOR,
			isContextFree()
				.and(messageContains("sensor")),
			(tokens, userContext) -> { return new Sensor(nextTokenTo("sensor").apply(tokens));},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Ex. sensor xxxx"
			),
	SECURITY_MODE_ON(SecurityModeVerticle.Actions.CHANGE_STATUS,
			isContextFree()
					.and(messageContains("on"))
					.and(messageContains("security")),
			(tokens, userContext) -> { return new UpdateState("on");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Enable the security mode in order to monitor the main door status. Ex. security on"
			),
	SECURITY_MODE_OFF(SecurityModeVerticle.Actions.CHANGE_STATUS,
			isContextFree()
					.and(messageContains("off"))
					.and(messageContains("security")),
			(tokens, userContext) -> { return new UpdateState("off");},
			msg -> { return reply(null, OperationUtils.forwarding(msg));},
			"Disable the security mode. Ex. security off"
	),
	GITHUB_VERSION_UPDATE(GithubVerticle.Actions.UPDATE_VERSION,
			isContextFree()
					.and(messageStartsWith("source: new version ")),
			(tokens, userContext) -> {return new TextMessage("dummy", userContext.thisMessage);},
			msg -> {return reply(null, OperationUtils.forwarding(msg));},
			"Mark device status"
	),
	GITHUB_FETCH_ISSUES(AzureDevOpsVerticle.Actions.FETCH_OPEN_WORK_ITEMS,
			isContextFree()
					.and(messageStartsWith("open issues")),
			(tokens, userContext) -> {return null;},
			msg -> {return reply(null, OperationUtils.forwarding(msg));},
			"Mark device status"
	),
	GITHUB_ADD_COMMENT_TO_ISSUE(AzureDevOpsVerticle.Actions.ADD_COMMENT_TO_WORK_ITEM,
			isContextFree()
					.and(messageStartsWith("add comment to issue")),
			(tokens, userContext) -> {return  new JiraMessage("manager", 1, "prueba");},
			msg -> {return reply(null, OperationUtils.forwarding(msg));},
			"Mark device status"
	),
	GITHUB_ISSUE_DETAILS(AzureDevOpsVerticle.Actions.FETCH_WORK_ITEM_DETAILS,
			isContextFree()
					.and(messageStartsWith("fetch issue")),
			(tokens, userContext) -> {return  new JiraMessage("manager", 1, "prueba");},
			msg -> {return reply(null, OperationUtils.forwarding(msg));},
			"Mark device status"
	),

//	REMINDER(Constant.PARRONT_VERTICLE, false, messageContains("reminder"), TranslationType.COPY),
//	BOILER_STATUS(Constant.OPERATION_BOILER_STATE_CHECKER, true, messageContains("checkBoilerStatus"), TranslationType.OPERATION),
//	BOILER_AUTOMATIC_MODE(Constant.THERMOSTAT_VERTICLE, true, messageContains("boiler||caldera")
//																.and(messageContains("automatic||automatico||automático")), TranslationType.ON_OFF),
	;
	
	final Predicate<Pair<String, UserContext>> mPredicate;
	final BiFunction<String[], UserContext, Object> mFunction;
	final Action mAddress;
	final Function<Message<Object>, Reply> mResponse;
	final String mHelpMessage;
	
	private Rules(Action address, Predicate<Pair<String, UserContext>> predicate, BiFunction<String[],UserContext,Object> variantFunction, Function<Message<Object>, Reply> response, String helpMessage) {
		this.mAddress = address;
		this.mPredicate = predicate;
		this.mFunction = variantFunction;
		this.mResponse = response;
		this.mHelpMessage = helpMessage;
	}

	public Action getAction() {
		return this.mAddress;
	}

	public Function<Message<Object>, Reply> getResponseProcessor() {
		return this.mResponse;
	}
	
	public String getHelpMessage() {
		return this.mHelpMessage;
	}

}
