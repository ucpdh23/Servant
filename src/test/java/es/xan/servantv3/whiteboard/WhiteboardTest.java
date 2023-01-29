package es.xan.servantv3.whiteboard;

import es.xan.servantv3.calendar.Notification;
import es.xan.servantv3.weather.WeatherUtils;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.time.LocalTime;
import java.util.*;

@RunWith(VertxUnitRunner.class)
public class WhiteboardTest {

    private Vertx vertx;
    private WhiteboardVerticle verticle;

    @Before
    public void before(TestContext context) {
        this.vertx = Vertx.vertx();
        this.verticle = new WhiteboardVerticle();

        vertx.deployVerticle(verticle, context.asyncAssertSuccess());
    }

    @Test
    public void doStuff(TestContext context) {
        Map<String, Object> data = new HashMap<>();
        data.put("notifications", new ArrayList<Notification>());
        data.put("notificationsToday", new ArrayList<Notification>());
        data.put("notificationsTomorrow", new ArrayList<Notification>());
        data.put("notificationsRestOfWeek", new ArrayList<Notification>());
        data.put("shoppingList", buildShoppingList(30));
        data.put("now", new Date().toString());
        data.put("hourlyInfo", buildWeatherInfo());

        verticle._create_image(data, new File("./output.html"), null);
    }

    private List<String> buildShoppingList(int items) {
        List<String> result = new ArrayList<>();

        for (int i=0; i < items; i++) {
            result.add("New Item for the kitchen " + i);
        }

        return result;
    }

    private List<WeatherUtils.HourlyInfo> buildWeatherInfo() {
        ArrayList<WeatherUtils.HourlyInfo> result = new ArrayList<>();

        for (int i=0; i < 24; i++) {
            WeatherUtils.HourlyInfo item = new WeatherUtils.HourlyInfo();
            item.price = 0.123F;
            item.temperature = 5 + i;
            item.weather = "a";
            item.time = LocalTime.of(i, 0);

            result.add(item);
        }

        return result;
    }


}
