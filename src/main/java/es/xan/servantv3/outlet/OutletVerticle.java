package es.xan.servantv3.outlet;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.SSHUtils;
import es.xan.servantv3.capabilities.AutoConfigurable;
import es.xan.servantv3.outlet.OutletVerticle.Actions.Configure;
import es.xan.servantv3.outlet.OutletVerticle.Actions.Switcher;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@AutoConfigurable
public class OutletVerticle extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutletVerticle.class);
	
	public OutletVerticle() {
		super(Constant.OUTLET_VERTICLE);
		
		supportedActions(Actions.values());
	}

	private String mHost = "192.168.1.108";
	private String mLogin = "ubnt";
	private String mPassword = "ubnt";
	
	private static String ON_COMMAND = "echo \"1\" > /proc/power/relay1";
	private static String OFF_COMMAND = "echo \"0\" > /proc/power/relay1";
	
	public enum Actions  implements Action {
		SWITCHER(Switcher.class),
		SET(Configure.class)
		;
		
		public static class Switcher { public String value; }
		public static class Configure { public String field, value; }

		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}
		
		@Override
		public Class<?> getPayloadClass() {
			return beanClass;
		}
		
	}
	
	public void set(Configure conf, Message<Object> message) {
		ReplyBuilder builderOn = MessageBuilder.createReply();
		switch (conf.field) {
		case "host":
			this.mHost = conf.value;
			builderOn.setOk();
			builderOn.setMessage(conf.value);
			break;
		case "login":
			this.mLogin= conf.value;
			builderOn.setOk();
			builderOn.setMessage(conf.value);
			break;
		case "password":
			this.mPassword= conf.value;
			builderOn.setOk();
			builderOn.setMessage(conf.value);
			break;
		default:
			builderOn.setError();
			builderOn.setMessage("field:" + conf.field + " value" + conf.value);
			break;
		}
		message.reply(builderOn.build());
	}
	
	public void switcher(Switcher switcher, Message<Object> message) {
		boolean ok = false;
		try {
			if ("on".equals(switcher.value)) {
				ok = SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, ON_COMMAND);
			} else {
				ok = SSHUtils.runRemoteCommand(mHost, mLogin, mPassword, OFF_COMMAND);
			}
		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
		
		ReplyBuilder builderOn = MessageBuilder.createReply();
		if (ok) {
			builderOn.setOk();
		} else {
			builderOn.setError();
		}
		message.reply(builderOn.build());
	}
}
