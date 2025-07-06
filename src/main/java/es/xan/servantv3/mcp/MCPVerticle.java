package es.xan.servantv3.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.brain.nlp.OperationUtils;
import es.xan.servantv3.brain.nlp.Rules;
import es.xan.servantv3.messages.MCPMessage;
import es.xan.servantv3.webservice.WebServerVerticle;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;


public class MCPVerticle extends AbstractServantVerticle implements McpServerTransportProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPVerticle.class);

    private JsonObject mConfiguration;

    private ObjectMapper objectMapper;

    private McpServerSession.Factory sessionFactory;
    private McpServerSession session;
    private McpAsyncServer mAsyncServer;

    public MCPVerticle() {
        super(Constant.MCP_VERTICLE);

        supportedActions(Actions.values());

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void start() {
        LOGGER.debug("starting MCP...");

        super.start();
        this.mConfiguration = Vertx.currentContext().config().getJsonObject("MCPVerticle");

        McpAsyncServer asyncServer = McpServer.async(this)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, false)
                        .tools(true)
                        .prompts(false)
                        .logging()
                        .build())
                .build();

        addTool(asyncServer, Rules.TEMPERATURE);
        addTool(asyncServer, Rules.SHOW_SHOPPING);

        asyncServer.addTool(createDummyTool());

        this.mAsyncServer = asyncServer;
    }

    private McpServerFeatures.AsyncToolSpecification createDummyTool() {

        // Sync tool specification
        var schema = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                    "operation" : {
                            "type" : "string"
                          },
                          "sql" : {
                            "type" : "string"
                          }
              }
            }
            """;
        return new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool("historicalData", "This tool can query the audit logs database in order to recover any historical information from the system. This database contains information about the temperature at home, when the door has been opened and closed and any other information about when the laundry has been started and stopped.", schema),
                (exchange, arguments) -> {
                    LOGGER.debug("historicalData tool invocation {}-{}", exchange, arguments);
                    // Tool implementation
                    return Mono.create(sink -> sink.success(new McpSchema.CallToolResult("The database cannot resolve this query.", false)));
                }
        );
    }


    private void addTool(McpAsyncServer asyncServer, Rules rule) {
        Action action = rule.getAction();
        var schema = _createSchema(action.getPayloadClass());

        var tool = new McpServerFeatures.AsyncToolSpecification(
                new McpSchema.Tool(rule.name(), rule.getHelpMessage(), schema),
                (exchange, arguments) -> {
                    return Mono.create(sink -> {
                        publishAction(action, arguments,  x -> {
                            if (x.succeeded()) {
                                OperationUtils.Reply reply = rule.getResponseProcessor().apply(x.result());
                                sink.success(new McpSchema.CallToolResult(reply.msg, false));
                            } else {
                                sink.error(x.cause());
                            }
                        });
                    });
                }
        );

        asyncServer.addTool(tool);
    }

    private String _createSchema(Class<?> clazz) {
        try {
            return SchemaConverter.convertClassToSchema(clazz);
        } catch (Exception e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
            return "";
        }
    }


    /*
     * Sync implementation
    @Override
    public void start() {
        LOGGER.debug("starting MCP...");

        super.start();
        this.mConfiguration = Vertx.currentContext().config().getJsonObject("MCPVerticle");

        McpSyncServer syncServer = McpServer.sync(this)
                .serverInfo("servant-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, false)     // Enable resource support
                        .tools(true)         // Enable tool support
                        .prompts(false)       // Enable prompt support
                        .logging()           // Enable logging support
                        .build())
                .build();

        // Register tools, resources, and prompts
        McpServerFeatures.SyncToolSpecification syncToolSpecification = toolCreation();
        syncServer.addTool(syncToolSpecification);

        LOGGER.info("started MCP");
    }


    private McpServerFeatures.SyncToolSpecification toolCreation() {

        // Sync tool specification
        var schema = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                    "operation" : {
                            "type" : "string"
                          },
                          "sql" : {
                            "type" : "string"
                          }
              }
            }
            """;
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("historicalData", "This tool can query the audit logs database in order to recover any historical information from the system. This database contains information about the temperature at home, when the door has been opened and closed and any other information about when the laundry has been started and stopped.", schema),
                (exchange, arguments) -> {
                    LOGGER.debug("historicalData tool invocation {}-{}", exchange, arguments);
                    // Tool implementation
                    return new McpSchema.CallToolResult("The database cannot resolve this query.", false);
                }
        );
    }
    */

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        LOGGER.debug("notifyClientes {}-{}", method, params);
        return Mono.empty();
    }

    @Override
    public Mono<Void> closeGracefully() {
        LOGGER.debug("closeGracefully ");
        return Mono.empty();
    }

    public enum Actions implements Action {
        CREATE_SESSION(MCPMessage.class),
        HANDLE_MESSAGE(MCPMessage.class)
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

    public void create_session(MCPMessage message) {
        LOGGER.debug("Creating Session", message.getSessionId());
        this.session = this.sessionFactory.create(new ServantMCPServerTransport());
        LOGGER.debug("Created Session {}", this.session);
    }

    public void handle_message(MCPMessage message) {
        LOGGER.debug("handle_message", message.getMessage());
        try {
            McpSchema.JSONRPCMessage rpcMessage = McpSchema.deserializeJsonRpcMessage(this.objectMapper, message.getMessage().toString());
            this.session.handle(rpcMessage).block();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ServantMCPServerTransport implements McpServerTransport {

        @Override
        public Mono<Void> closeGracefully() {
            LOGGER.debug("closeGracefully ServantMCPServerTransport");

            MCPVerticle.this.publishAction(WebServerVerticle.Actions.CLOSE_SSE);
            return Mono.empty();
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            LOGGER.debug("sendMessage {}", message);
            try {
                String value = objectMapper.writeValueAsString(message);
                MCPVerticle.this.publishAction(WebServerVerticle.Actions.PUBLIC_SSE_EVENT, new MCPMessage("", new JsonObject(value)));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Mono.empty();
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }
    }
}
