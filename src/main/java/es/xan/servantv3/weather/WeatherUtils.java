package es.xan.servantv3.weather;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class WeatherUtils {
    public static List<HourlyInfo> resolveHourlyInfo() {
        List<HourlyInfo> output = new ArrayList<>();

        for (int i=0; i < 24; i++) {
            HourlyInfo info = new HourlyInfo();
            info.time = LocalTime.of(i, 0);
            info.weather = "";
            info.temperature = 18;
            info.price = 0.4F;

            output.add(info);
        }

        return output;
    }

    public static class HourlyInfo {
        public LocalTime time;
        public String weather;
        public Integer temperature;
        public Float price;
    }
}
