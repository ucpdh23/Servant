package es.xan.servantv3.brain.adk;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.brain.nlp.OperationUtils;
import es.xan.servantv3.brain.nlp.Rules;
import es.xan.servantv3.mcp.SchemaConverter;
import io.modelcontextprotocol.spec.McpSchema;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ServantTool extends BaseTool {

    private final String schema;
    private final AbstractServantVerticle verticle;
    private final Action action;
    private final Rules rule;

    public ServantTool(Rules rules, AbstractServantVerticle verticle) throws Exception {
        super(rules.name(), rules.getHelpMessage(), true);

        this.rule = rules;
        this.action = rules.getAction();
        this.schema = SchemaConverter.convertClassToSchema(rules.getAction().getPayloadClass());
        this.verticle = verticle;
    }

    public Optional<FunctionDeclaration> declaration() {
        Schema.Builder builder = null; //new ParameterBu.builder();

        FunctionDeclaration declaration = FunctionDeclaration.builder()
                .name(rule.name())
                .description(rule.getHelpMessage())
                .parameters(builder)
                .build();

        //FunctionDeclaration item = FunctionDeclaration.fromJson(this.schema);
        return Optional.of(declaration);
    }

    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        @NonNull SingleOnSubscribe<Map<String, Object>> single = new SingleOnSubscribe<Map<String, Object>>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<Map<String, Object>> emitter) throws Throwable {
                final Map<String, Object> to_send = args == null || args.isEmpty()? null : args;
                verticle.publishAction(ServantTool.this.action, to_send, x -> {
                    if (x.succeeded()) {
                        OperationUtils.Reply reply = rule.getResponseProcessor().apply(x.result());
                        @NonNull Map<String, Object> output = new HashMap<>();
                        output.put("response", reply.msg);
                        emitter.onSuccess(output);
                    } else {
                        emitter.onError(x.cause());
                    }
                });

            }
        };
        return Single.create(single);
    }
}
