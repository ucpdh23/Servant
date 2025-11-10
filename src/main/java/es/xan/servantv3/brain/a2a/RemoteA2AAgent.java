package es.xan.servantv3.brain.a2a;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.Callbacks;
import com.google.adk.agents.InvocationContext;
import com.google.adk.events.Event;
import com.google.common.io.Resources;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.genai.types.Content;
import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import io.reactivex.rxjava3.core.Flowable;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent that communicates with a remote A2A agent via A2A client.
 *
 * <p>This agent supports multiple ways to specify the remote agent:
 *
 * <ol>
 *   <li>Direct AgentCard object
 *   <li>URL to agent card JSON
 *   <li>File path to agent card JSON
 * </ol>
 *
 * <p>The agent handles:
 *
 * <ul>
 *   <li>Agent card resolution and validation
 *   <li>A2A message conversion and error handling
 *   <li>Session state management across requests
 * </ul>
 */
public class RemoteA2AAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(RemoteA2AAgent.class);

    private Optional<AgentCard> agentCard;
    private final Optional<String> agentCardSource;
    private Optional<Client> a2aClient;
    private Optional<String> rpcUrl = Optional.empty();
    private String description;
    private boolean isResolved = false;

    public RemoteA2AAgent() {
        // Initialize with empty values - will be configured later
        super("", "", null, null, null);
        this.agentCard = Optional.empty();
        this.agentCardSource = Optional.empty();
        this.a2aClient = Optional.empty();
        this.description = "";
    }

