package es.xan.servantv3.modes

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.Events
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

class NightModeVerticle : AbstractServantVerticle(Constant.NIGHT_MODE_VERTICLE)  {

    companion object {
        val LOG = LoggerFactory.getLogger(NightModeVerticle::class.java.name)

        val ON_OFF_TRANSITION : AgentTransition<AgentInput, AgentState<AgentInput>> =  AgentTransition(
            { _ , input -> Actions.CHANGE_STATUS.equals(input.entity)},
            { _ , input ->
                when (input.entityAs(UpdateState::class.java).newStatus) {
                    "on" -> { StateMachine.ON }
                    else -> { StateMachine.OFF }
                }
            }
        )

    }

    init {
        supportedActions(Actions::class.java)
        supportedEvents(
            es.xan.servantv3.Events.DOOR_STATUS_CHANGED
        )
        registerStateMachine(StateMachine.__INIT__)
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
                        { _ , input -> Events.DOOR_STATUS_CHANGED.equals(input.entity)},
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