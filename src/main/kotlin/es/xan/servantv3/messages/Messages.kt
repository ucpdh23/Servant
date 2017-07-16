package es.xan.servantv3.messages

/**
 * Actions Messages
 */

/**
 * Message used to send a text to a user through gtalk
 */
data class TextMessage(var user: String, var message: String)

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


data class Person(var name: String, var inHome: Boolean)
data class Room(var name: String)

/**
 * Events Messages
 */
data class NewStatus(var status: String)

data class ParrotMessageReceived(var user: String, var message: String)


/**
 * POJOS
 */
data class Temperature(var room: String, var temperature: Float, var timestamp: Long)