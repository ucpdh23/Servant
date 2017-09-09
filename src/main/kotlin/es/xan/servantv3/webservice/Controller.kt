package es.xan.servantv3.webservice

import io.vertx.ext.web.Router

/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo.
 * from https://github.com/KotlinID/Kotlin-Vertx-Spring-Example/blob/master/src/main/kotlin/org/jasoet/vertx/controller/Controller.kt
 */


abstract class Controller(val handlers: Router.() -> Unit) {
    abstract val router: Router
    fun create(): Router {
        return router.apply {
            handlers()
        }
    }
}