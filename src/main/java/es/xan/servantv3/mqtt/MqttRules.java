package es.xan.servantv3.mqtt;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Events;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.NewStatus;
import es.xan.servantv3.messages.Temperature;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import es.xan.servantv3.messages.UpdateState;
import es.xan.servantv3.modes.NightModeVerticle;
import es.xan.servantv3.scrumleader.ScrumLeaderVerticle;
import es.xan.servantv3.temperature.TemperatureVerticle;
import es.xan.servantv3.thermostat.ThermostatVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.messages.MqttPublishMessage;

import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public enum MqttRules {
    TEMPERATURE_OUTSIDE(
            new TopicPredicate("rtl_433/112/temperature_C"),
            (message, vertx) -> {
                Temperature temperature = new Temperature("outside", Float.parseFloat(message.payload().toString()), new Date().getTime());
                vertx.publishAction(TemperatureVerticle.Actions.SAVE, temperature);    
            }   
    ),
    TEMPERATURE_INSIDE(
            new TopicPredicate("rtl_433/17/temperature_C"),
            (message, vertx) -> {
                Temperature temperature = new Temperature("inside", Float.parseFloat(message.payload().toString()), new Date().getTime());
                vertx.publishAction(TemperatureVerticle.Actions.SAVE, temperature);
            }
    ),
    MOVIMIENTO(
            new TopicPredicate("zigbee2mqtt/Movimiento"),
            (message, vertx) -> {
                Boolean contact = message.payload().toJsonObject().getBoolean("occupancy");
                vertx.publishEvent(Events.OCCUPANCY_CHANGED, new NewStatus(contact.toString()));
            }
    ),
    DOOR(
            new TopicPredicate("zigbee2mqtt/Puerta"),
            (message, vertx) -> {
                Boolean contact = message.payload().toJsonObject().getBoolean("contact");
                vertx.publishEvent(Events.DOOR_STATUS_CHANGED, new NewStatus(contact.toString()));
            }

    ),
    WATER(
            new TopicPredicate("zigbee2mqtt/Inundacion"),
            (message, vertx) -> {
                Boolean learking = message.payload().toJsonObject().getBoolean("water_leak");
                vertx.publishEvent(Events.WATER_LEAK_STATUS_CHANGED, new NewStatus(learking.toString()));
            }
    ),
    AWS_COST(
            new TopicPredicate("aws/cost"),
            (message, vertx) -> {
                TextMessageToTheBoss messageToTheBoss = new TextMessageToTheBoss("AWS Cost: " + message.payload().toString());
                vertx.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, messageToTheBoss);
            }
    ),
    MANDO(
            new TopicPredicate("zigbee2mqtt/mando"),
            (message, vertx) -> {
                String action = message.payload().toJsonObject().getString("action");
                if (action.equals("1_short_release")) {
                    vertx.publishAction(NightModeVerticle.Actions.CHANGE_STATUS, new UpdateState("on"));
                } else if (action.equals("1_long_release")) {
                    vertx.publishAction(NightModeVerticle.Actions.CHANGE_STATUS, new UpdateState("off"));
                } else if (action.equals("2_short_release")) {
                    vertx.publishAction(ThermostatVerticle.Actions.SWITCH_BOILER, new UpdateState("on"));
                } else if (action.equals("2_long_release")) {
                    vertx.publishAction(ThermostatVerticle.Actions.SWITCH_BOILER, new UpdateState("off"));
                }
            }
    ),
    BUILDGENTIC_WELCOME(
            new TopicPredicate("servant/buildgentic"),
            (message, vertx) -> {
                String str_action = message.payload().toJsonObject().getString("action");
                if ("welcome".equals(str_action)) {
                    vertx.publishAction(ScrumLeaderVerticle.Actions.WELCOME);
                } else {
                    ScrumLeaderVerticle.Actions action = ScrumLeaderVerticle.Actions.valueOf(str_action.toUpperCase());
                    if (message.payload().toJsonObject().fieldNames().contains("data")) {
                        JsonObject data = message.payload().toJsonObject().getJsonObject("data");
                        vertx.publishActionWithRawBean(action, data);
                    } else {
                        vertx.publishAction(action);
                    }
                }
            }
    )
    ;

    private Predicate<MqttPublishMessage> predicate;
    private BiConsumer<MqttPublishMessage, AbstractServantVerticle> action;

    MqttRules(
            Predicate<MqttPublishMessage> predicate,
            BiConsumer<MqttPublishMessage, AbstractServantVerticle> action) {
        this.predicate = predicate;
        this.action = action;
    }

    public void apply(MqttPublishMessage msg, AbstractServantVerticle vertx) {
        this.action.accept(msg, vertx);
    }

    protected final static MqttRules identifyRule(MqttPublishMessage msg) {
        for (MqttRules rule : MqttRules.values()) {
            if (rule.predicate.test(msg)) {
                return rule;
            }
        }

        return null;
    }
}

class TopicPredicate implements Predicate<MqttPublishMessage> {
    private final String expectedTopic;

    TopicPredicate(String expectedTopic) {
        this.expectedTopic = expectedTopic;
    }
    public boolean test(MqttPublishMessage t) {
        return t.topicName().startsWith(expectedTopic);
    }
}
