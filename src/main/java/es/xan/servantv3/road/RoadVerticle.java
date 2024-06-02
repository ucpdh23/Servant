package es.xan.servantv3.road;

import es.xan.servantv3.*;
import es.xan.servantv3.messages.DGTMessage;
import es.xan.servantv3.messages.TextMessage;
import es.xan.servantv3.parrot.ParrotVerticle;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RoadVerticle extends AbstractServantVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadVerticle.class);
    public RoadVerticle() {
        super(Constant.ROAD_VERTICLE);

        supportedActions(Actions.values());
    }

    @Override
    public void start() {
        LOGGER.debug("starting road...");
        super.start();

        vertx.setPeriodic(TimeUnit.MINUTES.toMillis(1), id -> publishAction(Actions.CHECK_MONITORING));

        LOGGER.info("started road");
    }

    Map<String, RoadUtils.TrackingInfo> monitoring_list = new HashMap<>();

    public enum Actions implements Action {
        START_MONITORING(TextMessage.class),
        CHECK_MONITORING(null),
        UPDATING_TRACKING_POINT(TextMessage.class),
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

    public void updating_tracking_point(TextMessage message) {

    }

    public void check_monitoring() throws IOException {
        for (String user : this.monitoring_list.keySet()) {
            check_monitoring_user(user);
        }
    }

    private void check_monitoring_user(String user) throws IOException {
        LOGGER.info("Checking for user [{}]...", user);

        RoadUtils.TrackingInfo trackingInfo = this.monitoring_list.get(user);

        int from = trackingInfo.currentWindowIndex;
        int to = trackingInfo.windows.size();

        List<RoadUtils.Window> subList = trackingInfo.windows.subList(from, to);
        List<Pair<DGTMessage, List<RoadUtils.Window>>> pairs = RoadUtils.resolveInfo(subList);

        RoadUtils.Changes changes = RoadUtils.resolveChanges(trackingInfo.dgtMessages, pairs);

        // Update List
        if (!changes.candidateToRemove.isEmpty()) {
            List<Pair<DGTMessage, List<RoadUtils.Window>>> toDelete = trackingInfo.dgtMessages.stream().filter(it -> changes.candidateToRemove.contains(it.getKey().getCodEle())).collect(Collectors.toList());
            toDelete.forEach(it -> trackingInfo.dgtMessages.remove(it));
        }
        if (!changes.candidateToAdded.isEmpty()) {
            List<Pair<DGTMessage, List<RoadUtils.Window>>> toAdd = pairs.stream().filter(it -> changes.candidateToAdded.contains(it.getKey().getCodEle())).collect(Collectors.toList());
            toAdd.forEach(it -> trackingInfo.dgtMessages.add(it));
        }
        if (!changes.candidateToUpdate.isEmpty()) {
            List<Pair<DGTMessage, List<RoadUtils.Window>>> toUpdate = pairs.stream().filter(it -> changes.candidateToUpdate.contains(it.getKey().getCodEle())).collect(Collectors.toList());
            toUpdate.forEach(it -> {
                Optional<Pair<DGTMessage, List<RoadUtils.Window>>> candidate = trackingInfo.dgtMessages.stream().filter(current -> current.getLeft().getCodEle() == it.getLeft().getCodEle()).findFirst();
                if (candidate.isPresent()) {
                    candidate.get().getKey().setDescripcion(it.getLeft().getDescripcion());
                }
            });
        }

        // Notify
        if (!changes.candidateToNotify.isEmpty() || !changes.candidateToAdded.isEmpty()) {
            StringBuilder body = new StringBuilder();

            StringBuilder auxBuilder = new StringBuilder();
            if (!changes.candidateToNotify.isEmpty()) {
                List<Pair<DGTMessage, List<RoadUtils.Window>>> toNotify = trackingInfo.dgtMessages.stream().filter(it -> changes.candidateToNotify.contains(it.getKey().getCodEle())).collect(Collectors.toList());
                for (Pair<DGTMessage, List<RoadUtils.Window>> item : toNotify) {
                    String plainText= Jsoup.parse(item.getLeft().getDescripcion()).text();
                    auxBuilder.append(plainText).append("\n");
                }

                body.append("Updates:\n");
                if (auxBuilder.length() > 400) {
                    body.append(auxBuilder.subSequence(0, 400));
                    body.append("...");
                } else {
                    body.append(auxBuilder.toString());
                }
            }

            auxBuilder = new StringBuilder();
            if (!changes.candidateToAdded.isEmpty()) {
                List<Pair<DGTMessage, List<RoadUtils.Window>>> toAdd = trackingInfo.dgtMessages.stream().filter(it -> changes.candidateToAdded.contains(it.getKey().getCodEle())).collect(Collectors.toList());
                for (Pair<DGTMessage, List<RoadUtils.Window>> item : toAdd) {
                    String plainText= Jsoup.parse(item.getLeft().getDescripcion()).text();
                    auxBuilder.append(plainText).append("\n");
                }

                body.append("\nMensages Nuevos:\n");
                if (auxBuilder.length() > 400) {
                    body.append(auxBuilder.subSequence(0, 400));
                    body.append("...");
                } else {
                    body.append(auxBuilder.toString());
                }
            }

            TextMessage message = new TextMessage(user, body.toString());
            publishAction(ParrotVerticle.Actions.SEND, message);
        }

    }

    public void start_monitoring(TextMessage message, final Message<Object> msg) throws Exception {
        String user = message.getUser();

        LOGGER.info("start_monitoring_1");
        vertx.executeBlocking((promise) -> {
            try {
                LOGGER.info("Adding monitoring for [{},{}]", message.getUser(), message.getMessage());
                List<RoadUtils.Window> windows = RoadUtils.composeWindows(message.getMessage());

                RoadUtils.TrackingInfo trackingInfo = new RoadUtils.TrackingInfo();
                trackingInfo.currentWindowIndex = 0;
                trackingInfo.trackPoint = new ArrayList<>();
                trackingInfo.windows = windows;
                trackingInfo.dgtMessages = new ArrayList<>();

                this.monitoring_list.put(user, trackingInfo);

                TextMessage response = new TextMessage(user, "Starting monitor " + windows.size() + " windows ...");
                publishAction(ParrotVerticle.Actions.SEND, response);
            } catch (Exception e) {
                throw new ServantException(e);
            }

        });
        LOGGER.info("start_monitoring_2");

        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        builderOn.setOk();
        builderOn.setMessage("Registering monitor...");
        msg.reply(builderOn.build());
    }

    public void stop_monitoring(TextMessage message, final Message<Object> msg) {
        String user = message.getUser();
        Object current = this.monitoring_list.remove(user);

        MessageBuilder.ReplyBuilder builderOn = MessageBuilder.createReply();
        if (current != null) {
            builderOn.setOk();
            builderOn.setMessage("removed track info");
        } else {
            builderOn.setError();
            builderOn.setMessage("cannot remove info");
        }

        msg.reply(builderOn.build());
    }



}
