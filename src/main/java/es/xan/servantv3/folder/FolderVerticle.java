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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
        RESOLVE_FILE(TextMessage.class),
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

    public void resolve_file(TextMessage textMessage, Message<Object> message) {
        String type = textMessage.getMessage();
        if (type.equals("")) {
            type = this.mDefaults.get(textMessage.getUser());
        }

        if (this.mTypes.containsKey(type)) {
            String filename = this._popFile(this.mTypes.get(type));
            VideoMessage response = new VideoMessage(textMessage.getUser(), "here you are", filename);
            publishAction(ParrotVerticle.Actions.SEND_VIDEO, response);
        }
    }

    public void store_file(VideoMessage toStore, Message<Object> message) {
        String type = toStore.getMessage().toLowerCase();

        if (type.equals(""))
            type = this.mDefaults.get(toStore.getUser());

        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        if (this.mTypes.containsKey(type)) {
            String filename = this._pushFile(this.mTypes.get(type), toStore.getFilepath());

            builder.setOk();
            builder.setMessage("Image stored in " + type + " as " + filename );

            VideoMessage vMsg = new VideoMessage(toStore.getUser(), type, filename);
            publishEvent(Events.NEW_FILE_STORED, vMsg);
        } else {
            builder.setError();
            builder.setMessage("Unsupported Type [" + type + "]");
        }

        message.reply(builder.build());

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
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String _popFile(File targetFolder) {
        // Directory to list files from
        Path dir = Paths.get(targetFolder.getAbsolutePath());

        // Get a sorted list of files by name
        try {
            List<Path> sortedFiles = java.nio.file.Files.list(dir)
                    .filter(java.nio.file.Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());

            Path firstFile = sortedFiles.get(0);
            return firstFile.toFile().getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
