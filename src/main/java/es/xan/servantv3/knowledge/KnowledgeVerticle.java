package es.xan.servantv3.knowledge;

import es.xan.servantv3.AbstractMongoVerticle;
import es.xan.servantv3.Action;
import es.xan.servantv3.Constant;
import es.xan.servantv3.messages.Knowledge;
import es.xan.servantv3.messages.Query;

import java.util.function.BiConsumer;

public class KnowledgeVerticle extends AbstractMongoVerticle<Knowledge> {

    private static final String COLLECTION = "knowledge";

    protected KnowledgeVerticle() {
        super(COLLECTION, Constant.KNOWLEDGE_VERTICLE);
        supportedActions(KnowledgeVerticle.Actions.values());
    }

    public enum Actions implements Action {
        SAVE(Knowledge.class),
        QUERY(Query.class),
        LAST_VALUES(null);


        private Class<?> mBeanClass;

        private Actions (Class<?> beanClass) {
            this.mBeanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return this.mBeanClass;
        }
    }

    @Override
    protected BiConsumer<Knowledge, String> onSaved() {
        return null;
    }
}
