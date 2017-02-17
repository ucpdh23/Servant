package es.xan.servantv3;

import io.vertx.core.json.JsonObject;


public class Query {
	public int limit;
	public Object filter = new JsonObject();
}
