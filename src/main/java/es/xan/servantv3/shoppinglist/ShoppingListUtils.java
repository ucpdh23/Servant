package es.xan.servantv3.shoppinglist;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ShoppingListUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListUtils.class);

    private static final File container = new File("/opt/documents/shoppingList");

    private static File currentFile;

    static {
        List<String> l_files = new ArrayList<>();

        String[] list = container.list();
        Arrays.sort(list, String::compareTo);

        String last = list[list.length-1];
        currentFile = new File(container, last);
    }


    public static String listToString(boolean withIndex) throws IOException {
        String output = "";

        final List<String> strings = FileUtils.readLines(currentFile, "utf-8");

        for (int i = 0; i < strings.size(); i++) {
            output +=   ((withIndex)? Integer.toString(i + 1) + ". " : "")
                        + strings.get(i)
                        + ((i < strings.size() - 1)? "\n" : "");
        }

        return output;
    }

    public static boolean deleteItem(int item) throws IOException {
        int index = item - 1;

        final List<String> strings = FileUtils.readLines(currentFile, "utf-8");

        if (index < 0 || index >= strings.size()) return false;

        strings.remove(index);

        FileUtils.writeLines(currentFile, strings, false);

        return true;
    }

    public static void clearList() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = new Date();
        String strDate = sdf.format(now);

        currentFile = new File(container, strDate + ".txt");
        FileUtils.writeLines(currentFile, new ArrayList<>(), false);
    }

    public static void addToList(String message) throws IOException {
        final List<String> strings = FileUtils.readLines(currentFile, "utf-8");

        strings.add(message);

        FileUtils.writeLines(currentFile, strings, false);
    }

    public static List<String> list() throws IOException {
        return FileUtils.readLines(currentFile, "utf-8");
    }
}
