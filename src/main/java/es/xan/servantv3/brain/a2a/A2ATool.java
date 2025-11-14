package es.xan.servantv3.brain.a2a;

import com.google.adk.events.Event;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import io.a2a.A2A;
import io.a2a.client.*;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.tnb.model.TaskStatus;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class A2ATool extends BaseTool {

    private static final Logger logger = LoggerFactory.getLogger(A2ATool.class);

    private final AgentCard agentCard;
    private Client a2aClient;
    private A2AConsumer consumer;


    private static final String cleanUpName(String name) {
        return name.replaceAll("\\s+","");
    }

    protected A2ATool(@NotNull AgentCard agentCard) {
        super(cleanUpName(agentCard.name()), agentCard.description(), true);

        this.agentCard = agentCard;

        init();
    }

    protected void init() {
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("text"))
                .build();

        // Create a handler that will be used for any errors that occur during streaming
        Consumer<Throwable> errorHandler = error -> {
            logger.warn(error.getMessage(), error);
        };

        this.consumer = new A2AConsumer();


        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        JSONRPCTransportConfig rpcConfig = new JSONRPCTransportConfig(new JdkA2AHttpClient(httpClient));

        this.a2aClient = Client.builder(this.agentCard)
                .clientConfig(clientConfig)
                .addConsumer(consumer)
                .streamingErrorHandler(errorHandler)
                .withTransport(JSONRPCTransport.class, rpcConfig)
                .build();
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        FunctionDeclaration.Builder builder = FunctionDeclaration.builder();
        builder.name(this.name());
        builder.description(this.description());

        Schema.Builder parameterBuilder = Schema.builder();
        parameterBuilder.title("remoteAction");
        parameterBuilder.description("prompt to request to the remote agent");
        parameterBuilder.type("string");
        builder.parameters(parameterBuilder.build());

        return Optional.of(builder.build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        Single<Map<String, Object>> responseContext = consumer.createResponseContext(toolContext.invocationId());

        Message a2aMessage = new Message.Builder(A2A.toUserMessage((String) args.get("remoteAction"))).contextId(toolContext.invocationId()).build();
        a2aClient.sendMessage(a2aMessage);

        return responseContext;
    }


    private static class A2AConsumer implements BiConsumer<ClientEvent, AgentCard> {
        private final Map<String, SingleEmitter<Map<String, Object>>> emitters = new HashMap<>();


        public Single<Map<String, Object>> createResponseContext(String contextId) {
            return Single.create(emitter -> {
                this.emitters.put(contextId, emitter);

                // Opcional: Permitir la limpieza si el Single es descartado
                emitter.setCancellable(() -> {
                    emitters.remove(contextId);
                    logger.debug("Emitter for context {} cancelled and removed.", contextId);
                });
            });
        }

        @Override
        public void accept(ClientEvent event, AgentCard agentCard) {
            if (event instanceof MessageEvent messageEvent) {
                Message responseMessage = messageEvent.getMessage();
                String contextId = responseMessage.getContextId();

                StringBuilder textBuilder = new StringBuilder();
                if (responseMessage.getParts() != null) {
                    for (Part<?> part : responseMessage.getParts()) {
                        if (part instanceof TextPart textPart) {
                            textBuilder.append(textPart.getText());
                        }
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("response", textBuilder.toString());


                SingleEmitter<Map<String, Object>> mapSingleEmitter = emitters.get(contextId);
                mapSingleEmitter.onSuccess(response);
            } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                io.a2a.spec.TaskStatus status = taskUpdateEvent.getTask().getStatus();
                logger.info("Received updateEvent event [{}-{}]", taskUpdateEvent.getTask().getId(), status);
                TaskState state = status.state();
                if (TaskState.COMPLETED.equals(state)) {
                    logger.info("[{}-{}]", taskUpdateEvent.getTask(), status.message());
                    StringBuilder textBuilder = new StringBuilder();

                    for (var artifact : taskUpdateEvent.getTask().getArtifacts()) {
                        for (var part : artifact.parts()) {
                            if (part instanceof TextPart textPart) {
                                textBuilder.append(textPart.getText());
                            }
                        }
                    }


                    Map<String, Object> response = new HashMap<>();
                    response.put("response", textBuilder.toString());

                    SingleEmitter<Map<String, Object>> mapSingleEmitter = emitters.get(taskUpdateEvent.getTask().getContextId());
                    mapSingleEmitter.onSuccess(response);
                }
            } else {
                logger.info("Received client event [{}-{}]", event.getClass().getSimpleName(), event);
            }
        }
    }


    /**
     *
     * @param baseUrl  ex. "http://192.168.1.2:8008"
     * @param cardPath ex. "/a2a/manager/.well-known/agent-card.json"
     * @return
     */
    public static A2ATool builder(String baseUrl, String cardPath) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        A2ACardResolver resolver = new A2ACardResolver(new JdkA2AHttpClient(httpClient), baseUrl, cardPath);
        AgentCard agentCard = resolver.getAgentCard();
        return new A2ATool(agentCard);
    }


}
