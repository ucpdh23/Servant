package es.xan.servantv3.shoppinglist;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ShoppingListUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListUtils.class);

    private static final List<String> mItems = new ArrayList<>();


    public static String listToString() {
        return StringUtils.join(ShoppingListUtils.mItems, ",\n");
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
