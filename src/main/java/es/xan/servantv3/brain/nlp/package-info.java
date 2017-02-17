/**
 * Package concerns about the human-computer interaction.
 * 
 * This module tries to understand a natural language message (NLM), transforming it into a Translation.
 * 
 * Currently, this conversion process is leaded by a matching process between a set of expected tokens and the incoming text.
 *
 * Translations are related to actions to be performed into the system.
 * 
 * {@link es.xan.servantv3.brain.nlp.TranslationFacade} is the entry point to this package. This class tries to get all
 * the information from the natural language message in order by generate a Message for the event bus.
 * 
 * For this mission, this class uses the rules set defined into the Rules class. Each rule contains information about how to convert a NLM into a message,
 * and whatever other actions to perform once that message is processed by the system.
 * 
 * TODO:
 *  Instead of processing each message isolated from the full conversation, a context could be managed with any useful detail. 
 *  
 * @author alopez
 *
 */
package es.xan.servantv3.brain.nlp;