package es.xan.servantv3.thermostat;

import static es.xan.servantv3.Scheduler.at;

import java.io.UnsupportedEncodingException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
import es.xan.servantv3.Scheduler;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.NewStatus;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import es.xan.servantv3.messages.UpdateState;
import es.xan.servantv3.thermostat.ThermostatVerticle.Actions.AutomaticMode;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages the boiler, switching it on or off 
 * @author Xan
 *
 */
public class ThermostatVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThermostatVerticle.class);
	
	public ThermostatVerticle() {
		super(Constant.THERMOSTAT_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private CloseableHttpClient mHttpclient;
	private Scheduler mScheduler;
	
	private Boolean mAutomaticMode;
	private boolean mBoilerOn;
	private JsonObject mConfiguration;
	private UUID mScheduledTask;
	
	public enum Actions implements Action {
		SWITCH_BOILER(UpdateState.class),
		AUTOMATIC_MODE(AutomaticMode.class)
		;
		
		public static class AutomaticMode { public Boolean enabled; }

		private Class<?> mMessageClass;
		
		Actions(Class<?> messageClass) {
			this.mMessageClass = messageClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return mMessageClass;
		}
	}

	public void switch_boiler(UpdateState status, final Message<Object> msg) {
		try {
			switch (status.getNewStatus().toLowerCase()) {
			case "on":
				boolean updatedOn = send("HIGH");
				
				if (updatedOn) {
					mBoilerOn = true;
					
					ReplyBuilder builderOn = MessageBuilder.createReply();
					builderOn.setOk();
					msg.reply(builderOn.build());
					
					this.publishEvent(Events.BOILER_SWITCHED, new NewStatus("on"));
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
					mBoilerOn = false;
					
					ReplyBuilder builderOff = MessageBuilder.createReply();
					builderOff.setOk();
					msg.reply(builderOff.build());
					this.publishEvent(Events.BOILER_SWITCHED, new NewStatus("off"));
				} else {
					ReplyBuilder builderOn = MessageBuilder.createReply();
					builderOn.setError();
					builderOn.setMessage("Please, try it back in 5 minutes");
					msg.reply(builderOn.build());
				}
				
				break;
			}
		} catch (Exception e) {
			LOGGER.warn("cannot process message [{}]", msg.body(), e);
		}
	}
	
	public void automatic_mode(AutomaticMode mode) {
		this.mAutomaticMode = mode.enabled;
		
		scheduleAutomaticActions();
	}
	
	
	@Override
	public void start() {
		super.start();
		this.mConfiguration = Vertx.currentContext().config().getJsonObject("ThermostatVerticle");

		this.mHttpclient = HttpClients.createDefault();
		
		this.mScheduler = new Scheduler(getVertx());

		this.mBoilerOn = Boolean.FALSE;
		this.mAutomaticMode = Boolean.FALSE;
		
		LOGGER.info("started Thermostat");
	}

	private boolean send(String operation) throws UnsupportedEncodingException {
		LOGGER.info("setting thermostat to [{}]", operation);
		
		final String url = mConfiguration.getString("url");
		final String token = mConfiguration.getString("token");
		
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
		    
		    return json.getBoolean("connected", Boolean.FALSE);
		} catch (Exception e) {
			LOGGER.warn("Cannot setting boiler to [{}]", operation, e);
			return false;
		}
	}
	
	private void scheduleAutomaticActions() {
		if (this.mAutomaticMode) {
			this.mScheduledTask = mScheduler.scheduleTask(at(LocalTime.of(23, 0)), (UUID id) -> { if (this.mBoilerOn) switchBoilerOffAndNotify();  return true; });
		} else {
			if (this.mScheduledTask != null) {
				this.mScheduler.removeScheduledTask(this.mScheduledTask);
			}
		}
	}

	private void switchBoilerOffAndNotify() {
		LOGGER.info("Its time to shut the boiler off");
		
		publishAction(Actions.SWITCH_BOILER, new UpdateState("off"),
				msg -> publishAction(HomeVerticle.Actions.NOTIFY_BOSS, new TextMessageToTheBoss("Automatic boiler off")));
		
	}


}
