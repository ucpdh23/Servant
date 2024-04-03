package es.xan.servantv3.road;

import es.xan.servantv3.AbstractServantVerticle;
import es.xan.servantv3.Constant;
import es.xan.servantv3.Action;
import es.xan.servantv3.messages.TextMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoadVerticle extends AbstractServantVerticle {
    protected RoadVerticle() {
        super(Constant.ROAD_VERTICLE);

        supportedActions(Actions.values());
    }

    Map<String, List<RoadUtils.Window>> monitoring_list = new HashMap<>();

    public enum Actions implements Action {
        START_MONITORING(TextMessage.class),
        CHECK_MONITORING(null),
        STOP_MONITORING(TextMessage.class)
        ;

        private Class<?> mBeanClass;

        private Actions (Class<?> beanClass) {
            this.mBeanClass = beanClass;
        }

        @Override
        public Class<?> getPayloadClass() {
            return this.mBeanClass;
        }

    }

    public void check_monitoring() throws IOException {
        for (String user : this.monitoring_list.keySet()) {
            check_monitoring_user(user);
        }
    }

    private void check_monitoring_user(String user) throws IOException {
        List<RoadUtils.Window> windows = this.monitoring_list.get(user);

        RoadUtils.resolveInfo(windows);

    }

    public void start_monitoring(TextMessage message) throws Exception {
        List<RoadUtils.Window> windows = RoadUtils.composeWindows(message.getMessage());

        String user = message.getUser();
        List<RoadUtils.Window> list = this.monitoring_list.getOrDefault(user, new ArrayList<>());
        list.addAll(windows);
    }

    public void stop_monitoring(TextMessage message) {
        String user = message.getUser();
        this.monitoring_list.remove(user);
    }

}
