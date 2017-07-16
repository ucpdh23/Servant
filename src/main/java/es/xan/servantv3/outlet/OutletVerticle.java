package es.xan.servantv3.outlet;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.SSHUtils;
import es.xan.servantv3.messages.Configure;
import es.xan.servantv3.messages.UpdateState;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OutletVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutletVerticle.class);
	
	public OutletVerticle() {
		super(Constant.OUTLET_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private String mHost;
	private String mLogin;
	private String mPassword;
	
	private static String ON_COMMAND = "echo \"1\" > /proc/power/relay1";
	private static String OFF_COMMAND = "echo \"0\" > /proc/power/relay1";
	
	public enum Actions  implements Action {
		SWITCHER(UpdateState.class),
		SET(Configure.class)
		;
		
		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return beanClass;
		}
		
	}
	
	public void start() {
		super.start();
		loadConfiguration(vertx.getOrCreateContext().config().getJsonObject("OutletVerticle"));
	}
	
	private void loadConfiguration(JsonObject config) {
		mHost = config.getString("host", "192.168.1.100");
		mLogin = config.getString("login", "ubnt");
		mPassword = config.getString("password", "ubnt");
	}
	
	public void set(Configure conf, Message<Object> message) {
		ReplyBuilder builderOn = MessageBuilder.createReply();
		switch (conf.getField()) {
		case "host":
			this.mHost = conf.getValue();
			builderOn.setOk();
			builderOn.setMessage(conf.getValue());
			break;
		case "login":
			this.mLogin= conf.getValue();
			builderOn.setOk();
			builderOn.setMessage(conf.getValue());
			break;
		case "password":
			this.mPassword= conf.getValue();
			builderOn.setOk();
			builderOn.setMessage(conf.getValue());
			break;
		default:
			builderOn.setError();
			builderOn.setMessage("field:" + conf.getField() + " value" + conf.getValue());
			break;
		}
		message.reply(builderOn.build());
	}
	
	public void switcher(UpdateState switcher, Message<Object> message) {
		boolean ok = false;
		try {
			if ("on".equals(switcher.getNewStatus())) {
				ok = SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, ON_COMMAND);
			} else {
				ok = SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, OFF_COMMAND);
			}
		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		} finally {
			ReplyBuilder builderOn = MessageBuilder.createReply();
			if (ok) {
				builderOn.setOk();
			} else {
				builderOn.setError();
			}
			message.reply(builderOn.build());
		}
	}
}
