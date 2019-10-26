package es.xan.servantv3.parrot;

//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.jivesoftware.smack.Chat;
//import org.jivesoftware.smack.ChatManager;
//import org.jivesoftware.smack.ConnectionConfiguration;
//import org.jivesoftware.smack.MessageListener;
//import org.jivesoftware.smack.SASLAuthentication;
//import org.jivesoftware.smack.SmackConfiguration;
//import org.jivesoftware.smack.SmackException.NotConnectedException;
//import org.jivesoftware.smack.XMPPConnection;
//import org.jivesoftware.smack.packet.Message;
//import org.jivesoftware.smack.packet.Presence;
//import org.jivesoftware.smack.tcp.XMPPTCPConnection;
//
//import io.vertx.core.json.JsonObject;
//import io.vertx.core.logging.Logger;
//import io.vertx.core.logging.LoggerFactory;
//
//
//
//
//public class GTalkService implements MessageListener {
//	
//	private static final Logger LOGGER = LoggerFactory.getLogger(GTalkService.class);
//	
//	private JsonObject mConfiguration;
//
//	private Map<String, Chat> mConversations = new HashMap<>();
//
//	private XMPPConnection mConnection;
//	private CommunicationListener mListener;
//	
//	public GTalkService(JsonObject configuration) {
//		this.mConfiguration = configuration;
//	}
//	
//	public boolean isInit() {
//		return mConnection != null;
//	}
//	
//	public void setCommunicationListener(CommunicationListener listener) {
//		this.mListener = listener;
//	}
//
//	public void start() {
//		try {
//			init();
//		} catch (Exception e) {
//			LOGGER.warn(e.getMessage(), e);
//		}
//	}
//
//	private void init() throws Exception {
//		final String host = mConfiguration.getString("host");
//		final int port = mConfiguration.getInteger("port");
//		final String service = mConfiguration.getString("service");
//		
//		mConnection = createConnection(host, port, service);
//		mConnection.connect();
//
//		JsonObject authentication = mConfiguration.getJsonObject("authentication");
//		String login = authentication.getString("login");
//		String password = authentication.getString("password");
//		
//		mConnection.login(login, password);
//
//		// set presence status info
//		Presence presence = new Presence(Presence.Type.available);
//		mConnection.sendPacket(presence);
//	}
//
//	public void createChat(String partner) throws Exception {
//		Chat chat = ChatManager.getInstanceFor(mConnection).createChat(partner, this);
//
//		// google bounces back the default message types, you must use chat
//		this.msg = new Message(partner, Message.Type.chat);
//		msg.setBody("Greetings! Sir");
//		chat.sendMessage(msg);
//	}
//
//	private Message msg;
//
//	@Override
//	public void processMessage(Chat chat, Message message) {
//
//		if (message.getType().equals(Message.Type.chat) && message.getBody() != null) {
//			
//			if (mListener != null) {
//				mListener.onMessage(message.getFrom(), message.getBody());
//			} else {
//				try {
//					Message msg = new Message();
//					msg.setBody("I am a Java bot. You said: " + message.getBody());
//					chat.sendMessage(msg);
//				} catch (NotConnectedException ex) {
//					LOGGER.warn(ex.getMessage(), ex);
//				}
//			}
//		} else {
//			LOGGER.trace("I received a message I don't undestand [{}]", message);
//		}
//	}
//
//	private static XMPPConnection createConnection(String host, int port,
//			String serviceName) throws NoSuchAlgorithmException, KeyManagementException {
//		SmackConfiguration.setDefaultPacketReplyTimeout(19000);
//		ConnectionConfiguration connConfig = new ConnectionConfiguration(host,
//				port, serviceName);
////		SSLContext context = SSLContext.getInstance("SSL");
////		context.init(null, null, new SecureRandom());
////		connConfig.setCustomSSLContext(context);
////		connConfig.setDebuggerEnabled(true);
//		connConfig.setReconnectionAllowed(true);
//		SASLAuthentication.supportSASLMechanism("PLAIN",0);  
//
//		return new XMPPTCPConnection(connConfig);
//	}
//
//	public boolean send(String receptor, String message) {
//		try {
//			Chat chat = mConversations.get(receptor);
//			
//			if (chat == null)  {
//				chat = ChatManager.getInstanceFor(mConnection).createChat(receptor, this);
//				mConversations.put(receptor, chat);
//			}
//			
//			this.msg = new Message(receptor, Message.Type.chat);
//			msg.setBody(message);
//			chat.sendMessage(msg);
//			
//			return true;
//		} catch (NotConnectedException e) {
//			LOGGER.warn(e.getMessage(), e);
//			return false;
//		}
//	}
//}
