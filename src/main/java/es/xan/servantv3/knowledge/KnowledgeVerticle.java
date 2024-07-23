package es.xan.servantv3.knowledge;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Events;
import es.xan.servantv3.knowledge.utils.parser.Parser;
import es.xan.servantv3.knowledge.utils.parser.Tokenizer;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.VideoMessage;
import es.xan.servantv3.neo4j.Neo4jVerticle;
import es.xan.servantv3.openia.OpenIAVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeVerticle extends AbstractServantVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeVerticle.class);

    public KnowledgeVerticle() {
        super(Constant.KNOWLEDGE_VERTICLE);
        LOGGER.info("Starting knowledge Verticle");
        supportedActions(KnowledgeVerticle.Actions.values());

        supportedEvents(
                Events.NEW_FILE_STORED
        );
    }

    public enum Actions implements Action {
        PROCESS_INPUT(TextMessage.class);

        private Class<?> mBeanClass;

        Actions (Class<?> beanClass) {
            this.mBeanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return this.mBeanClass;
        }
    }

    /**
     * If the file is the type (gas, electric, ) extract the information in order to store into neo4j like a charm :)
     * @param msg
     */
    public void new_file_stored(VideoMessage msg, Message<Object> message) {
        LOGGER.debug("processing new input");

        if ("gas".equals(msg.getMessage())  || "luz".equals(msg.getMessage())) {
            LOGGER.info("processing new input of expected type");

            // get Information from OpenIA
            VideoMessage openiaMessage = new VideoMessage(msg.getUser(), "Can you extract the total_cost, contract_number, the start billing period and the end billing period from this document as a plain JSON string? The attribute names must match the provided names and the date must be provided into the format yyyymmdd, for example 20240203 correspond with february 3rd, 2024. And for the total_cost just provide the number with period separator, please remove the euro char and any white space.", msg.getFilepath());
            publishAction(OpenIAVerticle.Actions.EXTRACT_INFORMATION, openiaMessage, (response) -> {
                JsonObject json = (JsonObject) response.result().body();
                JsonObject result = json.getJsonObject("result");
                String content = result.getJsonArray("choices").getJsonObject(0).getJsonObject("message").getString("content");
                Pattern pattern = Pattern.compile("```json.*(\\{.*\\}).*```", Pattern.MULTILINE | Pattern.DOTALL);
                Matcher matcher = pattern.matcher(content);

                if (matcher.find()) {
                    String jsonOutput = matcher.group(1);
                    LOGGER.info("knowledge [{}]", jsonOutput);
                    JsonObject datasource = new JsonObject(jsonOutput);

                    JsonObject fact = new JsonObject();
                    fact.put("value", buildCost(datasource.getString("total_cost")));
                    fact.put("type", "PAYMENT");
                    fact.put("when", buildPeriod(datasource.getString("start_billing_period"), datasource.getString("end_billing_period")));
                    fact.put("where", "HOME");
                    fact.put("who", "gas".equals(msg.getMessage())? "IBERDROLA" : "NATURGY");
                    fact.put("what",  buildBilling(msg.getMessage().toUpperCase(Locale.ROOT)));

                    publishAction(Neo4jVerticle.Actions.ADD_FACT, fact);
                }

            });
        }
    }

    private JsonObject buildBilling(String name) {
        JsonObject output = new JsonObject();
        output.put("type", "BILLING");
        output.put("name", name);

        return output;
    }

    public JsonObject buildPeriod(String start, String end) {
        JsonObject output = new JsonObject();
        output.put("type", "PERIOD");
        output.put("from", start);
        output.put("to", end);

        return output;
    }

    public JsonObject buildPeriod(String message) {
        JsonObject output = new JsonObject();
        output.put("type", "PERIOD");
        output.put("from", message.split("-")[0].trim());
        output.put("to", message.split("-")[1].trim());

        return output;
    }

    public JsonObject buildCost(String cost) {
        JsonObject output = new JsonObject();
        LOGGER.info("money [{}]", cost);

        Pattern pattern = Pattern.compile("(\\d+(,\\d+)?)");
        Matcher matcher = pattern.matcher(cost);
        if (matcher.find()) {
            String money = matcher.group(1);
            LOGGER.info("money [{}]", money);
            Float number = Float.parseFloat(cost); //money.replace(",", "."));
            output.put("type", "VALUE");
            output.put("value", number);
        }

        return output;
    }

    public void process_input(TextMessage text, Message<Object> msg) {
        Object ast = Parser.parse(new Tokenizer().tokenize(text.getMessage()));

        if (ast instanceof List) {

        } else {

        }
    }

    public final static void main(String[] args) {
        Object ast = Parser.parse(new Tokenizer().tokenize("(FACT: ())"));

    }
}
