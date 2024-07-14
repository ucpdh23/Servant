package es.xan.servantv3.security

import es.xan.servantv3.AbstractServantVerticle
import es.xan.servantv3.Action
import es.xan.servantv3.Constant
import es.xan.servantv3.Events
import es.xan.servantv3.api.AgentState
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.api.AgentTransition
import es.xan.servantv3.api.ServantContext
import es.xan.servantv3.messages.NewStatus
import es.xan.servantv3.messages.ServantEvent
import org.slf4j.LoggerFactory

class SecurityVerticle : AbstractServantVerticle(Constant.SECURITY_VERTICLE)  {
    companion object {
        val LOG = LoggerFactory.getLogger(SecurityVerticle::class.java.name)
    }

    init {
        supportedActions(Actions::class.java)

        supportedEvents(
            Events.DOOR_STATUS_CHANGED
        )

        /*super.registerStateMachine(
            DOOR_STATUS_CHANGED::class.java,

            arrayOf(Events.DOOR_STATUS_CHANGED, Events._EVENT_))*/
    }

    enum class Actions(val clazz : Class<*>? ) : Action {
        ;
    }



    enum class DOOR_STATUS_CHANGED : AgentState<ServantEvent> {
        STATE {
            override fun trans(v : ServantContext<ServantEvent>): Array<AgentTransition<ServantEvent, AgentState<ServantEvent>>> {
                return arrayOf(
                    AgentTransition(
                        { _ , _ -> true},
                        { _ , _ ->
                            v.publishAction(HomeVerticle.Actions.RECORD_VIDEO, null)
                            v.timed(WAITING_VIDEO, 5000)
                        }
                    )
                )
            }
        },
        WAITING_VIDEO {
            override fun trans(v: ServantContext<ServantEvent>): Array<AgentTransition<ServantEvent, AgentState<ServantEvent>>> {
                return arrayOf(
                    AgentTransition(
                        {_ , _ -> true},
                        {_ , _ -> STATE}
                    ),
                )
            }

            override fun timeout(): AgentState<ServantEvent> {
                return STATE
            }
        }
    }


}