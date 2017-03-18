package es.xan.servantv3.thermostat;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.NewStatus;

public class ThermostatVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThermostatVerticle.class);
	
	public ThermostatVerticle() {
		super(Constant.THERMOSTAT_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private CloseableHttpClient mHttpclient;
	
	public enum Actions implements Action {
		SWITCH_BOILER(NewStatus.class),
		;
		
		public static class NewStatus { public String status; }

		private Class<?> mMessageClass;
		
		Actions(Class<?> messageClass) {
			this.mMessageClass = messageClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return mMessageClass;
		}
	}

	public void switch_boiler(NewStatus status, final Message<Object> msg) {
		try {
			switch (status.status.toLowerCase()) {
			case "on":
				boolean updatedOn = send("HIGH");
				
				if (updatedOn) {
					boilerOn = true;
					
					ReplyBuilder builderOn = MessageBuilder.createReply();
					builderOn.setOk();
					msg.reply(builderOn.build());
					
					this.publishEvent(Events.BOILER_SWITCHED, new es.xan.servantv3.Events.NewStatus() {{
						this.status = "on";
					}});
				} else {
					ReplyBuilder builderOn = MessageBuilder.createReply();
					builderOn.setError();
					builderOn.setMessage("Please, try it back in 5 minutes");
					msg.reply(builderOn.build());
				}
				break;
			case "off":
				boolean updatedOff = send("LOW");
				
				if (updatedOff) {
					boilerOn = false;
					
					ReplyBuilder builderOff = MessageBuilder.createReply();
					builderOff.setOk();
					msg.reply(builderOff.build());
					this.publishEvent(Events.BOILER_SWITCHED, new es.xan.servantv3.Events.NewStatus() {{
						this.status = "off";
					}});
				} else {
					ReplyBuilder builderOn = MessageBuilder.createReply();
					builderOn.setError();
					builderOn.setMessage("Please, try it back in 5 minutes");
					msg.reply(builderOn.build());
					
//					publishAction(Actions.SWITCH_BOILER, status, reply -> reply.);
				}
				
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private boolean boilerOn;

	private JsonObject configuration;
	
	@Override
	public void start() {
		super.start();
		configuration = Vertx.currentContext().config().getJsonObject("ThermostatVerticle");

		mHttpclient = HttpClients.createDefault();

		boilerOn = false;
		
		LOGGER.info("started Thermostat");
	}

	private boolean send(String operation) throws ClientProtocolException, IOException {
		LOGGER.info("setting thermostat to [{}]", operation);
		
		final String url = configuration.getString("url");
		final String token = configuration.getString("token");
		
		final HttpPost httpPost = new HttpPost(url);
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair("access_token", token));
		nvps.add(new BasicNameValuePair("params", "r4," + operation));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps));

		try (CloseableHttpResponse response = mHttpclient.execute(httpPost)) {
			LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
		    final HttpEntity entity = response.getEntity();
		    
		    String content = EntityUtils.toString(entity);
		    LOGGER.info(content);
		    JsonObject json = new JsonObject(content);
		    
		    return json.getBoolean("connected");
		}
	}

}
