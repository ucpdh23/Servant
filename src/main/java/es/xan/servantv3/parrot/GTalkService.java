package es.xan.servantv3.parrot;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;




public class GTalkService implements MessageListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GTalkService.class);
	
	private JsonObject configuration;

	private Map<String, Chat> conversations = new HashMap<>();

	private XMPPConnection connection;
	private CommunicationListener listener;
	
	public GTalkService(JsonObject configuration) {
		this.configuration = configuration;
	}
	
	public boolean isInit() {
		return connection != null;
	}
	
	public void setCommunicationListener(CommunicationListener listener) {
		this.listener = listener;
	}

	public void start() {
		try {
			init();
		} catch (Exception e) {
			LOGGER.warn(e.getMessage(), e);
		}
	}

	private void init() throws Exception {
		final String host = configuration.getString("host");
		final int port = configuration.getInteger("port");
		final String service = configuration.getString("service");
		
		connection = createConnection(host, port, service);
		connection.connect();

		JsonObject authentication = configuration.getJsonObject("authentication");
		String login = authentication.getString("login");
		String password = authentication.getString("password");
		
		connection.login(login, password);

		// set presence status info
		Presence presence = new Presence(Presence.Type.available);
		connection.sendPacket(presence);
	}

	public void createChat(String partner) throws Exception {
		Chat chat = ChatManager.getInstanceFor(connection).createChat(partner, this);

		// google bounces back the default message types, you must use chat
		this.msg = new Message(partner, Message.Type.chat);
		msg.setBody("Greetings! Sir");
		chat.sendMessage(msg);
	}

	private Message msg;

	@Override
	public void processMessage(Chat chat, Message message) {

		if (message.getType().equals(Message.Type.chat) && message.getBody() != null) {
			
			if (listener != null) {
				listener.onMessage(message.getFrom(), message.getBody());
			} else {
				try {
					Message msg = new Message();
					msg.setBody("I am a Java bot. You said: " + message.getBody());
					chat.sendMessage(msg);
				} catch (NotConnectedException ex) {
					LOGGER.warn(ex.getMessage(), ex);
				}
			}
		} else {
			LOGGER.warn("I received a message I don't undestand [{}]", message);
		}
	}

	private static XMPPConnection createConnection(String host, int port,
			String serviceName) throws NoSuchAlgorithmException, KeyManagementException {
		SmackConfiguration.setDefaultPacketReplyTimeout(19000);
		ConnectionConfiguration connConfig = new ConnectionConfiguration(host,
				port, serviceName);
//		SSLContext context = SSLContext.getInstance("SSL");
//		context.init(null, null, new SecureRandom());
//		connConfig.setCustomSSLContext(context);
//		connConfig.setDebuggerEnabled(true);
		connConfig.setReconnectionAllowed(true);
		SASLAuthentication.supportSASLMechanism("PLAIN",0);  

		return new XMPPTCPConnection(connConfig);
	}

	public boolean send(String receptor, String message) {
		try {
			Chat chat = conversations.get(receptor);
			
			if (chat == null)  {
				chat = ChatManager.getInstanceFor(connection).createChat(receptor, this);
				conversations.put(receptor, chat);
			}
			
			this.msg = new Message(receptor, Message.Type.chat);
			msg.setBody(message);
			chat.sendMessage(msg);
			
			return true;
		} catch (NotConnectedException e) {
			LOGGER.warn(e.getMessage(), e);
			return false;
		}
	}
}
