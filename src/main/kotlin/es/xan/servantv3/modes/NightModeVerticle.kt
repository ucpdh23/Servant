package es.xan.servantv3.modes

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.Events
import es.xan.servantv3.Scheduler
import es.xan.servantv3.Scheduler.at
import es.xan.servantv3.api.AgentInput
import es.xan.servantv3.api.AgentState
import es.xan.servantv3.api.AgentTransition
import es.xan.servantv3.api.ServantContext
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.lamp.LampVerticle
import es.xan.servantv3.messages.NewStatus
import es.xan.servantv3.messages.TextMessageToTheBoss
import es.xan.servantv3.messages.UpdateState
import org.slf4j.LoggerFactory
import java.time.LocalTime

class NightModeVerticle : AbstractServantVerticle(Constant.NIGHT_MODE_VERTICLE)  {

    private var mScheduler: Scheduler? = null

    companion object {
        val LOG = LoggerFactory.getLogger(NightModeVerticle::class.java.name)

        val ON_OFF_TRANSITION : AgentTransition<AgentInput, AgentState<AgentInput>> =  AgentTransition(
            { _ , input -> Actions.CHANGE_STATUS.equals(input.operation)},
            { _ , input ->
                when (input.entityAs(UpdateState::class.java).newStatus) {
                    "on" -> { StateMachine.ON }
                    else -> { StateMachine.OFF }
                }
            }
        )

    }

    init {
        LOG.info("initializing nightnode...");
        registerStateMachine(StateMachine.__INIT__)

        supportedActions(Actions::class.java)
        supportedEvents(
            es.xan.servantv3.Events.DOOR_STATUS_CHANGED
        )
        LOG.info("initialized nightnode");
    }

    override fun start() {
        LOG.info("starting nightnode...");
        this.mScheduler = Scheduler(this.getVertx())
        this.mScheduler?.scheduleTask(at(LocalTime.of(6,0,0,0))) {
            _ ->
                if (this.agent != null && this.agent.currentState == StateMachine.ON)
                    publishAction(Actions.CHANGE_STATUS, UpdateState("off"))

                true
        }
        LOG.info("started nightnode");
    }

    enum class Actions(val clazz : Class<*>? ) : Action {
        CHANGE_STATUS(UpdateState::class.java),
        ;
    }

    enum class StateMachine : AgentState<AgentInput> {
        __INIT__ {
            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    ON_OFF_TRANSITION
                )
            }
        },
        ON {
            override fun entering(servantContext: ServantContext<AgentInput>) {
                servantContext.publishAction(LampVerticle.Actions.SWITCH_LAMP, UpdateState("off"))
            }

            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    ON_OFF_TRANSITION,
                    AgentTransition(
                        { _ , input -> Events.DOOR_STATUS_CHANGED.equals(input.operation)},
                        { _ , input ->
                            when (input.entityAs(NewStatus::class.java).status) {
                                "true" -> { v.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("door is changed")) }
                            }

                            ON
                        }
                    )
                )
            }
        },
        OFF {
            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    ON_OFF_TRANSITION
                )
            }
        }
    }

}