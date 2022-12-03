package es.xan.servantv3.shoppinglist;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListUtils.class);

    private static final List<String> mItems = new ArrayList<>();


    public static String listToString(boolean withIndex) {
        String output = "";

        for (int i = 0; i < ShoppingListUtils.mItems.size(); i++) {
            output +=   ((withIndex)? Integer.toString(i + 1) + ". " : "")
                        + ShoppingListUtils.mItems.get(i)
                        + ((i < ShoppingListUtils.mItems.size() - 1)? "\n" : "");
        }

        return output;
    }

    public static boolean deleteItem(int item) {
        int index = item - 1;

        if (index < 0 || index >= mItems.size()) return false;

        mItems.remove(index);

        return true;
    }

    public static void clearList() {
        ShoppingListUtils.mItems.clear();
    }

    public static void addToList(String message) {
        ShoppingListUtils.mItems.add(message);
    }

    public static List<String> list() {
        return ShoppingListUtils.mItems;
    }
}