//    private Flowable<Event> messageResponse;
    private final io.reactivex.rxjava3.processors.PublishProcessor<Event> messageResponse =
            io.reactivex.rxjava3.processors.PublishProcessor.create();

    // Internal constructor used by builder
    private RemoteA2AAgent(Builder builder) {
        super(
                builder.name,
                builder.description,
                builder.subAgents,
                builder.beforeAgentCallback,
                builder.afterAgentCallback);

        if (builder.agentCardOrSource == null) {
            throw new IllegalArgumentException("agentCardOrSource cannot be null");
        }

        if (builder.agentCardOrSource instanceof AgentCard) {
            this.agentCard = Optional.of((AgentCard) builder.agentCardOrSource);
            this.agentCardSource = Optional.empty();
            this.description = builder.description;
            // If builder description is empty, use the one from AgentCard
            if (this.description.isEmpty() && this.agentCard.get().description() != null) {
                this.description = this.agentCard.get().description();
            }
        } else if (builder.agentCardOrSource instanceof String) {
            this.agentCard = Optional.empty();
            String source = (String) builder.agentCardOrSource;
            if (source.trim().isEmpty()) {
                throw new IllegalArgumentException("agentCard string cannot be empty");
            }
            this.agentCardSource = Optional.of(source.trim());
        } else {
            throw new TypeError(
                    "agentCard must be AgentCard, URL string, or file path string, got "
                            + builder.agentCardOrSource.getClass());
        }

        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("text"))
                .build();

        //this.a2aClient = builder.a2aClient;
        BiConsumer<ClientEvent, AgentCard> consumer = (event, agentcard) -> {
            if (event instanceof MessageEvent messageEvent) {
                Message responseMessage = messageEvent.getMessage();
                StringBuilder textBuilder = new StringBuilder();
                if (responseMessage.getParts() != null) {
                    for (Part<?> part : responseMessage.getParts()) {
                        if (part instanceof TextPart textPart) {
                            textBuilder.append(textPart.getText());
                        }
                    }
                }

                List<com.google.genai.types.Part> parts = new ArrayList<>();

                com.google.genai.types.Part part = com.google.genai.types.Part.builder()
                        .text(textBuilder.toString())
                        .build();

                parts.add(part);
                Event _event = Event.builder()
                        .content(Content.builder().parts(parts).build())
                        .build();

                //messageResponse.just(_event);
                messageResponse.onNext(_event);
                messageResponse.onComplete();
            } else {
                System.out.println("Received client event: " + event.getClass().getSimpleName());
            }

        };

        // Create a handler that will be used for any errors that occur during streaming
        Consumer<Throwable> errorHandler = error -> {
            logger.warn(error.getMessage(), error);
        };

        this.a2aClient = Optional.of(Client.builder(this.agentCard.get())
                .clientConfig(clientConfig)
                .addConsumer(consumer)
                .streamingErrorHandler(errorHandler)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .build());

    }

    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link RemoteA2AAgent}. */
    public static class Builder {
        private String name;
        private Object agentCardOrSource;
        private String description = "";
        private List<? extends BaseAgent> subAgents;
        private List<Callbacks.BeforeAgentCallback> beforeAgentCallback;
        private List<Callbacks.AfterAgentCallback> afterAgentCallback;

        @CanIgnoreReturnValue
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder agentCardOrSource(Object agentCardOrSource) {
            this.agentCardOrSource = agentCardOrSource;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder subAgents(List<? extends BaseAgent> subAgents) {
            this.subAgents = subAgents;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder beforeAgentCallback(List<Callbacks.BeforeAgentCallback> beforeAgentCallback) {
            this.beforeAgentCallback = beforeAgentCallback;
            return this;
        }

        @CanIgnoreReturnValue
        public Builder afterAgentCallback(List<Callbacks.AfterAgentCallback> afterAgentCallback) {
            this.afterAgentCallback = afterAgentCallback;
            return this;
        }

        public RemoteA2AAgent build() {
            return new RemoteA2AAgent(this);
        }
    }

    public Optional<String> rpcUrl() {
        return rpcUrl;
    }

    private void ensureResolved() {
        // This method is similar to getClientFromAgentCardUrl in the A2A Java SDK. It is called at
        // runtime not constructor time.
        if (isResolved) {
            return;
        }

        try {
            // Resolve agent card if needed
            if (agentCard.isEmpty()) {
                if (agentCardSource.isPresent()) {
                    String source = agentCardSource.get();
                    this.agentCard = Optional.of(resolveAgentCard(source));
                } else {
                    // This case should not happen based on constructor logic
                }
            }

            // Set RPC URL
            this.rpcUrl = Optional.of(this.agentCard.get().url());

            // Update description if empty
            if (this.description == null && this.agentCard.get().description() != null) {
                this.description = this.agentCard.get().description();
            }

            if (this.a2aClient.isEmpty() && this.agentCard.isPresent()) {
                // this.a2aClient = Optional.of(new Client(this.agentCard.get()));
            }
            this.isResolved = true;

        } catch (Exception e) {
            throw new AgentCardResolutionError(
                    "Failed to initialize remote A2A agent " + name() + ": " + e, e);
        }
    }

    private AgentCard resolveAgentCard(String source) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            URL resourceUrl = Resources.getResource(source);
            agentCard = Optional.of(objectMapper.readValue(resourceUrl, AgentCard.class));
            return agentCard.get();
        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Failed to find AgentCard resource: "
                            + source
                            + ". Check if the resource exists and is included in the build.",
                    e);
        } catch (Exception e) {
            throw new IOException("Failed to load AgentCard from resource: " + source, e);
        }
    }

    @Override
    protected Flowable<Event> runAsyncImpl(InvocationContext invocationContext) {
        ensureResolved();

        // Construct A2A Message from the last ADK event
        List<Event> sessionEvents = invocationContext.session().events();

        if (sessionEvents.isEmpty()) {
            logger.warn("No events in session, cannot send message to remote agent.");
            return Flowable.empty();
        }

        Optional<Message> a2aMessageOpt = EventConverter.convertEventsToA2AMessage(invocationContext);

        if (a2aMessageOpt.isEmpty()) {
            logger.warn("Failed to convert event to A2A message.");
            return Flowable.empty();
        }

        Message originalMessage = a2aMessageOpt.get();
        String sessionId = invocationContext.session().id();
        String inboundContextId = originalMessage.getContextId();

        if (!isNullOrEmpty(inboundContextId) && !sessionId.equals(inboundContextId)) {
            logger.warn("Inbound context id differs from active session; using session id instead.");
        }

        Message a2aMessage = new Message.Builder(originalMessage).contextId(sessionId).build();

        a2aClient.get()
                 .sendMessage(a2aMessage);

        return messageResponse;
    }

    @Override
    protected Flowable<Event> runLiveImpl(InvocationContext invocationContext) {
        throw new UnsupportedOperationException(
                "_run_live_impl for " + getClass() + " via A2A is not implemented.");
    }

    private static String extractContextId(SendMessageResponse response) {
        if (response == null) {
            return "";
        }

        EventKind result = response.getResult();
        if (result instanceof Message message) {
            String contextId = nullToEmpty(message.getContextId());
            if (!contextId.isEmpty()) {
                return contextId;
            }
        }
        if (result instanceof Task task) {
            String contextId = nullToEmpty(task.getContextId());
            if (!contextId.isEmpty()) {
                return contextId;
            }
            TaskStatus status = task.getStatus();
            if (status != null && status.message() != null) {
                String statusContext = nullToEmpty(status.message().getContextId());
                if (!statusContext.isEmpty()) {
                    return statusContext;
                }
            }
        }
        return "";
    }

    /** Exception thrown when the agent card cannot be resolved. */
    public static class AgentCardResolutionError extends RuntimeException {
        public AgentCardResolutionError(String message) {
            super(message);
        }

        public AgentCardResolutionError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Exception thrown when the A2A client encounters an error. */
    public static class A2AClientError extends RuntimeException {
        public A2AClientError(String message) {
            super(message);
        }

        public A2AClientError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Exception thrown when a type error occurs. */
    public static class TypeError extends RuntimeException {
        public TypeError(String message) {
            super(message);
        }
    }
}
