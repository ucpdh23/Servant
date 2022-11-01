package es.xan.servantv3.weather;

import es.xan.servantv3.calendar.GCalendarUtils;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherUtils.class);

    static List<HourlyInfo> cache = new ArrayList<>();
    static LocalDate date;

    static CloseableHttpClient httpclient = HttpClients.createDefault();


    public static List<HourlyInfo> resolveHourlyInfo() {
        LocalDateTime time = LocalDateTime.now();

        if (cache.isEmpty()) {
            cache = _updateCache(time);
            date = LocalDate.of(time.getYear(), time.getMonth(), time.getDayOfMonth());
        } else if (time.getHour() > 22 && date.getDayOfWeek() == time.getDayOfWeek()) {
            LocalDateTime tomorrow = time.plus(12, ChronoUnit.HOURS);
            cache = _updateCache(tomorrow);
            date = LocalDate.of(tomorrow.getYear(), tomorrow.getMonth(), tomorrow.getDayOfMonth());
        }

        return cache;
    }

    private static List<HourlyInfo> _updateCache(LocalDateTime dateTime) {
        List<HourlyInfo> output = new ArrayList<>();

        final HttpGet httpGet = new HttpGet("https://www.aemet.es/xml/municipios_h/localidad_h_28079.xml");

        try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
            final HttpEntity responseBody = response.getEntity();

            String content = EntityUtils.toString(responseBody);

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource( new StringReader( content) );
            Document xmlDocument = builder.parse(inputSource);

            XPath xPath = XPathFactory.newInstance().newXPath();
            String temperatures_expression = "/root/prediccion/dia[@fecha='" + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "']/temperatura";
            NodeList nodeList = (NodeList) xPath.compile(temperatures_expression).evaluate(xmlDocument, XPathConstants.NODESET);

            Map<Integer, HourlyInfo> items = new HashMap<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element ele = (Element) nodeList.item(i);
                String periodo = ele.getAttribute("periodo");
                Integer hour = Integer.parseInt(periodo);
                Integer temperature = Integer.parseInt(ele.getTextContent());

                HourlyInfo info = new HourlyInfo();
                info.time = LocalTime.of(hour, 0);
                info.weather = "despejado";
                info.temperature = temperature;
                info.price = 0.4F;

                items.put(hour, info);

                output.add(info);
            }

            String estado_cielo_expression = "/root/prediccion/dia[@fecha='" + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "']/estado_cielo";
            NodeList nodeListCielo = (NodeList) xPath.compile(estado_cielo_expression).evaluate(xmlDocument, XPathConstants.NODESET);

            for (int i = 0; i < nodeListCielo.getLength(); i++) {
                Element ele = (Element) nodeListCielo.item(i);
                String periodo = ele.getAttribute("periodo");
                Integer hour = Integer.parseInt(periodo);

                HourlyInfo info = items.get(hour);
                if (info != null) {
                    info.weather = ele.getAttribute("descripcion");
                    info.weatherId = ele.getTextContent();
                }

            }

        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }

        return output;
    }

    public static void main(String agrs[]) {
        System.out.println(resolveHourlyInfo());
    }

    public static class HourlyInfo {
        public LocalTime time;
        public String weather;
        public String weatherId;
        public Integer temperature;
        public Float price;
    }
}
