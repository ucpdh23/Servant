package es.xan.servantv3.mqtt;



import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.messages.NewStatus;
import es.xan.servantv3.messages.UpdateState;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.*;

/**
 * Turns on and off the bedroom's lamp 
 * @author Xan
 *
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
mqttServer.endpointHandler(endpoint -> {

  // shows main connect info
  LOGGER.info("MQTT client [" + endpoint.clientIdentifier() + "] request to connect, clean session = " + endpoint.isCleanSession());

  if (endpoint.auth() != null) {
    LOGGER.debug("[username = " + endpoint.auth().getUsername() + ", password = " + endpoint.auth().getPassword() + "]");
  }
  LOGGER.debug("[properties = " + endpoint.connectProperties() + "]");
  if (endpoint.will() != null) {
    LOGGER.debug("[will topic = " + endpoint.will().getWillTopic() + " msg = " + new String(endpoint.will().getWillMessageBytes()) +
      " QoS = " + endpoint.will().getWillQos() + " isRetain = " + endpoint.will().isWillRetain() + "]");
  }

  LOGGER.debug("[keep alive timeout = " + endpoint.keepAliveTimeSeconds() + "]");

  // accept connection from the remote client
  endpoint.accept(false);
endpoint.publishHandler(message -> {

  System.out.println("Just received message [" + message.payload().toString() + "] with QoS [" + message.qosLevel() + "]");
/*
  if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
    endpoint.publishAcknowledge(message.messageId());
  } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
    endpoint.publishReceived(message.messageId());
  }*/

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