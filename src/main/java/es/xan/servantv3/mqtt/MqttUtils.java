package es.xan.servantv3.mqtt;

import com.google.gson.JsonParser;
import io.vertx.core.json.JsonObject;

public class MqttUtils {
    public static final JsonObject resolvePayload(String message) {
        var quotedText = message.split("payload")[1].trim();
        var txt_json = quotedText.substring(1, quotedText.length() - 1);
        var txt_json_escaped = txt_json.replaceAll("\\\\\"","\"");
        return new JsonObject(txt_json_escaped);
    }

    public static final void main(String[] agrs) {
        JsonObject entries = resolvePayload("""
                MQTT publish: topic 'zigbee2mqtt/Puerta', payload '{\\"ac_status\\":false,\\"battery\\":100,\\"battery_defect\\":false,\\"battery_low\\":false,\\"contact\\":true,\\"linkquality\\":144,\\"restore_reports\\":false,\\"supervision_reports\\":false,\\"tamper\\":false,\\"test\\":false,\\"trouble\\":false,\\"update\\":{\\"installed_version\\":16777241,\\"latest_version\\":16777241,\\"state\\":\\"idle\\"}}'
        """);

        System.out.println(entries.getBoolean("contact"));

    }
}
