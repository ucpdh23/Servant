package es.xan.servantv3.folder;

import com.google.common.io.Files;
import es.xan.servantv3.*;
import es.xan.servantv3.calendar.CalendarVerticle;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.VideoMessage;
import es.xan.servantv3.parrot.ParrotVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FolderVerticle extends AbstractServantVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(FolderVerticle.class);

    private File mFolder;
    private JsonObject mConfiguration;
    private Map<String, File> mTypes;

    private File mCheckFolder;
    private Map<String, String> checkedFiles = new HashMap<>();

    private Map<String, String> mDefaults;

    public FolderVerticle() throws SQLException {
        super(Constant.FOLDER_VERTICLE);

        initialize();

        supportedActions(FolderVerticle.Actions.values());
        supportedEvents(Events.PARROT_FILE_RECEIVED);
    }

    private void initialize() throws SQLException {
        LOGGER.info("loading known files...");

        try (Statement statement = App.connection.createStatement()) {
            statement.executeUpdate("create table if not exists folder_files (filename string, status string)");

            ResultSet rs = statement.executeQuery("select * from folder_files");
            while (rs.next()) {
                String filename = rs.getString("filename");
                String status = rs.getString("status");

                checkedFiles.put(filename, status);
            }
        }
    }

    @Override
    public void start() {
        super.start();
        this.mConfiguration = Vertx.currentContext().config().getJsonObject("FolderVerticle");

        this.mFolder = new File(this.mConfiguration.getString("path"));

        this.mCheckFolder = new File(this.mConfiguration.getString("checker"));

        JsonArray types = this.mConfiguration.getJsonArray("types");
        this.mTypes = this._populateTypes(types.getList(), this.mFolder);

        JsonArray defaults = this.mConfiguration.getJsonArray("default");
        this.mDefaults = this._populateDefaults(defaults.getList());

        vertx.setPeriodic(TimeUnit.SECONDS.toMillis(10), id -> {publishAction(FolderVerticle.Actions.CHECK_NEW_FILES);});

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
        RESOLVE_TYPES(null),
        CHECK_NEW_FILES(null)
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

    public void check_new_files(Message<Object> message) {
        for (File file : mCheckFolder.listFiles()) {
            String filename = file.getName();
            Boolean canWrite = file.canWrite();
            Boolean isFile = file.isFile();

            boolean to_publish = false;
            boolean to_insert = false;
            boolean to_update = false;

            String new_status = canWrite? "CAN_WRITE" : "NOT_EDITABLE";

            if (isFile) {
                if (canWrite) {
                    if (this.checkedFiles.containsKey(filename)) {
                        String status = this.checkedFiles.get(filename);
                        if (!status.equals(new_status)) {
                            to_publish = true;
                            to_update = true;
                            this.checkedFiles.put(filename, new_status);
                        }
                    } else {
                        to_insert = true;
                        to_publish = true;
                        this.checkedFiles.put(filename, new_status);
                    }
                }

                if (to_insert) {
                    try (Statement statement = App.connection.createStatement()) {
                        statement.executeUpdate("INSERT INTO folder_files (filename, status) VALUES ('%s','%s')".formatted(filename, new_status));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                } else if (to_update) {
                    try (Statement statement = App.connection.createStatement()) {
                        statement.executeUpdate("UPDATE folder_files SET status = '%s' WHERE filename = '%s'".formatted(new_status, filename));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (to_publish) {
                    VideoMessage vMsg = new VideoMessage("admin", "checker", file.getAbsolutePath());
                    publishEvent(Events.NEW_FILE_STORED, vMsg);
                }
            }
        }
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
