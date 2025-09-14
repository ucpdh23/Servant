package es.xan.servantv3.productivity;

import es.xan.servantv3.*;
import es.xan.servantv3.messages.HNData;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class ProductivityVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductivityVerticle.class);
    private UUID mScheduledTask;

    public ProductivityVerticle() throws SQLException  {
        super(Constant.PRODUCTIVITY_VERTICLE);

        initialize();

        supportedActions(ProductivityVerticle.Actions.values());
    }

    private void initialize() throws SQLException {
        LOGGER.info("loading known files...");

        try (Statement statement = App.connection.createStatement()) {
            statement.executeUpdate("create table if not exists productivity_hn (url string, name string, comments_counter INTEGER, comments_url string, tags string, date string, times INTEGER)");
        }
    }

    public void start() {
        super.start();

        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(15), id -> { publishAction(Actions.RETRIEVE_HN_INFO); });

        LOGGER.info("Started ProductivityVerticle");
    }

    public enum Actions implements Action {
        RETRIEVE_HN_INFO(null),
        RESOLVE_YESTERDAY_ITEMS(null),
        RESOLVE_TODAY_ITEMS(null),
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

    public void retrieve_hn_info(Message<Object> message) {
        List<HNData> data = ProductivityUtils.resolveData();
        Connection connection = App.connection;

        for (HNData item : data) {
            ProductivityUtils.upsertItem(connection, item);
        }

    }

    public void resolve_today_items(Message<Object> message) {
        Connection connection = App.connection;

        // compute yesterday date
        String date = ProductivityUtils.computeDate(0);

        List<HNData> data = ProductivityUtils.resolveDateData(connection, date);
        List<JsonObject> items = data.stream().map(item -> { return JsonUtils.toJson(item);}).collect(Collectors.toList());

        MessageBuilder.ReplyBuilder builder = MessageBuilder.createReply();
        builder.setOk();
        builder.setResult(items);

        message.reply(builder.build());
    }

    public void resolve_yesterday_items(Message<Object> message) {
        Connection connection = App.connection;

        // compute yesterday date
        String date = ProductivityUtils.computeDate(1);

        List<HNData> data = ProductivityUtils.resolveDateData(connection, date);

        MessageBuilder.ReplyBuilder builder = MessageBuilder.createReply();
        List<JsonObject> items = data.stream().map(item -> { return JsonUtils.toJson(item);}).collect(Collectors.toList());

        builder.setResult(items);

        message.reply(builder.build());
    }
}
