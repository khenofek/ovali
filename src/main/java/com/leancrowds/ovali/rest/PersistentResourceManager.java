package com.leancrowds.ovali.rest;

/*
 * Copywrite 2017 Khen Ofek
 * 
 * This file is part of Ovali.

    Ovali is free software: you can redistribute it and/or modify
    it under the terms of the Affero GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Ovali is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Affero GNU General Public License for more details.

    You should have received a copy of the Affero GNU General Public License
    along with Ovali.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.orient.OrientPersistorBatch;
import com.leancrowds.ovali.orient.OrientPersistorJsonBuilder;
import com.leancrowds.ovali.users.UsersManagerService;

import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PersistentResourceManager implements ResourceManagerInterface {
	private static final Logger logger = LoggerFactory.getLogger(PersistentResourceManager.class);
	protected JsonObject config = null;
	protected OvaliServiceManager sm;
	protected String resource;

	public PersistentResourceManager(String resource, OvaliServiceManager sm, JsonObject config) {
		this.config = config;
		this.sm = sm;
		this.resource = resource;
	}

	protected boolean isAuthorized(String restAction, JsonObject context) {
		if (context.getJsonObject("user") == null) return false;
		if (restAction.equals("read") || restAction.equals("query")) return true;
		if (UsersManagerService.isAdmin(context.getJsonObject("user"))) return true;
		return false;
	}
	@Override
	public void isAuthorizedAndValidated(String restAction, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		if (isAuthorized(restAction, context) == true)
			handler.handle(Future.succeededFuture(new JsonObject().put("status", "ok")));
		else handler.handle(Future.succeededFuture(new JsonObject().put("status", "error").put("message", "denied")));
	}

	private void generateCreationDoc(JsonObject doc, JsonObject context) {
		LocalDateTime nowInUtc = LocalDateTime.now(Clock.systemUTC());
		doc.put("created", nowInUtc.format(DateTimeFormatter.ISO_DATE_TIME));
		if (context.getJsonObject("restMap") != null)
			doc.mergeIn(context.getJsonObject("restMap").getJsonObject("parents"));
		if (context.getJsonObject("user") != null)
			if (!UsersManagerService.isAdmin(context.getJsonObject("user")) ||
					UsersManagerService.isAdmin(context.getJsonObject("user")) && doc.getString("users") == null)
				doc.put("users", context.getJsonObject("user").getString("@rid"));
	}	
	public void create(JsonObject doc, JsonObject context, OrientPersistorBatch batch) {
		generateCreationDoc(doc, context);
		batch.create(resource, doc);		
	}
	@Override
	public void create(JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		generateCreationDoc(doc, context);
		sm.getOrientPersistorService().create(resource, doc, handler);
	}
	@Override
	public void read(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		logger.info("read, ID: " + ID + ",context: " + context);
		if (context.getJsonObject("params").getString("expand") != null && context.getJsonObject("params").getString("envelop") != null) {
			Integer expandLevel = Integer.parseInt(context.getJsonObject("params").getString("expand"));
			sm.getOrientPersistorService().cmd("traverse * from " + ID + " while $depth <= " + expandLevel, res -> {
				JsonObject reply = new JsonObject().put("docs", new JsonArray()).put("records", new JsonObject()).put("status", "ok");
				for (int i=0;i<res.result().getJsonArray("docs").size();++i) {
					JsonObject doc = res.result().getJsonArray("docs").getJsonObject(i);
					if (doc.getString("@class").equals(resource))
						reply.getJsonArray("docs").add(doc);
					else reply.getJsonObject("records").put(doc.getString("@rid"), doc);
				}
				handler.handle(Future.succeededFuture(reply));
			});
		} else
			sm.getOrientPersistorService().get(ID, handler);		
	}
	@Override
	public void query(MultiMap query, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		logger.info("query, query: " + query + ",context: " + context);
		if (query == null && context.getJsonObject("restMap").getJsonObject("parents").size() == 0)
			sm.getOrientPersistorService().getall(resource, handler);
		else if (query.get("expand") != null && query.get("envelop") != null) {
			Integer expandLevel = Integer.parseInt(query.get("expand"));
			query.remove("expand");
			JsonObject properties = generateProperties(query, context);
			String orientQuery = OrientPersistorJsonBuilder.buildQuery(resource, properties, false);
			sm.getOrientPersistorService().cmd("traverse * from (" + orientQuery + ") while $depth <= " + expandLevel, res -> {
				JsonObject reply = new JsonObject().put("docs", new JsonArray()).put("records", new JsonObject()).put("status", "ok");
				for (int i=0;i<res.result().getJsonArray("docs").size();++i) {
					JsonObject doc = res.result().getJsonArray("docs").getJsonObject(i);
					if (doc.getString("@class").equals(resource))
						reply.getJsonArray("docs").add(doc);
					else reply.getJsonObject("records").put(doc.getString("@rid"), doc);
				}
				handler.handle(Future.succeededFuture(reply));
			});
		} else {
			JsonObject properties = generateProperties(query, context);
			sm.getOrientPersistorService().find(resource, properties, findRes -> {
				if (query.get("Range") != null) {
					sm.getOrientPersistorService().count(resource, properties, countRes -> {
						JsonObject countObject = countRes.result().getJsonArray("docs").getJsonObject(0);		
						findRes.result().put("rangeHeaders", new JsonObject().											
								put("Content-Range", query.get("Range") + "/" + String.valueOf(countObject.getInteger("count"))).
								put("Accept-Ranges", "items"));
						handler.handle(Future.succeededFuture(findRes.result()));
					});
				} else handler.handle(Future.succeededFuture(findRes.result()));
			});
		}
	}

	private JsonObject generateProperties(MultiMap query, JsonObject context) {
		JsonObject properties = new JsonObject();
		if (query.get("Range") != null) {
			String[] splitRange = query.get("Range").split("-");
			properties.put("offset", splitRange[0]);
			properties.put("limit", String.valueOf(Integer.parseInt(splitRange[1]) - Integer.parseInt(splitRange[0]) + 1));									
		}
		for (Map.Entry<String, String> queryEntry: query.entries()) {
			if (queryEntry.getKey().equals("envelop") || queryEntry.getKey().equals("Range")) continue;
			String property = queryEntry.getKey();
			if (queryEntry.getKey().endsWith("_rel")) 
				properties.put(property.substring(0, property.length()-4),'#' + queryEntry.getValue());
			else properties.put(property, queryEntry.getValue());
		}
		properties.mergeIn(context.getJsonObject("restMap").getJsonObject("parents"));
		return properties;
	}
	@Override
	public void update(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		sm.getOrientPersistorService().update("#" + ID, doc, handler);		
	}
	@Override
	public void override(String ID, JsonObject doc, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		doc.mergeIn(context.getJsonObject("restMap").getJsonObject("parents"));
		sm.getOrientPersistorService().override(ID, doc, handler);
	}
	@Override
	public void delete(String ID, JsonObject context, AsyncResultHandler<JsonObject> handler) {
		sm.getOrientPersistorService().delete(ID, handler);		
	}
}
