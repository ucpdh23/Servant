package es.xan.servantv3.brain.nlp;


import es.xan.servantv3.Action;
import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.brain.UserContext;
import es.xan.servantv3.brain.nlp.TranslationUtils.Reply;
import es.xan.servantv3.homeautomation.HomeUtils;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.lamp.LampVerticle;
import es.xan.servantv3.laundry.LaundryVerticle;
import es.xan.servantv3.messages.*;
import es.xan.servantv3.outlet.OutletVerticle;
import es.xan.servantv3.parrot.ParrotVerticle;
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
import static es.xan.servantv3.brain.nlp.TranslationUtils.reply;

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
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Information about all the available commands"
	),
	VIDEO(HomeVerticle.Actions.RECORD_VIDEO,
			isContextFree()
					.and(messageContains("record||grabar||graba"))
					.and(messageContains("video")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"graba video"
	),
	SHUTDOWN_SECURITY(HomeVerticle.Actions.SHUTDOWN_SECURITY,
			isContextFree()
					.and(messageContains("apagar"))
					.and(messageContains("security")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"apagar security"
	),
	START_SHOPPING_LIST(ShoppingListVerticle.Actions.START_LIST,
			isContextFree()
					.and(messageContains("comenzar"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention("Shopping"); return null; },
			msg -> { return reply( null, TranslationUtils.forwarding(msg));},
			"Ex. comenzar lista"
	),
	REMOVE_ITEM_FROM_SHOPPING_LIST(ShoppingListVerticle.Actions.REMOVE_FROM_LIST,
			isContextFree()
					.and(messageContains("eliminar||quitar||quita||elimina"))
					.and(messageContains("elemento||producto"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {return new TextMessage(userContext.getUser(), findNumber(tokens));},
			msg -> { return reply( null, TranslationUtils.forwarding(msg));},
			"Ex. eliminar elemento [number] de lista"
	),
	CONTINUE_SHOPPING_LIST(ShoppingListVerticle.Actions.CONTINUE_LIST,
			isContextFree()
					.and(messageContains("continuar"))
					.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention("Shopping"); return null; },
			msg -> { return reply( null, TranslationUtils.forwarding(msg));},
			"Ex. continuar lista"
	),
	END_SHOPPING_LIST(ShoppingListVerticle.Actions.END_LIST,
			isContext("Shopping")
				.and(messageContains("finalizar"))
				.and(messageContains("lista")),
			(tokens, userContext) -> {userContext.setAttention(""); return null; },
			msg -> { return reply( null, TranslationUtils.forwarding(msg));},
			"Ex. finalizar lista"
	),
	SHOW_SHOPPING(ShoppingListVerticle.Actions.GET_LIST,
			messageContains("muestra", "mostrar")
			.and(messageContains("lista")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. Muestra lista"
	),
	ADD_TO_SHOPPING_LIST(ShoppingListVerticle.Actions.SAVE_ITEM,
			isContext("Shopping"),
			(tokens, userContext) -> {return new TextMessage(userContext.getUser(), concatStrings(tokens));},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. Item"
	),
	PRINT_ACTION(WhiteboardVerticle.Actions.PRINT,
			isContextFree()
				.and(messageStartsWith("imprimir")),
			(tokens, userContext) -> {return new TextMessage("dummy", concatStrings(tokens));},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. imprimir hola"
	),
	LAUNDRY_STATUS(LaundryVerticle.Actions.CHECK_STATUS,
			isContextFree()
				.and(messageContains("laundry||lavadora"))
				.and(messageContains("status||estado")),
			(tokens, userContext) -> {return null;},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. laundry status"
		),
	OUTLET_STATUS(OutletVerticle.Actions.STATUS,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("status||estado")),
			(tokens, userContext) -> {return null;},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet status"
		),
	OUTLET_ON(OutletVerticle.Actions.SWITCHER,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_OFF(OutletVerticle.Actions.SWITCHER,
			isContextFree()
				.and(messageContains("outlet||enchufe"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_SET(OutletVerticle.Actions.SET,
			isContextFree()
				.and(messageContains("outlet"))
				.and(messageContains("field")),
			(tokens, userContext) -> {return new Configure(nextTokenTo("field").apply(tokens), nextTokenTo("value").apply(tokens));},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	
	BOILER_AUTOMATIC_ON(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("on||encender||activar||conectar"))
				.and(messageContains("automático||automatic||automatico")),
			(tokens, userContext) -> {return new AutomaticMode() {{ this.enabled = true; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. automatic boiler on"
			),

	BOILER_AUTOMATIC_OFF(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("off||apagar||desactivar||desconectar"))
				.and(messageContains("automático||automatic||automatico")),
			(tokens, userContext) -> {return new AutomaticMode() {{ this.enabled = false; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. automatic boiler off"
			),

	
	BOILER_ON(ThermostatVerticle.Actions.SWITCH_BOILER,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler on"
			),
			
	BOILER_OFF(ThermostatVerticle.Actions.SWITCH_BOILER,
			isContextFree()
				.and(messageContains("boiler||caldera||calefacción||calefaccion"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler off"
			),
	
	LAMP_ON(LampVerticle.Actions.SWITCH_LAMP,
			isContextFree()
				.and(messageContains("lamp||lampara||lámpara"))
				.and(messageContains("on||encender||activar||conectar")),
			(tokens, userContext) -> {return new UpdateState("on");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. lamp on"
			),
			
	LAMP_OFF(LampVerticle.Actions.SWITCH_LAMP,
			isContextFree()
				.and(messageContains("lamp||lampara||lámpara"))
				.and(messageContains("off||apagar||desactivar||desconectar")),
			(tokens, userContext) -> {return new UpdateState("off");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
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
			"Ex. temperature"
			),
	HOME(HomeVerticle.Actions.GET_HOME_STATUS,
			isContextFree()
				.and(messageContains("home||casa")),
			(tokens, userContext) -> {return null;},
			msg -> { return reply(null, HomeUtils.toString(msg));},
			"Ex. home"
			),
	
	RESET_SENSOR(SensorVerticle.Actions.RESET_SENSOR,
			isContextFree()
				.and(messageContains("sensor")),
			(tokens, userContext) -> { return new Sensor(nextTokenTo("sensor").apply(tokens));},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. sensor xxxx"
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
	
	public String getHelpMessage() {
		return this.mHelpMessage;
	}

}
