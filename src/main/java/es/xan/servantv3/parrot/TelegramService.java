package es.xan.servantv3.parrot;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.vertx.core.json.JsonObject;
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

				if (messageTextReceived.getLeft().startsWith("Photos#")) {
					mListener.onFile(entry.getKey(), messageTextReceived.getLeft());
				} else {
					mListener.onMessage(entry.getKey(), messageTextReceived.getLeft());
				}

				if (messageTextReceived.getRight()) {
					send(entry.getKey(), "recibído: " + messageTextReceived.getLeft());
				}
				return;
			}
		}
	}

	private Pair<String, Boolean> resolveMessage(Update update) {
		final String messageTextReceived = update.getMessage().getText();

		String messageTextSent = null;
		if (update.getMessage().getReplyToMessage() != null) {
			messageTextSent = update.getMessage().getReplyToMessage().getText();
		}

		if (update.getMessage().hasPhoto()) {
			String caption = update.getMessage().getCaption();

			List<PhotoSize> photos = update.getMessage().getPhoto();
			String output = "Photos#" + (caption != null? caption : "") + "#";
			String fileId = null;
			int maxFileSize = 0;
			for (PhotoSize photo : photos) {
				if (photo.getFileSize() > maxFileSize) {
					maxFileSize =photo.getFileSize();
					fileId = photo.getFileId();
				}
			}

			File file = ParrotUtils.downloadPhoto("https://api.telegram.org", this.mToken, fileId, this.mModeDebug);
			output += file.getAbsolutePath();

			return Pair.of(output, Boolean.FALSE);
		} else if (update.getMessage().hasVoice()) {
			Voice voice = update.getMessage().getVoice();

			File file = ParrotUtils.downloadAudio("https://api.telegram.org", this.mToken, voice.getFileId(), this.mModeDebug);
			return Pair.of(ParrotUtils.transcribe("https://api.wit.ai", this.mWitToken, file), Boolean.TRUE);
		} else {
			String messageToCommunicate = (messageTextSent != null)?
					"source: " + messageTextSent + "\treceived: " + messageTextReceived
					: messageTextReceived;

			return Pair.of(messageToCommunicate, Boolean.FALSE);
		}
	}

	public boolean sendVideo(String user, String text, File file) {
		String chatId = (String) this.mConversations.get(user);

		SendVideo message = SendVideo.builder()
				.chatId(chatId)
				.supportsStreaming(Boolean.FALSE)
				.caption(text)
				.video(new InputFile(file))
				.build();

		try {
			execute(message);
		} catch (TelegramApiException e) {
			LOGGER.warn(e.getMessage(), e);
			return false;
		}

		return true;
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
