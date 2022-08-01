package es.xan.servantv3.parrot;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramService extends TelegramLongPollingBot {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

	private JsonObject mConfiguration;
	private String mToken;
	private String mWitToken;
	private Boolean mModeDebug;
	private CommunicationListener mListener;
	private Map<String, Object> mConversations;

	public TelegramService(JsonObject configuration) {
		this.mConfiguration = configuration.getJsonObject("telegram");
		
		this.mToken = this.mConfiguration.getString("token");
		this.mWitToken = this.mConfiguration.getString("witToken");
		this.mConversations = this.mConfiguration.getJsonObject("conversations").getMap();
		this.mModeDebug = this.mConfiguration.getString("modeDebug") != null? Boolean.parseBoolean(this.mConfiguration.getString("modeDebug")) : false;
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
		Long chatId = update.getMessage().getChatId();
		
		for (Entry<String, Object> entry : this.mConversations.entrySet()) {
			if (entry.getValue().toString().equals(chatId.toString())) {
				Pair<String, Boolean> messageTextReceived = resolveMessage(update);
				mListener.onMessage(entry.getKey(), messageTextReceived.getLeft());

				if (messageTextReceived.getRight()) {
					send(entry.getKey(), "recibído: " + messageTextReceived.getLeft());
				}
				return;
			}
		}
	}

	private Pair<String, Boolean> resolveMessage(Update update) {
		final String messageTextReceived = update.getMessage().getText();

		if (messageTextReceived == null) {
			Voice voice = update.getMessage().getVoice();

			File file = AudioUtils.downloadAudio("https://api.telegram.org", this.mToken, voice.getFileId(), this.mModeDebug);
			return Pair.of(AudioUtils.transcribe("https://api.wit.ai", this.mWitToken, file), Boolean.TRUE);
		} else {
			return Pair.of(messageTextReceived, Boolean.FALSE);
		}
	}
	
	public boolean send(String user, String text) {
		String chatId = (String) this.mConversations.get(user);

		SendMessage message = SendMessage.builder()
								.chatId(chatId)
								.text(text)
								.build();

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
		try {
			final TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
			TelegramService telegramService = new TelegramService(config);
			telegramBotsApi.registerBot(telegramService);

			return telegramService;
		} catch (TelegramApiException e) {
			throw new RuntimeException(e);
		}
	}

}
