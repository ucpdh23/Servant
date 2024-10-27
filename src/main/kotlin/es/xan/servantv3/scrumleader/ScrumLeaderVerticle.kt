package es.xan.servantv3.scrumleader

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.messages.Agent
import es.xan.servantv3.messages.Device
import es.xan.servantv3.messages.TextMessageToTheBoss
import io.vertx.core.Vertx
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
        REGISTER(Agent::class.java)
        ;
    }

    fun welcome() {
        LOG.info("registered buildgentic");
        publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("Registered buildgentic"))
    }

    fun register(agent : Agent) {
        LOG.info("registering Agent [{}]", agent.name)
    }

}