package es.xan.servantv3.brain.nlp;

import io.vertx.core.eventbus.Message;

import java.util.function.Function;

import es.xan.servantv3.Action;
import es.xan.servantv3.brain.nlp.TranslationUtils.Reply;

/**
 * Information provided within a NLM.
 * @author alopez
 *
 */
public class Translation {
	public boolean everyDay = false;
	public long delayInfo;
	public Action action;
	public Object message;
	public boolean forwarding;
	public Function<Message<Object>, Reply> response;
}
