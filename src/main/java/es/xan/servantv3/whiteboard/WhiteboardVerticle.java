package es.xan.servantv3.whiteboard;

import es.xan.servantv3.*;
import es.xan.servantv3.messages.TextMessage;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;

public class WhiteboardVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhiteboardVerticle.class);

    public WhiteboardVerticle() {
        super(Constant.WHITEBOARD_VERTICLE);

        supportedActions(WhiteboardVerticle.Actions.values());
    }

    private CloseableHttpClient mHttpclient;

    private JsonObject mConfiguration;


    public enum Actions implements Action {
        PRINT(TextMessage.class)
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

    public void print(TextMessage message, final Message<Object> msg) {
        String text = message.getMessage();

        try {
            boolean updatedOn = send(text);

            if (updatedOn) {
                MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                builderOn.setOk();
                msg.reply(builderOn.build());

//                this.publishEvent(Events.LAMP_SWITCHED, new NewStatus(on?"on":"off"));
            } else {
                MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                builderOn.setError();
                builderOn.setMessage("Please, try it back in 5 minutes");
                msg.reply(builderOn.build());
            }
        } catch (Exception e) {
            LOGGER.warn("cannot process message [{}]", msg.body(), e);
        }
    }


    @Override
    public void start() {
        super.start();
        this.mConfiguration = Vertx.currentContext().config().getJsonObject("WhiteboardVerticle");

        this.mHttpclient = HttpClients.createDefault();

        LOGGER.info("started whiteboard");
    }

    private boolean send(String message) throws UnsupportedEncodingException {
        LOGGER.info("printing message [{}]", message);

        String url = mConfiguration.getString("url") + "/test";

        final HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity("{\"text\":\"" + message + "\"}");
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = mHttpclient.execute(httpPost)) {
            LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
            final HttpEntity responseBody = response.getEntity();

            String content = EntityUtils.toString(responseBody);
            LOGGER.info(content);

            return true;
        } catch (Exception e) {
            LOGGER.warn("Cannot print text [{}]", message, e);
            return false;
        }
    }
}
