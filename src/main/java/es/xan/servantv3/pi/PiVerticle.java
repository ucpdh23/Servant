package es.xan.servantv3.pi;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.homeautomation.HomeVerticle;
import es.xan.servantv3.messages.MqttMsg;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import es.xan.servantv3.mqtt.MqttVerticle;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.road.RoadUtils;
import es.xan.servantv3.road.RoadVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PiVerticle extends AbstractServantVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(PiVerticle.class);

    public PiVerticle() {
        super(Constant.PI_VERTICLE);

        supportedActions(PiVerticle.Actions.values());
    }

    @Override
    public void start() {
        LOGGER.debug("starting pi...");
        super.start();

        LOGGER.info("started pi");
    }

    private void piOutput(Message<Object> event) {
        String rawBody = (String) event.body();
        LOGGER.debug("text from pi [{}]", rawBody);
        JsonObject json = new JsonObject(rawBody);

        if (json.containsKey("result")) {
            String finalTexto = json.getJsonObject("result").getString("text");
            publishAction(HomeVerticle.Actions.NOTIFY_BOSS, new TextMessageToTheBoss(finalTexto));
        } else if ("log".equals(json.getString("method"))) {
            String logInfo = json.getJsonObject("params").getString("text");
            LOGGER.info("PI-LOG [{}]", logInfo);
        } else if (json.containsKey("error")) {
            String errorMsg = json.getJsonObject("error").getString("message");
            publishAction(HomeVerticle.Actions.NOTIFY_BOSS, new TextMessageToTheBoss(errorMsg));
        }
    }


    public enum Actions implements Action {
        START_PI(null),
        END_PI(null),
        PROCESS_USER_MESSAGE(TextMessage.class),
        PROCESS_PI_MESSAGE(TextMessage.class)

        ;

        private Class<?> mBeanClass;

        private Actions (Class<?> beanClass) {
            this.mBeanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return this.mBeanClass;
        }
    }

    public void start_pi(Message<Object> msg) {
        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        builder.setOk();
        builder.setMessage("Pi reading");

        msg.reply(builder.build());
    }

    public void end_pi(Message<Object> msg) {
        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        builder.setOk();
        builder.setMessage("Pi finalized");

        msg.reply(builder.build());
    }

    public void process_pi_message(TextMessage text, Message<Object> msg) {
        LOGGER.info("process_pi_message []", text);
        publishAction(HomeVerticle.Actions.NOTIFY_BOSS, new TextMessageToTheBoss(text.getMessage()));
    }

    public void process_user_message(TextMessage text, Message<Object> msg) {
        LOGGER.info("process_user_message []", text);

        JsonObject object = new JsonObject();
        object.put("message", text.getMessage());

        publishAction(MqttVerticle.Actions.PUBLISH_MSG, new MqttMsg("servant/pi/in", object));

        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        builder.setOk();
        builder.setMessage("Ok");

        msg.reply(builder.build());

    }

}
