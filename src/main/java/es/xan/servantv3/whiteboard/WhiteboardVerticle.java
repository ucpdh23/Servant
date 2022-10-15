package es.xan.servantv3.whiteboard;

import es.xan.servantv3.*;
import es.xan.servantv3.calendar.GCalendarUtils;
import es.xan.servantv3.calendar.Notification;
import es.xan.servantv3.messages.TextMessage;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WhiteboardVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhiteboardVerticle.class);

    private String calendar;
    private File secretFile;

    private Scheduler mScheduler;

    public WhiteboardVerticle() {
        super(Constant.WHITEBOARD_VERTICLE);

        supportedActions(WhiteboardVerticle.Actions.values());
    }

    private CloseableHttpClient mHttpclient;

    private JsonObject mConfiguration;


    public enum Actions implements Action {
        PRINT(TextMessage.class),
        PRINT_IMAGE(TextMessage.class),
        CREATE_DASHBOARD(null)
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

    public void create_dashboard(Message<Object> message) {
        try {
            LOGGER.info("creating dashboard...");
            //List<Notification> nextNotifications = GCalendarUtils.nextNotificationsInWeek(secretFile, calendar);
            final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

            Map<String, Object> data = new HashMap<>();
            RoutingContextImpl routingContext = new RoutingContextImpl(this.vertx, data);

            engine.render(routingContext, "templates/", "dashboard.html", res -> {
                if (res.succeeded()) {
                    Buffer buffer = res.result();

                    File output = new File("dashboard.html");
                    LOGGER.warn("creating file [{}-{}]", output.getAbsolutePath(), output.toURI());
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                        writer.write(buffer.toString());
                    } catch (IOException e) {
                        LOGGER.error("cannot create dashboard", e);
                        throw new RuntimeException(e);
                    }

                    try {
                        SSHUtils.runLocalCommand("xvfb-run --server-args=\"-screen 0, 800x600x24\"  cutycapt --url=file:///opt/servant/dashboard.html --out=/opt/servant/dashboard.bmp");
                    } catch (Exception e) {
                        LOGGER.error("Error", e);
                    }

                } else {
                    LOGGER.error("cannot create dashboard");
                }
            });

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
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

    public void print_image(TextMessage message, final Message<Object> msg) {
        String filename = message.getMessage();

        try {
            boolean updatedOn = printImage(new File(filename));

            if (updatedOn) {
                MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                builderOn.setOk();
                msg.reply(builderOn.build());
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

        // Check Calendar
        /*
        JsonObject config = vertx.getOrCreateContext().config().getJsonObject("CalendarVerticle");
        calendar = config.getString("calendar");
        secretFile = new File(config.getString("secret"));
        */
        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(5), id -> { publishAction(WhiteboardVerticle.Actions.CREATE_DASHBOARD);});

        this.mScheduler = new Scheduler(getVertx());


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

    private boolean printImage(File file) {
        LOGGER.info("printing image [{}]", file);

        String url = mConfiguration.getString("url") + "/img";

        final HttpPost httpPost = new HttpPost(url);
        FileEntity entity = new FileEntity(file);
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = mHttpclient.execute(httpPost)) {
            LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
            final HttpEntity responseBody = response.getEntity();

            String content = EntityUtils.toString(responseBody);
            LOGGER.info(content);

            return true;
        } catch (Exception e) {
            LOGGER.warn("Cannot print img [{}]", file, e);
            return false;
        }
    }
}
