package es.xan.servantv3.mqtt;


import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.Temperature;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import es.xan.servantv3.temperature.TemperatureVerticle;
import io.vertx.mqtt.MqttServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Mqtt bridge
 *
 * @author Xan
 */
public class MqttVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttVerticle.class);

    public MqttVerticle() {
        super(Constant.MQTT_VERTICLE);

        supportedActions(Actions.values());
    }

    public enum Actions implements Action {
        ;

        private Class<?> mMessageClass;

        Actions(Class<?> messageClass) {
            this.mMessageClass = messageClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return mMessageClass;
        }
    }

    @Override
    public void start() {
        super.start();

        MqttServer mqttServer = MqttServer.create(vertx);
        mqttServer.exceptionHandler(handler -> {
           System.out.println(handler);
        });
        mqttServer.endpointHandler(endpoint -> {

                    // shows main connect info
                    LOGGER.info("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession());

                    if (endpoint.auth() != null) {
                        LOGGER.debug("[username = " + endpoint.auth().getUsername() + ", password = " + endpoint.auth().getPassword() + "]");
                    }
                    LOGGER.debug("[properties = " + endpoint.connectProperties() + "]");
                    if (endpoint.will() != null) {
                        LOGGER.debug("[will topic = " + endpoint.will().getWillTopic() + " msg = " + endpoint.will() +
                                " QoS = " + endpoint.will() + " isRetain = " + endpoint.will() + "]");
                    }

                    LOGGER.debug("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]");

                    // accept connection from the remote client
                    endpoint.accept(false);
                    endpoint.publishHandler(message -> {

                        LOGGER.info("Just received message ["
                                + message.topicName() + "-"
                                + message.payload().toString() + "] with QoS [" + message.qosLevel() + "]");

                        if (message.topicName().startsWith("rtl_433/112/temperature_C")) {
                            Temperature temperature = new Temperature("outside", Float.parseFloat(message.payload().toString()), new Date().getTime());
                            publishAction(TemperatureVerticle.Actions.SAVE, temperature);
                        } else if (message.topicName().startsWith("rtl_433/17/temperature_C")) {
                            Temperature temperature = new Temperature("inside", Float.parseFloat(message.payload().toString()), new Date().getTime());
                            publishAction(TemperatureVerticle.Actions.SAVE, temperature);
                        } else if (message.topicName().startsWith("aws/cost")) {
                            TextMessageToTheBoss messageToTheBoss = new TextMessageToTheBoss("AWS Cost: " + message.payload().toString());
                            publishAction(HomeVerticle.Actions.NOTIFY_BOSS, messageToTheBoss);
                        }
                    }).publishReleaseHandler(messageId -> {

                        endpoint.publishComplete(messageId);
                    });
                })
                .listen(ar -> {

                    if (ar.succeeded()) {

                        LOGGER.info("MQTT server is listening on port " + ar.result().actualPort());
                    } else {

                        LOGGER.warn("Error on starting the server", ar.cause());
                        //ar.cause().printStackTrace();
                    }
                });
    }

}