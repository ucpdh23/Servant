package es.xan.servantv3.road;

import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.messages.DGTMessage;
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
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoadUtils {

    private static CloseableHttpClient sHttpClient = null;

    static {
        RoadUtils.sHttpClient = HttpClients.createDefault();
    }

    public static void main(String[] args) throws Exception {
        List<Window> windows = composeWindows("Para obtener la mejor ruta con el tráfico actual, visita https://maps.app.goo.gl/13JBpSpAzysPfkE96");
        for (Window window : windows) {
            resolveItemsFromWindow(window);
        }
    }


    public static List<Window> composeWindows(String message) throws UnsupportedEncodingException {
        List<Window> output = new ArrayList<>();

        String gMapURL = _extractRequest(message);

        if (gMapURL == null)
            return null;

        String trkpt = _resolveGPXfromGoogleMap(gMapURL);
        Window current = null;
        for (TrackPoint point : parseXML(trkpt)) {
            if (current == null) {
                current = new Window(point.lat, point.lon);
                output.add(current);
            }

            double distance = current.calculateDistance(point);
            if (distance > 25) {
                current = new Window(point.lat, point.lon);
                output.add(current);
            }
        }

        return output;
    }

    public static List<TrackPoint> parseXML(String xmlString) {
        List<TrackPoint> trackPoints = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlString));
            Document document = builder.parse(is);

            NodeList trkptNodes = document.getElementsByTagName("trkpt");
            for (int i = 0; i < trkptNodes.getLength(); i++) {
                Element trkptElement = (Element) trkptNodes.item(i);
                double lat = Double.parseDouble(trkptElement.getAttribute("lat"));
                double lon = Double.parseDouble(trkptElement.getAttribute("lon"));
                trackPoints.add(new TrackPoint(lat, lon));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trackPoints;
    }


    public static class TrackPoint {
        private double lat;
        private double lon;


        public TrackPoint(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }


    private static String resolveCurrentDateTimeFormatted() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return currentDateTime.format(formatter);
    }

    private static String _resolveGPXfromGoogleMap(String gMapURL) throws UnsupportedEncodingException {
        // maps.app.goo.gl%2FLtLS3sGiDrtjtGHR6
        String withoutHttps = gMapURL.substring(8);
        String encodedMapURL = URLEncoder.encode(withoutHttps, "UTF-8");

        String strDate = resolveCurrentDateTimeFormatted();

        try {
            HttpGet request = new HttpGet("https://mapstogpx.com/load.php?d=default&lang=en&elev=off&tmode=off&pttype=fixed&o=gpx&cmt=off&desc=off&descasname=off&w=on&dtstr=" + strDate + "&gdata=" + encodedMapURL);
            request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            request.addHeader("Accept-Language", "es-ES,es;q=0.9");
            request.addHeader("Connection", "keep-alive");
            request.addHeader("Referer", "https://mapstogpx.com/");
            request.addHeader("Sec-Fetch-Dest", "document");
            request.addHeader("Sec-Fetch-Mode", "navigate");
            request.addHeader("Sec-Fetch-Site", "same-origin");
            request.addHeader("Sec-Fetch-User", "?1");
            request.addHeader("Upgrade-Insecure-Requests", "1");
            request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
            request.addHeader("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
            request.addHeader("sec-ch-ua-mobile", "?0");
            request.addHeader("sec-ch-ua-platform", "\"Windows\"");

            try (CloseableHttpResponse response = sHttpClient.execute(request)) {
                // Process the response here
                // For example, you can get the response status code
                int statusCode = response.getStatusLine().getStatusCode();
                System.out.println("Response status code: " + statusCode);

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    return EntityUtils.toString(entity);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String _extractRequest(String input) {
        String regex = "\\bhttps://[\\w\\-]+(\\.[\\w\\-]+)+[/\\w\\-\\.@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // Iterate through the matches and print them
        while (matcher.find()) {
            String httpsUrl = matcher.group();
            return httpsUrl;
        }

        return null;
    }

    public static List<DGTMessage> resolveInfo(List<Window> windows) throws IOException {
        Map<String, DGTMessage> output = new HashMap<>();

        for (Window item : windows) {
            List<DGTMessage> items = resolveItemsFromWindow(item);
            items.forEach(it -> output.put(it.getCodEle(), it));
        }

        return new ArrayList<>(output.values());
    }

    private static List<DGTMessage> resolveItemsFromWindow(Window item) throws IOException {
        HttpGet request = new HttpGet("https://infocar.dgt.es/etraffic/BuscarElementos?" +
                "latNS=" + item.firstLat + "&longNS=" + item.firstLon +
                "&latSW=" + item.lastLat + "&longSW=" + item.lastLon +
                "&zoom=6&accion=getElementos&Camaras=true&SensoresTrafico=true&SensoresMeteorologico=true" +
                "&Paneles=true&Radares=true&IncidenciasRETENCION=true&IncidenciasOBRAS=true&IncidenciasMETEOROLOGICA=true&IncidenciasPUERTOS=true" +
                "&IncidenciasOTROS=true&IncidenciasEVENTOS=true&IncidenciasRESTRICCIONES=true&niveles=true&caracter=acontecimiento");

        request.addHeader("accept", "*/*");
        request.addHeader("accept-language", "es-ES,es;q=0.9");

        Map<String, DGTMessage> output = new HashMap<>();

        try (CloseableHttpResponse response = sHttpClient.execute(request)) {
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String responseString = EntityUtils.toString(entity);
                List<DGTMessage> messages = JsonUtils.toListBean(responseString, DGTMessage.class);

                messages.forEach(it -> output.put(it.getCodEle(), it));
            }
        }

        return new ArrayList<>(output.values());
    }

    public static class Window {
        double firstLat;
        double firstLon;

        double lastLat;
        double lastLon;

        double centerLat;
        double centerLon;

        public Window(double lat, double lon) {
            centerLat = lat;
            centerLon = lon;

            firstLat = centerLat + 0.45;
            firstLon = centerLon + 0.45;
            lastLat = centerLat - 0.45;
            lastLon = centerLon - 0.45;
        }

        double calculateDistance(TrackPoint point2) {
            final int R = 6371; // Radius of the Earth in kilometers
            double lat1 = Math.toRadians(centerLat);
            double lon1 = Math.toRadians(centerLon);
            double lat2 = Math.toRadians(point2.lat);
            double lon2 = Math.toRadians(point2.lon);

            double dLat = lat2 - lat1;
            double dLon = lon2 - lon1;

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                            Math.sin(dLon / 2) * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return R * c; // Distance in kilometers
        }
    }
}
