package es.xan.servantv3.modes

import es.xan.servantv3.*
import es.xan.servantv3.Scheduler.at
import es.xan.servantv3.api.*
import es.xan.servantv3.homeautomation.HomeVerticle
import es.xan.servantv3.lamp.LampVerticle
import es.xan.servantv3.messages.NewStatus
import es.xan.servantv3.messages.TextMessageToTheBoss
import es.xan.servantv3.messages.UpdateState
import org.slf4j.LoggerFactory
import java.time.LocalTime

/**
 @NLPRules(
    [
        NLPRule(
            RuleWhen(
                RuleUtils.isContextFree()
                    .and(RuleUtils.messageStartsWith("modo noche"))),
            RuleThenPublishAction(
                SecurityModeVerticle.Actions.CHANGE_STATUS,
                { _, _ -> UpdateState("off")}
            )
        ),
    ]
)
 */
class NightModeVerticle : AbstractServantVerticle(Constant.NIGHT_MODE_VERTICLE)  {

    private var mScheduler: Scheduler? = null

    companion object {
        val LOG = LoggerFactory.getLogger(NightModeVerticle::class.java.name)

        fun onOffTransaction(nextStep : StateMachine) : AgentTransition<AgentInput, AgentState<AgentInput>> {
            return AgentTransition(
                When { _ , input -> Events.REMOTE_CONTROL.equals(input.operation) },
                Then { _ , input ->
                    when (input.entityAs(UpdateState::class.java).newStatus) {
                        "1_short_release" -> { nextStep }
                        "1_long_release" -> { StateMachine.OFF }
                        else -> { StateMachine.__INIT__ }
                    }
                }
            )
        }

    }

    init {
        LOG.info("initializing nightnode...");
        registerStateMachine(StateMachine.__INIT__)

        supportedActions(Actions::class.java)
        supportedEvents(
            es.xan.servantv3.Events.DOOR_STATUS_CHANGED,
            es.xan.servantv3.Events.REMOTE_CONTROL,
        )
        LOG.info("initialized nightnode");
    }

    override fun start() {
        super.start()

        LOG.info("starting nightnode...");
        this.mScheduler = Scheduler(this.getVertx())
        this.mScheduler?.scheduleTask(at(LocalTime.of(6,0,0,0))) {
            _ ->
                if (currentStateIs(StateMachine.STEP_1, StateMachine.STEP_2))
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
                    onOffTransaction(StateMachine.STEP_1)
                )
            }
        },
        STEP_2 {
            override fun entering(servantContext: ServantContext<AgentInput>) {
                servantContext.publishAction(LampVerticle.Actions.SWITCH_BEDROOM_LAMP, UpdateState("off"))
            }

            override fun trans(v: ServantContext<AgentInput>): Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    onOffTransaction(StateMachine.__INIT__),
                    AgentTransition(
                        When { _ , input ->
                            Events.DOOR_STATUS_CHANGED.equals(input.operation)},
                        Then { _ , input ->
                            when (input.entityAs(NewStatus::class.java).status) {
                                "true" -> { v.publishAction(HomeVerticle.Actions.NOTIFY_BOSS, TextMessageToTheBoss("door is changed")) }
                            }

                            STEP_2
                        }
                    )
                );
            }
        },
        STEP_1 {
            override fun entering(servantContext: ServantContext<AgentInput>) {
                servantContext.publishAction(LampVerticle.Actions.SWITCH_LIVINGROOM_LAMP, UpdateState("off"))
                servantContext.publishAction(LampVerticle.Actions.SWITCH_BEDROOM_LAMP, UpdateState("on"))
            }

            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    onOffTransaction(StateMachine.STEP_2),
                )
            }
        },
        OFF {
            override fun trans(v : ServantContext<AgentInput>) : Array<AgentTransition<AgentInput, AgentState<AgentInput>>> {
                return arrayOf(
                    onOffTransaction(StateMachine.STEP_1)
                )
            }
        }
    }

}