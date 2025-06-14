/**
 * Package concerns about the human-computer interaction by processing the incoming text and matching against a list of expected commands
 * 
 * This module tries to transform the message into a specific operation. Operations are handled by specific rules that matches words (tokens), expressions and other conditions.
 * An {@link es.xan.servantv3.brain.nlp.Operation} includes an action which defines an interaction inside Servant.
 * 
 * {@link es.xan.servantv3.brain.nlp.OperationFacade} is the entry point to this package. This class tries to get all
 * the information from the natural language message in order by generate a Message for the event bus.
 * 
 *
 * @author alopez
 *
 */
package es.xan.servantv3.brain.nlp;