package es.xan.servantv3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SSHUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SSHUtils.class);
	
	public static class RemoteComamndResult {
		public int exitStatus;
		public String output;
	}
	
	private static RemoteComamndResult runRemoteCommandExtended(String host, String login, String password, String command) throws JSchException, IOException {
		JSch jsch = new JSch();
		
		LOGGER.info("host [{}] login [{}] password [{}] command [{}]", host, login, password, command);
		
		Session session = jsch.getSession(login, host, 22);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(password);
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
			      
		List<String> output = new ArrayList<>();
		//Read each line from the buffered reader and add it to result list
		// You can also simple print the result here
		while ((line = reader.readLine()) != null) {
			output.add(line);
		}
		
		//retrieve the exit status of the remote command corresponding to this channel
		int exitStatus = channelExec.getExitStatus();
			 
		//Safely disconnect channel and disconnect session. If not done then it may cause resource leak
		channelExec.disconnect();
		session.disconnect();
		
		RemoteComamndResult result = new RemoteComamndResult();
		result.exitStatus = exitStatus;
		result.output = String.join("\n", output);
		
		return result;
	}
	
	public static boolean runRemoteCommand(String host, String login, String password, String command) throws JSchException, IOException {
		final RemoteComamndResult result = runRemoteCommandExtended(host, login, password, command);
		
		if (result.exitStatus < 0) {
			LOGGER.info("proccess yielded [{},{}]", result.exitStatus, result.output);
			return true;
		} else if(result.exitStatus > 0) {
			LOGGER.warn("proccess yielded [{},{}]", result.exitStatus, result.output);
			return false;
		} else {
			LOGGER.debug("proccess yielded [{},{}]", result.exitStatus, result.output);
			return true;
		}
	}
}
