package es.xan.servantv3.scrumleader

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.SSHUtils
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.*
import es.xan.servantv3.mqtt.MqttVerticle
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.util.HashMap
import java.util.concurrent.TimeUnit

class ScrumLeaderVerticle : AbstractServantVerticle(Constant.SCRUMLEAEDER_VERTICLE) {
    companion object {
        val LOG = LoggerFactory.getLogger(ScrumLeaderVerticle::class.java.name)
        val TTL = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
    }

    private var mConfiguration: JsonObject? = null

    init {
        ScrumLeaderVerticle.LOG.info("loading ScrumLeaderVerticle...")
        supportedActions(Actions::class.java)
        ScrumLeaderVerticle.LOG.info("loaded ScrumLeaderVerticle")
    }

    override fun start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("ScrumLeaderVerticle")
    }

    enum class Actions(val clazz : Class<*>? ) : Action {
        /**
         * Updates the status of the passing device
         */
        WELCOME(null),
        REGISTER(Agent::class.java),
        NOP(null),
        EXECUTED(Executed::class.java),
        REFRESH_SOURCE(Agent::class.java)
        ;
    }

    fun refresh_source(agent : Agent) {
        var refresh = this.mConfiguration?.getJsonObject("refresh_command");

        var host =      refresh?.getString("host");
        var username =  refresh?.getString("username");
        var password =  refresh?.getString("password");
        var command =   refresh?.getString("command")

        SSHUtils.runRemoteCommand(host, username, password, command);
    }

    fun executed(msg: Executed) {
        LOG.info("EXECUTED", msg.message)
    }

    fun nop() {
        LOG.info("NOP")

        val payload : JsonObject = JsonObject.of(
            "action", "execute",
            "query", "can you try to improve the current README.md file appending new content?"
        )

        Thread.sleep(4000)
        publishAction(MqttVerticle.Actions.PUBLISH_MSG, MqttMsg("servant/buildgentic/manager", payload))
    }

    fun welcome() {
        LOG.info("registered buildgentic");
        publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("Registered buildgentic"))
    }

    fun register(agent : Agent) {
        LOG.info("registering Agent [{}]", agent.name)
        publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("Registered " + agent.name))
        val payload : JsonObject = JsonObject.of(
            "action", "registration",
            "tools", JsonArray.of(JsonObject.of(
                "tool", "refresh_source",
                "description", "refresh the source folder",
                "argument", "_agent"
            ))
        )

        Thread.sleep(4000)
        publishAction(MqttVerticle.Actions.PUBLISH_MSG, MqttMsg("servant/buildgentic/" + agent.name, payload))
    }

}