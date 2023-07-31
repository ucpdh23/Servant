package es.xan.servantv3.folder;

import com.google.common.io.Files;
import es.xan.servantv3.*;
import es.xan.servantv3.messages.Recorded;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.VideoMessage;
import es.xan.servantv3.parrot.ParrotVerticle;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderVerticle extends AbstractServantVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderVerticle.class);

    private File mFolder;
    private JsonObject mConfiguration;
    private Map<String, File> mTypes;

    private Map<String, String> mDefaults;

    public FolderVerticle() {
        super(Constant.FOLDER_VERTICLE);

        supportedActions(FolderVerticle.Actions.values());

        supportedEvents(Events.PARROT_FILE_RECEIVED);
    }

    @Override
    public void start() {
        super.start();
        this.mConfiguration = Vertx.currentContext().config().getJsonObject("FolderVerticle");

        this.mFolder = new File(this.mConfiguration.getString("path"));

        JsonArray types = this.mConfiguration.getJsonArray("types");
        this.mTypes = this._populateTypes(types.getList(), this.mFolder);

        JsonArray defaults = this.mConfiguration.getJsonArray("default");
        this.mDefaults = this._populateDefaults(defaults.getList());


        LOGGER.info("started Folder");
    }

    private Map<String, String> _populateDefaults(List list) {
        HashMap<String, String> output = new HashMap<>();
        for (Object item : list) {
            JsonObject item_object = (JsonObject) item;
            output.put(
                    item_object.getString("email"),
                    item_object.getString("type").toLowerCase());
        }

        return output;
    }

    private Map<String, File> _populateTypes(List list, File folder) {
        HashMap<String, File> output = new HashMap<>();
        for (Object item : list) {
            String type = (String) item;

            File typeFolder = new File(folder, type.toLowerCase());
            typeFolder.mkdir();
            LOGGER.info("managing folder " + typeFolder.getAbsolutePath());

            output.put(type.toLowerCase(), typeFolder);
        }

        return output;
    }

    public enum Actions implements Action {
        STORE_FILE(VideoMessage.class),
        RESOLVE_TYPES(null)
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

    public void parrot_file_received(VideoMessage toStore, Message<Object> message) {
        store_file(toStore, message);
    }

    public void store_file(VideoMessage toStore, Message<Object> message) {
        String type = toStore.getMessage().toLowerCase();

        if (type.equals(""))
            type = this.mDefaults.get(toStore.getUser());

        if (this.mTypes.containsKey(type)) {
            String filename = this._pushFile(this.mTypes.get(type), toStore.getFilepath());
            publishAction(ParrotVerticle.Actions.SEND, new TextMessage(toStore.getUser(), "Image stored in " + type + " as " + filename ));
        } else {
            publishAction(ParrotVerticle.Actions.SEND, new TextMessage(toStore.getUser(), "Unsupported Type [" + type + "]"));
        }

    }

    /*

        File targetFolder = new File(this.mFolder, type.toLowerCase());

        if (!targetFolder.exists()) {
            MessageUtils.temporaryYesOrNoResponse(message,
                "folder " + type.toLowerCase() + " doesn't exists, do you want to create it? (yes or no)",
                yes -> {
                    targetFolder.mkdir();
                    this._pushFile(targetFolder, toStore.getFilepath());
                },
                no -> {});
        } else {

        }
    }
*/
    private String _pushFile(File targetFolder, String filepath) {
        File source = new File(filepath);

        String extension = FilenameUtils.getExtension(filepath);
        String fileName = new SimpleDateFormat("yyyyMMddHHmmSS").format(new Date()) + "." + extension;
        File targetFile = new File(targetFolder, fileName);

        try {
            Files.copy(source, targetFile);
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void load_file(Recorded recorded, Message<Object> message) {

    }



}
