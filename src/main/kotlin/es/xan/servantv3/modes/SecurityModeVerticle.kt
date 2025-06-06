package es.xan.servantv3.modes

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.Events
import es.xan.servantv3.api.*
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
            When { _ , input -> Actions.CHANGE_STATUS.equals(input.operation)},
            Then { _ , input ->
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
            Events.DOOR_STATUS_CHANGED,
            Events.OCCUPANCY_CHANGED
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
                        When { _ , input -> Events.DOOR_STATUS_CHANGED.equals(input.operation) && "false" == input.entityAs(NewStatus::class.java).status},
                        Then { _ , _ ->
                            v.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("door is opened"))
                            v.publishAction(HomeVerticle.Actions.RECORD_VIDEO)
                            v.timed(WAITING_VIDEO, 20000)
                        }
                    ),
                    AgentTransition(
                        When { _ , input -> Events.OCCUPANCY_CHANGED.equals(input.operation) && "true" == input.entityAs(NewStatus::class.java).status},
                        Then { _ , input ->
                            v.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("occupancy " + input.entityAs(NewStatus::class.java).status))
                            ON
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
                        When { _ , input -> Events.VIDEO_RECORDED.equals(input.operation)},
                        Then { _ , _ -> ON }
                    ),
                    AgentTransition(
                        When { _ , input -> Events.OCCUPANCY_CHANGED.equals(input.operation) && "true" == input.entityAs(NewStatus::class.java).status},
                        Then { _ , input ->
                            v.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("occupancy " + input.entityAs(NewStatus::class.java).status))
                            WAITING_VIDEO
                        }
                    ),
                )
            }

            override fun timeout(): AgentState<AgentInput> {
                LoggerFactory.getLogger(WAITING_VIDEO::class.java).warn("timeout");

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