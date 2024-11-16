package es.xan.servantv3.mqtt;


import com.google.gson.JsonParser;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.MqttMsg;
import es.xan.servantv3.messages.NewStatus;
import es.xan.servantv3.messages.Temperature;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import es.xan.servantv3.temperature.TemperatureVerticle;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.*;
import io.vertx.mqtt.messages.codes.MqttSubAckReasonCode;
import jakarta.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Mqtt bridge
 *
 * @author Xan
 */
public class MqttVerticle extends AbstractServantVerticle {

    private MqttClient client = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttVerticle.class);

    public MqttVerticle() {
        super(Constant.MQTT_VERTICLE);

        supportedActions(Actions.values());
    }

    public enum Actions implements Action {
        PUBLISH_MSG(MqttMsg.class)
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

    public void publish_msg(MqttMsg msg) {
        for (Map.Entry<String, MqttEndpoint> entry : this.endpoints.entrySet()) {
            if (entry.getValue().isConnected()) {
                try {
                    entry.getValue().publish(msg.getTopic(), msg.getPayload().toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
                } catch (IllegalStateException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            } else {
                LOGGER.warn("[{}] is not connected", entry.getKey());
            }
        }
    }

    Map<String, MqttEndpoint> endpoints = new HashMap<>();

    @Override
    public void start() {
        super.start();

        var config = new MqttServerOptions();
        config.setTcpKeepAlive(true);
        config.setMaxMessageSize(500000);
        config.setReceiveBufferSize(500000);

        MqttServer mqttServer = MqttServer.create(vertx, config);
        mqttServer.exceptionHandler(handler -> {
            LOGGER.warn("handling exception", handler);
        });
        mqttServer.endpointHandler(endpoint -> {
                    LOGGER.info("registering endpoint [{}]", endpoint.clientIdentifier());

                    endpoints.put(endpoint.clientIdentifier(), endpoint);

                    // shows main connect info
                    LOGGER.info("MQTT client [{}] request to connect, clean session = [{}]",
                            endpoint.clientIdentifier(), endpoint.isCleanSession());

                    if (endpoint.auth() != null) {
                        LOGGER.debug("[username = {}, password = {}",
                                endpoint.auth().getUsername(), endpoint.auth().getPassword());
                    }
                    LOGGER.debug("[properties = {}]", endpoint.connectProperties().listAll());
                    if (endpoint.will() != null) {
                        LOGGER.debug("[will topic = {} msg = {} QoS = {}]",
                                endpoint.will().getWillTopic(), endpoint.will(), endpoint.will() );
                    }

                    LOGGER.debug("[keep alive timeout = {}", endpoint.keepAliveTimeSeconds());

                    // accept connection from the remote client
                    endpoint.accept(false);
                    endpoint.subscribeHandler(subscribe -> {

                        List<MqttSubAckReasonCode> reasonCodes = new ArrayList<>();
                        for (MqttTopicSubscription s: subscribe.topicSubscriptions()) {
                            reasonCodes.add(MqttSubAckReasonCode.qosGranted(s.qualityOfService()));
                        }
                        // ack the subscriptions request
                        endpoint.subscribeAcknowledge(subscribe.messageId(), reasonCodes, MqttProperties.NO_PROPERTIES);

                    });
                    endpoint.publishHandler(message -> {
                        if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                            endpoint.publishAcknowledge(message.messageId());
                        } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
                            endpoint.publishReceived(message.messageId());
                        }

                        MqttRules rule = MqttRules.identifyRule(message);
                        if (MqttRules.MANDO.equals(rule)) {
                            LOGGER.info("action: [{}]", message.payload().toJsonObject().getString("action"));
                            LOGGER.info("payload: [{}]", message.payload().toString());
                        }

                        if (MqttRules.BUILDGENTIC_WELCOME.equals(rule)) {
                            LOGGER.info("buildgentic: [{}]", message.payload().toJsonObject().getString("action"));
                            LOGGER.info("buildgentic [{}]", message.payload().toString());
                        }

                        if (rule != null)  {
                            LOGGER.info("Found rule [{}]", rule);
                            rule.apply(message, this);
                        } else {
                            LOGGER.info("Not found rule for message.topic [{}]", message.topicName());
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
                    }
                });

        client = MqttClient.create(vertx);
        client.connect(1883, "localhost").onComplete(s -> {
            LOGGER.info("Mqtt client connected");
        });
        client.publishCompletionHandler(it -> {
            LOGGER.info("Mqtt client message published [{}]", it);
        });
    }

}
