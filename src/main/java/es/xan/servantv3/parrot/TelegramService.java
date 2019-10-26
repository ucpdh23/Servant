package es.xan.servantv3.parrot;

import java.util.Map;
import java.util.Map.Entry;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TelegramService extends TelegramLongPollingBot {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

	private JsonObject mConfiguration;
	private String mToken;
	private CommunicationListener mListener;
	private Map<String, Object> mConversations;

	public TelegramService(JsonObject configuration) {
		this.mConfiguration = configuration.getJsonObject("telegram");
		
		this.mToken = this.mConfiguration.getString("token");
		this.mConversations = this.mConfiguration.getJsonObject("conversations").getMap();
	}

	@Override
	public String getBotUsername() {
		return "Servant";
	}
	
	public void setCommunicationListener(CommunicationListener listener) {
		this.mListener = listener;
	}

	@Override
	public void onUpdateReceived(Update update) {
		final String messageTextReceived = update.getMessage().getText();

		Long chatId = update.getMessage().getChatId();
		
		for (Entry<String, Object> entry : this.mConversations.entrySet()) {
			if (entry.getValue().toString().equals(chatId.toString())) {
				mListener.onMessage(entry.getKey(), messageTextReceived);
			}
		}
	}
	
	public boolean send(String user, String text) {
		String chatId = (String) this.mConversations.get(user);
		
		SendMessage message = new SendMessage().setChatId(chatId).setText(text);
		
		try {
			execute(message);
		} catch (TelegramApiException e) {
			LOGGER.warn(e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public String getBotToken() {
		return this.mToken;
	}

	public static TelegramService build(JsonObject config) {
		ApiContextInitializer.init();
		
		final TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
		
		TelegramService telegramService = new TelegramService(config);
		
		try {
			telegramBotsApi.registerBot(telegramService);
		} catch (TelegramApiException e) {
			throw new RuntimeException(e);
		}
		
		return telegramService;
	}

}
