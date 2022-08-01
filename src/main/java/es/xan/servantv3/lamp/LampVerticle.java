package es.xan.servantv3.lamp;

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

/**
 * Turns on and off the bedroom's lamp 
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

	
	public enum Actions implements Action {
		SWITCH_LAMP(UpdateState.class)
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
	
	public void switch_lamp(UpdateState status, final Message<Object> msg) {
		boolean on = false;
		
		switch (status.getNewStatus().toLowerCase()) {
		case "on":
			on = true;
			break;
		}
			
		switch_status(on, msg);
	}
	
	private void switch_status(boolean on, final Message<Object> msg) {
		try {
			boolean updatedOn = send(on);
				
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

	private boolean send(boolean on) throws UnsupportedEncodingException {
		LOGGER.info("setting lamp to [{}]", on);
		
		String url = mConfiguration.getString("url");
		String token = mConfiguration.getString("token");
		
		if (on) {
			url = url + "/switchOn";
		} else {
			url = url + "/switchOff";
		}
		
		final HttpPost httpPost = new HttpPost(url);
/*		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("access_token", token));
		nvps.add(new BasicNameValuePair("params", "empty"));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));*/

		try (CloseableHttpResponse response = mHttpclient.execute(httpPost)) {
			LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
		    final HttpEntity entity = response.getEntity();
		    
		    String content = EntityUtils.toString(entity);
		    LOGGER.info(content);
		    JsonObject json = new JsonObject(content);
		    
		    return json.getBoolean("connected", Boolean.FALSE);
		} catch (Exception e) {
			LOGGER.warn("Cannot setting boiler to [{}]", on, e);
			return false;
		}
	}
}
