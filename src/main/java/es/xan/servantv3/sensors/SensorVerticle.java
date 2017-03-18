package es.xan.servantv3.sensors;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.MessageBuilder.ReplyBuilder;
import es.xan.servantv3.sensors.SensorVerticle.Actions.Sensor;

public class SensorVerticle extends AbstractServantVerticle {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SensorVerticle.class);
	
	private String mHost;
	private String mLogin;
	private String mPassword;
	
	private Map<String, String> mSensors;
	
	public SensorVerticle() {
		super(Constant.SENSORS_VERTICLE);
		
		supportedActions(Actions.values());
	}
	
	public enum Actions implements Action {
		RESET_SENSOR(Sensor.class)
		;
		
		public static class Sensor{ public String sensor; };
		
		Class<?> beanClass;
		
		private Actions(Class<?> beanClass) {
			this.beanClass = beanClass;
		}

		@Override
		public Class<?> getPayloadClass() {
			return this.beanClass;
		}
		
	}
	
	public void start() {
		super.start();
		loadConfiguration(vertx.getOrCreateContext().config().getJsonObject("SensorVerticle"));
	}
	
	private void loadConfiguration(JsonObject config) {
		mHost = config.getString("server");
		mLogin = config.getString("usr");
		mPassword = config.getString("pws");
		
		loadSensors(config.getJsonArray("items"));
	}

	private void loadSensors(JsonArray array) {
		mSensors = new HashMap<>();
		
		for (int i=0; i < array.getList().size(); i++) {
			final JsonObject sensor = (JsonObject) array.getList().get(i);
			String name = sensor.getString("name");
			String command = sensor.getString("command");
			
			mSensors.put(name, command);
		}
	}

	public void reset_sensor(Sensor sensor, Message<Object> message) {
		LOGGER.info("Asking to reset sensor [{}]", sensor.sensor);
		final String command = mSensors.get(sensor.sensor);
		
		final ReplyBuilder builder = MessageBuilder.createReply();
		if (command == null) {
			builder.setError();
			
			message.reply(builder.build());
		} else {
			
			boolean result = false;
			try {
				result = runRemoteCommand(command);
			} catch (JSchException | IOException e) {
				LOGGER.warn(e.getMessage(), e);
			}
			
			if (result) builder.setOk();
			else builder.setError();
			message.reply(builder.build());
		}
	}

	private boolean runRemoteCommand(String command) throws JSchException, IOException {
		JSch jsch = new JSch();
		
		Session session = jsch.getSession(mLogin, mHost, 22);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(mPassword);
		session.connect();
			 
		//create the excution channel over the session
		ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
			 
		// Gets an InputStream for this channel. All data arriving in as messages from the remote side can be read from this stream.
		InputStream in = channelExec.getInputStream();
			 
		// Set the command that you want to execute
		// In our case its the remote shell script
		channelExec.setCommand(command);
			 
		// Execute the command
		channelExec.connect();
		
		// Read the output from the input stream we set above
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
			      
		List<String> result = new ArrayList<>();
		//Read each line from the buffered reader and add it to result list
		// You can also simple print the result here
		while ((line = reader.readLine()) != null) {
			result.add(line);
		}
		
		//retrieve the exit status of the remote command corresponding to this channel
		int exitStatus = channelExec.getExitStatus();
			 
		//Safely disconnect channel and disconnect session. If not done then it may cause resource leak
		channelExec.disconnect();
		session.disconnect();
			 
		if (exitStatus < 0) {
			return true;
		} else if(exitStatus > 0) {
			return false;
		} else {
			return true;
		}
	}

}
