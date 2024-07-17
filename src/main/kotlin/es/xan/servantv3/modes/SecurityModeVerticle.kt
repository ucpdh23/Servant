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

class SecurityModeVerticle : AbstractServantVerticle(Constant.SECURITY_MODE_VERTICLE)  {

    companion object {
        val LOG = LoggerFactory.getLogger(SecurityModeVerticle::class.java.name)

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
        registerStateMachine(StateMachine.__INIT__)

        supportedActions(Actions::class.java)
        supportedEvents(
            es.xan.servantv3.Events.DOOR_STATUS_CHANGED
        )
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
            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    ON_OFF_TRANSITION,
                    AgentTransition(
                        { _ , input -> Events.DOOR_STATUS_CHANGED.equals(input.operation)},
                        { _ , _ ->
                            v.publishAction(HomeVerticle.Actions.RECORD_VIDEO, null)
                            v.timed(WAITING_VIDEO, 20000)
                        }
                    )
                )
            }
        },
        WAITING_VIDEO {
            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    ON_OFF_TRANSITION,
                    AgentTransition(
                        { _ , input -> Events.VIDEO_RECORDED.equals(input.operation)},
                        { _ , _ -> ON }
                    )
                )
            }

            override fun timeout(): AgentState<AgentInput> {
                return ON
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