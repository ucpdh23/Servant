package es.xan.servantv3.parrot;


import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Random;

public class AudioUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioUtils.class);

    private static CloseableHttpClient HTTP_CLIENT;

    static {
        HTTP_CLIENT = HttpClients.createDefault();
    }



    public static File downloadAudio(String telegramAPIURL, String botToken, String fileId, Boolean modeDebug) {
        LOGGER.warn(fileId);

        HttpPost reqFilePath = new HttpPost(telegramAPIURL + "/bot" + botToken + "/getFile");

        String jsonString="{\"file_id\":\"" + fileId + "\"}";
        HttpEntity stringEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
        reqFilePath.setEntity(stringEntity);

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(reqFilePath)) {
            LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
            final HttpEntity entity = response.getEntity();

            String content = EntityUtils.toString(entity);
            LOGGER.info(content);
            JsonObject json = new JsonObject(content);
            String filePath = json.getJsonObject("result").getString("file_path");

            String oga_fileName = ""+ fileId + ".oga";
            URL fileToGet = new URL("https://api.telegram.org/file/bot"+ botToken +"/"+ filePath);
            try (ReadableByteChannel rbc = Channels.newChannel(fileToGet.openStream());  FileOutputStream fos = new FileOutputStream(oga_fileName)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String mp3_fileName = modeDebug?
                    "audio.mp3"
                    : transformToMP3(oga_fileName);

            return new File(mp3_fileName);
        } catch (Exception e) {
            LOGGER.warn("Cannot setting boiler to [{}]", e);
        }

        return null;
    }

    public static String transformToMP3(String filename) {
        Random rand = new Random();
        int random = rand.nextInt(10000);
        String outputFile = String.format("%08d", random) + ".mp3";

        try {
            Process p = Runtime.getRuntime().exec("ffmpeg -i " + filename + " " + outputFile);
            p.waitFor();

            new File(filename).delete();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return outputFile;
    }

    public static String transcribe(String witAPIURL, String witAPIKey, File file) {
        HttpPost reqFilePath = new HttpPost(witAPIURL + "/dictation?v=20220622");

        HttpEntity stringEntity = new FileEntity(file);
        reqFilePath.setEntity(stringEntity);

        reqFilePath.addHeader("Authorization", "Bearer " + witAPIKey);
        reqFilePath.addHeader("Content-Type", "audio/mpeg3");

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(reqFilePath)) {
            LOGGER.info("StatusCode: [{}]", response.getStatusLine().getStatusCode());
            final HttpEntity entity = response.getEntity();

            String content = EntityUtils.toString(entity);
            LOGGER.info(content);

            String[] jsons = content.split("\n}");
            String item = jsons[jsons.length - 1];
            System.out.println(item);
            JsonObject json = new JsonObject(item + "}");

            System.out.println(json.getString("text"));

            file.delete();

            String received = json.getString("text");
            if (received.endsWith(".")) {
                return StringUtils.chop(received);
            } else {
                return received;
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot setting boiler to [{}]", e);
        }

        return null;
    }
}
