package com.example;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.rest.PersistentResourceManager;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.json.JsonObject;

public class ContactsResourceManager extends PersistentResourceManager {

	public ContactsResourceManager(String resource, OvaliServiceManager sm, JsonObject config) {
		super(resource, sm, config);
	}
	@Override
	protected boolean isAuthorized(String restAction, JsonObject context) {		
		if (restAction.equals("create")) return true;
		else return super.isAuthorized(restAction, context);
	}
	@Override
	public void create(JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		doc.put("Referer", context.getJsonObject("headers").getString("Referer"));
		super.create(doc, context, handler);
	}
}
