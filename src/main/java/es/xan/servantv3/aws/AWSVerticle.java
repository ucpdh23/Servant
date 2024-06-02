package es.xan.servantv3.aws;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AWSVerticle extends AbstractServantVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AWSVerticle.class);
    private JsonObject mConfiguration;

    public AWSVerticle() {
        super(Constant.AWS_VERTICLE);

        supportedActions(Actions.values());
    }

    public enum Actions implements Action {
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

    @Override
    public void start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("AWSVerticle");

        LOGGER.info("started AWS Verticle");
    }

}
