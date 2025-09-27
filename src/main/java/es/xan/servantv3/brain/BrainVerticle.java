package es.xan.servantv3.brain;

import com.google.adk.events.Event;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.adk.models.langchain4j.LangChain4j;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import dev.langchain4j.model.openai.OpenAiChatModel;
import es.xan.servantv3.*;

import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.GoogleSearchTool;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

        JsonObject config = vertx.getOrCreateContext().config().getJsonObject("BrainVerticle");
        String model = config.getString("model");
        String apiKey = config.getString("secret");

        OpenAiChatModel dmrChatModel = OpenAiChatModel.builder()
                .modelName(model)
                .temperature(0D)
                .apiKey(apiKey)
                .build();


        rootAgent = LlmAgent.builder()
                .name("servant_assistant")
                .description("Tu nombre es Servant. Eres un asistente virtual.")
                .model(new LangChain4j(dmrChatModel))
                .instruction("Por favor, ayuda en aquello en lo que se te requiera")
//                .tools(new GoogleSearchTool())
                .build();

        runner = new InMemoryRunner(rootAgent);

        JsonArray users = config.getJsonArray("users");
        for (int i=0; i < users.size(); i++) {
            JsonObject userInfo = users.getJsonObject(i);
            String name = userInfo.getString("email");
            String user_id = UUID.randomUUID().toString();

            Session session = runner
                    .sessionService()
                    .createSession("servant_assistant", name)
                    .blockingGet();


            this.mSessions.put(name, session);
            LOGGER.debug("creating Session [{}-{}]", name, user_id);
        }

        LOGGER.info("Started brainVerticle");

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
