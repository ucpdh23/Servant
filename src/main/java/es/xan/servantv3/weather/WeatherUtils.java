package es.xan.servantv3.weather;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
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
    static LocalDateTime date;

    static CloseableHttpClient httpclient = HttpClients.createDefault();


    public static List<HourlyInfo> resolveHourlyInfo() {
        LocalDateTime time = LocalDateTime.now();

        if (cache.isEmpty() || date.getHour() != time.getHour()) {
            cache = _updateCache(time);
            date = time;
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

            composeItems(output, xmlDocument, dateTime.getHour(), dateTime, 24);
            if (output.size() < 24) {
                LocalDateTime tomorrow = dateTime.plus(1, ChronoUnit.DAYS);
                composeItems(output, xmlDocument, 0, tomorrow , 24 - output.size());
            }

            updateEnergyPrice(output, dateTime);

        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }

        return output;
    }

    private static void updateEnergyPrice(List<HourlyInfo> output, LocalDateTime dateTime) {
        // Compute Today's prices
        Map<Integer, Float> todaysPrices = computeTodaysPrices();
        Map<Integer, Float> tomorrowsPrices = computeTomorrowsPrices();

        boolean isToday = true;
        for (HourlyInfo info : output) {
            info.price = isToday?
                    todaysPrices.get(info.time.getHour()) :
                    tomorrowsPrices.get(info.time.getHour());

            if (info.price == null) {
                info.price = 0F;
            }

            if (info.time.getHour() == 23) {
                isToday = false;
            }
        }
    }

    private static Map<Integer, Float> computeTomorrowsPrices() {
        Map<Integer, Float> output = new HashMap<>();

        try {
            org.jsoup.nodes.Document doc = Jsoup.connect("https://tarifaluzhora.es/info/precio-kwh-manana").get();

            Elements priceRows = doc.select("table tbody tr");

            for (org.jsoup.nodes.Element priceRow : priceRows) {
                String line = priceRow.text();
                
                //System.out.println(line);

                Integer hour = Integer.parseInt(line.split(":")[0]);
                //System.out.println("hour" + hour);
                
                //System.out.println(line.split(" ")[4]);
                
                Float price = Float.parseFloat(line.split(" ")[4].replace(',','.').split("€")[0]);
                //System.out.println("price"+price);

                output.put(hour, price);
            }

        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }

        return output;
    }

    private static Map<Integer, Float> computeTodaysPrices() {
        Map<Integer, Float> output = new HashMap<>();

        try {
            org.jsoup.nodes.Document doc = Jsoup.connect("https://tarifaluzhora.es/").get();

            Elements priceRows = doc.select(".col-xs-11");

            for (org.jsoup.nodes.Element priceRow : priceRows) {
                String line = priceRow.text();
                System.out.println(line);
                Integer hour = Integer.parseInt(line.split(":")[0]);
                Float price = Float.parseFloat(line.split(" ")[3]);

                output.put(hour, price);
            }

        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }

        return output;
    }

    private static void composeItems(List<HourlyInfo> output, Document xmlDocument, int firstHour, LocalDateTime dateTime, int counter) throws XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String temperatures_expression = "/root/prediccion/dia[@fecha='" + dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "']/temperatura";
        NodeList nodeList = (NodeList) xPath.compile(temperatures_expression).evaluate(xmlDocument, XPathConstants.NODESET);

        Map<Integer, HourlyInfo> items = new HashMap<>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            if (counter == 0) break;

            Element ele = (Element) nodeList.item(i);
            String periodo = ele.getAttribute("periodo");
            Integer hour = Integer.parseInt(periodo);

            if (hour < firstHour)
                continue;

            Integer temperature = Integer.parseInt(ele.getTextContent());

            HourlyInfo info = new HourlyInfo();
            info.time = LocalTime.of(hour, 0);
            info.weather = "despejado";
            info.temperature = temperature;
            info.price = 0.4F;

            items.put(hour, info);

            counter--;
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

    }

    public static void main(String agrs[]) {
        // updateEnergyPrice(null, LocalDateTime.now());
        System.out.println(computeTodaysPrices());
    }

    public static class HourlyInfo {
        public LocalTime time;
        public String weather;
        public String weatherId;
        public Integer temperature;
        public Float price;
    }
}
