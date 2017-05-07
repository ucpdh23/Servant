package es.xan.servantv3.brain.nlp;


import static es.xan.servantv3.brain.nlp.RuleUtils.messageContains;
import static es.xan.servantv3.brain.nlp.RuleUtils.messageIs;
import static es.xan.servantv3.brain.nlp.RuleUtils.nextTokenTo;
import static es.xan.servantv3.brain.nlp.TranslationUtils.reply;
import io.vertx.core.eventbus.Message;

import java.util.function.Function;
import java.util.function.Predicate;

import es.xan.servantv3.Action;
import es.xan.servantv3.brain.STSVerticle;
import es.xan.servantv3.brain.nlp.TranslationUtils.Reply;
import es.xan.servantv3.homeautomation.HomeUtils;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.sensors.SensorVerticle;
import es.xan.servantv3.sensors.SensorVerticle.Actions.Sensor;
import es.xan.servantv3.temperature.TemperatureUtils;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.AutomaticMode;
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.NewStatus;

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
//	RESPONSE_YES(Constant.QUESTIONS_VERTICLE_REPLY, false, messageIs("yes||si"), send("yes")), 
//	RESPONSE_NO(Constant.QUESTIONS_VERTICLE_REPLY, false, messageIs("no"), send("no")), 
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
			tokens -> {return new NewStatus() {{ this.status = "on"; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler on"
			),
			
	BOILER_OFF(ThermostatVerticle.Actions.SWITCH_BOILER, 
			messageContains("boiler||caldera||calefacción||calefaccion")
				.and(messageContains("off||apagar||desactivar||desconectar")),
			tokens -> {return new NewStatus() {{ this.status = "off"; }};},
			msg -> { return reply(null, TranslationUtils.forwarding(msg));},
			"Ex. boiler off"
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
			tokens -> { return new Sensor() {{ this.sensor = nextTokenTo("sensor").apply(tokens);}};},
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
