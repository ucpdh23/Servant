package es.xan.servantv3.road;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.LatLng;
import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.ServantException;
import es.xan.servantv3.ThrowingSupplier;
import es.xan.servantv3.messages.DGTMessage;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.impl.URIDecoder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static es.xan.servantv3.ThrowingUtils.retry3times;
import static es.xan.servantv3.ThrowingUtils.throwingSuplierWrapper;

public class RoadUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadUtils.class);

    private static CloseableHttpClient sHttpClient = null;

    static {
        RoadUtils.sHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(CookieSpecs.STANDARD)
                        .build())
                .build();
    }

    public static List<Window> composeWindows(String message) throws IOException, URISyntaxException {

        String gMapURL = _extractRequest(message);
        LOGGER.info("extracted [{}]", gMapURL);

        if (gMapURL == null)
            return null;

        List<Window> windowList = retry3times(
                () -> {

                    List<Window> output = new ArrayList<>();

                    String trkpt = _resolveGPXfromGoogleMap(gMapURL);
                    Window current = null;
                    for (TrackPoint point : throwingSuplierWrapper(() -> parseXML(trkpt))) {
                        if (current == null) {
                            current = new Window(point.lat, point.lon);
                            output.add(current);
                        }

                        double distance = current.calculateDistance(point);
                        if (distance > 2) {
                            current = new Window(point.lat, point.lon);
                            output.add(current);
                        }
                    }

                    return output;
                },
                (List<Window> output) -> output != null && output.size() > 0
        );

        if (windowList == null) {
            Pair<TrackPoint, TrackPoint> extremes = _resolveSourceAndOrigin(gMapURL);
            List<TrackPoint> trackPoints = graphhopperRoute(extremes.getLeft(), extremes.getRight());

            windowList = new ArrayList<>();

            Window current = null;
            for (TrackPoint point : trackPoints) {
                if (current == null) {
                    current = new Window(point.lat, point.lon);
                    windowList.add(current);
                }

                double distance = current.calculateDistance(point);
                if (distance > 2) {
                    current = new Window(point.lat, point.lon);
                    windowList.add(current);
                }
            }
        }

        return windowList;
    }

    private static String toParameter(TrackPoint point) {
        return Double.toString(point.lat) + "," + Double.toString(point.lon);
    }

    private static List<String> routingOpenstreetmap(TrackPoint from, TrackPoint to) throws URISyntaxException, IOException {
        List<String> output = new ArrayList<>();

        StringBuilder uriPath = new StringBuilder("https://routing.openstreetmap.de/routed-car/route/v1/driving/");
        uriPath.append(toParameter(from));
        uriPath.append(";");
        uriPath.append(toParameter(to));

        URI uri = new URIBuilder(uriPath.toString())
                .addParameter("overview", "false")
                .addParameter("geometries", "polyline")
                .addParameter("steps", "true")
                .build();

        HttpGet request = new HttpGet(uri);

        request.addHeader("Accept", "application/json");
        request.addHeader("Accept-Language", "es-ES,es;q=0.9");

        try (CloseableHttpResponse response = sHttpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Response status code: " + statusCode);

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    JSONObject jsonObject = null;
                    String jsonString = EntityUtils.toString(entity);

                    JSONObject object = new JSONObject(jsonString);
                    JSONObject path = (JSONObject) ((JSONArray) object.get("routes")).get(0);

                    JSONArray steps = path.getJSONArray("steps");

                    for (int i=0;i < steps.length(); i++) {
                        JSONObject step = steps.getJSONObject(i);
                        String name = step.getString("name");
                        if (!Strings.isNullOrEmpty(name)) output.add(name);

                        String ref = step.getString("ref");
                        if (!Strings.isNullOrEmpty(ref)) output.add(ref);
                    }
                }
            }
        }

        output.forEach(it -> LOGGER.info("location: [{}]", it));

        return output;



    }

    private static List<TrackPoint> graphhopperRoute(TrackPoint from, TrackPoint to) throws URISyntaxException, IOException {
        List<TrackPoint> output = new ArrayList<>();

        URI uri = new URIBuilder("https://graphhopper.com/api/1/route")
                .addParameter("vehicle", "car")
                .addParameter("locale", "es")
                .addParameter("key", "LijBPDQGfu7Iiq80w3HzwB4RUDJbMbhs6BU0dEnn")
                .addParameter("elevation", "false")
                .addParameter("instructions", "true")
                .addParameter("turn_costs", "true")
                .addParameter("point", toParameter(from))
                .addParameter("point", toParameter(to))
                .build();

        HttpGet request = new HttpGet(uri);

        request.addHeader("Accept", "application/json");
        request.addHeader("Accept-Language", "es-ES,es;q=0.9");
        request.addHeader("Accept-Encoding", "gzip, deflate, br, zstd");

        try (CloseableHttpResponse response = sHttpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Response status code: " + statusCode);

            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    JSONObject jsonObject = null;
                    String jsonString = EntityUtils.toString(entity);

                    JSONObject object = new JSONObject(jsonString);
                    JSONObject path = (JSONObject) ((JSONArray) object.get("paths")).get(0);
                    String points = path.getString("points");

                    EncodedPolyline polyline = new EncodedPolyline(points);
                    for (LatLng item : polyline.decodePath()) {
                        TrackPoint point = new TrackPoint(item.lat, item.lng);
                        output.add(point);
                    }
                }
            }
        }

        return output;
    }

    private static Pair<TrackPoint, TrackPoint> _resolveSourceAndOrigin(String gMapURL) throws IOException, URISyntaxException {
        HttpGet request = new HttpGet(gMapURL);

        CloseableHttpClient instance = HttpClients.custom().disableRedirectHandling().build();

        Pair<String,String> extremes = null;
        try (CloseableHttpResponse response = instance.execute(request)) {
            // Process the response here
            // For example, you can get the response status code
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Response status code: " + statusCode);

            if (statusCode == 302) {
                Header[] locations = response.getHeaders("location");
                String destination = locations[0].getValue();

                String start = null;
                String end = null;
                boolean processing = false;
                for (String part : destination.split("/")) {
                    if ("dir".equals(part)) {
                        processing = true;
                    } else if (processing) {
                        if (start == null) start = part;
                        else if (end == null) end = part;
                        else break;
                    }
                }

                List<TrackPoint> trackPoints = resolveTrackPonts(Lists.newArrayList(start, end));
                LOGGER.info("track from [{}] to [{}]", trackPoints.get(0), trackPoints.get(1));

                return Pair.of(trackPoints.get(0), trackPoints.get(1));
            } else {
                LOGGER.warn("statusCode", statusCode);
            }


        }

        return null;
    }


    private static List<TrackPoint> resolveTrackPonts(List<String> extremes) throws URISyntaxException, IOException {
        List<TrackPoint> output = new ArrayList<>();
        for (String extreme : extremes) {
            TrackPoint coordinate = resolveCoordinate(extreme);

            if (coordinate == null) {
                extreme = URIDecoder.decodeURIComponent(extreme);
                HttpGet request = new HttpGet("https://nominatim.openstreetmap.org/search");
                URI uri = new URIBuilder(request.getURI())
                        .addParameter("q", extreme)
                        .addParameter("format", "json")
                        .addParameter("viewbox", "-11.634521484375,36.10237644873644,5.614013671875001,43.6599240747891")
                        .build();

                request.setURI(uri);

                try (CloseableHttpResponse response = sHttpClient.execute(request)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    System.out.println("Response status code: " + statusCode);

                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        JSONObject jsonObject = null;
                        String jsonString = EntityUtils.toString(entity);
                        if (jsonString.startsWith("[")) {
                            JSONArray jsonArray = new JSONArray(jsonString);
                            jsonObject = (JSONObject) jsonArray.get(0);

                        } else if (jsonString.startsWith("{")) {
                            jsonObject = new JSONObject(jsonString);
                        }

                        coordinate = new TrackPoint(jsonObject.getDouble("lat"), jsonObject.getDouble("lon"));
                    }
                }
            }

            if (coordinate != null) {
                output.add(coordinate);
            }
        }

        return output;
    }

    private static TrackPoint resolveCoordinate(String extreme) {
        Pattern compile = Pattern.compile("(-?(\\d+(.\\d+)?)),(-?(\\d+(.\\d+)?))");
        Matcher matcher = compile.matcher(extreme);
        if (matcher.matches()) {
            String lat = matcher.group(1);
            String lon = matcher.group(4);

            TrackPoint point = new TrackPoint(Double.parseDouble(lat), Double.parseDouble(lon));

            return point;
        }

        return null;
    }

    public static List<TrackPoint> parseXML(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        List<TrackPoint> trackPoints = new ArrayList<>();

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

    private static String _resolveGPXfromGoogleMap(String gMapURL) throws ServantException {
        // maps.app.goo.gl%2FLtLS3sGiDrtjtGHR6
        String withoutHttps = gMapURL.substring(8);
        final String withoutHttps_2 = withoutHttps.split("\\?")[0];
        String encodedMapURL = throwingSuplierWrapper(() -> URLEncoder.encode(withoutHttps_2, "UTF-8"));

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

    public static List<Pair<DGTMessage, List<Window>>> resolveInfo(List<Window> windows) throws IOException {
        Map<String, DGTMessage> messageIndex = new HashMap<>();
        Map<String, List<Window>> messageWindows = new HashMap<>();

        for (Window item : windows) {
            List<DGTMessage> items = resolveItemsFromWindow(item);
            items.forEach(it -> {
                messageIndex.put(it.getCodEle(), it);
                List<Window> windowList = messageWindows.getOrDefault(it.getCodEle(), new ArrayList<>());
                windowList.add(item);
            });
        }

        List<Pair<DGTMessage, List<Window>>> output = new ArrayList<>();

        for (Map.Entry<String, DGTMessage> entry : messageIndex.entrySet()) {
            output.add(Pair.of(entry.getValue(), messageWindows.getOrDefault(entry.getKey(), new ArrayList<>())));
        }

        return output;
    }

    public static Changes resolveChanges(List<Pair<DGTMessage, List<Window>>> current, List<Pair<DGTMessage, List<Window>>> news) {
        Changes output = new Changes();

        Map<String, Pair<DGTMessage, List<RoadUtils.Window>>> index = news.stream().collect(Collectors.toMap(it -> it.getLeft().getCodEle(), it -> it));

        Set<String> visited = new HashSet<>();
        for (Pair<DGTMessage, List<RoadUtils.Window>> item : current) {
            String code = item.getLeft().getCodEle();

            if (index.containsKey(code)) {
                Pair<DGTMessage, List<RoadUtils.Window>> messageListPair = index.get(code);
                String newMessage = messageListPair.getLeft().getDescripcion();
                String oldMessage = item.getLeft().getDescripcion();
                if (!newMessage.equals(oldMessage)) {
                    output.candidateToUpdate.add(code);
                }
            } else {
                output.candidateToRemove.add(code);
            }

            visited.add(code);
        }

        for (Map.Entry<String, Pair<DGTMessage, List<RoadUtils.Window>>> entry : index.entrySet()) {
            if (!visited.contains(entry.getKey())) {
                output.candidateToAdded.add(entry.getKey());
                output.candidateToNotify.add(entry.getKey());
            }
        }

        return output;
    }

    public static class Changes {
        Set<String> candidateToUpdate = new TreeSet<>();
        Set<String> candidateToNotify = new TreeSet<>();
        Set<String> candidateToRemove = new TreeSet<>();
        Set<String> candidateToAdded = new TreeSet<>();

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

    public static class TrackingInfo {
        List<Window> windows;
        int currentWindowIndex;
        List<TrackPoint> trackPoint;
        List<Pair<DGTMessage, List<Window>>> dgtMessages;
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

            firstLat = centerLat + 0.03; //0.45;
            firstLon = centerLon + 0.03; //0.45;
            lastLat = centerLat - 0.03; //0.45;
            lastLon = centerLon - 0.03; //0.45;
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
