package es.xan.servantv3.shoppinglist;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.MessageBuilder;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.whiteboard.WhiteboardVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class ShoppingListVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShoppingListVerticle.class);


    public ShoppingListVerticle() {
        super(Constant.SHOPPINGLIST_VERTICLE);

        supportedActions(ShoppingListVerticle.Actions.values());
    }

    private JsonObject mConfiguration;


    public enum Actions implements Action {
        START_LIST(null),
        CONTINUE_LIST(null),
        REMOVE_FROM_LIST(TextMessage.class),
        SAVE_ITEM(TextMessage.class),
        GET_LIST(null),
        PRINT_LIST(null),
        END_LIST(null)
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

    public void print_list( final Message<Object> msg) {
        File img = createImage();
        publishAction(WhiteboardVerticle.Actions.PRINT_IMAGE, new TextMessage(null, img.getAbsolutePath()));


        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        builderOn.setMessage("Continue list...");
        msg.reply(builderOn.build());
    }

    private File createImage() {
        return null;
    }

    public void continue_list( final Message<Object> msg) {
        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        builderOn.setMessage("Continue list...");
        msg.reply(builderOn.build());
    }

    public void end_list( final Message<Object> msg) {
        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        builderOn.setMessage("Oido Cocina");
        msg.reply(builderOn.build());
    }

    public void remove_from_list(TextMessage message, final Message<Object> msg) throws IOException {
        String text = message.getMessage();
        try {
            int itemToDelete = Integer.parseInt(text);
            ShoppingListUtils.deleteItem(itemToDelete);

            MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
            builderOn.setOk();
            msg.reply(builderOn.build());
        } catch (Exception e) {
            String output = ShoppingListUtils.listToString(true);

            MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
            builderOn.setOk();
            builderOn.setMessage(output);
            msg.reply(builderOn.build());
        }

    }

    public void get_list( final Message<Object> msg) throws IOException {
        String output = ShoppingListUtils.listToString(false);

        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        builderOn.setMessage(output);
        msg.reply(builderOn.build());
    }

    public void start_list( final Message<Object> msg) throws IOException {
        ShoppingListUtils.clearList();

        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        msg.reply(builderOn.build());
    }

    public void save_item(TextMessage message, final Message<Object> msg) {
        String text = message.getMessage();

        try {
            boolean updatedOn = saveText(text);

            if (updatedOn) {
                MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                builderOn.setOk();
                msg.reply(builderOn.build());
            } else {
                MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
                builderOn.setError();
                builderOn.setMessage("Please, try it back in 5 minutes");
                msg.reply(builderOn.build());
            }
        } catch (Exception e) {
            LOGGER.warn("cannot process message [{}]", msg.body(), e);
        }
    }


    @Override
    public void start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("ShoppinglistVerticle");

        LOGGER.info("started shoppingList");
    }

    private boolean saveText(String message) throws IOException {
        LOGGER.info("saving message [{}]", message);

        ShoppingListUtils.addToList(message);

        return true;
    }
}
