package es.xan.servantv3.lamp;

import es.xan.servantv3.*;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.messages.MqttMsg;
import es.xan.servantv3.messages.NewStatus;
import es.xan.servantv3.messages.UpdateState;
import es.xan.servantv3.mqtt.MqttVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * Turns on and off lamps.
 * Right now there are two lamps support by this mechanism.
 * Both are connected to the zibgee home network, therefore servant uses the mqtt bridge
 *
 *
 * @author Xan
 *
 */
public class LampVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(LampVerticle.class);
	
	public LampVerticle() {
		super(Constant.LAMP_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private CloseableHttpClient mHttpclient;

	private JsonObject mConfiguration;

	/**
	 * UpdateState.newStatus = on | off
	 */
	public enum Actions implements Action {
		SWITCH_LIVINGROOM_LAMP(UpdateState.class),
		SWITCH_BEDROOM_LAMP(UpdateState.class),
		SWITCH_CHILDRENROOM_OUTLET(UpdateState.class),
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

	public void switch_childrenroom_outlet(UpdateState status, final Message<Object> msg) {
		boolean on = ("on".equals(status.getNewStatus().toLowerCase()))? true : false;

		String topic = this.mConfiguration.getJsonObject("topics").getString("childrenroom");

		switch_status(topic, on, msg);
	}

	public void switch_bedroom_lamp(UpdateState status, final Message<Object> msg) {
		boolean on = ("on".equals(status.getNewStatus().toLowerCase()))? true : false;

		String topic = this.mConfiguration.getJsonObject("topics").getString("bedroom");

		switch_status(topic, on, msg);
	}

	public void switch_livingroom_lamp(UpdateState status, final Message<Object> msg) {
		boolean on = ("on".equals(status.getNewStatus().toLowerCase()))? true : false;

		String topic = this.mConfiguration.getJsonObject("topics").getString("livingroom");

		switch_status(topic, on, msg);
	}
	
	private void switch_status(String topic, boolean on, final Message<Object> msg) {
		try {
			boolean updatedOn = send(topic, on);
				
			if (updatedOn) {
				ReplyBuilder builderOn = MessageBuilder.createReply();
				builderOn.setOk();
				msg.reply(builderOn.build());
					
				this.publishEvent(Events.LAMP_SWITCHED, new NewStatus(on?"on":"off"));
			} else {
				ReplyBuilder builderOn = MessageBuilder.createReply();
				builderOn.setError();
				builderOn.setMessage("Please, try it back in 5 minutes");
				msg.reply(builderOn.build());
			}
		} catch (Exception e) {
			LOGGER.warn("cannot process message [{}]", msg.body(), e);
		}
	}
	
	@Override
	public void start() {
		super.start();
		this.mConfiguration = Vertx.currentContext().config().getJsonObject("LampVerticle");

		this.mHttpclient = HttpClients.createDefault();
		
		LOGGER.info("started Thermostat");
	}

	private boolean send(String topic, boolean on) throws UnsupportedEncodingException {
		JsonObject object = new JsonObject()
			.put("state", on? "ON" : "OFF");

		publishAction(MqttVerticle.Actions.PUBLISH_MSG, new MqttMsg(topic + "/set", object));

		return true;
	}
}
