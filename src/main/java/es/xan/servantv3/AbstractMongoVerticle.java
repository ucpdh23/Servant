package es.xan.servantv3;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;

import java.util.Date;
import java.util.function.BiConsumer;

import es.xan.servantv3.MessageBuilder.ReplyBuilder;

/**
 * 
 * @author alopez
 *
 * @param <T>
 */
public abstract class AbstractMongoVerticle<T> extends AbstractServantVerticle {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMongoVerticle.class);
	
	protected MongoClient mongoClient;

	private String mCollection;
	
	private static final long MAX_ELAPSED_TIME = 1000; // ms
	
	abstract protected BiConsumer<T, String> onSaved();
	
	protected AbstractMongoVerticle(String collection, String vertice) {
		super(vertice);
		
		this.mCollection = collection;
	}
	
	static {
		System.setProperty("org.mongodb.async.type", "netty");
	}

	@Override
	public void start() {
		super.start();
		
		JsonObject config = Vertx.currentContext().config().getJsonObject("mongodb");

		String uri = config.getString("uri");
		String db = config.getString("database");

		JsonObject mongoconfig = new JsonObject()
			.put("connection_string", uri)
			.put("db_name", db);

		mongoClient = MongoClient.createShared(vertx, mongoconfig);
		LOGGER.debug("mongoClient " + mongoClient);
	}

	public void save(T item, final Message<Object> msg) {
		boolean toSave = saveFilter(item);
		
		if (!toSave) {
			LOGGER.debug("Discarting item [{}]", item);
			ReplyBuilder builder = MessageBuilder.createReply();
			builder.setError();
			msg.reply(builder.build());
			return;
		}
		LOGGER.debug("saving [{}]", item);
		
		final long init = new Date().getTime();
		mongoClient.save(mCollection, new JsonObject(JsonUtils.toJson(item)), res -> {
			final long elapsed = new Date().getTime() - init;
			if (elapsed > MAX_ELAPSED_TIME) LOGGER.info("Inserting in mongo required [{}] millis", elapsed);
			
			onSaved(item, res);
			if (res.succeeded()) {
				ReplyBuilder builder = MessageBuilder.createReply();
				builder.setId(res.result());
				msg.reply(builder.build());
			} else {
				LOGGER.warn(res.cause().getMessage(), res.cause());
				
				ReplyBuilder builder = MessageBuilder.createReply();
				builder.setError();
				msg.reply(builder.build());
			}
		});
	}
	
	protected boolean saveFilter(T item) {
		return true;
	}

	private void onSaved(T item, AsyncResult<String> res) {
		final BiConsumer<T, String> onSaved = onSaved();
		if (onSaved != null) onSaved.accept(item, res.result());
	}

	public void query(Query query, final Message<Object> msg) {
		final FindOptions options = new FindOptions();
		options.setLimit(query.limit);
		
		final long init = new Date().getTime();
		mongoClient.findWithOptions(mCollection, query.filter != null? new JsonObject(JsonUtils.toJson(query.filter)) : new JsonObject(), options, res -> {
			final long elapsed = new Date().getTime() - init;
			if (elapsed > MAX_ELAPSED_TIME) LOGGER.info("Querying in mongo required [{}] millis", elapsed);

			ReplyBuilder builder = MessageBuilder.createReply();
			builder.setResult(res.result());
			msg.reply(builder.build());
		});
	}
} 