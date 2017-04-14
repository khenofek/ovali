package com.leancrowds.ovali.orient;

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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import com.leancrowds.ovali.OvaliGenericDelegator;
import com.leancrowds.ovali.OvaliServiceManager;
import com.leancrowds.ovali.orient.OrientPersistorOps;


public class OrientPersistorJsonBuilder extends OvaliGenericDelegator implements OrientPersistorOps {

	public OrientPersistorJsonBuilder(OvaliServiceManager sm, JsonObject config) {
		super(sm, config);
	}

	@Override
	public void get(String id, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "getone");
		persistorMessage.put("rid", "#" + id);
		act(persistorMessage,res);
	}
	@Override
	public void getall(String cls, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "getall");
		persistorMessage.put("class", cls);
		act(persistorMessage, res);
	}	
	@Override
	public void find(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res) {		
		JsonObject persistorMessage = new JsonObject();
		//persistorMessage.put("action", "find").put("class", cls).put("properties", properties);
		persistorMessage.put("action", "sqlCommand").put("command", buildQuery(cls, properties, false));
		act(persistorMessage, res);
	}
	@Override
	public void count(String cls, JsonObject properties, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		//persistorMessage.put("action", "find").put("class", cls).put("properties", properties).put("count", true);
		persistorMessage.put("action", "sqlCommand").put("command", buildQuery(cls, properties, true));
		act(persistorMessage, res);		
	}	
	@Override
	public void update(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "update");
		persistorMessage.put("rid", "#" + id);
		persistorMessage.put("doc", doc);
		act(persistorMessage, res);
	}
	@Override
	public void create(String cls, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "save");
		persistorMessage.put("class", cls);
		persistorMessage.put("doc", doc);
		act(persistorMessage,res);
	}
	@Override
	public void override(String id, JsonObject doc, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "override");
		persistorMessage.put("rid", "#" + id);
		persistorMessage.put("doc", doc);
		act(persistorMessage, res);
	}
	@Override
	public void delete(String id, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "delete");
		persistorMessage.put("rid", "#" + id);
		act(persistorMessage, res);
	}
	@Override
	public void cmd(String command, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "sqlCommand").put("command", command);
		act(persistorMessage, res);
	}
	@Override
	public void nonIndempotentCmd(String command, Handler<AsyncResult<JsonObject>> res) {
		JsonObject persistorMessage = new JsonObject();
		persistorMessage.put("action", "sqlCommand").put("non-idempotent", true).put("command", command);
		act(persistorMessage, res);
	}

	static public String buildQuery(String docClass, JsonObject docProperties, boolean count) {		
		String whereClose = " where ";
		String limit = null;
		String offset = null;
		String facet = null;
		String sort = null;
		String logicalConnection = "";
		String conditionType = " = ";
		String fieldName;
		for (String property: docProperties.fieldNames()) {
			if (property.equals("limit")) {
				limit = docProperties.getString(property);
				continue;
			}
			if (property.equals("offset")) {
				offset = docProperties.getString(property);
				continue;
			}
			if (property.equals("facet")) {
				facet = docProperties.getString(property);
				continue;
			}
			if (property.equals("sort")) {
				sort = docProperties.getString(property).replace("_", " ");
				continue;
			}			
			String propertyName = docProperties.getString(property);
			if (property.endsWith("_ge")) { conditionType = " >= "; fieldName = property.substring(0, property.length()-3); }
			else if (property.endsWith("_gt")) { conditionType = " > "; fieldName = property.substring(0, property.length()-3); }
			else if (property.endsWith("_le")) { conditionType = " <= "; fieldName = property.substring(0, property.length()-3); }
			else if (property.endsWith("_lt")) { conditionType = " < "; fieldName = property.substring(0, property.length()-3); }
			else if (property.endsWith("_ne")) { conditionType = " <> "; fieldName = property.substring(0, property.length()-3); }
			else if (property.endsWith("_lk")) { 
				conditionType = " like "; fieldName = property.substring(0, property.length()-3); 
				propertyName = "%" + docProperties.getString(property) + "%"; 
			} else { conditionType = " = "; fieldName = property; }
			whereClose += logicalConnection + fieldName + conditionType + "'" + propertyName + "'";
			logicalConnection = " and ";
		}
		String query = null;
		if (count) { 
			query = "select count(*) from " + docClass;
			if (!whereClose.equals(" where ")) query += whereClose;
		}
		else {
			if (facet != null) query = "select " + facet + ",count(*) from ";
			else query = "select * from ";
			query += docClass;
			if (!whereClose.equals(" where ")) query += whereClose;
			if (facet != null) query += " group by " + facet;
			if (sort != null) query += " order by " + sort;
			if (offset != null) query += " skip " + offset;
			if (limit != null) query += " limit " + limit;
		}
		return query;
	}
	
}
