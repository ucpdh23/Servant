package es.xan.servantv3.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.JsonUtils;
import es.xan.servantv3.messages.MCPMessage;
import es.xan.servantv3.webservice.WebServerVerticle;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.File;

public class MCPVerticle extends AbstractServantVerticle implements McpServerTransportProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPVerticle.class);

    private JsonObject mConfiguration;

    private ObjectMapper objectMapper;

    private McpServerSession.Factory sessionFactory;
    private McpServerSession session;

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

        McpSyncServer syncServer = McpServer.sync(this)
                .serverInfo("my-server", "1.0.0")
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
                "a" : {
                  "type" : "number"
                },
                "b" : {
                  "type" : "number"
                }
              }
            }
            """;
        return new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("calculator", "Basic calculator", schema),
                (exchange, arguments) -> {
                    // Tool implementation
                    return new McpSchema.CallToolResult("result", false);
                }
        );
    }

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
            this.session.handle(rpcMessage).block();;
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
