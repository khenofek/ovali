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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OrientPersistorBatch extends OrientPersistorJsonBuilder {
	JsonArray actions = new JsonArray();
	
	public OrientPersistorBatch() {
		super(null,null);
	}
	@Override
	public void act(JsonObject params, Handler<AsyncResult<JsonObject>> res) {
		actions.add(params);
	}
	public JsonObject getJsonParams() {
		return new JsonObject().put("action", "batch").put("actions", actions);
	}
	public JsonArray getActions() { return actions; }
	public void get(String id) {
		get(id, null);
	}
	public void find(String cls, JsonObject properties) {
		find(cls,properties,null);
	}
	public void count(String cls, JsonObject properties) {
		count(cls,properties,null);
	}	
	public void create(String cls, JsonObject doc) {
		create(cls, doc, null);
	}
	public void update(String id, JsonObject doc) {
		update(id,doc,null);
	}
	public void delete(String id) {
		delete(id,null);
	}
	public void cmd(String command) {
		cmd(command, null);		
	}	
	public void nonIndempotentCmd(String command) {
		nonIndempotentCmd(command, null);
	}
}
