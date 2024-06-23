package es.xan.servantv3.openia;

import com.google.gson.JsonArray;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.messages.VideoMessage;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OpenIAVerticle  extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenIAVerticle.class);
    private JsonObject mConfiguration;
    private String mAPIKey;
    private CloseableHttpClient mHttpClient;

    public OpenIAVerticle() {
        super(Constant.OPENIA_VERTICLE);

        supportedActions(OpenIAVerticle.Actions.values());
    }

    public enum Actions implements Action {
        EXTRACT_INFORMATION(VideoMessage.class)
        ;

        private Class<?> mMessageClass;

        Actions(Class<?> messageClass) {
            this.mMessageClass = messageClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return mMessageClass;
        }
    }

    @Override
    public void start() {
        super.start();
        LOGGER.info("starting OpenIA Verticle");

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("OpenIAVerticle");
        LOGGER.info("...1... [{}]", this.mConfiguration);
        this.mAPIKey = this.mConfiguration.getString("API_KEY");
        LOGGER.info("...2... [{}]", this.mAPIKey);

        this.mHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build())
                .build();

        LOGGER.info("started OpenIA Verticle");
    }

    public void extract_information(VideoMessage message, Message<Object> msg) throws IOException {
        HttpPost request = new HttpPost("https://api.openai.com/v1/chat/completions");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Bearer " + this.mAPIKey);

        com.google.gson.JsonObject jsonMsgCntTypeText = new com.google.gson.JsonObject();
        jsonMsgCntTypeText.addProperty("type", "text");
        jsonMsgCntTypeText.addProperty("text", message.getMessage());

        com.google.gson.JsonObject _jsonMsgCntTypeImageUrl = new com.google.gson.JsonObject();
        _jsonMsgCntTypeImageUrl.addProperty("url", "data:image/jpeg;base64," + buildImageBase64(message.getFilepath()));

        com.google.gson.JsonObject jsonMsgCntTypeImageUrl = new com.google.gson.JsonObject();
        jsonMsgCntTypeImageUrl.addProperty("type", "image_url");
        jsonMsgCntTypeImageUrl.add("image_url", _jsonMsgCntTypeImageUrl);

        JsonArray jsonMsgCnt = new JsonArray();
        jsonMsgCnt.add(jsonMsgCntTypeText);
        jsonMsgCnt.add(jsonMsgCntTypeImageUrl);

        com.google.gson.JsonObject jsonMessage = new com.google.gson.JsonObject();
        jsonMessage.addProperty("role", "user");
        jsonMessage.add("content", jsonMsgCnt);

        JsonArray jsonMessages = new JsonArray();
        jsonMessages.add(jsonMessage);

        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("model", "gpt-4o");
        json.add("messages", jsonMessages);
        json.addProperty("max_tokens", 250);

        request.setEntity(new StringEntity(json.toString()));

        try (CloseableHttpResponse response = this.mHttpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            LOGGER.info("Response status code [{}]", statusCode);

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String jsonString = EntityUtils.toString(entity);
                    LOGGER.debug("jsonString [{}]", jsonString);

                    MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                    builderOn.setOk();
                    builderOn.setResult(new JsonObject(jsonString));
                    msg.reply(builderOn.build());
                }
            }
        }


    }

    private static String buildImageBase64(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
