package es.xan.servantv3.messages

import es.xan.servantv3.road.RoadUtils.Window
import io.vertx.core.json.JsonObject

/**
 * Actions Messages
 */

data class ServantEvent(var event: Event, var data : Object)

data class Recording(var time: Integer, var code: String)

data class Recorded(var filepath: String, var code: String)

/**
 * Message used to send a text to a user through gtalk
 */
data class TextMessage(var user: String, var message: String)

data class VideoMessage(var user: String, var message: String, var filepath: String)

/**
 * like textmessage but using the default user defined in the configuration file
 */
data class TextMessageToTheBoss(var message: String)

/**
 * Information required to open a chat
 */
data class OpenChat(var user: String)


/**
 * Sensor information
 */
data  class Sensor(var sensor: String)


/**
 * Updates the state of a component
 */ 
data class UpdateState(var newStatus: String)

/**
 * Configures a dynamic property of a verticle
 */
data class Configure(var field: String, var value: String)

/**
 * DTOs
 */

data class VersionInfo(var filename: String, var tagName: String, var url: String)

data class Event(var name: String, var status: String, var timestamp: Long)

data class Person(var name: String, var inHome: Boolean)
data class Room(var name: String)

enum class DeviceStatus {
	UP,
	UNKNOWN,
	DOWN,
	QUARANTINE,
	NEW,

}

enum class DeviceSecurity {
	SECURE,
	INSECURE,
	UNKNOWN
}

data class Device(var user: String, var mac: String, var status: DeviceStatus, var security: DeviceSecurity, var firstTimestamp : Long, var lastTimestamp: Long)

/**
 * Events Messages
 */
data class NewStatus(var status: String)

data class Power(var power: Float)

data class ParrotMessageReceived(var user: String, var message: String)

data class MqttMsg(val topic : String, val payload: JsonObject)

/**
 * POJOS
 */
data class Temperature(var room: String, var temperature: Float, var timestamp: Long)

data class Cost(var price: Float, var timestamp: Long)


data class Knowledge(var _what: String, var _who: String, var _when: String, var _how: String, var _where: String, var _why: String, var _whose: String)


data class Query(var limit: Int, var sort: Map<String,Any>, var filter: Map<String,Any>, var fields: Map<String,Any>)


data class DGTMessage(var precision: String, var poblacion: String, var descripcion: String, var carretera: String, var codEle: String)
