package es.xan.servantv3.brain.nlp;


import static es.xan.servantv3.brain.nlp.RuleUtils.messageContains;
import static es.xan.servantv3.brain.nlp.RuleUtils.messageIs;
import static es.xan.servantv3.brain.nlp.RuleUtils.nextTokenTo;
import static es.xan.servantv3.brain.nlp.RuleUtils.contains;
import static es.xan.servantv3.brain.nlp.TranslationUtils.reply;

import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import es.xan.servantv3.Action;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.brain.nlp.TranslationUtils.Reply;
import es.xan.servantv3.homeautomation.HomeUtils;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.lamp.LampVerticle;
import es.xan.servantv3.laundry.LaundryVerticle;
import es.xan.servantv3.messages.Configure;
import es.xan.servantv3.messages.Sensor;
import es.xan.servantv3.messages.UpdateState;
import es.xan.servantv3.outlet.OutletVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.temperature.TemperatureUtils;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.AutomaticMode;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

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
			messageIs("help||ayuda"),
			tokens -> {return null;},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Information about all the available commands"
			),
	LAUNDRY_STATUS(LaundryVerticle.Actions.CHECK_STATUS,
			messageContains("laundry||lavadora")
			.and(messageContains("status||estado")),
		tokens -> {return null;},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. laundry status"
		),
	OUTLET_STATUS(OutletVerticle.Actions.STATUS,
			messageContains("outlet||enchufe")
			.and(messageContains("status||estado")),
		tokens -> {return null;},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet status"
		),
	OUTLET_ON(OutletVerticle.Actions.SWITCHER,
			messageContains("outlet||enchufe")
			.and(messageContains("on||encender||activar||conectar")),
		tokens -> {return new UpdateState("on");},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_OFF(OutletVerticle.Actions.SWITCHER,
			messageContains("outlet||enchufe")
			.and(messageContains("off||apagar||desactivar||desconectar")),
		tokens -> {return new UpdateState("off");},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	OUTLET_SET(OutletVerticle.Actions.SET,
			messageContains("outlet")
			.and(messageContains("field")),
		tokens -> {return new Configure(nextTokenTo("field").apply(tokens), nextTokenTo("value").apply(tokens));},
		msg -> { return reply(null, TranslationUtils.forwarding(msg));},
		"Ex. outlet on"
		),
	
	BOILER_AUTOMATIC_ON(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			messageContains("boiler||caldera||calefacción||calefaccion")
				.and(messageContains("on||encender||activar||conectar"))
				.and(messageContains("automático||automatic||automatico")),
			tokens -> {return new AutomaticMode() {{ this.enabled = true; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. automatic boiler on"
			),

	BOILER_AUTOMATIC_OFF(ThermostatVerticle.Actions.AUTOMATIC_MODE,
			messageContains("boiler||caldera||calefacción||calefaccion")
				.and(messageContains("off||apagar||desactivar||desconectar"))
				.and(messageContains("automático||automatic||automatico")),
			tokens -> {return new AutomaticMode() {{ this.enabled = false; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. automatic boiler off"
			),

	
	BOILER_ON(ThermostatVerticle.Actions.SWITCH_BOILER,
			messageContains("boiler||caldera||calefacción||calefaccion")
				.and(messageContains("on||encender||activar||conectar")),
			tokens -> {return new UpdateState("on");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler on"
			),
			
	BOILER_OFF(ThermostatVerticle.Actions.SWITCH_BOILER, 
			messageContains("boiler||caldera||calefacción||calefaccion")
				.and(messageContains("off||apagar||desactivar||desconectar")),
			tokens -> {return new UpdateState("off");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler off"
			),
	
	LAMP_ON(LampVerticle.Actions.SWITCH_LAMP,
			messageContains("lamp||lampara||lámpara")
				.and(messageContains("on||encender||activar||conectar")),
			tokens -> {return new UpdateState("on");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. lamp on"
			),
			
	LAMP_OFF(LampVerticle.Actions.SWITCH_LAMP, 
			messageContains("lamp||lampara||lámpara")
				.and(messageContains("off||apagar||desactivar||desconectar")),
			tokens -> {return new UpdateState("off");},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. lamp off"
			),

	
	TEMPERATURE_QUERY(TemperatureVerticle.Actions.QUERY,
			messageContains("temperatura||temperature").
				and(messageContains("minimun||mínima||minima")),
			tokens -> {
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
			messageContains("temperatura||temperature"),
			tokens -> {return null;},
			msg -> { return reply(null, TemperatureUtils.toString(msg));},
			"Ex. temperature"
			),
	HOME(HomeVerticle.Actions.GET_HOME_STATUS,
			messageContains("home||casa"),
			tokens -> {return null;},
			msg -> { return reply(null, HomeUtils.toString(msg));},
			"Ex. home"
			),
	
	RESET_SENSOR(SensorVerticle.Actions.RESET_SENSOR,
			messageContains("sensor"),
			tokens -> { return new Sensor(nextTokenTo("sensor").apply(tokens));},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. sensor xxxx"
			),
//	REMINDER(Constant.PARRONT_VERTICLE, false, messageContains("reminder"), TranslationType.COPY),
//	BOILER_STATUS(Constant.OPERATION_BOILER_STATE_CHECKER, true, messageContains("checkBoilerStatus"), TranslationType.OPERATION),
//	BOILER_AUTOMATIC_MODE(Constant.THERMOSTAT_VERTICLE, true, messageContains("boiler||caldera")
//																.and(messageContains("automatic||automatico||automático")), TranslationType.ON_OFF),
	;
	
	final Predicate<String> mPredicate;
	final Function<String[], Object> mFunction;
	final Action mAddress;
	final Function<Message<Object>, Reply> mResponse;
	final String mHelpMessage;
	
	private Rules(Action address, Predicate<String> predicate, Function<String[],Object> variantFunction, Function<Message<Object>, Reply> response, String helpMessage) {
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
