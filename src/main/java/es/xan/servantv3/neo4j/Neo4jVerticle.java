package es.xan.servantv3.neo4j;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.EagerResult;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Neo4jVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(Neo4jVerticle.class);
    private JsonObject mConfiguration;

    private org.neo4j.driver.Driver driver;

    public Neo4jVerticle() {
        super(Constant.NEO4J_VERTICLE);

        supportedActions(Actions.values());
    }

    public enum Actions implements Action {
        ADD_FACT(JsonObject.class)
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


    public void add_fact(JsonObject fact, final Message<Object> msg) {
        LOGGER.info("Fact to store [{}]", fact);

        String property = fact.getString("type");

        String id = "";
        JsonObject value = fact.getJsonObject("value");
        if ("VALUE".equals(value.getString("type"))) {
            Map map = new HashMap<String, String>();
            map.put("value", value.getFloat("value"));
            map.put("type", value.getString("type"));

            EagerResult result = this.driver.executableQuery("CREATE (o:FACT:" + property + " {value: $value, type: $type}) RETURN o").withParameters(map).execute();

            for (Record rec : result.records()) {
                id = rec.get("o").asNode().elementId();
                break;
            }

        } else if ("MESSAGE".equals(value.getString("type"))) {
            Map map = new HashMap<String, String>();
            map.put("message", value.getString("message"));
            map.put("type", value.getString("type"));

            EagerResult result = this.driver.executableQuery("CREATE (o:FACT:" + property + " {message: $message, type: $type}) RETURN o").withParameters(map).execute();

            for (String key : result.keys()) {
                LOGGER.info(key);
            }
        }

        if (fact.getValue("what") != null) processWhat(fact.getValue("what"), id);
        if (fact.getValue("when") != null) processWhen(fact.getValue("when"), id);
        if (fact.getValue("where") != null) processLink(Question.WHERE, fact.getValue("where"), id);
        if (fact.getValue("who") != null) processLink(Question.WHO, fact.getValue("who"), id);
        if (fact.getValue("why") != null) processWhy(fact.getValue("why"), id);

    }

    private void processWhy(Object obj, String id) {
        if (JsonObject.class.isInstance(obj)) {
            JsonObject why = (JsonObject) obj;

            Map map = new HashMap<String, String>();
            map.put("type", why.getString("type"));
            map.put("name", why.getString("name"));
            map.put("elementId", id);

            EagerResult result = this.driver.executableQuery("MATCH (startNode) WHERE elementId(startNode) = $elementId MERGE (o:" + why.getString("type") + " {name: $name}) MERGE (startNode)-[:WHY]->(o) RETURN startNode, o").withParameters(map).execute();
        }

    }

    private void processWhat(Object obj, String id) {
        if (JsonObject.class.isInstance(obj)) {
            JsonObject what = (JsonObject) obj;

            Map map = new HashMap<String, String>();
            map.put("type", what.getString("type"));
            map.put("name", what.getString("name"));
            map.put("elementId", id);

            EagerResult result = this.driver.executableQuery("MATCH (startNode) WHERE elementId(startNode) = $elementId MERGE (o:" + what.getString("type") + " {name: $name}) MERGE (startNode)-[:WHAT]->(o) RETURN startNode, o").withParameters(map).execute();

            for (String key : result.keys()) {
                LOGGER.info(key);
            }
        }
    }

    enum Question {
        WHO("ENTITY"),
        WHERE("PLACE");

        private final String mDefaultType;

        private Question(String defaultType) {
            this.mDefaultType = defaultType;
        }

        protected String getType() {
            return this.mDefaultType;
        }

    }

    private void processWhen(Object obj, String id) {
        if (JsonObject.class.isInstance(obj)) {
            JsonObject when = (JsonObject) obj;

            if ("PERIOD".equals(when.getString("type"))) {
                Map map = new HashMap<String, String>();
                map.put("from", when.getString("from"));
                map.put("to", when.getString("to"));
                map.put("elementId", id);

                EagerResult result = this.driver.executableQuery("MATCH (startNode) WHERE elementId(startNode) = $elementId MERGE (o:PERIOD {from: $from, to: $to}) MERGE (startNode)-[:WHEN]->(o) RETURN startNode, o").withParameters(map).execute();

                for (String key : result.keys()) {
                    LOGGER.info(key);
                }
            }
        }
    }

    private void processLink(Question question, Object obj, String rootId) {
        if (JsonObject.class.isInstance(obj)) {

        } else if (String.class.isInstance(obj)) {
            String content = (String) obj;

            Map map = new HashMap<String, String>();
            map.put("name", content);
            map.put("elementId", rootId);

            EagerResult result = this.driver.executableQuery("MATCH (startNode) WHERE elementId(startNode) = $elementId MERGE (o:" + question.getType() + " {name: $name}) MERGE (startNode)-[:" + question.name() + "]->(o) RETURN startNode, o").withParameters(map).execute();
        }

    }



    @Override
    public void start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("Neo4jVerticle");

        final String dbUri = this.mConfiguration.getString("uri");
        final String dbUser = this.mConfiguration.getString("username");
        final String dbPassword = this.mConfiguration.getString("password");
        LOGGER.info("[Neo4j: {}-{}-{}]", dbUri, dbUser, dbPassword);

        this.driver = GraphDatabase.driver(dbUri, AuthTokens.basic(dbUser, dbPassword));
        LOGGER.info("Driver [{}]", driver);
        this.driver.verifyConnectivity();

        LOGGER.info("started Neo4j Verticle");
    }

}
