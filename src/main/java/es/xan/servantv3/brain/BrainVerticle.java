package es.xan.servantv3.brain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.events.Event;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.models.langchain4j.LangChain4j;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.BaseTool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import es.xan.servantv3.*;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.mcp.SseServerParameters;
import com.google.adk.tools.mcp.McpToolset;
import es.xan.servantv3.brain.adk.ServantTool;
import es.xan.servantv3.brain.nlp.Rules;
import es.xan.servantv3.messages.ParrotMessageReceived;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.messages.TextMessageToTheBoss;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;

import java.io.File;
import java.util.*;

public class BrainVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrainVerticle.class);


    private LlmAgent rootAgent;
    private InMemoryRunner runner;
    private final Map<String, Session> mSessions;

    public BrainVerticle() {
        super(Constant.BRAIN_VERTICLE);

        supportedActions(Actions.values());

        this.mSessions = new HashMap<>();
    }

    public void start() {
        super.start();

        this.vertx.setTimer(30000, id -> {
            LOGGER.info("Starting agent...");

            JsonObject config = vertx.getOrCreateContext().config().getJsonObject("BrainVerticle");
            String model = config.getString("model");
            String apiKey = config.getString("secret");

            String mcpServerUrl = config.getString("mcpServerUrl");

            OpenAiChatModel dmrChatModel = OpenAiChatModel.builder()
                    .modelName(model)
                    .temperature(0D)
                    .apiKey(apiKey)
                    .build();

            LOGGER.info("loading tools from url [{}]", mcpServerUrl);
            SseServerParameters params = SseServerParameters.builder().url(mcpServerUrl).build();

            McpToolset mcpToolset = new McpToolset(params, new ObjectMapper());
            Flowable<BaseTool> tools = mcpToolset.getTools(null);

            List<BaseTool> allTools = new ArrayList<>();

            tools.subscribeWith(new DisposableSubscriber<BaseTool>() {
                @Override
                public void onNext(BaseTool baseTool) {
                    LOGGER.info("adding Tool [{}]", baseTool);
                    allTools.add(baseTool);
                }

                @Override
                public void onError(Throwable throwable) {
                    LOGGER.warn(throwable.getMessage(), throwable);
                }

                @Override
                public void onComplete() {
                    create_root_agent(dmrChatModel, config, allTools);
                }
            });

            LOGGER.info("Started brainVerticle");
        });

    }

    private void create_root_agent(OpenAiChatModel dmrChatModel, JsonObject config, List<BaseTool> allTools) {
        LOGGER.info("creating root agent [{}-{}]", dmrChatModel, allTools);

        rootAgent = LlmAgent.builder()
                .name("servant_assistant")
                .description("Tu nombre es Servant. Eres un asistente virtual.")
                .model(new LangChain4j(dmrChatModel))
                .instruction("Por favor, ayuda en aquello en lo que se te requiera")
                .tools(allTools)
                .subAgents()
                .build();

        runner = new InMemoryRunner(rootAgent);

        JsonArray users = config.getJsonArray("users");
        for (int i=0; i < users.size(); i++) {
            JsonObject userInfo = users.getJsonObject(i);
            String name = userInfo.getString("email");
            LOGGER.info("creating session for user [{}]", name);

            Session session = runner
                    .sessionService()
                    .createSession("servant_assistant", name)
                    .blockingGet();


            this.mSessions.put(name, session);
            LOGGER.debug("creating Session [{}-{}]", name, session.id());
        }
    }


    public enum Actions implements es.xan.servantv3.Action {
        START_CHATBOT(null),
        END_CHATBOT(null),
        PROCESS_MESSAGE(TextMessage.class);

        Class<?> beanClass;

        Actions(Class<?> beanClass) {
            this.beanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return beanClass;
        }
    }

    public void start_chatbot(Message<Object> msg) {
        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        builder.setOk();
        builder.setMessage("Chatbot reading");

        msg.reply(builder.build());
    }

    public void end_chatbot(Message<Object> msg) {
        MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
        builder.setOk();
        builder.setMessage("Chatbot finalized");

        msg.reply(builder.build());
    }

    public void process_message(TextMessage parrotMessage, Message<Object> msg) {
        String user = parrotMessage.getUser();
        Session session = this.mSessions.get(user);

        Content userMsg = Content.fromParts(Part.fromText(parrotMessage.getMessage()));
        Flowable<Event> eventFlowable = this.runner.runAsync(user, session.id(), userMsg);
        eventFlowable.subscribeWith(new DisposableSubscriber<Event>() {
            @Override
            public void onNext(Event event) {
                LOGGER.info("evento : [{}]", event.stringifyContent());

                MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
                builder.setOk();
                builder.setMessage(event.stringifyContent());

                msg.reply(builder.build());
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.warn(throwable.getMessage(), throwable);

                MessageBuilder.ReplyBuilder builder = new MessageBuilder.ReplyBuilder();
                builder.setError();
                builder.setMessage(throwable.getMessage());

                msg.reply(builder.build());
            }

            @Override
            public void onComplete() {
                LOGGER.info("flowable: completed");
            }
        });
    }


}
