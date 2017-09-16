package es.xan.servantv3;

import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.function.Consumer;

public class MessageBuilder {
	
	public static ReplyBuilder createReply() {
		ReplyBuilder builder = new ReplyBuilder();
		builder.setOperation("reply");
		
		return builder;
	}

	public static SaverBuilder createSave() {
		SaverBuilder builder = new SaverBuilder();
		builder.setOperation("save");
		
		return builder;
	}
	
	public static QueryBuilder createQuery() {
		QueryBuilder builder = new QueryBuilder();
		builder.setOperation("query");
		
		return builder;
	}
	
	public static class ReplyBuilder {
		private JsonObject object = new JsonObject();
		
		void setOperation(String operation) {
			object.put("operation", operation);
		}
		
		public void setResult(JsonObject result) {
			object.put("result", result);
		}
		
		public JsonObject build() {
			return object;
		}

		public void setResult(List<JsonObject> result) {
			object.put("resultSize", result.size());
			object.put("result", result);
		}

		public void setId(String result) {
			object.put("result", result);
		}

		public void setError() {
			object.put("status", Constant.REPLY_KO);
		}
		
		public void setOk() {
			object.put("status", Constant.REPLY_OK);
		}

		public void setMessage(String message) {
			object.put("message", message);
		}
		
	}

	public static class SaverBuilder {
		private JsonObject object = new JsonObject();
		
		void setOperation(String operation) {
			object.put("operation", operation);
		}
		
		public JsonObject entity() {
			JsonObject entity = new JsonObject();
			object.put("entity", entity);
			
			return entity;
		}
		
		public JsonObject entity(JsonObject entity) {
			object.put("entity", entity);
			return entity;
		}
		
		public JsonObject build() {
			return object;
		}
	}
	
	public static class QueryBuilder {
		private JsonObject object = new JsonObject();
		
		QueryBuilder() {
			JsonObject sort = new JsonObject();
			object.put("sort", sort);
			
			JsonObject entity = new JsonObject();
			object.put("filter", entity);
			
			JsonObject fields = new JsonObject();
			object.put("fields", fields);
		}
		
		void setOperation(String operation) {
			object.put("operation", operation);
		}
		
		public QueryBuilder filtering(Consumer<JsonObject> lambda) {
			lambda.accept(this.filter());
			return this;
		}
		
		public QueryBuilder sorting(Consumer<JsonObject> lambda) {
			lambda.accept(this.sort());
			return this;
		}
		
		public QueryBuilder fielding(Consumer<JsonObject> lambda) {
			lambda.accept(this.fields());
			return this;
		}
		
		public QueryBuilder limit(int value) {
			object.put("limit", value);
			return this;
		}
		
		public JsonObject filter() {
			return object.getJsonObject("filter");
		}
		
		public JsonObject sort() {
			return object.getJsonObject("sort");
		}
		
		public JsonObject fields() {
			return object.getJsonObject("fields");
		}
		
		public int limit() {
			return object.getInteger("limit");
		}
		
		public JsonObject build() {
			return object;
//			JsonObject result = new JsonObject();
//			
//			result.put("bean", object);
//			
//			return result;
		}
	}

	public static EventBuilder createEvent() {
		return new EventBuilder();
	}
	
	public static class EventBuilder {
		private JsonObject object = new JsonObject();
		
		EventBuilder() {
		}
		
		public void setAction(String action) {
			object.put("action", action);
		}
		
		public void setBean(JsonObject value) {
			object.put("bean", value);
		}
		
		public JsonObject build() {
			return object;
		}

	}

	public static ActionBuilder createAction() {
		return new ActionBuilder();
	}
		
		public static class ActionBuilder {
			private JsonObject object = new JsonObject();
			
			ActionBuilder() {
			}
			
			public void setAction(String action) {
				object.put("action", action);
			}
			
			public void setBean(JsonObject value) {
				object.put("bean", value);
			}
			
			public void setBean(Object obj) {
				setBean(JsonUtils.toJson(obj));
			}
			
			public JsonObject build() {
				return object;
			}

		}
}
