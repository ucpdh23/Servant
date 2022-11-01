package es.xan.servantv3.whiteboard;

import es.xan.servantv3.*;
import es.xan.servantv3.calendar.GCalendarUtils;
import es.xan.servantv3.calendar.Notification;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.shoppinglist.ShoppingListUtils;
import es.xan.servantv3.weather.WeatherUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.DAYS;

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
            List<Notification> nextNotifications = GCalendarUtils.nextNotificationsInWeek(secretFile, calendar);
            List<WeatherUtils.HourlyInfo> hourlyInfo = WeatherUtils.resolveHourlyInfo();
            final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();
            engine.getThymeleafTemplateEngine().addDialect(new Java8TimeDialect());

            Map<String, Object> data = new HashMap<>();
            data.put("notifications", nextNotifications);
            data.putAll(separateNotifications(nextNotifications));
            data.put("shoppingList", ShoppingListUtils.list());
            data.put("now", new Date().toString());
            data.put("hourlyInfo", hourlyInfo);

            RoutingContextImpl routingContext = new RoutingContextImpl(this.vertx, data);

            engine.render(routingContext, "templates/", "dashboard.html", res -> {
                if (res.succeeded()) {
                    Buffer buffer = res.result();

                    File output = new File("dashboard.html");
                    LOGGER.info("creating file [{}-{}]", output.getAbsolutePath(), output.toURI());
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
                        writer.write(buffer.toString());
                    } catch (IOException e) {
                        LOGGER.error("cannot create dashboard", e);
                        throw new RuntimeException(e);
                    }

                    try {
                        SSHUtils.runLocalCommand("xvfb-run -e /opt/servant/error cutycapt --url=file:///opt/servant/dashboard.html --out=/opt/servant/dashboard.bmp --insecure");

                        String pre_md5 = resolveMD5("/opt/servant/dashboard.bin");
                        File binFile = createBinFile(new File("/opt/servant/dashboard.bmp"));
                        String post_md5 = resolveMD5("/opt/servant/dashboard.bin");

                        if (!pre_md5.equals(post_md5)) {
                            printImage(binFile);
                        }


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

    public static void main(String args[]) {
        LocalDate todayDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        now = now.plus(7, ChronoUnit.HOURS);


        System.out.println(DAYS.between(todayDate, now));

    }

    private Map<String, List<Notification>> separateNotifications(List<Notification> nextNotifications) {
        List<Notification> today = new ArrayList<>();
        List<Notification> tomorrow = new ArrayList<>();
        List<Notification> restOfWeek = new ArrayList<>();

        LocalDate todayDate = LocalDate.now();
        for (Notification notif : nextNotifications) {
            LocalDateTime date = notif.date;

            long daysBetween = DAYS.between(todayDate, date);
            if (daysBetween <= 0) {
                today.add(notif);
            } else if (daysBetween == 1) {
                tomorrow.add(notif);
            } else {
                restOfWeek.add(notif);
            }
        }

        Map<String, List<Notification>> output = new HashMap<>();
        output.put("notificationsToday", today);
        output.put("notificationsTomorrow", tomorrow);
        output.put("notificationsRestOfWeek", restOfWeek);

        return output;
    }

    private String resolveMD5(String filePath) {
        try (FileInputStream fis = new FileInputStream(new File(filePath));) {
            byte data[] = org.apache.commons.codec.digest.DigestUtils.md5(fis);
            char md5Chars[] = Hex.encodeHex(data);
            return String.valueOf(md5Chars);
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return "";
    }

    private File createBinFile(File input) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(input);
        } catch (IOException e) {

        }
        int height = img.getHeight();
        int width = img.getWidth();

        int rgb;
        int red;
        int green;
        int blue;

        boolean components[] = {false, false, false, false, false, false, false, false};
        int position = 0;

        File output = new File("/opt/servant/dashboard.bin");
        try (OutputStream outputStream = new FileOutputStream(output, false)) {
            for (int h = 0; h < 480 & h < height; h++)
            {
                for (int w = 0; w < 800; w++)
                {
                    rgb = img.getRGB(w,  h);

                    red = (rgb >> 16 ) & 0x000000FF;
                    green = (rgb >> 8 ) & 0x000000FF;
                    blue = (rgb) & 0x000000FF;

                    if (red < 126 && green < 126 && blue < 126) {
                        components[position] = true;
                    } else {
                        components[position] = false;
                    }


                    if (position == components.length -1) {
//                        System.out.print(" 0x" + _byte(components[0], components[1], components[2], components[3]) + _byte(components[4], components[5], components[6], components[7]) + ",");

                        String bits = "";
                        for (int i=0;i<8;i++)
                            bits = bits + (components[i]? "1" : "0");

                        byte b = (byte) Integer.parseInt(bits, 2);
                        byte[] byte_array = {b};
                        outputStream.write(byte_array);

                        position = 0;
                    } else {
                        position++;
                    }
                }
  //              System.out.println("");
            }


        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return output;
    }

    public static String _byte(boolean c1, boolean c2, boolean c3, boolean c4) {
        byte result = 0;
        result += c4? 1 : 0;
        result += c3? 2 : 0;
        result += c2? 4 : 0;
        result += c1? 8 : 0;

        return Integer.toHexString(result);
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
        JsonObject config = vertx.getOrCreateContext().config().getJsonObject("CalendarVerticle");
        calendar = config.getString("calendar");
        secretFile = new File(config.getString("secret"));

        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(1), id -> { publishAction(WhiteboardVerticle.Actions.CREATE_DASHBOARD);});

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

        String url = mConfiguration.getString("url") + "/upload";

        final HttpPost httpPost = new HttpPost(url);

        HttpEntity entity = MultipartEntityBuilder
                .create()
                .addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, "dashboard.bin")
                .build();

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
