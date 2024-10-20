package es.xan.servantv3.buildgentic

import com.google.common.cache.CacheBuilder
import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.github.AzureDevOpsVerticle
import es.xan.servantv3.messages.JiraMessage
import es.xan.servantv3.messages.MqttMsg
import es.xan.servantv3.mqtt.MqttVerticle
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ScrumMasterVerticle : AbstractServantVerticle(Constant.SCRUMMASTER_VERTICLE) {
    companion object {
        val LOG = LoggerFactory.getLogger(ScrumMasterVerticle::class.java.name)
    }

    init {
        AzureDevOpsVerticle.LOG.info("loading ScrumMasterVerticle...")
        supportedActions(Actions::class.java)
        AzureDevOpsVerticle.LOG.info("loaded ScrumMasterVerticle")
    }

    private var mConfiguration: JsonObject? = null

    override fun start() {
        super.start();

        this.mConfiguration = Vertx.currentContext().config().getJsonObject("ScrumMasterVerticle")
    }

    enum class Actions(val clazz : Class<*>? ) : Action {
        CHECK_PENDING_WORKITEMS(null),
        SHARE_SERVANT_CAPACITIES(null),
        ;
    }

    fun check_pending_workitems(msg: Message<Any>) {


    }

    fun share_servant_capacities(msg: Message<Any>) {
        val jsonBody = buildJsonObject {
            put("op", "add")
            put("path", "/fields/System.AssignedTo")
        }

        val topic = this.mConfiguration.getString("buildgentic_topic");

        publishAction(MqttVerticle.Actions.PUBLISH_MSG, MqttMsg(topic + "/set", jsonBody));
    }


}