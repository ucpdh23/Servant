package es.xan.servantv3.brain.nlp;

import es.xan.servantv3.Action;
import io.vertx.core.eventbus.Message;

import java.util.function.Function;

import es.xan.servantv3.brain.nlp.OperationUtils.Reply;

/**
 * Information provided within a NLM.
 * @author alopez
 *
 */
public class Operation {
	public boolean everyDay = false;
	public long delayInfo = 0;
	public Action action;
	public Object message;
	public boolean forwarding;
	public Function<Message<Object>, Reply> response;
}
